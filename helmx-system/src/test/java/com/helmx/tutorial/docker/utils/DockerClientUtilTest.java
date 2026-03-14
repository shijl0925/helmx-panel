package com.helmx.tutorial.docker.utils;

import com.alibaba.fastjson2.JSONObject;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.InvocationBuilder;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;
import com.helmx.tutorial.docker.dto.*;
import com.helmx.tutorial.docker.entity.DockerEnv;
import com.helmx.tutorial.docker.entity.Registry;
import com.helmx.tutorial.docker.mapper.DockerEnvMapper;
import com.helmx.tutorial.docker.mapper.RegistryMapper;
import com.helmx.tutorial.docker.utils.ImageBuildTask;
import com.helmx.tutorial.docker.utils.ImageBuildTaskManager;
import com.helmx.tutorial.docker.utils.ImagePullTaskManager;
import com.helmx.tutorial.docker.utils.ImagePushTaskManager;
import com.helmx.tutorial.docker.utils.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    @Mock
    private RegistryMapper registryMapper;

    @Mock
    private PasswordUtil passwordUtil;

    @Mock
    private ImagePullTaskManager imagePullTaskManager;

    @Mock
    private ImagePushTaskManager imagePushTaskManager;

    @Mock
    private ImageBuildTaskManager imageBuildTaskManager;

    @Mock
    private com.github.dockerjava.transport.DockerHttpClient dockerHttpClient;

    private DockerClientUtil dockerClientUtil;

    @BeforeEach
    void setUp() {
        dockerClientUtil = new DockerClientUtil();
        ReflectionTestUtils.setField(dockerClientUtil, "connectionManager", connectionManager);
        ReflectionTestUtils.setField(dockerClientUtil, "dockerHostValidator", dockerHostValidator);
        ReflectionTestUtils.setField(dockerClientUtil, "dockerEnvMapper", dockerEnvMapper);
        ReflectionTestUtils.setField(dockerClientUtil, "remoteHostMetricsCollector", remoteHostMetricsCollector);
        ReflectionTestUtils.setField(dockerClientUtil, "registryMapper", registryMapper);
        ReflectionTestUtils.setField(dockerClientUtil, "passwordUtil", passwordUtil);
        ReflectionTestUtils.setField(dockerClientUtil, "imagePullTaskManager", imagePullTaskManager);
        ReflectionTestUtils.setField(dockerClientUtil, "imagePushTaskManager", imagePushTaskManager);
        ReflectionTestUtils.setField(dockerClientUtil, "imageBuildTaskManager", imageBuildTaskManager);
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
    void resolveRootBlockDevice_returnsNullWhenBothMountFilesAreUnavailable() {
        DockerClientUtil macosUtil = new DockerClientUtil() {
            @Override
            Path mountInfoPath() {
                return tempDir.resolve("missing-mountinfo");
            }

            @Override
            Path mountsPath() {
                return tempDir.resolve("missing-mounts");
            }
        };

        String rootBlockDevice = ReflectionTestUtils.invokeMethod(macosUtil, "resolveRootBlockDevice");

        assertNull(rootBlockDevice);
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

    // ─── Helper: set up a minimal loadStatus mock environment ──────────────────
    private ListContainersCmd setupLoadStatusMocks(List<Container> containers) {
        ListImagesCmd mockImagesCmd = mock(ListImagesCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listImagesCmd()).thenReturn(mockImagesCmd);
        when(mockImagesCmd.exec()).thenReturn(new ArrayList<>());

        ListNetworksCmd mockNetworksCmd = mock(ListNetworksCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listNetworksCmd()).thenReturn(mockNetworksCmd);
        when(mockNetworksCmd.exec()).thenReturn(new ArrayList<>());

        ListVolumesCmd mockVolumesCmd = mock(ListVolumesCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listVolumesCmd()).thenReturn(mockVolumesCmd);
        ListVolumesResponse mockVolumesResp = mock(ListVolumesResponse.class);
        when(mockVolumesResp.getVolumes()).thenReturn(new ArrayList<>());
        when(mockVolumesCmd.exec()).thenReturn(mockVolumesResp);

        ListContainersCmd mockContainersCmd = mock(ListContainersCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listContainersCmd()).thenReturn(mockContainersCmd);
        when(mockContainersCmd.exec()).thenReturn(containers);
        return mockContainersCmd;
    }

    // ─── Static utility methods ─────────────────────────────────────────────────

    @Test
    void stringsToMap_validKeyValuePairs_skipsEntriesWithoutEquals() {
        Map<String, String> result = DockerClientUtil.stringsToMap(
                new String[]{"key1=value1", "key2=value2", "no-equals-here"});
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
        assertFalse(result.containsKey("no-equals-here"));
        assertEquals(2, result.size());
    }

    @Test
    void toJSON_serializesObjectToJSONObject() {
        // A plain map serialises cleanly; we just verify it round-trips.
        JSONObject json = DockerClientUtil.toJSON(Map.of("hello", "world"));
        assertEquals("world", json.getString("hello"));
    }

    @Test
    void toJSON_returnsEmptyJSONObjectOnSerializationFailure() {
        // Passing a List makes JSON.parse return a JSONArray; casting to JSONObject
        // throws ClassCastException, which is caught and returns an empty JSONObject.
        JSONObject result = DockerClientUtil.toJSON(List.of("a", "b"));
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ─── Host / connection helpers ──────────────────────────────────────────────

    @Test
    void clearCurrentHost_removesThreadLocalHost_subsequentGetClientThrows() {
        setupDockerHost();
        assertDoesNotThrow(dockerClientUtil::getCurrentDockerClient);
        dockerClientUtil.clearCurrentHost();
        assertThrows(IllegalStateException.class, dockerClientUtil::getCurrentDockerClient);
    }

    @Test
    void isConnectionHealthy_returnsFalseWhenNoHostSet() {
        assertFalse(dockerClientUtil.isConnectionHealthy());
    }

    @Test
    void isConnectionHealthy_returnsTrueOnSuccessfulPing() {
        setupDockerHost();
        PingCmd pingCmd = mock(PingCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.pingCmd()).thenReturn(pingCmd);
        assertTrue(dockerClientUtil.isConnectionHealthy());
    }

    @Test
    void isConnectionHealthy_returnsFalseOnException() {
        setupDockerHost();
        PingCmd pingCmd = mock(PingCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.pingCmd()).thenReturn(pingCmd);
        when(pingCmd.exec()).thenThrow(new RuntimeException("Connection refused"));
        assertFalse(dockerClientUtil.isConnectionHealthy());
    }

    // ─── Docker info / container listing ───────────────────────────────────────

    @Test
    void getInfo_callsInfoCmd_returnsResult() {
        setupDockerHost();
        InfoCmd mockCmd = mock(InfoCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.infoCmd()).thenReturn(mockCmd);
        Info mockInfo = mock(Info.class);
        when(mockCmd.exec()).thenReturn(mockInfo);
        assertSame(mockInfo, dockerClientUtil.getInfo());
    }

    @Test
    void listContainers_callsListContainersCmdWithShowAll() {
        setupDockerHost();
        ListContainersCmd mockCmd = mock(ListContainersCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listContainersCmd()).thenReturn(mockCmd);
        List<Container> containers = List.of(mock(Container.class));
        when(mockCmd.exec()).thenReturn(containers);
        assertSame(containers, dockerClientUtil.listContainers());
        verify(mockCmd).withShowAll(true);
    }

    @Test
    void searchContainers_withNoFilters_callsExec() {
        setupDockerHost();
        ListContainersCmd mockCmd = mock(ListContainersCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listContainersCmd()).thenReturn(mockCmd);
        List<Container> result = new ArrayList<>();
        when(mockCmd.exec()).thenReturn(result);
        assertSame(result, dockerClientUtil.searchContainers(new ContainerQueryRequest()));
    }

    @Test
    void searchContainers_withContainerIdFilter_setsIdFilter() {
        setupDockerHost();
        ListContainersCmd mockCmd = mock(ListContainersCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listContainersCmd()).thenReturn(mockCmd);
        when(mockCmd.exec()).thenReturn(new ArrayList<>());
        ContainerQueryRequest criteria = new ContainerQueryRequest();
        criteria.setContainerId("abc123");
        dockerClientUtil.searchContainers(criteria);
        verify(mockCmd).withIdFilter(List.of("abc123"));
    }

    @Test
    void searchContainers_withNameFilter_setsNameFilter() {
        setupDockerHost();
        ListContainersCmd mockCmd = mock(ListContainersCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listContainersCmd()).thenReturn(mockCmd);
        when(mockCmd.exec()).thenReturn(new ArrayList<>());
        ContainerQueryRequest criteria = new ContainerQueryRequest();
        criteria.setName("mycontainer");
        dockerClientUtil.searchContainers(criteria);
        verify(mockCmd).withNameFilter(List.of("mycontainer"));
    }

    @Test
    void searchContainers_withStateFilter_setsStatusFilter() {
        setupDockerHost();
        ListContainersCmd mockCmd = mock(ListContainersCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listContainersCmd()).thenReturn(mockCmd);
        when(mockCmd.exec()).thenReturn(new ArrayList<>());
        ContainerQueryRequest criteria = new ContainerQueryRequest();
        criteria.setState("running");
        dockerClientUtil.searchContainers(criteria);
        verify(mockCmd).withStatusFilter(List.of("running"));
    }

    @Test
    void searchContainers_withGenericFiltersMap_callsWithFilter() {
        setupDockerHost();
        ListContainersCmd mockCmd = mock(ListContainersCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listContainersCmd()).thenReturn(mockCmd);
        when(mockCmd.exec()).thenReturn(new ArrayList<>());
        ContainerQueryRequest criteria = new ContainerQueryRequest();
        criteria.setFilters(Map.of("label", "app=web"));
        dockerClientUtil.searchContainers(criteria);
        verify(mockCmd).withFilter("label", Collections.singleton("app=web"));
    }

    @Test
    void getContainerTop_callsTopContainerCmd_returnsJSON() {
        setupDockerHost();
        TopContainerCmd mockCmd = mock(TopContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.topContainerCmd("ctr1")).thenReturn(mockCmd);
        TopContainerResponse mockResp = mock(TopContainerResponse.class);
        when(mockCmd.exec()).thenReturn(mockResp);
        assertNotNull(dockerClientUtil.getContainerTop("ctr1"));
    }

    @Test
    void inspectContainer_callsInspectContainerCmd() {
        setupDockerHost();
        InspectContainerCmd mockCmd = mock(InspectContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.inspectContainerCmd("ctr1")).thenReturn(mockCmd);
        InspectContainerResponse mockResp = mock(InspectContainerResponse.class);
        when(mockCmd.exec()).thenReturn(mockResp);
        assertSame(mockResp, dockerClientUtil.inspectContainer("ctr1"));
    }

    @Test
    void getContainerNetworks_returnsNetworkNamesFromInspect() {
        setupDockerHost();
        InspectContainerCmd mockCmd = mock(InspectContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.inspectContainerCmd("ctr1")).thenReturn(mockCmd);
        InspectContainerResponse mockResp = mock(InspectContainerResponse.class);
        when(mockCmd.exec()).thenReturn(mockResp);
        NetworkSettings mockNetSettings = mock(NetworkSettings.class);
        when(mockResp.getNetworkSettings()).thenReturn(mockNetSettings);
        Map<String, ContainerNetwork> nets = new LinkedHashMap<>();
        nets.put("bridge", mock(ContainerNetwork.class));
        nets.put("host", mock(ContainerNetwork.class));
        when(mockNetSettings.getNetworks()).thenReturn(nets);
        assertEquals(Set.of("bridge", "host"), dockerClientUtil.getContainerNetworks("ctr1"));
    }

    // ─── Container lifecycle ────────────────────────────────────────────────────

    @Test
    void startContainer_success() {
        setupDockerHost();
        StartContainerCmd mockCmd = mock(StartContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.startContainerCmd("ctr1")).thenReturn(mockCmd);
        Map<String, Object> result = dockerClientUtil.startContainer("ctr1");
        assertEquals("success", result.get("status"));
    }

    @Test
    void startContainer_failure_returnsFailedMap() {
        setupDockerHost();
        StartContainerCmd mockCmd = mock(StartContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.startContainerCmd("ctr1")).thenReturn(mockCmd);
        when(mockCmd.exec()).thenThrow(new RuntimeException("Cannot start"));
        Map<String, Object> result = dockerClientUtil.startContainer("ctr1");
        assertEquals("failed", result.get("status"));
        assertTrue(result.get("message").toString().contains("Cannot start"));
    }

    @Test
    void stopContainer_success() {
        setupDockerHost();
        StopContainerCmd mockCmd = mock(StopContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.stopContainerCmd("ctr1")).thenReturn(mockCmd);
        assertEquals("success", dockerClientUtil.stopContainer("ctr1").get("status"));
    }

    @Test
    void stopContainer_failure_returnsFailedMap() {
        setupDockerHost();
        StopContainerCmd mockCmd = mock(StopContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.stopContainerCmd("ctr1")).thenReturn(mockCmd);
        when(mockCmd.exec()).thenThrow(new RuntimeException("stop error"));
        assertEquals("failed", dockerClientUtil.stopContainer("ctr1").get("status"));
    }

    @Test
    void restartContainer_success() {
        setupDockerHost();
        RestartContainerCmd mockCmd = mock(RestartContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.restartContainerCmd("ctr1")).thenReturn(mockCmd);
        assertEquals("success", dockerClientUtil.restartContainer("ctr1").get("status"));
    }

    @Test
    void restartContainer_failure_returnsFailedMap() {
        setupDockerHost();
        RestartContainerCmd mockCmd = mock(RestartContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.restartContainerCmd("ctr1")).thenReturn(mockCmd);
        when(mockCmd.exec()).thenThrow(new RuntimeException("restart error"));
        assertEquals("failed", dockerClientUtil.restartContainer("ctr1").get("status"));
    }

    @Test
    void killContainer_success() {
        setupDockerHost();
        KillContainerCmd mockCmd = mock(KillContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.killContainerCmd("ctr1")).thenReturn(mockCmd);
        assertEquals("success", dockerClientUtil.killContainer("ctr1").get("status"));
    }

    @Test
    void killContainer_failure_returnsFailedMap() {
        setupDockerHost();
        KillContainerCmd mockCmd = mock(KillContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.killContainerCmd("ctr1")).thenReturn(mockCmd);
        when(mockCmd.exec()).thenThrow(new RuntimeException("kill error"));
        assertEquals("failed", dockerClientUtil.killContainer("ctr1").get("status"));
    }

    @Test
    void pauseContainer_success() {
        setupDockerHost();
        PauseContainerCmd mockCmd = mock(PauseContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.pauseContainerCmd("ctr1")).thenReturn(mockCmd);
        assertEquals("success", dockerClientUtil.pauseContainer("ctr1").get("status"));
    }

    @Test
    void pauseContainer_failure_returnsFailedMap() {
        setupDockerHost();
        PauseContainerCmd mockCmd = mock(PauseContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.pauseContainerCmd("ctr1")).thenReturn(mockCmd);
        when(mockCmd.exec()).thenThrow(new RuntimeException("pause error"));
        assertEquals("failed", dockerClientUtil.pauseContainer("ctr1").get("status"));
    }

    @Test
    void unpauseContainer_success() {
        setupDockerHost();
        UnpauseContainerCmd mockCmd = mock(UnpauseContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.unpauseContainerCmd("ctr1")).thenReturn(mockCmd);
        assertEquals("success", dockerClientUtil.unpauseContainer("ctr1").get("status"));
    }

    @Test
    void unpauseContainer_failure_returnsFailedMap() {
        setupDockerHost();
        UnpauseContainerCmd mockCmd = mock(UnpauseContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.unpauseContainerCmd("ctr1")).thenReturn(mockCmd);
        when(mockCmd.exec()).thenThrow(new RuntimeException("unpause error"));
        assertEquals("failed", dockerClientUtil.unpauseContainer("ctr1").get("status"));
    }

    @Test
    void removeContainer_success() {
        setupDockerHost();
        RemoveContainerCmd mockCmd = mock(RemoveContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.removeContainerCmd("ctr1")).thenReturn(mockCmd);
        assertEquals("success", dockerClientUtil.removeContainer("ctr1").get("status"));
    }

    @Test
    void removeContainer_failure_returnsFailedMap() {
        setupDockerHost();
        RemoveContainerCmd mockCmd = mock(RemoveContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.removeContainerCmd("ctr1")).thenReturn(mockCmd);
        when(mockCmd.exec()).thenThrow(new RuntimeException("remove error"));
        assertEquals("failed", dockerClientUtil.removeContainer("ctr1").get("status"));
    }

    @Test
    void removeContainerForce_success_callsWithForceTrue() {
        setupDockerHost();
        RemoveContainerCmd mockCmd = mock(RemoveContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.removeContainerCmd("ctr1")).thenReturn(mockCmd);
        assertEquals("success", dockerClientUtil.removeContainerForce("ctr1").get("status"));
        verify(mockCmd).withForce(true);
    }

    @Test
    void removeContainerForce_failure_returnsFailedMap() {
        setupDockerHost();
        RemoveContainerCmd mockCmd = mock(RemoveContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.removeContainerCmd("ctr1")).thenReturn(mockCmd);
        when(mockCmd.withForce(true)).thenReturn(mockCmd);
        when(mockCmd.exec()).thenThrow(new RuntimeException("force remove error"));
        assertEquals("failed", dockerClientUtil.removeContainerForce("ctr1").get("status"));
    }

    // ─── Container rename ───────────────────────────────────────────────────────

    @Test
    void renameContainer_nullName_returnsFailed() {
        setupDockerHost();
        RenameContainerCmd mockCmd = mock(RenameContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.renameContainerCmd("ctr1")).thenReturn(mockCmd);
        ContainerRenameRequest req = new ContainerRenameRequest();
        req.setContainerId("ctr1");
        req.setNewName(null);
        assertEquals("failed", dockerClientUtil.renameContainer(req).get("status"));
    }

    @Test
    void renameContainer_emptyName_returnsFailed() {
        setupDockerHost();
        RenameContainerCmd mockCmd = mock(RenameContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.renameContainerCmd("ctr1")).thenReturn(mockCmd);
        ContainerRenameRequest req = new ContainerRenameRequest();
        req.setContainerId("ctr1");
        req.setNewName("");
        assertEquals("failed", dockerClientUtil.renameContainer(req).get("status"));
    }

    @Test
    void renameContainer_invalidName_returnsFailed() {
        setupDockerHost();
        RenameContainerCmd mockCmd = mock(RenameContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.renameContainerCmd("ctr1")).thenReturn(mockCmd);
        ContainerRenameRequest req = new ContainerRenameRequest();
        req.setContainerId("ctr1");
        req.setNewName("@invalid");
        assertEquals("failed", dockerClientUtil.renameContainer(req).get("status"));
    }

    @Test
    void renameContainer_validName_success() {
        setupDockerHost();
        RenameContainerCmd mockCmd = mock(RenameContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.renameContainerCmd("ctr1")).thenReturn(mockCmd);
        ContainerRenameRequest req = new ContainerRenameRequest();
        req.setContainerId("ctr1");
        req.setNewName("newname");
        assertEquals("success", dockerClientUtil.renameContainer(req).get("status"));
    }

    @Test
    void renameContainer_dockerException_returnsFailed() {
        setupDockerHost();
        RenameContainerCmd mockCmd = mock(RenameContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.renameContainerCmd("ctr1")).thenReturn(mockCmd);
        when(mockCmd.exec()).thenThrow(new RuntimeException("rename failed"));
        ContainerRenameRequest req = new ContainerRenameRequest();
        req.setContainerId("ctr1");
        req.setNewName("newname");
        assertEquals("failed", dockerClientUtil.renameContainer(req).get("status"));
    }

    // ─── Container logs ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    @Test
    void getContainerLogs_tailNumPositive_callsWithTail() throws InterruptedException {
        setupDockerHost();
        LogContainerCmd mockLogCmd = mock(LogContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.logContainerCmd("ctr1")).thenReturn(mockLogCmd);
        // Return the actual callback argument so the compiler-inserted cast succeeds;
        // call onComplete() immediately so awaitCompletion does not block.
        doAnswer(inv -> {
            ResultCallback<Frame> cb = inv.getArgument(0);
            try { cb.onComplete(); } catch (Exception ignored) {}
            return cb;
        }).when(mockLogCmd).exec(any(ResultCallback.class));

        dockerClientUtil.getContainerLogs("ctr1", 100);

        verify(mockLogCmd).withTail(100);
        verify(mockLogCmd, never()).withTailAll();
    }

    @SuppressWarnings("unchecked")
    @Test
    void getContainerLogs_tailNumZero_callsWithTailAll() throws InterruptedException {
        setupDockerHost();
        LogContainerCmd mockLogCmd = mock(LogContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.logContainerCmd("ctr1")).thenReturn(mockLogCmd);
        doAnswer(inv -> {
            ResultCallback<Frame> cb = inv.getArgument(0);
            try { cb.onComplete(); } catch (Exception ignored) {}
            return cb;
        }).when(mockLogCmd).exec(any(ResultCallback.class));

        dockerClientUtil.getContainerLogs("ctr1", 0);

        verify(mockLogCmd).withTailAll();
        verify(mockLogCmd, never()).withTail(anyInt());
    }

    // ─── Container commit ───────────────────────────────────────────────────────

    @Test
    void commitContainer_withAuthor_success() {
        setupDockerHost();
        CommitCmd mockCmd = mock(CommitCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.commitCmd("ctr1")).thenReturn(mockCmd);
        when(mockCmd.exec()).thenReturn("sha256:abc123");
        Map<String, Object> result = dockerClientUtil.commitContainer("ctr1", "myrepo", "author");
        assertEquals("success", result.get("status"));
        assertEquals("sha256:abc123", result.get("imageId"));
        verify(mockCmd).withAuthor("author");
    }

    @Test
    void commitContainer_withoutAuthor_doesNotCallWithAuthor() {
        setupDockerHost();
        CommitCmd mockCmd = mock(CommitCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.commitCmd("ctr1")).thenReturn(mockCmd);
        when(mockCmd.exec()).thenReturn("sha256:def456");
        Map<String, Object> result = dockerClientUtil.commitContainer("ctr1", "myrepo", null);
        assertEquals("success", result.get("status"));
        verify(mockCmd, never()).withAuthor(anyString());
    }

    @Test
    void commitContainer_dockerException_returnsFailed() {
        setupDockerHost();
        CommitCmd mockCmd = mock(CommitCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.commitCmd("ctr1")).thenReturn(mockCmd);
        when(mockCmd.exec()).thenThrow(new RuntimeException("commit failed"));
        assertEquals("failed", dockerClientUtil.commitContainer("ctr1", "myrepo", "author").get("status"));
    }

    // ─── Image operations ───────────────────────────────────────────────────────

    @Test
    void listImages_callsListImagesCmd() {
        setupDockerHost();
        ListImagesCmd mockCmd = mock(ListImagesCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listImagesCmd()).thenReturn(mockCmd);
        List<Image> images = List.of(mock(Image.class));
        when(mockCmd.exec()).thenReturn(images);
        assertSame(images, dockerClientUtil.listImages());
    }

    @Test
    void inspectImage_callsInspectImageCmd() {
        setupDockerHost();
        InspectImageCmd mockCmd = mock(InspectImageCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.inspectImageCmd("img1")).thenReturn(mockCmd);
        InspectImageResponse mockResp = mock(InspectImageResponse.class);
        when(mockCmd.exec()).thenReturn(mockResp);
        assertSame(mockResp, dockerClientUtil.inspectImage("img1"));
    }

    @Test
    void removeImage_callsRemoveImageCmdWithForce() {
        setupDockerHost();
        RemoveImageCmd mockCmd = mock(RemoveImageCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.removeImageCmd("img1")).thenReturn(mockCmd);
        dockerClientUtil.removeImage("img1", true);
        verify(mockCmd).withForce(true);
        verify(mockCmd).exec();
    }

    @Test
    void tagImage_withExplicitTag_parsesCorrectly() {
        setupDockerHost();
        TagImageCmd mockCmd = mock(TagImageCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.tagImageCmd("img1", "myrepo", "1.0")).thenReturn(mockCmd);
        assertEquals("success", dockerClientUtil.tagImage("img1", "myrepo:1.0").get("status"));
    }

    @Test
    void tagImage_withoutTag_defaultsToLatest() {
        setupDockerHost();
        TagImageCmd mockCmd = mock(TagImageCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.tagImageCmd("img1", "myrepo", "latest")).thenReturn(mockCmd);
        assertEquals("success", dockerClientUtil.tagImage("img1", "myrepo").get("status"));
    }

    @Test
    void tagImage_dockerException_returnsFailed() {
        setupDockerHost();
        TagImageCmd mockCmd = mock(TagImageCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.tagImageCmd(anyString(), anyString(), anyString())).thenReturn(mockCmd);
        when(mockCmd.exec()).thenThrow(new RuntimeException("Tag error"));
        assertEquals("failed", dockerClientUtil.tagImage("img1", "myrepo:1.0").get("status"));
    }

    @Test
    void importImage_success() {
        setupDockerHost();
        LoadImageCmd mockCmd = mock(LoadImageCmd.class, Answers.RETURNS_SELF);
        InputStream mockStream = mock(InputStream.class);
        when(dockerClient.loadImageCmd(mockStream)).thenReturn(mockCmd);
        assertEquals("success", dockerClientUtil.importImage(mockStream).get("status"));
    }

    @Test
    void importImage_dockerException_returnsFailed() {
        setupDockerHost();
        LoadImageCmd mockCmd = mock(LoadImageCmd.class, Answers.RETURNS_SELF);
        InputStream mockStream = mock(InputStream.class);
        when(dockerClient.loadImageCmd(mockStream)).thenReturn(mockCmd);
        doThrow(new RuntimeException("Load failed")).when(mockCmd).exec();
        assertEquals("failed", dockerClientUtil.importImage(mockStream).get("status"));
    }

    @Test
    void bulkRemoveImages_allSuccess() {
        setupDockerHost();
        RemoveImageCmd mockCmd1 = mock(RemoveImageCmd.class, Answers.RETURNS_SELF);
        RemoveImageCmd mockCmd2 = mock(RemoveImageCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.removeImageCmd("img1")).thenReturn(mockCmd1);
        when(dockerClient.removeImageCmd("img2")).thenReturn(mockCmd2);
        List<Map<String, Object>> results = dockerClientUtil.bulkRemoveImages(List.of("img1", "img2"), false);
        assertEquals(2, results.size());
        assertEquals("success", results.get(0).get("status"));
        assertEquals("success", results.get(1).get("status"));
    }

    @Test
    void bulkRemoveImages_partialFailure() {
        setupDockerHost();
        RemoveImageCmd mockCmd1 = mock(RemoveImageCmd.class, Answers.RETURNS_SELF);
        RemoveImageCmd mockCmd2 = mock(RemoveImageCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.removeImageCmd("img1")).thenReturn(mockCmd1);
        when(dockerClient.removeImageCmd("img2")).thenReturn(mockCmd2);
        when(mockCmd2.exec()).thenThrow(new RuntimeException("Remove failed"));
        List<Map<String, Object>> results = dockerClientUtil.bulkRemoveImages(List.of("img1", "img2"), true);
        assertEquals(2, results.size());
        assertEquals("success", results.get(0).get("status"));
        assertEquals("failed", results.get(1).get("status"));
    }

    @Test
    void getImageDiskUsage_filtersNoneTagsAndMarksInUse() {
        setupDockerHost();

        Image img1 = mock(Image.class);
        when(img1.getId()).thenReturn("sha256:aabbccddeeff001122334455");
        when(img1.getRepoTags()).thenReturn(new String[]{"ubuntu:20.04"});
        when(img1.getSize()).thenReturn(100L);
        when(img1.getVirtualSize()).thenReturn(200L);

        Image img2 = mock(Image.class); // should be filtered out (<none>:<none>)
        when(img2.getRepoTags()).thenReturn(new String[]{"<none>:<none>"});

        ListImagesCmd mockListImagesCmd = mock(ListImagesCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listImagesCmd()).thenReturn(mockListImagesCmd);
        when(mockListImagesCmd.exec()).thenReturn(List.of(img1, img2));

        ListContainersCmd mockListContainersCmd = mock(ListContainersCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listContainersCmd()).thenReturn(mockListContainersCmd);
        Container container = mock(Container.class);
        when(container.getImageId()).thenReturn("sha256:aabbccddeeff001122334455");
        when(mockListContainersCmd.exec()).thenReturn(List.of(container));

        List<ImageUsageItem> result = dockerClientUtil.getImageDiskUsage();
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsUsed());
        assertEquals("aabbccddeeff", result.get(0).getId());
    }

    @Test
    void pruneCmd_null_returnsFailed() {
        assertEquals("failed", dockerClientUtil.pruneCmd(null).get("status"));
    }

    @Test
    void pruneCmd_empty_returnsFailed() {
        assertEquals("failed", dockerClientUtil.pruneCmd("").get("status"));
    }

    @Test
    void pruneCmd_invalidEnumValue_returnsFailed() {
        assertEquals("failed", dockerClientUtil.pruneCmd("INVALID_TYPE").get("status"));
    }

    @Test
    void pruneCmd_validNonImagesType_returnsSuccess() {
        setupDockerHost();
        PruneCmd mockPruneCmd = mock(PruneCmd.class);
        when(dockerClient.pruneCmd(PruneType.CONTAINERS)).thenReturn(mockPruneCmd);
        PruneResponse mockResp = mock(PruneResponse.class);
        when(mockResp.getSpaceReclaimed()).thenReturn(1024L);
        when(mockPruneCmd.exec()).thenReturn(mockResp);
        assertEquals("success", dockerClientUtil.pruneCmd("CONTAINERS").get("status"));
    }

    @Test
    void pruneCmd_imagesType_callsCleanupDanglingImagesAndReturnsSuccess() {
        setupDockerHost();
        PruneCmd mockPruneCmd = mock(PruneCmd.class);
        when(dockerClient.pruneCmd(PruneType.IMAGES)).thenReturn(mockPruneCmd);
        PruneResponse mockResp = mock(PruneResponse.class);
        when(mockResp.getSpaceReclaimed()).thenReturn(0L);
        when(mockPruneCmd.exec()).thenReturn(mockResp);

        // cleanupDanglingImages -> listImagesCmd().withDanglingFilter(true).exec()
        ListImagesCmd mockListCmd = mock(ListImagesCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listImagesCmd()).thenReturn(mockListCmd);
        when(mockListCmd.exec()).thenReturn(new ArrayList<>());

        assertEquals("success", dockerClientUtil.pruneCmd("IMAGES").get("status"));
        verify(mockListCmd).withDanglingFilter(true);
    }

    @Test
    void searchImagesOnHub_withResults_returnsMappedEntries() {
        setupDockerHost();
        SearchImagesCmd mockCmd = mock(SearchImagesCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.searchImagesCmd("ubuntu")).thenReturn(mockCmd);
        SearchItem mockItem = mock(SearchItem.class);
        when(mockItem.getName()).thenReturn("ubuntu");
        when(mockItem.getDescription()).thenReturn("Ubuntu Linux");
        when(mockItem.getStarCount()).thenReturn(10);
        when(mockCmd.exec()).thenReturn(List.of(mockItem));
        List<Map<String, Object>> results = dockerClientUtil.searchImagesOnHub("ubuntu", 5);
        assertEquals(1, results.size());
        assertEquals("ubuntu", results.get(0).get("name"));
    }

    @Test
    void searchImagesOnHub_exception_returnsEmptyList() {
        setupDockerHost();
        SearchImagesCmd mockCmd = mock(SearchImagesCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.searchImagesCmd("ubuntu")).thenReturn(mockCmd);
        when(mockCmd.exec()).thenThrow(new RuntimeException("search failed"));
        assertTrue(dockerClientUtil.searchImagesOnHub("ubuntu", 5).isEmpty());
    }

    // ─── Volume operations ──────────────────────────────────────────────────────

    @Test
    void listVolumed_callsListVolumesCmd() {
        setupDockerHost();
        ListVolumesCmd mockCmd = mock(ListVolumesCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listVolumesCmd()).thenReturn(mockCmd);
        ListVolumesResponse mockResp = mock(ListVolumesResponse.class);
        List<InspectVolumeResponse> volumes = List.of(mock(InspectVolumeResponse.class));
        when(mockResp.getVolumes()).thenReturn(volumes);
        when(mockCmd.exec()).thenReturn(mockResp);
        assertSame(volumes, dockerClientUtil.listVolumed());
    }

    @Test
    void searchVolumed_withNameFilter_setsFilter() {
        setupDockerHost();
        ListVolumesCmd mockCmd = mock(ListVolumesCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listVolumesCmd()).thenReturn(mockCmd);
        ListVolumesResponse mockResp = mock(ListVolumesResponse.class);
        when(mockResp.getVolumes()).thenReturn(new ArrayList<>());
        when(mockCmd.exec()).thenReturn(mockResp);
        VolumeQueryRequest criteria = new VolumeQueryRequest();
        criteria.setName("myvolume");
        dockerClientUtil.searchVolumed(criteria);
        verify(mockCmd).withFilter("name", Collections.singleton("myvolume"));
    }

    @Test
    void searchVolumed_withoutNameFilter_doesNotSetFilter() {
        setupDockerHost();
        ListVolumesCmd mockCmd = mock(ListVolumesCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listVolumesCmd()).thenReturn(mockCmd);
        ListVolumesResponse mockResp = mock(ListVolumesResponse.class);
        when(mockResp.getVolumes()).thenReturn(new ArrayList<>());
        when(mockCmd.exec()).thenReturn(mockResp);
        dockerClientUtil.searchVolumed(new VolumeQueryRequest());
        verify(mockCmd, never()).withFilter(anyString(), any());
    }

    @Test
    void inspectVolume_callsInspectVolumeCmd() {
        setupDockerHost();
        InspectVolumeCmd mockCmd = mock(InspectVolumeCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.inspectVolumeCmd("myvol")).thenReturn(mockCmd);
        InspectVolumeResponse mockResp = mock(InspectVolumeResponse.class);
        when(mockCmd.exec()).thenReturn(mockResp);
        assertSame(mockResp, dockerClientUtil.inspectVolume("myvol"));
    }

    @Test
    void isVolumeInUse_trueWhenContainerMountMatchesName() {
        setupDockerHost();
        ListContainersCmd mockCmd = mock(ListContainersCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listContainersCmd()).thenReturn(mockCmd);
        Container container = mock(Container.class);
        ContainerMount mount = mock(ContainerMount.class);
        when(mount.getName()).thenReturn("myvolume");
        when(container.getMounts()).thenReturn(List.of(mount));
        when(mockCmd.exec()).thenReturn(List.of(container));
        assertTrue(dockerClientUtil.isVolumeInUse("myvolume"));
    }

    @Test
    void isVolumeInUse_falseWhenNoMountMatchesName() {
        setupDockerHost();
        ListContainersCmd mockCmd = mock(ListContainersCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listContainersCmd()).thenReturn(mockCmd);
        Container container = mock(Container.class);
        ContainerMount mount = mock(ContainerMount.class);
        when(mount.getName()).thenReturn("othervolume");
        when(container.getMounts()).thenReturn(List.of(mount));
        when(mockCmd.exec()).thenReturn(List.of(container));
        assertFalse(dockerClientUtil.isVolumeInUse("myvolume"));
    }

    @Test
    void getVolumeContainers_returnsContainerInfoForMatchingMounts() {
        setupDockerHost();
        ListContainersCmd mockCmd = mock(ListContainersCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listContainersCmd()).thenReturn(mockCmd);
        Container container = mock(Container.class);
        when(container.getId()).thenReturn("ctr1");
        when(container.getNames()).thenReturn(new String[]{"/mycontainer"});
        ContainerMount mount = mock(ContainerMount.class);
        when(mount.getName()).thenReturn("myvol");
        when(mount.getDestination()).thenReturn("/data");
        when(mount.getRw()).thenReturn(true);
        when(container.getMounts()).thenReturn(List.of(mount));
        when(mockCmd.exec()).thenReturn(List.of(container));
        List<Map<String, String>> result = dockerClientUtil.getVolumeContainers("myvol");
        assertEquals(1, result.size());
        assertEquals("ctr1", result.get(0).get("containerId"));
        assertEquals("mycontainer", result.get(0).get("containerName"));
        assertEquals("rw", result.get(0).get("mode"));
    }

    @Test
    void getVolumeContainers_rwFalse_modeIsRo() {
        setupDockerHost();
        ListContainersCmd mockCmd = mock(ListContainersCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listContainersCmd()).thenReturn(mockCmd);
        Container container = mock(Container.class);
        when(container.getId()).thenReturn("ctr2");
        when(container.getNames()).thenReturn(new String[]{"/readonly-ctr"});
        ContainerMount mount = mock(ContainerMount.class);
        when(mount.getName()).thenReturn("myvol");
        when(mount.getDestination()).thenReturn("/data");
        when(mount.getRw()).thenReturn(false);
        when(container.getMounts()).thenReturn(List.of(mount));
        when(mockCmd.exec()).thenReturn(List.of(container));
        List<Map<String, String>> result = dockerClientUtil.getVolumeContainers("myvol");
        assertEquals("ro", result.get(0).get("mode"));
    }

    @Test
    void createVolume_success() {
        setupDockerHost();
        CreateVolumeCmd mockCmd = mock(CreateVolumeCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.createVolumeCmd()).thenReturn(mockCmd);
        VolumeCreateRequest req = new VolumeCreateRequest();
        req.setName("myvol");
        req.setDriver("local");
        assertEquals("success", dockerClientUtil.createVolume(req).get("status"));
    }

    @Test
    void createVolume_dockerException_returnsFailed() {
        setupDockerHost();
        CreateVolumeCmd mockCmd = mock(CreateVolumeCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.createVolumeCmd()).thenReturn(mockCmd);
        when(mockCmd.exec()).thenThrow(new RuntimeException("Create failed"));
        VolumeCreateRequest req = new VolumeCreateRequest();
        req.setName("myvol");
        req.setDriver("local");
        assertEquals("failed", dockerClientUtil.createVolume(req).get("status"));
    }

    @Test
    void removeVolume_whenVolumeIsInUse_returnsFailed() {
        setupDockerHost();
        RemoveVolumeCmd mockRemoveCmd = mock(RemoveVolumeCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.removeVolumeCmd("myvol")).thenReturn(mockRemoveCmd);
        ListContainersCmd mockListCmd = mock(ListContainersCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listContainersCmd()).thenReturn(mockListCmd);
        Container container = mock(Container.class);
        ContainerMount mount = mock(ContainerMount.class);
        when(mount.getName()).thenReturn("myvol");
        when(container.getMounts()).thenReturn(List.of(mount));
        when(mockListCmd.exec()).thenReturn(List.of(container));
        Map<String, Object> result = dockerClientUtil.removeVolume("myvol");
        assertEquals("failed", result.get("status"));
        assertTrue(result.get("message").toString().contains("in use"));
    }

    @Test
    void removeVolume_notInUse_success() {
        setupDockerHost();
        RemoveVolumeCmd mockRemoveCmd = mock(RemoveVolumeCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.removeVolumeCmd("myvol")).thenReturn(mockRemoveCmd);
        ListContainersCmd mockListCmd = mock(ListContainersCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listContainersCmd()).thenReturn(mockListCmd);
        when(mockListCmd.exec()).thenReturn(new ArrayList<>());
        assertEquals("success", dockerClientUtil.removeVolume("myvol").get("status"));
    }

    @Test
    void removeVolume_dockerException_returnsFailed() {
        setupDockerHost();
        RemoveVolumeCmd mockRemoveCmd = mock(RemoveVolumeCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.removeVolumeCmd("myvol")).thenReturn(mockRemoveCmd);
        ListContainersCmd mockListCmd = mock(ListContainersCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listContainersCmd()).thenReturn(mockListCmd);
        when(mockListCmd.exec()).thenReturn(new ArrayList<>());
        when(mockRemoveCmd.exec()).thenThrow(new RuntimeException("remove failed"));
        assertEquals("failed", dockerClientUtil.removeVolume("myvol").get("status"));
    }

    // ─── Network operations ─────────────────────────────────────────────────────

    @Test
    void listNetworks_withNameFilter_setsNameFilter() {
        setupDockerHost();
        ListNetworksCmd mockCmd = mock(ListNetworksCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listNetworksCmd()).thenReturn(mockCmd);
        when(mockCmd.exec()).thenReturn(new ArrayList<>());
        dockerClientUtil.listNetworks("bridge");
        verify(mockCmd).withNameFilter("bridge");
    }

    @Test
    void listNetworks_withoutNameFilter_doesNotCallWithNameFilter() {
        setupDockerHost();
        ListNetworksCmd mockCmd = mock(ListNetworksCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.listNetworksCmd()).thenReturn(mockCmd);
        when(mockCmd.exec()).thenReturn(new ArrayList<>());
        dockerClientUtil.listNetworks(null);
        verify(mockCmd, never()).withNameFilter(anyString());
    }

    @Test
    void inspectNetwork_callsInspectNetworkCmdWithId() {
        setupDockerHost();
        InspectNetworkCmd mockCmd = mock(InspectNetworkCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.inspectNetworkCmd()).thenReturn(mockCmd);
        Network mockNetwork = mock(Network.class);
        when(mockCmd.exec()).thenReturn(mockNetwork);
        assertSame(mockNetwork, dockerClientUtil.inspectNetwork("net1"));
        verify(mockCmd).withNetworkId("net1");
    }

    @Test
    void isNetworkInUse_trueWhenContainersMapNonEmpty() {
        setupDockerHost();
        InspectNetworkCmd mockCmd = mock(InspectNetworkCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.inspectNetworkCmd()).thenReturn(mockCmd);
        Network mockNetwork = mock(Network.class);
        when(mockNetwork.getContainers()).thenReturn(Map.of("ctr1", mock(Network.ContainerNetworkConfig.class)));
        when(mockCmd.exec()).thenReturn(mockNetwork);
        assertTrue(dockerClientUtil.isNetworkInUse("net1"));
    }

    @Test
    void isNetworkInUse_falseWhenContainersMapEmpty() {
        setupDockerHost();
        InspectNetworkCmd mockCmd = mock(InspectNetworkCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.inspectNetworkCmd()).thenReturn(mockCmd);
        Network mockNetwork = mock(Network.class);
        when(mockNetwork.getContainers()).thenReturn(Map.of());
        when(mockCmd.exec()).thenReturn(mockNetwork);
        assertFalse(dockerClientUtil.isNetworkInUse("net1"));
    }

    @Test
    void createNetwork_success() {
        setupDockerHost();
        CreateNetworkCmd mockCmd = mock(CreateNetworkCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.createNetworkCmd()).thenReturn(mockCmd);
        NetworkCreateRequest req = new NetworkCreateRequest();
        req.setName("mynet");
        req.setDriver("bridge");
        assertEquals("success", dockerClientUtil.createNetwork(req).get("status"));
    }

    @Test
    void createNetwork_withIPv4Config_setsIpam() {
        setupDockerHost();
        CreateNetworkCmd mockCmd = mock(CreateNetworkCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.createNetworkCmd()).thenReturn(mockCmd);
        NetworkCreateRequest req = new NetworkCreateRequest();
        req.setName("mynet");
        req.setDriver("bridge");
        req.setEnableIpv4(true);
        req.setSubnet("192.168.1.0/24");
        req.setGateway("192.168.1.1");
        assertEquals("success", dockerClientUtil.createNetwork(req).get("status"));
        verify(mockCmd).withIpam(any(Network.Ipam.class));
    }

    @Test
    void createNetwork_withIPv6Config_setsIpam() {
        setupDockerHost();
        CreateNetworkCmd mockCmd = mock(CreateNetworkCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.createNetworkCmd()).thenReturn(mockCmd);
        NetworkCreateRequest req = new NetworkCreateRequest();
        req.setName("mynet");
        req.setDriver("bridge");
        req.setEnableIpv6(true);
        req.setSubnetV6("fd00::/64");
        assertEquals("success", dockerClientUtil.createNetwork(req).get("status"));
        verify(mockCmd).withIpam(any(Network.Ipam.class));
    }

    @Test
    void createNetwork_dockerException_returnsFailed() {
        setupDockerHost();
        CreateNetworkCmd mockCmd = mock(CreateNetworkCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.createNetworkCmd()).thenReturn(mockCmd);
        when(mockCmd.exec()).thenThrow(new RuntimeException("Network error"));
        NetworkCreateRequest req = new NetworkCreateRequest();
        req.setName("mynet");
        req.setDriver("bridge");
        assertEquals("failed", dockerClientUtil.createNetwork(req).get("status"));
    }

    @Test
    void removeNetwork_whenInUse_returnsFailed() {
        setupDockerHost();
        RemoveNetworkCmd mockRemoveCmd = mock(RemoveNetworkCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.removeNetworkCmd("net1")).thenReturn(mockRemoveCmd);
        InspectNetworkCmd mockInspectCmd = mock(InspectNetworkCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.inspectNetworkCmd()).thenReturn(mockInspectCmd);
        Network mockNetwork = mock(Network.class);
        when(mockNetwork.getContainers()).thenReturn(Map.of("ctr1", mock(Network.ContainerNetworkConfig.class)));
        when(mockInspectCmd.exec()).thenReturn(mockNetwork);
        Map<String, Object> result = dockerClientUtil.removeNetwork("net1");
        assertEquals("failed", result.get("status"));
        assertTrue(result.get("message").toString().contains("in use"));
    }

    @Test
    void removeNetwork_success() {
        setupDockerHost();
        RemoveNetworkCmd mockRemoveCmd = mock(RemoveNetworkCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.removeNetworkCmd("net1")).thenReturn(mockRemoveCmd);
        InspectNetworkCmd mockInspectCmd = mock(InspectNetworkCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.inspectNetworkCmd()).thenReturn(mockInspectCmd);
        Network mockNetwork = mock(Network.class);
        when(mockNetwork.getContainers()).thenReturn(Map.of());
        when(mockInspectCmd.exec()).thenReturn(mockNetwork);
        assertEquals("success", dockerClientUtil.removeNetwork("net1").get("status"));
    }

    @Test
    void removeNetwork_dockerException_returnsFailed() {
        setupDockerHost();
        RemoveNetworkCmd mockRemoveCmd = mock(RemoveNetworkCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.removeNetworkCmd("net1")).thenReturn(mockRemoveCmd);
        InspectNetworkCmd mockInspectCmd = mock(InspectNetworkCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.inspectNetworkCmd()).thenReturn(mockInspectCmd);
        Network mockNetwork = mock(Network.class);
        when(mockNetwork.getContainers()).thenReturn(Map.of());
        when(mockInspectCmd.exec()).thenReturn(mockNetwork);
        when(mockRemoveCmd.exec()).thenThrow(new RuntimeException("remove error"));
        assertEquals("failed", dockerClientUtil.removeNetwork("net1").get("status"));
    }

    @Test
    void connectNetwork_success() {
        setupDockerHost();
        ConnectToNetworkCmd mockCmd = mock(ConnectToNetworkCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.connectToNetworkCmd()).thenReturn(mockCmd);
        Map<String, Object> result = dockerClientUtil.connectNetwork("net1", "ctr1");
        assertEquals("success", result.get("status"));
        verify(mockCmd).withNetworkId("net1");
        verify(mockCmd).withContainerId("ctr1");
    }

    @Test
    void connectNetwork_failure_returnsFailedMap() {
        setupDockerHost();
        ConnectToNetworkCmd mockCmd = mock(ConnectToNetworkCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.connectToNetworkCmd()).thenReturn(mockCmd);
        when(mockCmd.exec()).thenThrow(new RuntimeException("connect failed"));
        assertEquals("failed", dockerClientUtil.connectNetwork("net1", "ctr1").get("status"));
    }

    @Test
    void disconnectNetwork_success() {
        setupDockerHost();
        DisconnectFromNetworkCmd mockCmd = mock(DisconnectFromNetworkCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.disconnectFromNetworkCmd()).thenReturn(mockCmd);
        assertEquals("success", dockerClientUtil.disconnectNetwork("net1", "ctr1").get("status"));
    }

    @Test
    void disconnectNetwork_failure_returnsFailedMap() {
        setupDockerHost();
        DisconnectFromNetworkCmd mockCmd = mock(DisconnectFromNetworkCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.disconnectFromNetworkCmd()).thenReturn(mockCmd);
        when(mockCmd.exec()).thenThrow(new RuntimeException("disconnect failed"));
        assertEquals("failed", dockerClientUtil.disconnectNetwork("net1", "ctr1").get("status"));
    }

    // ─── Container file operations (path validation) ────────────────────────────

    @Test
    void listContainerFiles_invalidPath_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> dockerClientUtil.listContainerFiles("ctr1", "/path;rm -rf /"));
    }

    @Test
    void readContainerFileContent_blankPath_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> dockerClientUtil.readContainerFileContent("ctr1", "", "UTF-8"));
    }

    @Test
    void readContainerFileContent_pathWithDotDot_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> dockerClientUtil.readContainerFileContent("ctr1", "/etc/../etc/passwd", "UTF-8"));
    }

    @Test
    void writeContainerFileContent_blankPath_returnsFailedMap() {
        Map<String, Object> result = dockerClientUtil.writeContainerFileContent("ctr1", "", "content", "UTF-8");
        assertEquals("failed", result.get("status"));
        assertTrue(result.get("message").toString().contains("blank"));
    }

    @Test
    void writeContainerFileContent_invalidPath_returnsFailedMap() {
        Map<String, Object> result = dockerClientUtil.writeContainerFileContent("ctr1", "/path;rm", "content", "UTF-8");
        assertEquals("failed", result.get("status"));
        assertTrue(result.get("message").toString().contains("Invalid"));
    }

    // ─── Bulk / resource operations ─────────────────────────────────────────────

    @Test
    void bulkOperateContainers_startOperation_success() {
        setupDockerHost();
        StartContainerCmd mockCmd = mock(StartContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.startContainerCmd("ctr1")).thenReturn(mockCmd);
        BulkContainerOperationRequest req = new BulkContainerOperationRequest();
        req.setContainerIds(List.of("ctr1"));
        req.setOperation("start");
        List<Map<String, Object>> results = dockerClientUtil.bulkOperateContainers(req);
        assertEquals(1, results.size());
        assertEquals("success", results.get(0).get("status"));
    }

    @Test
    void bulkOperateContainers_stopOperation_success() {
        setupDockerHost();
        StopContainerCmd mockCmd = mock(StopContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.stopContainerCmd("ctr1")).thenReturn(mockCmd);
        BulkContainerOperationRequest req = new BulkContainerOperationRequest();
        req.setContainerIds(List.of("ctr1"));
        req.setOperation("stop");
        assertEquals("success", dockerClientUtil.bulkOperateContainers(req).get(0).get("status"));
    }

    @Test
    void bulkOperateContainers_restartOperation_success() {
        setupDockerHost();
        RestartContainerCmd mockCmd = mock(RestartContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.restartContainerCmd("ctr1")).thenReturn(mockCmd);
        BulkContainerOperationRequest req = new BulkContainerOperationRequest();
        req.setContainerIds(List.of("ctr1"));
        req.setOperation("restart");
        assertEquals("success", dockerClientUtil.bulkOperateContainers(req).get(0).get("status"));
    }

    @Test
    void bulkOperateContainers_killOperation_success() {
        setupDockerHost();
        KillContainerCmd mockCmd = mock(KillContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.killContainerCmd("ctr1")).thenReturn(mockCmd);
        BulkContainerOperationRequest req = new BulkContainerOperationRequest();
        req.setContainerIds(List.of("ctr1"));
        req.setOperation("kill");
        assertEquals("success", dockerClientUtil.bulkOperateContainers(req).get(0).get("status"));
    }

    @Test
    void bulkOperateContainers_pauseOperation_success() {
        setupDockerHost();
        PauseContainerCmd mockCmd = mock(PauseContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.pauseContainerCmd("ctr1")).thenReturn(mockCmd);
        BulkContainerOperationRequest req = new BulkContainerOperationRequest();
        req.setContainerIds(List.of("ctr1"));
        req.setOperation("pause");
        assertEquals("success", dockerClientUtil.bulkOperateContainers(req).get(0).get("status"));
    }

    @Test
    void bulkOperateContainers_unpauseOperation_success() {
        setupDockerHost();
        UnpauseContainerCmd mockCmd = mock(UnpauseContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.unpauseContainerCmd("ctr1")).thenReturn(mockCmd);
        BulkContainerOperationRequest req = new BulkContainerOperationRequest();
        req.setContainerIds(List.of("ctr1"));
        req.setOperation("unpause");
        assertEquals("success", dockerClientUtil.bulkOperateContainers(req).get(0).get("status"));
    }

    @Test
    void bulkOperateContainers_removeWithForceFalse_usesRemoveContainer() {
        setupDockerHost();
        RemoveContainerCmd mockCmd = mock(RemoveContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.removeContainerCmd("ctr1")).thenReturn(mockCmd);
        BulkContainerOperationRequest req = new BulkContainerOperationRequest();
        req.setContainerIds(List.of("ctr1"));
        req.setOperation("remove");
        req.setForce(false);
        assertEquals("success", dockerClientUtil.bulkOperateContainers(req).get(0).get("status"));
        verify(mockCmd, never()).withForce(true);
    }

    @Test
    void bulkOperateContainers_removeWithForceTrue_usesRemoveContainerForce() {
        setupDockerHost();
        RemoveContainerCmd mockCmd = mock(RemoveContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.removeContainerCmd("ctr1")).thenReturn(mockCmd);
        BulkContainerOperationRequest req = new BulkContainerOperationRequest();
        req.setContainerIds(List.of("ctr1"));
        req.setOperation("remove");
        req.setForce(true);
        assertEquals("success", dockerClientUtil.bulkOperateContainers(req).get(0).get("status"));
        verify(mockCmd).withForce(true);
    }

    @Test
    void bulkOperateContainers_unknownOperation_returnsFailed() {
        BulkContainerOperationRequest req = new BulkContainerOperationRequest();
        req.setContainerIds(List.of("ctr1"));
        req.setOperation("teleport");
        List<Map<String, Object>> results = dockerClientUtil.bulkOperateContainers(req);
        assertEquals(1, results.size());
        assertEquals("failed", results.get(0).get("status"));
    }

    @Test
    void updateContainerResources_successWithAllFields() {
        setupDockerHost();
        UpdateContainerCmd mockCmd = mock(UpdateContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.updateContainerCmd("ctr1")).thenReturn(mockCmd);
        ContainerResourceUpdateRequest req = new ContainerResourceUpdateRequest();
        req.setContainerId("ctr1");
        req.setCpuShares(1024);
        req.setCpuQuota(50000L);
        req.setCpuPeriod(100000L);
        req.setMemory(536870912L);
        req.setMemorySwap(1073741824L);
        req.setMemoryReservation(268435456L);
        req.setBlkioWeight(500);
        assertEquals("success", dockerClientUtil.updateContainerResources(req).get("status"));
    }

    @Test
    void updateContainerResources_dockerException_returnsFailed() {
        setupDockerHost();
        UpdateContainerCmd mockCmd = mock(UpdateContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.updateContainerCmd("ctr1")).thenReturn(mockCmd);
        when(mockCmd.exec()).thenThrow(new RuntimeException("update failed"));
        ContainerResourceUpdateRequest req = new ContainerResourceUpdateRequest();
        req.setContainerId("ctr1");
        assertEquals("failed", dockerClientUtil.updateContainerResources(req).get("status"));
    }

    @Test
    void getContainerDiff_successWithAllKindLabels() {
        setupDockerHost();
        ContainerDiffCmd mockCmd = mock(ContainerDiffCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.containerDiffCmd("ctr1")).thenReturn(mockCmd);
        ChangeLog mod = mock(ChangeLog.class); when(mod.getPath()).thenReturn("/etc/hosts"); when(mod.getKind()).thenReturn(0);
        ChangeLog add = mock(ChangeLog.class); when(add.getPath()).thenReturn("/tmp/new");   when(add.getKind()).thenReturn(1);
        ChangeLog del = mock(ChangeLog.class); when(del.getPath()).thenReturn("/tmp/old");   when(del.getKind()).thenReturn(2);
        ChangeLog unk = mock(ChangeLog.class); when(unk.getPath()).thenReturn("/tmp/unk");   when(unk.getKind()).thenReturn(null);
        when(mockCmd.exec()).thenReturn(List.of(mod, add, del, unk));
        List<Map<String, Object>> result = dockerClientUtil.getContainerDiff("ctr1");
        assertEquals(4, result.size());
        assertEquals("Modified", result.get(0).get("kindLabel"));
        assertEquals("Added",    result.get(1).get("kindLabel"));
        assertEquals("Deleted",  result.get(2).get("kindLabel"));
        assertEquals("Unknown",  result.get(3).get("kindLabel"));
    }

    @Test
    void getContainerDiff_nullListReturnsEmpty() {
        setupDockerHost();
        ContainerDiffCmd mockCmd = mock(ContainerDiffCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.containerDiffCmd("ctr1")).thenReturn(mockCmd);
        when(mockCmd.exec()).thenReturn(null);
        assertTrue(dockerClientUtil.getContainerDiff("ctr1").isEmpty());
    }

    @Test
    void getContainerDiff_exceptionThrowsRuntimeException() {
        setupDockerHost();
        ContainerDiffCmd mockCmd = mock(ContainerDiffCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.containerDiffCmd("ctr1")).thenReturn(mockCmd);
        when(mockCmd.exec()).thenThrow(new RuntimeException("diff failed"));
        assertThrows(RuntimeException.class, () -> dockerClientUtil.getContainerDiff("ctr1"));
    }

    // ─── createContainer ────────────────────────────────────────────────────────

    @Test
    void createContainer_noImageName_returnsFailed() {
        ContainerCreateRequest req = new ContainerCreateRequest();
        req.setImage(null);
        assertEquals("failed", dockerClientUtil.createContainer(req).get("status"));
    }

    @Test
    void createContainer_invalidContainerName_returnsFailed() {
        ContainerCreateRequest req = new ContainerCreateRequest();
        req.setImage("ubuntu");
        req.setName("@invalid-name");
        assertEquals("failed", dockerClientUtil.createContainer(req).get("status"));
    }

    @Test
    void createContainer_success() {
        setupDockerHost();
        CreateContainerCmd mockCreateCmd = mock(CreateContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.createContainerCmd("ubuntu")).thenReturn(mockCreateCmd);
        CreateContainerResponse mockResp = mock(CreateContainerResponse.class);
        when(mockResp.getId()).thenReturn("new-ctr-id");
        when(mockCreateCmd.exec()).thenReturn(mockResp);
        StartContainerCmd mockStartCmd = mock(StartContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.startContainerCmd("new-ctr-id")).thenReturn(mockStartCmd);
        ContainerCreateRequest req = new ContainerCreateRequest();
        req.setImage("ubuntu");
        Map<String, Object> result = dockerClientUtil.createContainer(req);
        assertEquals("success", result.get("status"));
        assertEquals("new-ctr-id", result.get("containerId"));
    }

    // ─── loadStatus bug fix ──────────────────────────────────────────────────────

    @Test
    void loadStatus_emptyContainerList_allStateKeysPresentWithZeroCounts() {
        setupDockerHost();
        setupLoadStatusMocks(new ArrayList<>());
        Map<String, Object> result = dockerClientUtil.loadStatus();
        // All state keys must be present even when the containers list is empty
        assertTrue(result.containsKey("created"),    "missing key: created");
        assertTrue(result.containsKey("running"),    "missing key: running");
        assertTrue(result.containsKey("paused"),     "missing key: paused");
        assertTrue(result.containsKey("stopped"),    "missing key: stopped");
        assertTrue(result.containsKey("exited"),     "missing key: exited");
        assertTrue(result.containsKey("restarting"), "missing key: restarting");
        assertTrue(result.containsKey("removing"),   "missing key: removing");
        assertTrue(result.containsKey("dead"),       "missing key: dead");
        assertEquals(0L, result.get("created"));
        assertEquals(0L, result.get("running"));
    }

    @Test
    void loadStatus_containersWithVariousStates_countsAreCorrect() {
        setupDockerHost();
        Container running1 = mock(Container.class);
        when(running1.getState()).thenReturn("running");
        Container running2 = mock(Container.class);
        when(running2.getState()).thenReturn("running");
        Container exited = mock(Container.class);
        when(exited.getState()).thenReturn("exited");
        Container created = mock(Container.class);
        when(created.getState()).thenReturn("created");
        Container dead = mock(Container.class);
        when(dead.getState()).thenReturn("dead");
        setupLoadStatusMocks(List.of(running1, running2, exited, created, dead));
        Map<String, Object> result = dockerClientUtil.loadStatus();
        assertEquals(2L, result.get("running"));
        assertEquals(1L, result.get("exited"));
        assertEquals(1L, result.get("created"));
        assertEquals(1L, result.get("dead"));
        assertEquals(0L, result.get("paused"));
        assertEquals(5,  result.get("containerCount"));
    }

    // ─── Private methods via ReflectionTestUtils.invokeMethod ───────────────────

    // extractRegistryUrl

    @Test
    void extractRegistryUrl_officialSingleName_returnsNull() {
        assertNull(ReflectionTestUtils.invokeMethod(dockerClientUtil, "extractRegistryUrl", "ubuntu"));
    }

    @Test
    void extractRegistryUrl_taggedOfficialImage_returnsNull() {
        assertNull(ReflectionTestUtils.invokeMethod(dockerClientUtil, "extractRegistryUrl", "ubuntu:20.04"));
    }

    @Test
    void extractRegistryUrl_dockerHubUserNamespace_returnsNull() {
        assertNull(ReflectionTestUtils.invokeMethod(dockerClientUtil, "extractRegistryUrl", "user/repo"));
    }

    @Test
    void extractRegistryUrl_dockerHubUserNamespaceWithTag_returnsNull() {
        assertNull(ReflectionTestUtils.invokeMethod(dockerClientUtil, "extractRegistryUrl", "user/repo:tag"));
    }

    @Test
    void extractRegistryUrl_privateRegistryWithDot_returnsHttps() {
        assertEquals("https://myregistry.com",
                ReflectionTestUtils.invokeMethod(dockerClientUtil, "extractRegistryUrl",
                        "myregistry.com/myimage"));
    }

    @Test
    void extractRegistryUrl_privateRegistryWithPort_returnsHttpsWithPort() {
        assertEquals("https://myregistry.com:5000",
                ReflectionTestUtils.invokeMethod(dockerClientUtil, "extractRegistryUrl",
                        "myregistry.com:5000/myimage:tag"));
    }

    @Test
    void extractRegistryUrl_privateRegistryWithNamespace_returnsHttps() {
        assertEquals("https://myregistry.com",
                ReflectionTestUtils.invokeMethod(dockerClientUtil, "extractRegistryUrl",
                        "myregistry.com/namespace/myimage"));
    }

    @Test
    void extractRegistryUrl_null_returnsNull() {
        assertNull(ReflectionTestUtils.invokeMethod(dockerClientUtil, "extractRegistryUrl", (Object) null));
    }

    @Test
    void extractRegistryUrl_empty_returnsNull() {
        assertNull(ReflectionTestUtils.invokeMethod(dockerClientUtil, "extractRegistryUrl", ""));
    }

    // isValidContainerName

    @Test
    void isValidContainerName_validNames_returnsTrue() {
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(dockerClientUtil, "isValidContainerName", "mycontainer"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(dockerClientUtil, "isValidContainerName", "my-container"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(dockerClientUtil, "isValidContainerName", "my_container.1"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(dockerClientUtil, "isValidContainerName", "A1"));
    }

    @Test
    void isValidContainerName_invalidNames_returnsFalse() {
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(dockerClientUtil, "isValidContainerName", ""));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(dockerClientUtil, "isValidContainerName", (Object) null));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(dockerClientUtil, "isValidContainerName", "@invalid"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(dockerClientUtil, "isValidContainerName", "-invalid"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(dockerClientUtil, "isValidContainerName", "_invalid"));
    }

    // parseRestartPolicy

    @Test
    void parseRestartPolicy_no_returnsNoRestart() {
        RestartPolicyRequest rp = new RestartPolicyRequest();
        rp.setName("no");
        RestartPolicy result = ReflectionTestUtils.invokeMethod(dockerClientUtil, "parseRestartPolicy", rp);
        // RestartPolicy.noRestart() in docker-java stores name as "" (empty string)
        assertNotNull(result);
        assertEquals(RestartPolicy.noRestart(), result);
    }

    @Test
    void parseRestartPolicy_always_returnsAlwaysRestart() {
        RestartPolicyRequest rp = new RestartPolicyRequest();
        rp.setName("always");
        RestartPolicy result = ReflectionTestUtils.invokeMethod(dockerClientUtil, "parseRestartPolicy", rp);
        assertNotNull(result);
        assertEquals("always", result.getName());
    }

    @Test
    void parseRestartPolicy_unlessStopped_returnsUnlessStoppedRestart() {
        RestartPolicyRequest rp = new RestartPolicyRequest();
        rp.setName("unless-stopped");
        RestartPolicy result = ReflectionTestUtils.invokeMethod(dockerClientUtil, "parseRestartPolicy", rp);
        assertNotNull(result);
        assertEquals("unless-stopped", result.getName());
    }

    @Test
    void parseRestartPolicy_onFailureWithNullRetries_usesZero() {
        RestartPolicyRequest rp = new RestartPolicyRequest();
        rp.setName("on-failure");
        rp.setMaximumRetryCount(null);
        RestartPolicy result = ReflectionTestUtils.invokeMethod(dockerClientUtil, "parseRestartPolicy", rp);
        assertNotNull(result);
        assertEquals("on-failure", result.getName());
        assertEquals(0, result.getMaximumRetryCount());
    }

    @Test
    void parseRestartPolicy_onFailureWithRetryCount_usesCount() {
        RestartPolicyRequest rp = new RestartPolicyRequest();
        rp.setName("on-failure");
        rp.setMaximumRetryCount(3);
        RestartPolicy result = ReflectionTestUtils.invokeMethod(dockerClientUtil, "parseRestartPolicy", rp);
        assertNotNull(result);
        assertEquals(3, result.getMaximumRetryCount());
    }

    @Test
    void parseRestartPolicy_unknown_returnsNull() {
        RestartPolicyRequest rp = new RestartPolicyRequest();
        rp.setName("unknown-policy");
        RestartPolicy result = ReflectionTestUtils.invokeMethod(dockerClientUtil, "parseRestartPolicy", rp);
        assertNull(result);
    }

    // parseLsLine

    @Test
    void parseLsLine_regularFileLine_returnsFileType() {
        Map<String, Object> entry = ReflectionTestUtils.invokeMethod(dockerClientUtil, "parseLsLine",
                "-rw-r--r-- 1 root root 1024 Jan  1 12:00 file.txt");
        assertNotNull(entry);
        assertEquals("file", entry.get("type"));
        assertEquals("file.txt", entry.get("name"));
    }

    @Test
    void parseLsLine_directoryLine_returnsDirectoryType() {
        Map<String, Object> entry = ReflectionTestUtils.invokeMethod(dockerClientUtil, "parseLsLine",
                "drwxr-xr-x 2 root root 4096 Jan  1 12:00 mydir");
        assertNotNull(entry);
        assertEquals("directory", entry.get("type"));
        assertEquals("mydir", entry.get("name"));
    }

    @Test
    void parseLsLine_symlinkLine_returnsSymlinkWithLinkTarget() {
        Map<String, Object> entry = ReflectionTestUtils.invokeMethod(dockerClientUtil, "parseLsLine",
                "lrwxrwxrwx 1 root root 7 Jan  1 12:00 link -> /target/path");
        assertNotNull(entry);
        assertEquals("symlink", entry.get("type"));
        assertEquals("link", entry.get("name"));
        assertEquals("/target/path", entry.get("linkTarget"));
    }

    @Test
    void parseLsLine_dotEntry_returnsNull() {
        assertNull(ReflectionTestUtils.invokeMethod(dockerClientUtil, "parseLsLine",
                "drwxr-xr-x 2 root root 4096 Jan  1 12:00 ."));
    }

    @Test
    void parseLsLine_dotDotEntry_returnsNull() {
        assertNull(ReflectionTestUtils.invokeMethod(dockerClientUtil, "parseLsLine",
                "drwxr-xr-x 2 root root 4096 Jan  1 12:00 .."));
    }

    @Test
    void parseLsLine_fewerThanEightParts_returnsNull() {
        assertNull(ReflectionTestUtils.invokeMethod(dockerClientUtil, "parseLsLine",
                "-rw-r--r-- 1 root root 1024 Jan"));
    }

    // extractBaseImageFromDockerfile

    @Test
    void extractBaseImageFromDockerfile_singleFrom_returnsSingleImage() {
        List<String> result = ReflectionTestUtils.invokeMethod(dockerClientUtil,
                "extractBaseImageFromDockerfile", "FROM ubuntu:20.04\nRUN apt-get update");
        assertEquals(List.of("ubuntu:20.04"), result);
    }

    @Test
    void extractBaseImageFromDockerfile_multiStage_returnsAllBaseImages() {
        String content = "FROM ubuntu:20.04 AS builder\nRUN make\nFROM alpine:3.18\nCOPY --from=builder /app .";
        List<String> result = ReflectionTestUtils.invokeMethod(dockerClientUtil,
                "extractBaseImageFromDockerfile", content);
        assertEquals(List.of("ubuntu:20.04", "alpine:3.18"), result);
    }

    @Test
    void extractBaseImageFromDockerfile_commentsAndBlankLinesIgnored() {
        String content = "# This is a comment\n\nFROM node:18\n# Another comment";
        List<String> result = ReflectionTestUtils.invokeMethod(dockerClientUtil,
                "extractBaseImageFromDockerfile", content);
        assertEquals(List.of("node:18"), result);
    }

    @Test
    void extractBaseImageFromDockerfile_nullInput_returnsEmptyList() {
        List<String> result = ReflectionTestUtils.invokeMethod(dockerClientUtil,
                "extractBaseImageFromDockerfile", (Object) null);
        assertTrue(result.isEmpty());
    }

    @Test
    void extractBaseImageFromDockerfile_blankInput_returnsEmptyList() {
        List<String> result = ReflectionTestUtils.invokeMethod(dockerClientUtil,
                "extractBaseImageFromDockerfile", "  ");
        assertTrue(result.isEmpty());
    }

    // createExposedPort

    @Test
    void createExposedPort_tcp_returnsTcpPort() {
        ExposedPort result = ReflectionTestUtils.invokeMethod(dockerClientUtil,
                "createExposedPort", 80, "tcp");
        assertEquals(ExposedPort.tcp(80), result);
    }

    @Test
    void createExposedPort_udp_returnsUdpPort() {
        ExposedPort result = ReflectionTestUtils.invokeMethod(dockerClientUtil,
                "createExposedPort", 53, "udp");
        assertEquals(ExposedPort.udp(53), result);
    }

    @Test
    void createExposedPort_sctp_returnsSctpPort() {
        ExposedPort result = ReflectionTestUtils.invokeMethod(dockerClientUtil,
                "createExposedPort", 9000, "sctp");
        assertEquals(ExposedPort.sctp(9000), result);
    }

    @Test
    void createExposedPort_null_defaultsToTcp() {
        ExposedPort result = ReflectionTestUtils.invokeMethod(dockerClientUtil,
                "createExposedPort", 8080, (String) null);
        assertEquals(ExposedPort.tcp(8080), result);
    }

    // ─── getCurrentDockerHttpClient ────────────────────────────────────────────

    @Test
    void getCurrentDockerHttpClient_withoutHost_throwsIllegalStateException() {
        assertThrows(IllegalStateException.class, dockerClientUtil::getCurrentDockerHttpClient);
    }

    // ─── getContainerStats ─────────────────────────────────────────────────────

    @Test
    void getContainerStats_callsStatsCmdAndReturnJson() {
        setupDockerHost();
        StatsCmd statsCmd = mock(StatsCmd.class);
        when(dockerClient.statsCmd("ctr1")).thenReturn(statsCmd);
        when(statsCmd.withNoStream(true)).thenReturn(statsCmd);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            InvocationBuilder.AsyncResultCallback<Statistics> cb = invocation.getArgument(0);
            cb.onNext(new Statistics());
            return cb;
        }).when(statsCmd).exec(any());

        JSONObject result = dockerClientUtil.getContainerStats("ctr1", true);
        assertNotNull(result);
        verify(dockerClient).statsCmd("ctr1");
    }

    // ─── getAuthConfigForImage (private, via reflection) ──────────────────────

    @Test
    void getAuthConfigForImage_noRegistry_returnsNull() {
        when(registryMapper.selectList(any())).thenReturn(Collections.emptyList());

        AuthConfig result = ReflectionTestUtils.invokeMethod(
                dockerClientUtil, "getAuthConfigForImage", "ubuntu:latest");
        assertNull(result);
    }

    @Test
    void getAuthConfigForImage_registryAuthFalse_returnsNull() {
        Registry registry = new Registry();
        registry.setUrl("https://docker.io");
        registry.setAuth(false);
        when(registryMapper.selectList(any())).thenReturn(List.of(registry));

        AuthConfig result = ReflectionTestUtils.invokeMethod(
                dockerClientUtil, "getAuthConfigForImage", "ubuntu:latest");
        assertNull(result);
    }

    @Test
    void getAuthConfigForImage_withAuthRegistry_returnsAuthConfig() {
        Registry registry = new Registry();
        registry.setUrl("https://myregistry.com");
        registry.setUsername("user");
        registry.setPassword("encrypted");
        registry.setAuth(true);
        when(registryMapper.selectList(any())).thenReturn(List.of(registry));
        when(passwordUtil.decrypt("encrypted")).thenReturn("plaintext");

        AuthConfig result = ReflectionTestUtils.invokeMethod(
                dockerClientUtil, "getAuthConfigForImage", "myregistry.com/myimage:tag");
        assertNotNull(result);
        assertEquals("user", result.getUsername());
        assertEquals("plaintext", result.getPassword());
    }

    @Test
    void getAuthConfigForImage_mapperThrows_returnsNull() {
        when(registryMapper.selectList(any())).thenThrow(new RuntimeException("DB error"));

        AuthConfig result = ReflectionTestUtils.invokeMethod(
                dockerClientUtil, "getAuthConfigForImage", "ubuntu:latest");
        assertNull(result);
    }

    // ─── pullImageIfNotExists ──────────────────────────────────────────────────

    @Test
    void pullImageIfNotExists_imageExists_setsTaskSuccessWithoutPull() throws InterruptedException {
        setupDockerHost();
        InspectImageCmd inspectCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("ubuntu:latest")).thenReturn(inspectCmd);
        // exec() doesn't throw → image exists

        Map<String, String> result = dockerClientUtil.pullImageIfNotExists("ubuntu:latest", true);

        assertNotNull(result.get("taskId"));
        assertEquals("ubuntu:latest", result.get("imageName"));
        verify(dockerClient, never()).pullImageCmd(anyString());
        verify(imagePullTaskManager).addTask(anyString(), any());
    }

    @Test
    void pullImageIfNotExists_checkExistsFalse_startsPullAsync() throws InterruptedException {
        setupDockerHost();

        Map<String, String> result = dockerClientUtil.pullImageIfNotExists("ubuntu:latest", false);

        assertNotNull(result.get("taskId"));
        assertEquals("ubuntu:latest", result.get("imageName"));
        verify(imagePullTaskManager).addTask(anyString(), any());
    }

    // ─── pushImage ─────────────────────────────────────────────────────────────

    @Test
    void pushImage_returnsTaskIdAndStartsAsyncPush() {
        setupDockerHost();

        Map<String, String> result = dockerClientUtil.pushImage("myrepo/image:tag");

        assertNotNull(result.get("taskId"));
        assertEquals("myrepo/image:tag", result.get("imageName"));
        verify(imagePushTaskManager).addTask(anyString(), any());
    }

    // ─── doPullImage ───────────────────────────────────────────────────────────

    @Test
    void doPullImage_imageExists_skipsPull() {
        InspectImageCmd inspectCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("ubuntu:latest")).thenReturn(inspectCmd);
        // exec() doesn't throw → image exists

        ImageBuildTask task = new ImageBuildTask();
        dockerClientUtil.doPullImage(dockerClient, task, "ubuntu:latest");

        verify(dockerClient, never()).pullImageCmd(anyString());
    }

    @Test
    void doPullImage_imageNotExists_pullsImage() throws InterruptedException {
        when(registryMapper.selectList(any())).thenReturn(Collections.emptyList());
        InspectImageCmd inspectCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("ubuntu:latest")).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenThrow(new NotFoundException("not found"));

        PullImageCmd pullCmd = mock(PullImageCmd.class);
        when(dockerClient.pullImageCmd("ubuntu:latest")).thenReturn(pullCmd);
        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            com.github.dockerjava.api.async.ResultCallback<PullResponseItem> cb = inv.getArgument(0);
            cb.onComplete();
            return cb;
        }).when(pullCmd).exec(any());

        ImageBuildTask task = new ImageBuildTask();
        assertDoesNotThrow(() -> dockerClientUtil.doPullImage(dockerClient, task, "ubuntu:latest"));
        verify(dockerClient).pullImageCmd("ubuntu:latest");
    }

    // ─── execCommand ───────────────────────────────────────────────────────────

    @Test
    void execCommand_success() throws InterruptedException {
        ContainerExecRequest request = new ContainerExecRequest();
        request.setHost("tcp://docker");
        request.setContainerId("ctr1");
        request.setCommand(new String[]{"ls", "-la"});

        when(connectionManager.checkConnectionHealth("tcp://docker")).thenReturn(true);
        when(connectionManager.getDockerClient("tcp://docker")).thenReturn(dockerClient);

        ExecCreateCmd execCreateCmd = mock(ExecCreateCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.execCreateCmd("ctr1")).thenReturn(execCreateCmd);
        ExecCreateCmdResponse execCreateResponse = mock(ExecCreateCmdResponse.class);
        when(execCreateCmd.exec()).thenReturn(execCreateResponse);
        when(execCreateResponse.getId()).thenReturn("exec-123");

        ExecStartCmd execStartCmd = mock(ExecStartCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.execStartCmd("exec-123")).thenReturn(execStartCmd);
        doAnswer(inv -> {
            ResultCallback.Adapter<Frame> cb = inv.getArgument(0);
            cb.onComplete();
            return cb;
        }).when(execStartCmd).exec(any());

        ContainerExecResponse response = dockerClientUtil.execCommand(request);
        assertEquals("success", response.getStatus());
    }

    // ─── inspectExecCmd ────────────────────────────────────────────────────────

    @Test
    void inspectExecCmd_returnsRunningStatus() {
        setupDockerHost();
        InspectExecCmd inspectCmd = mock(InspectExecCmd.class);
        when(dockerClient.inspectExecCmd("exec-1")).thenReturn(inspectCmd);
        InspectExecResponse response = mock(InspectExecResponse.class);
        when(inspectCmd.exec()).thenReturn(response);
        when(response.isRunning()).thenReturn(true);

        assertTrue(dockerClientUtil.inspectExecCmd("exec-1"));
    }

    // ─── resizeTerminal ────────────────────────────────────────────────────────

    @Test
    void resizeTerminal_callsResizeExecCmd() {
        setupDockerHost();
        ResizeExecCmd resizeCmd = mock(ResizeExecCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.resizeExecCmd("exec-1")).thenReturn(resizeCmd);

        dockerClientUtil.resizeTerminal("exec-1", 24, 80);

        verify(resizeCmd).withSize(24, 80);
        verify(resizeCmd).exec();
    }

    // ─── getImageHistory ───────────────────────────────────────────────────────

    @Test
    void getImageHistory_success_returnsHistoryItems() throws Exception {
        dockerClientUtil.setCurrentHost("tcp://docker");
        when(connectionManager.getDockerHttpClient("tcp://docker")).thenReturn(dockerHttpClient);

        com.github.dockerjava.transport.DockerHttpClient.Response httpResponse =
                mock(com.github.dockerjava.transport.DockerHttpClient.Response.class);
        when(dockerHttpClient.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusCode()).thenReturn(200);

        String json = "[{\"Id\":\"sha256:abc\",\"Created\":1700000000,\"CreatedBy\":\"/bin/sh\",\"Size\":1024,\"Comment\":\"test\",\"Tags\":[\"ubuntu:latest\"]}]";
        when(httpResponse.getBody()).thenReturn(new ByteArrayInputStream(json.getBytes()));

        List<ImageHistoryItem> items = dockerClientUtil.getImageHistory("abc123");

        assertEquals(1, items.size());
        assertEquals("/bin/sh", items.get(0).getLayer());
    }

    @Test
    void getImageHistory_nonOkStatus_returnsEmptyList() throws Exception {
        dockerClientUtil.setCurrentHost("tcp://docker");
        when(connectionManager.getDockerHttpClient("tcp://docker")).thenReturn(dockerHttpClient);

        com.github.dockerjava.transport.DockerHttpClient.Response httpResponse =
                mock(com.github.dockerjava.transport.DockerHttpClient.Response.class);
        when(dockerHttpClient.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusCode()).thenReturn(404);

        List<ImageHistoryItem> items = dockerClientUtil.getImageHistory("badimage");
        assertTrue(items.isEmpty());
    }

    // ─── exportContainer ───────────────────────────────────────────────────────

    @Test
    void exportContainer_success_returnsInputStream() throws Exception {
        dockerClientUtil.setCurrentHost("tcp://docker");
        when(connectionManager.getDockerHttpClient("tcp://docker")).thenReturn(dockerHttpClient);

        com.github.dockerjava.transport.DockerHttpClient.Response httpResponse =
                mock(com.github.dockerjava.transport.DockerHttpClient.Response.class);
        when(dockerHttpClient.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusCode()).thenReturn(200);
        when(httpResponse.getBody()).thenReturn(new ByteArrayInputStream("tar-data".getBytes()));

        InputStream result = dockerClientUtil.exportContainer("ctr1");
        assertNotNull(result);
        result.close();
    }

    @Test
    void exportContainer_nonOkStatus_throwsRuntimeException() throws Exception {
        dockerClientUtil.setCurrentHost("tcp://docker");
        when(connectionManager.getDockerHttpClient("tcp://docker")).thenReturn(dockerHttpClient);

        com.github.dockerjava.transport.DockerHttpClient.Response httpResponse =
                mock(com.github.dockerjava.transport.DockerHttpClient.Response.class);
        when(dockerHttpClient.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusCode()).thenReturn(500);

        assertThrows(RuntimeException.class, () -> dockerClientUtil.exportContainer("ctr1"));
    }

    // ─── updateContainer ───────────────────────────────────────────────────────

    @Test
    void updateContainer_success_recreatesContainer() {
        setupDockerHost();

        InspectContainerCmd inspectCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd("old-ctr")).thenReturn(inspectCmd);
        InspectContainerResponse containerInfo = mock(InspectContainerResponse.class);
        when(inspectCmd.exec()).thenReturn(containerInfo);
        when(containerInfo.getName()).thenReturn("/mycontainer");
        InspectContainerResponse.ContainerState state = mock(InspectContainerResponse.ContainerState.class);
        when(containerInfo.getState()).thenReturn(state);
        when(state.getRunning()).thenReturn(false);
        ContainerConfig config = mock(ContainerConfig.class);
        when(containerInfo.getConfig()).thenReturn(config);
        when(config.getImage()).thenReturn("nginx:latest");

        RenameContainerCmd renameCmd = mock(RenameContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.renameContainerCmd("old-ctr")).thenReturn(renameCmd);

        CreateContainerCmd createCmd = mock(CreateContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.createContainerCmd("nginx:latest")).thenReturn(createCmd);
        CreateContainerResponse createResponse = mock(CreateContainerResponse.class);
        when(createCmd.exec()).thenReturn(createResponse);
        when(createResponse.getId()).thenReturn("new-ctr");

        StartContainerCmd startCmd = mock(StartContainerCmd.class);
        when(dockerClient.startContainerCmd("new-ctr")).thenReturn(startCmd);

        RemoveContainerCmd removeCmd = mock(RemoveContainerCmd.class);
        when(dockerClient.removeContainerCmd("old-ctr")).thenReturn(removeCmd);

        ContainerCreateRequest request = new ContainerCreateRequest();
        request.setContainerId("old-ctr");

        Map<String, Object> result = dockerClientUtil.updateContainer(request);

        assertEquals("success", result.get("status"));
        assertEquals("old-ctr", result.get("containerId"));
        assertEquals("new-ctr", result.get("newContainerId"));
    }

    // ─── copyFileFromContainer ─────────────────────────────────────────────────

    @Test
    void copyFileFromContainer_returnsFileBytes() throws Exception {
        setupDockerHost();

        ByteArrayOutputStream tarOutput = new ByteArrayOutputStream();
        try (org.apache.commons.compress.archivers.tar.TarArchiveOutputStream tarOs =
                new org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(tarOutput)) {
            byte[] content = "hello world".getBytes();
            org.apache.commons.compress.archivers.tar.TarArchiveEntry entry =
                    new org.apache.commons.compress.archivers.tar.TarArchiveEntry("file.txt");
            entry.setSize(content.length);
            tarOs.putArchiveEntry(entry);
            tarOs.write(content);
            tarOs.closeArchiveEntry();
            tarOs.finish();
        }

        CopyArchiveFromContainerCmd copyCmd = mock(CopyArchiveFromContainerCmd.class);
        when(dockerClient.copyArchiveFromContainerCmd("ctr1", "/file.txt")).thenReturn(copyCmd);
        when(copyCmd.exec()).thenReturn(new ByteArrayInputStream(tarOutput.toByteArray()));

        byte[] result = dockerClientUtil.copyFileFromContainer("ctr1", "/file.txt");

        assertEquals("hello world", new String(result));
    }

    // ─── readContainerFileContent ──────────────────────────────────────────────

    @Test
    void readContainerFileContent_validPath_returnsContent() throws Exception {
        setupDockerHost();

        ByteArrayOutputStream tarOutput = new ByteArrayOutputStream();
        try (org.apache.commons.compress.archivers.tar.TarArchiveOutputStream tarOs =
                new org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(tarOutput)) {
            byte[] content = "file content".getBytes();
            org.apache.commons.compress.archivers.tar.TarArchiveEntry entry =
                    new org.apache.commons.compress.archivers.tar.TarArchiveEntry("config.txt");
            entry.setSize(content.length);
            tarOs.putArchiveEntry(entry);
            tarOs.write(content);
            tarOs.closeArchiveEntry();
            tarOs.finish();
        }

        CopyArchiveFromContainerCmd copyCmd = mock(CopyArchiveFromContainerCmd.class);
        when(dockerClient.copyArchiveFromContainerCmd("ctr1", "/etc/config.txt")).thenReturn(copyCmd);
        when(copyCmd.exec()).thenReturn(new ByteArrayInputStream(tarOutput.toByteArray()));

        String result = dockerClientUtil.readContainerFileContent("ctr1", "/etc/config.txt", "UTF-8");
        assertEquals("file content", result);
    }

    // ─── writeContainerFileContent ─────────────────────────────────────────────

    @Test
    void writeContainerFileContent_validPath_success() {
        setupDockerHost();

        CopyArchiveToContainerCmd copyCmd = mock(CopyArchiveToContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.copyArchiveToContainerCmd("ctr1")).thenReturn(copyCmd);

        Map<String, Object> result = dockerClientUtil.writeContainerFileContent(
                "ctr1", "/etc/config.txt", "hello", "UTF-8");

        assertEquals("success", result.get("status"));
    }

    // ─── cloneVolume ───────────────────────────────────────────────────────────

    @Test
    void cloneVolume_success() throws InterruptedException {
        setupDockerHost();

        CreateVolumeCmd createVolumeCmd = mock(CreateVolumeCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.createVolumeCmd()).thenReturn(createVolumeCmd);

        InspectImageCmd inspectCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("busybox:latest")).thenReturn(inspectCmd);
        // exec() doesn't throw → image exists

        CreateContainerCmd createContainerCmd = mock(CreateContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.createContainerCmd("busybox:latest")).thenReturn(createContainerCmd);
        CreateContainerResponse createResponse = mock(CreateContainerResponse.class);
        when(createContainerCmd.exec()).thenReturn(createResponse);
        when(createResponse.getId()).thenReturn("clone-ctr");

        StartContainerCmd startCmd = mock(StartContainerCmd.class);
        when(dockerClient.startContainerCmd("clone-ctr")).thenReturn(startCmd);

        WaitContainerCmd waitCmd = mock(WaitContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.waitContainerCmd("clone-ctr")).thenReturn(waitCmd);
        WaitContainerResultCallback waitCallback = mock(WaitContainerResultCallback.class);
        when(waitCmd.start()).thenReturn(waitCallback);
        when(waitCallback.awaitCompletion(anyLong(), any())).thenReturn(true);

        RemoveContainerCmd removeCmd = mock(RemoveContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.removeContainerCmd("clone-ctr")).thenReturn(removeCmd);

        Map<String, Object> result = dockerClientUtil.cloneVolume("src", "dst", "local");

        assertEquals("success", result.get("status"));
    }
}
