package com.helmx.tutorial.docker.utils;

import com.github.dockerjava.api.DockerClient;
import com.helmx.tutorial.docker.entity.DockerEnv;
import com.helmx.tutorial.docker.mapper.DockerEnvMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private DockerEnvMapper dockerEnvMapper;

    @Mock
    private RemoteHostMetricsCollector remoteHostMetricsCollector;

    private DockerClientUtil dockerClientUtil;

    @BeforeEach
    void setUp() {
        dockerClientUtil = new DockerClientUtil();
        ReflectionTestUtils.setField(dockerClientUtil, "connectionManager", connectionManager);
        ReflectionTestUtils.setField(dockerClientUtil, "dockerHostValidator", dockerHostValidator);
        ReflectionTestUtils.setField(dockerClientUtil, "dockerEnvMapper", dockerEnvMapper);
        ReflectionTestUtils.setField(dockerClientUtil, "remoteHostMetricsCollector", remoteHostMetricsCollector);
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
        assertTrue(getMetricAsDouble(metrics, "hostCpuUsage") >= 0D);
        assertTrue(getMetricAsDouble(metrics, "hostCpuUsage") <= 100D);
        assertTrue(getMetricAsDouble(metrics, "hostMemoryUsage") >= 0D);
        assertTrue(getMetricAsDouble(metrics, "hostMemoryUsage") <= 100D);
        assertTrue(getMetricAsDouble(metrics, "hostDiskUsage") >= 0D);
        assertTrue(getMetricAsDouble(metrics, "hostDiskUsage") <= 100D);
        assertTrue(getMetricAsDouble(metrics, "DiskReadTrafficNew") >= 0D);
        assertTrue(getMetricAsDouble(metrics, "WriteTrafficNew") >= 0D);
        assertFalse(((String) metrics.get("hostMemoryTotal")).isBlank());
        assertFalse(((String) metrics.get("hostDiskTotal")).isBlank());
    }

    @Test
    void resolveRootBlockDevice_mapsMountInfoDeviceNumberToReadableStatFile() {
        String rootDeviceNumber = ReflectionTestUtils.invokeMethod(dockerClientUtil, "resolveRootDeviceNumber");
        assertNotNull(rootDeviceNumber);

        String rootBlockDevice = ReflectionTestUtils.invokeMethod(dockerClientUtil, "resolveRootBlockDevice");
        assertNotNull(rootBlockDevice);
        assertFalse(rootBlockDevice.isBlank());
        assertTrue(Files.exists(Path.of("/sys/class/block", rootBlockDevice, "stat")));
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
        assertEquals(0D, metrics.get("DiskReadTrafficNew"));
        assertEquals(0D, metrics.get("WriteTrafficNew"));
    }

    @Test
    void loadHostResourceUsage_remoteDockerHostWithSshConfig_returnsRemoteMetrics() {
        dockerClientUtil.setCurrentHost("tcp://192.0.2.10:2375");
        DockerEnv dockerEnv = new DockerEnv();
        dockerEnv.setHost("tcp://192.0.2.10:2375");
        dockerEnv.setStatus(1);
        dockerEnv.setSshEnabled(true);
        dockerEnv.setSshPort(22);
        dockerEnv.setSshUsername("root");
        dockerEnv.setSshPassword("encrypted");
        dockerEnv.setSshHostKeyFingerprint("SHA256:example");

        Map<String, Object> remoteMetrics = Map.of(
                "hostMetricsAvailable", true,
                "hostCpuUsage", 22.5D,
                "hostMemoryUsage", 33.3D,
                "hostMemoryUsed", "2.00 GB",
                "hostMemoryTotal", "6.00 GB",
                "hostDiskUsage", 44.4D,
                "hostDiskUsed", "10.00 GB",
                "hostDiskTotal", "20.00 GB",
                "DiskReadTrafficNew", 128.5D,
                "WriteTrafficNew", 64.25D
        );

        when(dockerEnvMapper.selectOne(any())).thenReturn(dockerEnv);
        when(remoteHostMetricsCollector.collect("tcp://192.0.2.10:2375", dockerEnv)).thenReturn(remoteMetrics);

        Map<String, Object> metrics = dockerClientUtil.loadHostResourceUsage();

        assertEquals(remoteMetrics, metrics);
    }

    @Test
    void loadHostResourceUsage_remoteDockerHostWithPublicKeySshConfig_returnsRemoteMetrics() {
        dockerClientUtil.setCurrentHost("tcp://192.0.2.11:2375");
        DockerEnv dockerEnv = new DockerEnv();
        dockerEnv.setHost("tcp://192.0.2.11:2375");
        dockerEnv.setStatus(1);
        dockerEnv.setSshEnabled(true);
        dockerEnv.setSshPort(22);
        dockerEnv.setSshUsername("root");
        dockerEnv.setSshPassword(null);
        dockerEnv.setSshHostKeyFingerprint("SHA256:example");

        Map<String, Object> remoteMetrics = Map.of(
                "hostMetricsAvailable", true,
                "hostCpuUsage", 18.5D,
                "hostMemoryUsage", 28.8D,
                "hostMemoryUsed", "1.80 GB",
                "hostMemoryTotal", "6.25 GB",
                "hostDiskUsage", 31.2D,
                "hostDiskUsed", "8.00 GB",
                "hostDiskTotal", "25.64 GB",
                "DiskReadTrafficNew", 16.0D,
                "WriteTrafficNew", 8.0D
        );

        when(dockerEnvMapper.selectOne(any())).thenReturn(dockerEnv);
        when(remoteHostMetricsCollector.collect("tcp://192.0.2.11:2375", dockerEnv)).thenReturn(remoteMetrics);

        Map<String, Object> metrics = dockerClientUtil.loadHostResourceUsage();

        assertEquals(remoteMetrics, metrics);
    }

    private double getMetricAsDouble(Map<String, Object> metrics, String key) {
        return ((Number) metrics.get(key)).doubleValue();
    }
}
