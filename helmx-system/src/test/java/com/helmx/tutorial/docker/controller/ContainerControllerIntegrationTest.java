package com.helmx.tutorial.docker.controller;

import com.alibaba.fastjson2.JSONObject;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ContainerNetworkSettings;
import com.github.dockerjava.api.model.ContainerPort;
import com.helmx.tutorial.docker.utils.DockerClientUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ContainerControllerIntegrationTest {

    @Mock
    private DockerClientUtil dockerClientUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ContainerController controller = new ContainerController();
        ReflectionTestUtils.setField(controller, "dockerClientUtil", dockerClientUtil);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void searchDockerContainers_appliesSortingAndPagination() throws Exception {
        Container beta = createContainer("container-2", "/beta", 2L, "running", "docker.io/library/nginx:latest", "sha256:image-2");
        Container alpha = createContainer("container-1", "/alpha", 1L, "exited", "docker.io/library/alpine:latest", "sha256:image-1");

        when(dockerClientUtil.searchContainers(any())).thenReturn(new ArrayList<>(List.of(beta, alpha)));

        mockMvc.perform(post("/api/v1/ops/containers/search")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "sortBy": "name",
                                  "sortOrder": "asc",
                                  "page": 1,
                                  "pageSize": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.pages").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value("container-1"))
                .andExpect(jsonPath("$.data.items[0].name").value("alpha"))
                .andExpect(jsonPath("$.data.items[0].imageId").value("image-1"))
                .andExpect(jsonPath("$.data.items[0].state").value("exited"));

        verify(dockerClientUtil).setCurrentHost("unix:///var/run/docker.sock");
    }

    @Test
    void getDockerContainerStats_simpleRequestReturnsMappedStats() throws Exception {
        JSONObject stats = JSONObject.parseObject("""
                {
                  "cpu_stats": {
                    "cpu_usage": {
                      "total_usage": 2000000000
                    },
                    "system_cpu_usage": 4000000000,
                    "online_cpus": 2
                  },
                  "precpu_stats": {
                    "cpu_usage": {
                      "total_usage": 1000000000
                    },
                    "system_cpu_usage": 2000000000
                  },
                  "memory_stats": {
                    "usage": 512,
                    "limit": 1024,
                    "stats": {
                      "cache": 128
                    }
                  }
                }
                """);
        when(dockerClientUtil.getContainerStats("container-1", false)).thenReturn(stats);

        mockMvc.perform(post("/api/v1/ops/containers/stats")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "containerId": "container-1",
                                  "simple": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.cpuPercent").value(100.0))
                .andExpect(jsonPath("$.data.memoryUsage").value("512 B"))
                .andExpect(jsonPath("$.data.memoryLimit").value("1.00 KB"))
                .andExpect(jsonPath("$.data.memoryPercent").value(50.0));
    }

    @Test
    void operateDockerContainer_successRemovesMessageFromPayload() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "Container start successfully");
        result.put("containerId", "container-1");
        when(dockerClientUtil.startContainer("container-1")).thenReturn(result);

        mockMvc.perform(post("/api/v1/ops/containers/operate")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "containerId": "container-1",
                                  "operation": "start"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("Container start successfully"))
                .andExpect(jsonPath("$.data.status").value("success"))
                .andExpect(jsonPath("$.data.containerId").value("container-1"));

        verify(dockerClientUtil).startContainer("container-1");
    }

    @Test
    void listContainerFiles_invalidPathReturnsBadRequest() throws Exception {
        when(dockerClientUtil.listContainerFiles("container-1", "../etc")).thenThrow(new IllegalArgumentException("Invalid container path"));

        mockMvc.perform(post("/api/v1/ops/containers/files")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "containerId": "container-1",
                                  "path": "../etc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid container path"));
    }

    @Test
    void bulkOperateContainers_returnsPartialCompletionCounts() throws Exception {
        List<Map<String, Object>> results = List.of(
                Map.of("containerId", "container-1", "status", "success"),
                Map.of("containerId", "container-2", "status", "failed", "message", "already stopped"),
                Map.of("containerId", "container-3", "status", "success")
        );
        when(dockerClientUtil.bulkOperateContainers(any())).thenReturn(results);

        mockMvc.perform(post("/api/v1/ops/containers/bulk")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "containerIds": ["container-1", "container-2", "container-3"],
                                  "operation": "restart"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("Operations partially completed"))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.failCount").value(1))
                .andExpect(jsonPath("$.data.results[1].containerId").value("container-2"))
                .andExpect(jsonPath("$.data.results[1].status").value("failed"));
    }

    @Test
    void readContainerFile_invalidPathReturnsBadRequest() throws Exception {
        when(dockerClientUtil.readContainerFileContent("container-1", "/root/../secret", "UTF-8"))
                .thenThrow(new IllegalArgumentException("Invalid file path"));

        mockMvc.perform(post("/api/v1/ops/containers/file/read")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "containerId": "container-1",
                                  "filePath": "/root/../secret",
                                  "encoding": "UTF-8"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid file path"));
    }

    @Test
    void writeContainerFile_failedWriteReturnsServerError() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "failed");
        result.put("message", "permission denied");
        when(dockerClientUtil.writeContainerFileContent(eq("container-1"), eq("/etc/app.conf"), eq("demo"), eq("UTF-8")))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/ops/containers/file/write")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "containerId": "container-1",
                                  "filePath": "/etc/app.conf",
                                  "content": "demo",
                                  "encoding": "UTF-8"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("permission denied"))
                .andExpect(jsonPath("$.data.status").value("failed"));
    }

    private Container createContainer(String id, String name, long created, String state, String image, String imageId) {
        Container container = new Container();
        ReflectionTestUtils.setField(container, "id", id);
        ReflectionTestUtils.setField(container, "names", new String[]{name});
        ReflectionTestUtils.setField(container, "created", created);
        ReflectionTestUtils.setField(container, "state", state);
        ReflectionTestUtils.setField(container, "status", state + " for test");
        ReflectionTestUtils.setField(container, "image", image);
        ReflectionTestUtils.setField(container, "imageId", imageId);
        ReflectionTestUtils.setField(container, "ports", new ContainerPort[]{
                new ContainerPort().withIp("0.0.0.0").withPublicPort(8080).withPrivatePort(80).withType("tcp")
        });
        ReflectionTestUtils.setField(container, "networkSettings", new ContainerNetworkSettings().withNetworks(Map.of(
                "bridge", new ContainerNetwork().withIpv4Address("172.17.0.2").withGateway("172.17.0.1")
        )));
        return container;
    }
}
