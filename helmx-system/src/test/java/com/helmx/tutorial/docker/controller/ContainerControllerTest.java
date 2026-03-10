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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.request.async.WebAsyncTask;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;

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

        WebAsyncTask<ResponseEntity<StreamingResponseBody>> task = containerController.copyFileFromContainer(request);
        @SuppressWarnings("unchecked")
        ResponseEntity<StreamingResponseBody> response = (ResponseEntity<StreamingResponseBody>) task.getCallable().call();

        assertEquals(ContainerController.COPY_FILE_FROM_CONTAINER_TIMEOUT_MILLIS, task.getTimeout());
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
    void copyFileFromContainer_sanitizesAndPreservesUnicodeFilename() throws Exception {
        ContainerCopyRequest request = new ContainerCopyRequest();
        request.setHost("unix:///var/run/docker.sock");
        request.setContainerId("container-1");
        request.setContainerPath("/tmp/测试 文档.txt");

        WebAsyncTask<ResponseEntity<StreamingResponseBody>> task = containerController.copyFileFromContainer(request);
        @SuppressWarnings("unchecked")
        ResponseEntity<StreamingResponseBody> response = (ResponseEntity<StreamingResponseBody>) task.getCallable().call();

        assertEquals(ContainerController.COPY_FILE_FROM_CONTAINER_TIMEOUT_MILLIS, task.getTimeout());
        assertEquals("测试_文档.txt", response.getHeaders().getContentDisposition().getFilename());
    }

    @Test
    void copyFileFromContainer_mockMvcStreamsOctetStreamResponse() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(containerController).build();

        doAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(2);
            outputStream.write("downloaded-content".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(dockerClientUtil).copyFileFromContainer(eq("container-1"), eq("/tmp/example.txt"), any(OutputStream.class));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/ops/containers/copy/from")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "containerId": "container-1",
                                  "containerPath": "/tmp/example.txt"
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        assertEquals(ContainerController.COPY_FILE_FROM_CONTAINER_TIMEOUT_MILLIS,
                mvcResult.getRequest().getAsyncContext().getTimeout());

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"example.txt\""))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andReturn();
    }
}
