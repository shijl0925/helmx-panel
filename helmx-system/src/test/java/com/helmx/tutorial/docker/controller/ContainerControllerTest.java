package com.helmx.tutorial.docker.controller;

import com.helmx.tutorial.docker.dto.ContainerOperation;
import com.helmx.tutorial.docker.dto.ContainerCopyRequest;
import com.helmx.tutorial.docker.utils.DockerClientUtil;
import com.helmx.tutorial.dto.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class ContainerControllerTest {

    @Mock
    private DockerClientUtil dockerClientUtil;

    @InjectMocks
    private ContainerController containerController;

    @Test
    void operateDockerContainer_unknownOperation_returnsBadRequest() {
        ContainerOperation request = new ContainerOperation();
        request.setHost("unix:///var/run/docker.sock");
        request.setContainerId("container-1");
        request.setOperation("invalid");

        ResponseEntity<Result> response = containerController.OperateDockerContainer(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getCode());
        assertEquals("Unknown operation: invalid", response.getBody().getMessage());
        verifyNoInteractions(dockerClientUtil);
    }

    @Test
    void copyFileFromContainer_sanitizesFilename_andClearsHost() {
        ContainerCopyRequest request = new ContainerCopyRequest();
        request.setHost("unix:///var/run/docker.sock");
        request.setContainerId("container-1");
        request.setContainerPath("..");

        byte[] content = "demo".getBytes();
        when(dockerClientUtil.copyFileFromContainer("container-1", "..")).thenReturn(content);

        ResponseEntity<?> response = containerController.copyFileFromContainer(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(content, (byte[]) response.getBody());
        HttpHeaders headers = response.getHeaders();
        assertEquals("attachment; filename=\"download.bin\"", headers.getFirst(HttpHeaders.CONTENT_DISPOSITION));
        verify(dockerClientUtil).setCurrentHost("unix:///var/run/docker.sock");
        verify(dockerClientUtil).clearCurrentHost();
    }

    @Test
    void copyFileFromContainer_normalizesDotHeavyFilename() {
        ContainerCopyRequest request = new ContainerCopyRequest();
        request.setHost("unix:///var/run/docker.sock");
        request.setContainerId("container-3");
        request.setContainerPath("/tmp/...hidden..tar...");

        byte[] content = "demo".getBytes();
        when(dockerClientUtil.copyFileFromContainer("container-3", "/tmp/...hidden..tar...")).thenReturn(content);

        ResponseEntity<?> response = containerController.copyFileFromContainer(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("attachment; filename=\"hidden.tar\"", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        verify(dockerClientUtil).clearCurrentHost();
    }

    @Test
    void copyFileFromContainer_failureReturnsGenericMessage_andClearsHost() {
        ContainerCopyRequest request = new ContainerCopyRequest();
        request.setHost("unix:///var/run/docker.sock");
        request.setContainerId("container-2");
        request.setContainerPath("/tmp/demo.txt");

        doThrow(new RuntimeException("sensitive internal detail"))
                .when(dockerClientUtil).copyFileFromContainer("container-2", "/tmp/demo.txt");

        ResponseEntity<?> response = containerController.copyFileFromContainer(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        Result body = (Result) response.getBody();
        assertEquals("Failed to copy file from container", body.getMessage());
        verify(dockerClientUtil).setCurrentHost("unix:///var/run/docker.sock");
        verify(dockerClientUtil).clearCurrentHost();
    }
}
