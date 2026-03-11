package com.helmx.tutorial.docker.utils;

import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DockerClientUtilTest {

    @Mock
    private DockerConnectionManager connectionManager;

    @Mock
    private DockerHostValidator dockerHostValidator;

    @Mock
    private DockerClient dockerClient;

    private DockerClientUtil dockerClientUtil;

    @BeforeEach
    void setUp() {
        dockerClientUtil = new DockerClientUtil();
        ReflectionTestUtils.setField(dockerClientUtil, "connectionManager", connectionManager);
        ReflectionTestUtils.setField(dockerClientUtil, "dockerHostValidator", dockerHostValidator);
    }

    @Test
    void setCurrentHost_validationFailure_clearsPreviousThreadLocalHost() {
        when(connectionManager.checkConnectionHealth("tcp://good")).thenReturn(true);
        when(connectionManager.getDockerClient("tcp://good")).thenReturn(dockerClient);

        dockerClientUtil.setCurrentHost("tcp://good");
        assertSame(dockerClient, dockerClientUtil.getCurrentDockerClient());

        doThrow(new IllegalArgumentException("host denied"))
                .when(dockerHostValidator).validateHostAllowlist("tcp://blocked");

        assertThrows(IllegalArgumentException.class, () -> dockerClientUtil.setCurrentHost("tcp://blocked"));
        assertThrows(IllegalStateException.class, dockerClientUtil::getCurrentDockerClient);
    }

    @Test
    void getCurrentDockerClient_unhealthyConnection_removesAndRecreatesClient() {
        dockerClientUtil.setCurrentHost("tcp://docker");
        when(connectionManager.checkConnectionHealth("tcp://docker")).thenReturn(false);
        when(connectionManager.getDockerClient("tcp://docker")).thenReturn(dockerClient);

        assertSame(dockerClient, dockerClientUtil.getCurrentDockerClient());

        verify(connectionManager).removeClient("tcp://docker");
        verify(connectionManager).getDockerClient("tcp://docker");
    }

    @Test
    void getCurrentDockerClient_withoutHost_throwsIllegalStateException() {
        assertThrows(IllegalStateException.class, dockerClientUtil::getCurrentDockerClient);
    }

    @Test
    void loadHostResourceUsage_localDockerHost_returnsUsageMetrics() {
        dockerClientUtil.setCurrentHost("unix:///var/run/docker.sock");

        Map<String, Object> metrics = dockerClientUtil.loadHostResourceUsage();

        assertTrue((Boolean) metrics.get("hostMetricsAvailable"));
        assertTrue(((Double) metrics.get("hostCpuUsage")) >= 0D);
        assertTrue(((Double) metrics.get("hostCpuUsage")) <= 100D);
        assertTrue(((Double) metrics.get("hostMemoryUsage")) >= 0D);
        assertTrue(((Double) metrics.get("hostMemoryUsage")) <= 100D);
        assertTrue(((Double) metrics.get("hostDiskUsage")) >= 0D);
        assertTrue(((Double) metrics.get("hostDiskUsage")) <= 100D);
        assertFalse(((String) metrics.get("hostMemoryTotal")).isBlank());
        assertFalse(((String) metrics.get("hostDiskTotal")).isBlank());
    }

    @Test
    void loadHostResourceUsage_remoteDockerHost_returnsUnavailableDefaults() {
        dockerClientUtil.setCurrentHost("tcp://192.0.2.10:2375");

        Map<String, Object> metrics = dockerClientUtil.loadHostResourceUsage();

        assertFalse((Boolean) metrics.get("hostMetricsAvailable"));
        assertEquals(0D, metrics.get("hostCpuUsage"));
        assertEquals(0D, metrics.get("hostMemoryUsage"));
        assertEquals(0D, metrics.get("hostDiskUsage"));
        assertEquals("0B", metrics.get("hostMemoryTotal"));
        assertEquals("0B", metrics.get("hostDiskTotal"));
    }
}
