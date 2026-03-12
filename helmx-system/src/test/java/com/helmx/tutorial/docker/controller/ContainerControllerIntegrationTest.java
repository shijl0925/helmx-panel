package com.helmx.tutorial.docker.controller;

import com.alibaba.fastjson2.JSONObject;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ContainerNetworkSettings;
import com.github.dockerjava.api.model.ContainerPort;
import com.helmx.tutorial.docker.dto.ContainerExecResponse;
import com.helmx.tutorial.docker.utils.DockerClientUtil;
import com.helmx.tutorial.dto.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ContainerControllerIntegrationTest {

    @Mock
    private DockerClientUtil dockerClientUtil;

    private ContainerController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new ContainerController();
        ReflectionTestUtils.setField(controller, "dockerClientUtil", dockerClientUtil);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createDockerContainer_successReturnsCreatedContainer() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "Container created successfully");
        result.put("containerId", "container-1");
        when(dockerClientUtil.createContainer(any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/ops/containers")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "name": "demo-app",
                                  "image": "nginx:latest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("Container created successfully"))
                .andExpect(jsonPath("$.data.containerId").value("container-1"));
    }

    @Test
    void createDockerContainer_failureReturnsInternalServerError() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "failed");
        result.put("message", "Create failed");
        when(dockerClientUtil.createContainer(any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/ops/containers")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "name": "demo-app",
                                  "image": "nginx:latest"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Create failed"))
                .andExpect(jsonPath("$.data.status").value("failed"));
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
    void getContainerLogs_returnsLogPayload() throws Exception {
        when(dockerClientUtil.getContainerLogs("container-1", 200)).thenReturn("line-1\nline-2");

        mockMvc.perform(post("/api/v1/ops/containers/logs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "containerId": "container-1",
                                  "tail": 200
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("Get container logs successfully!"))
                .andExpect(jsonPath("$.data").value("line-1\nline-2"));
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
    void copyFileToContainer_successReturnsConfirmation() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "example.txt", "text/plain", "demo".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/ops/containers/copy/to")
                        .file(file)
                        .param("host", "unix:///var/run/docker.sock")
                        .param("containerId", "container-1")
                        .param("containerPath", "/tmp/example.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").value("File copied successfully to container"));
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
    void renameDockerContainer_successReturnsUpdatedMessage() throws Exception {
        when(dockerClientUtil.renameContainer(any()))
                .thenReturn(new HashMap<>(Map.of("status", "success", "message", "Container renamed")));

        mockMvc.perform(post("/api/v1/ops/containers/rename")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "containerId": "container-1",
                                  "newName": "renamed-app"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("Container renamed"))
                .andExpect(jsonPath("$.data.status").value("success"));
    }

    @Test
    void updateDockerContainer_successReturnsNewContainerId() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "Container updated");
        result.put("newContainerId", "container-2");
        when(dockerClientUtil.updateContainer(any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/ops/containers/update")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "containerId": "container-1",
                                  "name": "demo-app",
                                  "image": "nginx:latest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("Container updated"))
                .andExpect(jsonPath("$.data.newContainerId").value("container-2"));
    }

    @Test
    void updateContainerNetworks_disconnectsRemovedAndConnectsNewNetwork() throws Exception {
        when(dockerClientUtil.getContainerNetworks("container-1")).thenReturn(new HashSet<>(Set.of("bridge", "legacy")));
        when(dockerClientUtil.disconnectNetwork("legacy", "container-1"))
                .thenReturn(Map.of("status", "success"));
        when(dockerClientUtil.connectNetwork("blue", "container-1"))
                .thenReturn(Map.of("status", "success"));

        mockMvc.perform(post("/api/v1/ops/containers/networks")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "containerId": "container-1",
                                  "networks": ["bridge", "blue"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").value("Networks updated successfully"));

        verify(dockerClientUtil).disconnectNetwork("legacy", "container-1");
        verify(dockerClientUtil).connectNetwork("blue", "container-1");
        verify(dockerClientUtil, never()).disconnectNetwork("bridge", "container-1");
    }

    @Test
    void disconnectContainerNetwork_successReturnsConfirmation() throws Exception {
        when(dockerClientUtil.disconnectNetwork("bridge", "container-1")).thenReturn(Map.of("status", "success"));

        mockMvc.perform(post("/api/v1/ops/containers/disconnect")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "containerId": "container-1",
                                  "network": "bridge"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").value("Network disconnected successfully"));
    }

    @Test
    void executeCommandInContainer_successReturnsCommandOutput() throws Exception {
        ContainerExecResponse response = new ContainerExecResponse();
        response.setExecId("exec-1");
        response.setStatus("success");
        response.setOutput("hello");
        when(dockerClientUtil.execCommand(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/ops/containers/exec")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "containerId": "container-1",
                                  "command": ["echo", "hello"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("Command executed successfully"))
                .andExpect(jsonPath("$.data.execId").value("exec-1"))
                .andExpect(jsonPath("$.data.output").value("hello"));
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

    @Test
    void updateContainerResources_successReturnsConfirmation() throws Exception {
        when(dockerClientUtil.updateContainerResources(any()))
                .thenReturn(new HashMap<>(Map.of(
                        "status", "success",
                        "message", "Container resources updated successfully"
                )));

        mockMvc.perform(post("/api/v1/ops/containers/resources")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "containerId": "container-1",
                                  "cpuShares": 512,
                                  "memory": 1048576
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("Container resources updated successfully"))
                .andExpect(jsonPath("$.data.status").value("success"));
    }

    @Test
    void getContainerDiff_returnsFilesystemChanges() throws Exception {
        when(dockerClientUtil.getContainerDiff("container-1"))
                .thenReturn(List.of(
                        Map.of("path", "/app/config.yml", "kind", 0, "kindLabel", "Modified"),
                        Map.of("path", "/app/new.txt", "kind", 1, "kindLabel", "Added")
                ));

        mockMvc.perform(post("/api/v1/ops/containers/diff")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "containerId": "container-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.containerId").value("container-1"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.changes[0].kindLabel").value("Modified"))
                .andExpect(jsonPath("$.data.changes[1].path").value("/app/new.txt"));
    }

    @Test
    void renameDockerContainer_nullStatusDoesNotThrowNPE() throws Exception {
        // Before the fix: status.equals("success") threw NPE when status was null
        Map<String, Object> result = new HashMap<>();
        result.put("status", null);
        result.put("message", "backend error");
        when(dockerClientUtil.renameContainer(any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/ops/containers/rename")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "containerId": "container-1",
                                  "newName": "renamed-app"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500));
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
