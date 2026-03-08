package com.helmx.tutorial.security.security.service;

import com.helmx.tutorial.system.mapper.UserMapper;
import com.helmx.tutorial.system.service.UserService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserPermissionService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(2);

    private final UserService userService;
    private final UserMapper userMapper;
    private final Clock clock;
    private final ConcurrentHashMap<Long, CacheEntry<Boolean>> superAdminCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CacheEntry<Set<String>>> permissionCache = new ConcurrentHashMap<>();

    public UserPermissionService(UserService userService, UserMapper userMapper) {
        this(userService, userMapper, Clock.systemUTC());
    }

    UserPermissionService(UserService userService, UserMapper userMapper, Clock clock) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.clock = clock;
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
        T loaded = loader.load();
        cache.put(userId, new CacheEntry<>(loaded, now + CACHE_TTL.toMillis()));
        return loaded;
    }

    private record CacheEntry<T>(T value, long expiresAt) {
    }

    @FunctionalInterface
    private interface ValueLoader<T> {
        T load();
    }
}
