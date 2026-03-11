package com.helmx.tutorial.docker.utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
}
