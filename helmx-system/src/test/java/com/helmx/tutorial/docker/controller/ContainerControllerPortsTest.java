package com.helmx.tutorial.docker.controller;

import com.helmx.tutorial.docker.dto.ContainerPortMapping;
import com.helmx.tutorial.docker.utils.DockerClientUtil;
import com.helmx.tutorial.security.security.service.UserPermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ContainerControllerPortsTest {

    @Mock
    private DockerClientUtil dockerClientUtil;

    @Mock
    private UserPermissionService userPermissionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ContainerController controller = new ContainerController();
        ReflectionTestUtils.setField(controller, "dockerClientUtil", dockerClientUtil);
        ReflectionTestUtils.setField(controller, "userPermissionService", userPermissionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getContainerPortMappings_returnsAllPublishedPorts() throws Exception {
        ContainerPortMapping mapping1 = new ContainerPortMapping();
        mapping1.setContainerId("abc123456789");
        mapping1.setContainerName("web");
        mapping1.setState("running");
        mapping1.setImage("nginx:latest");
        mapping1.setIp("0.0.0.0");
        mapping1.setPublicPort(8080);
        mapping1.setPrivatePort(80);
        mapping1.setType("tcp");

        ContainerPortMapping mapping2 = new ContainerPortMapping();
        mapping2.setContainerId("def987654321");
        mapping2.setContainerName("db");
        mapping2.setState("running");
        mapping2.setImage("postgres:15");
        mapping2.setIp("127.0.0.1");
        mapping2.setPublicPort(5432);
        mapping2.setPrivatePort(5432);
        mapping2.setType("tcp");

        when(dockerClientUtil.getContainerPortMappings(true)).thenReturn(List.of(mapping1, mapping2));

        mockMvc.perform(post("/api/v1/ops/containers/ports")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "all": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].containerName").value("web"))
                .andExpect(jsonPath("$.data[0].publicPort").value(8080))
                .andExpect(jsonPath("$.data[0].privatePort").value(80))
                .andExpect(jsonPath("$.data[0].type").value("tcp"))
                .andExpect(jsonPath("$.data[0].ip").value("0.0.0.0"))
                .andExpect(jsonPath("$.data[0].state").value("running"))
                .andExpect(jsonPath("$.data[1].containerName").value("db"))
                .andExpect(jsonPath("$.data[1].publicPort").value(5432));

        verify(dockerClientUtil).setCurrentHost("unix:///var/run/docker.sock");
        verify(dockerClientUtil).getContainerPortMappings(true);
    }

    @Test
    void getContainerPortMappings_returnsEmptyListWhenNoPortsPublished() throws Exception {
        when(dockerClientUtil.getContainerPortMappings(true)).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/ops/containers/ports")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "all": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
