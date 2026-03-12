package com.helmx.tutorial.security.security.service;

import com.helmx.tutorial.system.mapper.UserMapper;
import com.helmx.tutorial.system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * Shared permission lookup service for authorization checks that would otherwise
 * repeatedly hit {@link UserService#isSuperAdmin(Long)} and
 * {@link UserMapper#selectUserPermissions(Long)}.
 * <p>
 * Results are cached for a short, configurable TTL to reduce bursty repeated
 * lookups across Docker HTTP and WebSocket flows, while keeping permission
 * revocation delay small. The cache is bounded by a configurable max entry
 * count, evicts expired entries first, and then removes the oldest remaining
 * entries when still over capacity. Cache refresh uses 32 striped locks so
 * different user IDs can refresh concurrently without serializing all misses.
 */
@Service
public class UserPermissionService {

    private final UserService userService;
    private final UserMapper userMapper;
    private final Clock clock;
    private final Duration cacheTtl;
    private final int maxEntries;
    private final Object[] cacheLocks;
    private final Object evictionLock = new Object();
    private final ConcurrentHashMap<Long, CacheEntry<Boolean>> superAdminCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CacheEntry<Set<String>>> permissionCache = new ConcurrentHashMap<>();

    @Autowired
    public UserPermissionService(
            UserService userService,
            UserMapper userMapper,
            @Value("${security.permission-cache.ttl-seconds:2}") long ttlSeconds,
            @Value("${security.permission-cache.max-entries:1024}") int maxEntries
    ) {
        this(
                userService,
                userMapper,
                Clock.systemUTC(),
                Duration.ofSeconds(Math.max(1, ttlSeconds)),
                Math.max(1, maxEntries)
        );
    }

    UserPermissionService(UserService userService, UserMapper userMapper, Clock clock) {
        this(userService, userMapper, clock, Duration.ofSeconds(2), 1024);
    }

    UserPermissionService(UserService userService, UserMapper userMapper, Clock clock, Duration cacheTtl, int maxEntries) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.clock = clock;
        this.cacheTtl = cacheTtl;
        this.maxEntries = maxEntries;
        this.cacheLocks = IntStream.range(0, 32)
                .mapToObj(index -> new Object())
                .toArray(Object[]::new);
    }

    public boolean hasPermission(Long userId, String permission) {
        if (permission == null || permission.isBlank()) {
            return false;
        }
        return hasAllPermissions(userId, List.of(permission));
    }

    public boolean hasAllPermissions(Long userId, String... permissions) {
        return hasAllPermissions(userId, permissions == null ? List.of() : Arrays.asList(permissions));
    }

    public boolean hasAllPermissions(Long userId, Collection<String> permissions) {
        // Keep parity with AuthorityConfig.check(...): requiring "all of zero permissions"
        // is treated as allowed.
        if (permissions == null || permissions.isEmpty()) {
            return true;
        }
        if (userId == null) {
            return false;
        }
        if (isSuperAdmin(userId)) {
            return true;
        }
        return getPermissions(userId).containsAll(permissions);
    }

    public boolean hasAnyPermission(Long userId, String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        if (userId == null) {
            return false;
        }
        if (isSuperAdmin(userId)) {
            return true;
        }
        Set<String> userPermissions = getPermissions(userId);
        return Arrays.stream(permissions).anyMatch(userPermissions::contains);
    }

    private boolean isSuperAdmin(Long userId) {
        return getCachedValue(superAdminCache, userId, () -> userService.isSuperAdmin(userId));
    }

    private Set<String> getPermissions(Long userId) {
        return getCachedValue(permissionCache, userId, () -> {
            Set<String> permissions = userMapper.selectUserPermissions(userId);
            return permissions == null ? Collections.emptySet() : Set.copyOf(permissions);
        });
    }

    private <T> T getCachedValue(ConcurrentHashMap<Long, CacheEntry<T>> cache, Long userId, ValueLoader<T> loader) {
        long now = clock.millis();
        CacheEntry<T> cached = cache.get(userId);
        if (cached != null && cached.expiresAt() > now) {
            return cached.value();
        }
        synchronized (lockFor(userId)) {
            long refreshedNow = clock.millis();
            CacheEntry<T> refreshed = cache.get(userId);
            if (refreshed != null && refreshed.expiresAt() > refreshedNow) {
                return refreshed.value();
            }
            T loaded = loader.load();
            cacheValue(cache, userId, loaded, refreshedNow);
            return loaded;
        }
    }

    private <T> void cacheValue(ConcurrentHashMap<Long, CacheEntry<T>> cache, Long userId, T loaded, long now) {
        synchronized (evictionLock) {
            evictIfNeededInternal(cache, now);
            cache.put(userId, new CacheEntry<>(loaded, now + cacheTtl.toMillis()));
        }
    }

    private <T> void evictIfNeeded(ConcurrentHashMap<Long, CacheEntry<T>> cache, long now) {
        synchronized (evictionLock) {
            evictIfNeededInternal(cache, now);
        }
    }

    private <T> void evictIfNeededInternal(ConcurrentHashMap<Long, CacheEntry<T>> cache, long now) {
        cache.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        int overflow = cache.size() - maxEntries + 1;
        if (overflow <= 0) {
            return;
        }
        List<Map.Entry<Long, CacheEntry<T>>> oldestEntries = cache.entrySet().stream()
                .sorted(Comparator.comparingLong(entry -> entry.getValue().expiresAt()))
                .limit(overflow)
                .toList();
        oldestEntries.forEach(entry -> cache.remove(entry.getKey(), entry.getValue()));
    }

    private Object lockFor(Long userId) {
        return cacheLocks[Math.floorMod(userId.hashCode(), cacheLocks.length)];
    }

    private record CacheEntry<T>(T value, long expiresAt) {
    }

    @FunctionalInterface
    private interface ValueLoader<T> {
        T load();
    }
}
