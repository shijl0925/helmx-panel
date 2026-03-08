package com.helmx.tutorial.security.security.service;

import com.helmx.tutorial.system.mapper.UserMapper;
import com.helmx.tutorial.system.service.UserService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class UserPermissionServiceTest {

    @Test
    void hasAllPermissions_reusesCachedPermissionQueriesWithinTtl() {
        UserService userService = mock(UserService.class);
        UserMapper userMapper = mock(UserMapper.class);
        MutableClock clock = new MutableClock(Instant.parse("2026-03-08T00:00:00Z"));
        UserPermissionService service = new UserPermissionService(userService, userMapper, clock);

        when(userService.isSuperAdmin(1L)).thenReturn(false);
        when(userMapper.selectUserPermissions(1L)).thenReturn(Set.of("Ops:Container:Logs", "Ops:Container:Exec"));

        assertTrue(service.hasAllPermissions(1L, "Ops:Container:Logs"));
        assertTrue(service.hasAllPermissions(1L, "Ops:Container:Exec"));

        verify(userService, times(1)).isSuperAdmin(1L);
        verify(userMapper, times(1)).selectUserPermissions(1L);
    }

    @Test
    void hasAnyPermission_refreshesCacheAfterTtlExpires() {
        UserService userService = mock(UserService.class);
        UserMapper userMapper = mock(UserMapper.class);
        MutableClock clock = new MutableClock(Instant.parse("2026-03-08T00:00:00Z"));
        UserPermissionService service = new UserPermissionService(userService, userMapper, clock);

        when(userService.isSuperAdmin(2L)).thenReturn(false);
        when(userMapper.selectUserPermissions(2L))
                .thenReturn(Set.of("Ops:Events:List"))
                .thenReturn(Set.of("Ops:Container:List"));

        assertTrue(service.hasAnyPermission(2L, "Ops:Events:List"));

        clock.advanceSeconds(3);

        assertTrue(service.hasAnyPermission(2L, "Ops:Container:List"));

        verify(userService, times(2)).isSuperAdmin(2L);
        verify(userMapper, times(2)).selectUserPermissions(2L);
    }

    @Test
    void hasPermission_shortCircuitsForSuperAdmin() {
        UserService userService = mock(UserService.class);
        UserMapper userMapper = mock(UserMapper.class);
        UserPermissionService service = new UserPermissionService(userService, userMapper);

        when(userService.isSuperAdmin(3L)).thenReturn(true);

        assertTrue(service.hasPermission(3L, "Ops:Container:Logs"));
        assertFalse(service.hasPermission(null, "Ops:Container:Logs"));
        assertFalse(service.hasPermission(3L, " "));

        verify(userService).isSuperAdmin(3L);
        verifyNoInteractions(userMapper);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }
    }
}
