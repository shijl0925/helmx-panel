package com.helmx.tutorial.docker.controller;

import com.helmx.tutorial.docker.dto.ContainerCopyRequest;
import com.helmx.tutorial.docker.dto.ContainerOperation;
import com.helmx.tutorial.docker.utils.DockerClientUtil;
import com.helmx.tutorial.dto.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

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
    void copyFileFromContainer_streamsFileContentAndClearsHost() throws Exception {
        ContainerCopyRequest request = new ContainerCopyRequest();
        request.setHost("unix:///var/run/docker.sock");
        request.setContainerId("container-1");
        request.setContainerPath("/tmp/example.txt");

        doAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(2);
            outputStream.write("downloaded-content".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(dockerClientUtil).copyFileFromContainer(eq("container-1"), eq("/tmp/example.txt"), any(OutputStream.class));

        ResponseEntity<?> response = containerController.copyFileFromContainer(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("attachment; filename=\"example.txt\"", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof StreamingResponseBody);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ((StreamingResponseBody) response.getBody()).writeTo(outputStream);

        assertEquals("downloaded-content", outputStream.toString(StandardCharsets.UTF_8));
        InOrder inOrder = inOrder(dockerClientUtil);
        inOrder.verify(dockerClientUtil).setCurrentHost("unix:///var/run/docker.sock");
        inOrder.verify(dockerClientUtil).copyFileFromContainer(eq("container-1"), eq("/tmp/example.txt"), any(OutputStream.class));
        inOrder.verify(dockerClientUtil).clearCurrentHost();
    }

    @Test
    void copyFileFromContainer_sanitizesAndPreservesUnicodeFilename() {
        ContainerCopyRequest request = new ContainerCopyRequest();
        request.setHost("unix:///var/run/docker.sock");
        request.setContainerId("container-1");
        request.setContainerPath("/tmp/测试 文档.txt");

        ResponseEntity<?> response = containerController.copyFileFromContainer(request);

        assertEquals("测试_文档.txt", response.getHeaders().getContentDisposition().getFilename());
    }
}
