package com.helmx.tutorial.docker.controller;

import com.helmx.tutorial.docker.utils.DockerClientUtil;
import com.helmx.tutorial.security.security.service.UserPermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for HomeController, focusing on the checkPermission business logic.
 */
@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private DockerClientUtil dockerClientUtil;

    @Mock
    private UserPermissionService userPermissionService;

    private HomeController homeController;
    private Method checkPermissionMethod;

    @BeforeEach
    void setUp() throws Exception {
        homeController = new HomeController();
        ReflectionTestUtils.setField(homeController, "dockerClientUtil", dockerClientUtil);
        ReflectionTestUtils.setField(homeController, "userPermissionService", userPermissionService);

        checkPermissionMethod = HomeController.class.getDeclaredMethod("checkPermission", Long.class, String.class);
        checkPermissionMethod.setAccessible(true);
    }

    private boolean invokeCheckPermission(Long userId, String pruneType) throws Exception {
        return (boolean) checkPermissionMethod.invoke(homeController, userId, pruneType);
    }

    // ── Bug fix: unknown pruneType must be denied ─────────────────────────────

    @Test
    void checkPermission_unknownPruneType_returnsFalseWithoutCallingPermissionService() throws Exception {
        // Before the fix: !pruneType.isEmpty() evaluated to true for "UNKNOWN",
        // so hasPermission was called with an empty permission string.
        // After the fix: !permission.isEmpty() is false → method returns false immediately.
        boolean result = invokeCheckPermission(1L, "UNKNOWN");

        assertFalse(result, "Unknown prune type must be denied");
        verify(userPermissionService, never()).hasPermission(any(), any());
    }

    @Test
    void checkPermission_emptyPruneType_returnsFalseWithoutCallingPermissionService() throws Exception {
        boolean result = invokeCheckPermission(1L, "");

        assertFalse(result, "Empty prune type must be denied");
        verify(userPermissionService, never()).hasPermission(any(), any());
    }

    // ── Known prune types resolve to their correct permission strings ─────────

    @Test
    void checkPermission_buildType_checksCorrectPermission() throws Exception {
        when(userPermissionService.hasPermission(eq(1L), eq("Ops:Build:Prune"))).thenReturn(true);

        assertTrue(invokeCheckPermission(1L, "BUILD"));
        verify(userPermissionService).hasPermission(1L, "Ops:Build:Prune");
    }

    @Test
    void checkPermission_containersType_checksCorrectPermission() throws Exception {
        when(userPermissionService.hasPermission(eq(2L), eq("Ops:Container:Prune"))).thenReturn(true);

        assertTrue(invokeCheckPermission(2L, "CONTAINERS"));
        verify(userPermissionService).hasPermission(2L, "Ops:Container:Prune");
    }

    @Test
    void checkPermission_imagesType_checksCorrectPermission() throws Exception {
        when(userPermissionService.hasPermission(eq(3L), eq("Ops:Image:Prune"))).thenReturn(false);

        assertFalse(invokeCheckPermission(3L, "IMAGES"));
        verify(userPermissionService).hasPermission(3L, "Ops:Image:Prune");
    }

    @Test
    void checkPermission_networksType_checksCorrectPermission() throws Exception {
        when(userPermissionService.hasPermission(eq(4L), eq("Ops:Network:Prune"))).thenReturn(true);

        assertTrue(invokeCheckPermission(4L, "NETWORKS"));
        verify(userPermissionService).hasPermission(4L, "Ops:Network:Prune");
    }

    @Test
    void checkPermission_volumesType_checksCorrectPermission() throws Exception {
        when(userPermissionService.hasPermission(eq(5L), eq("Ops:Volume:Prune"))).thenReturn(true);

        assertTrue(invokeCheckPermission(5L, "VOLUMES"));
        verify(userPermissionService).hasPermission(5L, "Ops:Volume:Prune");
    }

    @Test
    void checkPermission_userWithoutPermission_returnsFalse() throws Exception {
        when(userPermissionService.hasPermission(eq(10L), eq("Ops:Image:Prune"))).thenReturn(false);

        assertFalse(invokeCheckPermission(10L, "IMAGES"));
    }
}
