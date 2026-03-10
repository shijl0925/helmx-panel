package com.helmx.tutorial.docker.controller;

import com.helmx.tutorial.docker.dto.ContainerHealthStatus;
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
class ContainerControllerHealthTest {

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
    void getContainerHealthStatuses_returnsHealthyAndUnhealthyEntries() throws Exception {
        ContainerHealthStatus healthy = new ContainerHealthStatus();
        healthy.setContainerId("abc123456789");
        healthy.setContainerName("web");
        healthy.setImage("nginx:latest");
        healthy.setState("running");
        healthy.setHealth("healthy");
        healthy.setFailingStreak(0);
        healthy.setLastCheck("2026-03-10T10:00:00Z");

        ContainerHealthStatus unhealthy = new ContainerHealthStatus();
        unhealthy.setContainerId("def987654321");
        unhealthy.setContainerName("db");
        unhealthy.setImage("postgres:15");
        unhealthy.setState("running");
        unhealthy.setHealth("unhealthy");
        unhealthy.setFailingStreak(3);
        unhealthy.setLastCheck("2026-03-10T10:05:00Z");

        ContainerHealthStatus noCheck = new ContainerHealthStatus();
        noCheck.setContainerId("ghi111213141");
        noCheck.setContainerName("cache");
        noCheck.setImage("redis:7");
        noCheck.setState("running");
        noCheck.setHealth("none");
        noCheck.setFailingStreak(0);

        when(dockerClientUtil.getContainerHealthStatuses(true)).thenReturn(List.of(healthy, unhealthy, noCheck));

        mockMvc.perform(post("/api/v1/ops/containers/health")
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
                .andExpect(jsonPath("$.data[0].health").value("healthy"))
                .andExpect(jsonPath("$.data[0].failingStreak").value(0))
                .andExpect(jsonPath("$.data[0].lastCheck").value("2026-03-10T10:00:00Z"))
                .andExpect(jsonPath("$.data[1].containerName").value("db"))
                .andExpect(jsonPath("$.data[1].health").value("unhealthy"))
                .andExpect(jsonPath("$.data[1].failingStreak").value(3))
                .andExpect(jsonPath("$.data[2].health").value("none"));

        verify(dockerClientUtil).setCurrentHost("unix:///var/run/docker.sock");
        verify(dockerClientUtil).getContainerHealthStatuses(true);
    }

    @Test
    void getContainerHealthStatuses_returnsEmptyListWhenNoContainers() throws Exception {
        when(dockerClientUtil.getContainerHealthStatuses(true)).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/ops/containers/health")
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
