package com.helmx.tutorial.docker.controller;

import com.helmx.tutorial.docker.dto.ContainerOperation;
import com.helmx.tutorial.docker.utils.DockerClientUtil;
import com.helmx.tutorial.dto.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
