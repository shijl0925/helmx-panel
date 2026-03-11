package com.helmx.tutorial.docker.utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CreateVolumeCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectVolumeCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.RemoveVolumeCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    private void setupDockerHost() {
        when(connectionManager.checkConnectionHealth("tcp://docker")).thenReturn(true);
        when(connectionManager.getDockerClient("tcp://docker")).thenReturn(dockerClient);
        dockerClientUtil.setCurrentHost("tcp://docker");
    }

    @Test
    void backupVolume_skipsImagePullWhenHelperAlreadyExists() throws Exception {
        setupDockerHost();

        InspectVolumeCmd inspectVolumeCmd = mock(InspectVolumeCmd.class);
        when(dockerClient.inspectVolumeCmd("mydata")).thenReturn(inspectVolumeCmd);

        InspectImageCmd inspectImageCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("busybox:latest")).thenReturn(inspectImageCmd);

        CreateContainerCmd createContainerCmd = mock(CreateContainerCmd.class, Answers.RETURNS_SELF);
        CreateContainerResponse createContainerResponse = mock(CreateContainerResponse.class);
        when(dockerClient.createContainerCmd("busybox:latest")).thenReturn(createContainerCmd);
        when(createContainerCmd.exec()).thenReturn(createContainerResponse);
        when(createContainerResponse.getId()).thenReturn("backup-container");

        CopyArchiveFromContainerCmd copyCmd = mock(CopyArchiveFromContainerCmd.class);
        when(dockerClient.copyArchiveFromContainerCmd("backup-container", "/volume")).thenReturn(copyCmd);
        when(copyCmd.exec()).thenReturn(new ByteArrayInputStream("archive".getBytes()));

        RemoveContainerCmd removeContainerCmd = mock(RemoveContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.removeContainerCmd("backup-container")).thenReturn(removeContainerCmd);

        try (InputStream inputStream = dockerClientUtil.backupVolume("mydata", "/")) {
            assertNotNull(inputStream);
            Assertions.assertEquals("archive", new String(inputStream.readAllBytes()));
        }

        verify(dockerClient, never()).pullImageCmd(anyString());
        verify(copyCmd).close();
        verify(removeContainerCmd).withForce(true);
        verify(removeContainerCmd).exec();
    }

    @Test
    void backupVolume_rejectsPathTraversalOutsideVolume() {
        setupDockerHost();

        InspectVolumeCmd inspectVolumeCmd = mock(InspectVolumeCmd.class);
        when(dockerClient.inspectVolumeCmd("mydata")).thenReturn(inspectVolumeCmd);

        assertThrows(IllegalArgumentException.class, () -> dockerClientUtil.backupVolume("mydata", "../../etc"));
        verify(dockerClient, never()).createContainerCmd(anyString());
    }

    @Test
    void backupVolume_throwsWhenHelperImagePullFails() {
        setupDockerHost();

        InspectVolumeCmd inspectVolumeCmd = mock(InspectVolumeCmd.class);
        when(dockerClient.inspectVolumeCmd("mydata")).thenReturn(inspectVolumeCmd);

        InspectImageCmd inspectImageCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("busybox:latest")).thenReturn(inspectImageCmd);
        when(inspectImageCmd.exec()).thenThrow(new NotFoundException("No such image: busybox:latest"));

        PullImageCmd pullImageCmd = mock(PullImageCmd.class);
        when(dockerClient.pullImageCmd("busybox:latest")).thenReturn(pullImageCmd);
        when(pullImageCmd.exec(any())).thenThrow(new RuntimeException("Network unavailable"));

        assertThrows(RuntimeException.class, () -> dockerClientUtil.backupVolume("mydata", "/"));
        verify(dockerClient, never()).createContainerCmd(anyString());
    }

    @Test
    void cloneVolume_throwsWhenHelperImagePullFails() {
        setupDockerHost();

        InspectVolumeCmd sourceInspectCmd = mock(InspectVolumeCmd.class);
        InspectVolumeCmd targetInspectCmd = mock(InspectVolumeCmd.class);
        when(dockerClient.inspectVolumeCmd("src-vol")).thenReturn(sourceInspectCmd);
        when(dockerClient.inspectVolumeCmd("target-vol")).thenReturn(targetInspectCmd);
        when(targetInspectCmd.exec()).thenThrow(new NotFoundException("No such volume: target-vol"));

        InspectImageCmd inspectImageCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("busybox:latest")).thenReturn(inspectImageCmd);
        when(inspectImageCmd.exec()).thenThrow(new NotFoundException("No such image: busybox:latest"));

        PullImageCmd pullImageCmd = mock(PullImageCmd.class);
        when(dockerClient.pullImageCmd("busybox:latest")).thenReturn(pullImageCmd);
        when(pullImageCmd.exec(any())).thenThrow(new RuntimeException("Network unavailable"));

        Map<String, Object> result = dockerClientUtil.cloneVolume("src-vol", "target-vol", "local");

        Assertions.assertEquals("failed", result.get("status"));
        Assertions.assertEquals("Failed to pull helper image: busybox:latest", result.get("message"));
        verify(dockerClient, never()).createContainerCmd(anyString());
    }

    @Test
    void cloneVolume_removesTargetVolumeWhenHelperContainerFails() throws Exception {
        setupDockerHost();

        InspectVolumeCmd sourceInspectCmd = mock(InspectVolumeCmd.class);
        InspectVolumeCmd targetInspectCmd = mock(InspectVolumeCmd.class);
        when(dockerClient.inspectVolumeCmd("src-vol")).thenReturn(sourceInspectCmd);
        when(dockerClient.inspectVolumeCmd("target-vol")).thenReturn(targetInspectCmd);
        when(targetInspectCmd.exec()).thenThrow(new NotFoundException("No such volume: target-vol"));

        InspectImageCmd inspectImageCmd = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("busybox:latest")).thenReturn(inspectImageCmd);

        CreateVolumeCmd createVolumeCmd = mock(CreateVolumeCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.createVolumeCmd()).thenReturn(createVolumeCmd);

        CreateContainerCmd createContainerCmd = mock(CreateContainerCmd.class, Answers.RETURNS_SELF);
        CreateContainerResponse createContainerResponse = mock(CreateContainerResponse.class);
        when(dockerClient.createContainerCmd("busybox:latest")).thenReturn(createContainerCmd);
        when(createContainerCmd.exec()).thenReturn(createContainerResponse);
        when(createContainerResponse.getId()).thenReturn("clone-container");

        StartContainerCmd startContainerCmd = mock(StartContainerCmd.class);
        when(dockerClient.startContainerCmd("clone-container")).thenReturn(startContainerCmd);

        WaitContainerCmd waitContainerCmd = mock(WaitContainerCmd.class);
        WaitContainerResultCallback waitCallback = mock(WaitContainerResultCallback.class);
        when(dockerClient.waitContainerCmd("clone-container")).thenReturn(waitContainerCmd);
        when(waitContainerCmd.start()).thenReturn(waitCallback);
        when(waitCallback.awaitCompletion(60L, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);

        InspectContainerCmd inspectContainerCmd = mock(InspectContainerCmd.class);
        InspectContainerResponse inspectContainerResponse = mock(InspectContainerResponse.class);
        ContainerState containerState = mock(ContainerState.class);
        when(containerState.getExitCodeLong()).thenReturn(1L);
        when(dockerClient.inspectContainerCmd("clone-container")).thenReturn(inspectContainerCmd);
        when(inspectContainerCmd.exec()).thenReturn(inspectContainerResponse);
        when(inspectContainerResponse.getState()).thenReturn(containerState);

        RemoveContainerCmd removeContainerCmd = mock(RemoveContainerCmd.class, Answers.RETURNS_SELF);
        when(dockerClient.removeContainerCmd("clone-container")).thenReturn(removeContainerCmd);

        RemoveVolumeCmd removeVolumeCmd = mock(RemoveVolumeCmd.class);
        when(dockerClient.removeVolumeCmd("target-vol")).thenReturn(removeVolumeCmd);

        Map<String, Object> result = dockerClientUtil.cloneVolume("src-vol", "target-vol", "local");

        Assertions.assertEquals("failed", result.get("status"));
        Assertions.assertEquals("Failed to clone volume: helper container exited with code 1", result.get("message"));
        verify(removeContainerCmd).withForce(true);
        verify(removeContainerCmd).exec();
        verify(removeVolumeCmd).exec();
    }
}
