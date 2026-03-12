package com.helmx.tutorial.docker.utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.NotFoundException;
import com.helmx.tutorial.docker.entity.DockerEnv;
import com.helmx.tutorial.docker.mapper.DockerEnvMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DockerClientUtilTest {

    @TempDir
    Path tempDir;

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

    // ─── ensureVolumeHelperImage (via backupVolume / cloneVolume) ───────────────

    private void setupDockerHost() {
        when(connectionManager.checkConnectionHealth("tcp://docker")).thenReturn(true);
        when(connectionManager.getDockerClient("tcp://docker")).thenReturn(dockerClient);
        dockerClientUtil.setCurrentHost("tcp://docker");
    }

    @Test
    void backupVolume_skipsImagePullWhenHelperAlreadyExists() throws Exception {
        setupDockerHost();

        // Image already exists: inspectImageCmd.exec() returns without throwing
        InspectImageCmd inspectCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("busybox:latest")).thenReturn(inspectCmd);

        // Mock the rest of backupVolume so it completes normally
        CreateContainerCmd createCmd = mock(CreateContainerCmd.class, Answers.RETURNS_SELF);
        CreateContainerResponse createResponse = mock(CreateContainerResponse.class);
        when(dockerClient.createContainerCmd("busybox:latest")).thenReturn(createCmd);
        when(createCmd.exec()).thenReturn(createResponse);
        when(createResponse.getId()).thenReturn("ctr-abc123");

        CopyArchiveFromContainerCmd copyCmd = mock(CopyArchiveFromContainerCmd.class);
        when(dockerClient.copyArchiveFromContainerCmd("ctr-abc123", "/backup-data")).thenReturn(copyCmd);
        when(copyCmd.exec()).thenReturn(new ByteArrayInputStream(new byte[0]));

        InputStream result = dockerClientUtil.backupVolume("mydata", "/");

        assertNotNull(result);
        verify(dockerClient, never()).pullImageCmd(anyString());
    }

    @Test
    void backupVolume_throwsWhenHelperImagePullFails() {
        setupDockerHost();

        // Image not found locally
        InspectImageCmd inspectCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("busybox:latest")).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenThrow(new NotFoundException("No such image: busybox:latest"));

        // Pull fails
        PullImageCmd pullCmd = mock(PullImageCmd.class);
        when(dockerClient.pullImageCmd("busybox:latest")).thenReturn(pullCmd);
        when(pullCmd.exec(any())).thenThrow(new RuntimeException("Network unavailable"));

        assertThrows(RuntimeException.class, () -> dockerClientUtil.backupVolume("mydata", "/"));

        // Container creation must not have been attempted
        verify(dockerClient, never()).createContainerCmd(anyString());
    }

    @Test
    void cloneVolume_throwsWhenHelperImagePullFails() {
        setupDockerHost();

        // Target volume creation succeeds
        CreateVolumeCmd createVolumeCmd = mock(CreateVolumeCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.createVolumeCmd()).thenReturn(createVolumeCmd);

        // Image not found locally
        InspectImageCmd inspectCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("busybox:latest")).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenThrow(new NotFoundException("No such image: busybox:latest"));

        // Pull fails
        PullImageCmd pullCmd = mock(PullImageCmd.class);
        when(dockerClient.pullImageCmd("busybox:latest")).thenReturn(pullCmd);
        when(pullCmd.exec(any())).thenThrow(new RuntimeException("Network unavailable"));

        assertThrows(RuntimeException.class,
                () -> dockerClientUtil.cloneVolume("src-vol", "target-vol", "local"));

        // Container creation must not have been attempted
        verify(dockerClient, never()).createContainerCmd(anyString());
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
    void resolveRootBlockDevice_fallsBackToMountsWhenMountInfoIsUnavailable() throws Exception {
        Path mountsFile = tempDir.resolve("mounts");
        Files.writeString(mountsFile, "/dev/sda1 / ext4 rw,relatime 0 0\n");
        DockerClientUtil fallbackUtil = new DockerClientUtil() {
            @Override
            Path mountInfoPath() {
                return tempDir.resolve("missing-mountinfo");
            }

            @Override
            Path mountsPath() {
                return mountsFile;
            }
        };

        String rootBlockDevice = ReflectionTestUtils.invokeMethod(fallbackUtil, "resolveRootBlockDevice");

        assertEquals("sda1", rootBlockDevice);
    }

    @Test
    void loadHostResourceUsage_remoteDockerHost_returnsUnavailableDefaults() {
        dockerClientUtil.setCurrentHost("tcp://192.0.2.10:2375");

        Map<String, Object> metrics = dockerClientUtil.loadHostResourceUsage();

        assertFalse((Boolean) metrics.get("hostMetricsAvailable"));
        assertEquals("Remote host metrics unavailable: no active Docker environment matched the current host",
                metrics.get("hostMetricsDebug"));
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
