package com.helmx.tutorial.docker.controller;

import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.helmx.tutorial.docker.utils.DockerClientUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VolumeControllerIntegrationTest {

    @Mock
    private DockerClientUtil dockerClientUtil;

    private VolumeController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new VolumeController();
        ReflectionTestUtils.setField(controller, "dockerClientUtil", dockerClientUtil);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void searchDockerVolumes_appliesPaginationAndAddsDetailFields() throws Exception {
        InspectVolumeResponse logsVolume = createVolume("logs", "/var/lib/docker/volumes/logs", "Scope", "local");
        InspectVolumeResponse dataVolume = createVolume("data", "/var/lib/docker/volumes/data", "Scope", "global");
        InspectVolumeResponse dataDetail = createVolume("data", "/var/lib/docker/volumes/data", "Scope", "global", "CreatedAt", "2026-03-10T00:00:00Z");

        when(dockerClientUtil.searchVolumed(any())).thenReturn(new ArrayList<>(List.of(logsVolume, dataVolume)));
        when(dockerClientUtil.inspectVolume("data")).thenReturn(dataDetail);
        when(dockerClientUtil.isVolumeInUse("data")).thenReturn(true);

        mockMvc.perform(post("/api/v1/ops/volumes/search")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "page": 1,
                                  "pageSize": 1,
                                  "sortOrder": "asc"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.pages").value(2))
                .andExpect(jsonPath("$.data.items[0].name").value("data"))
                .andExpect(jsonPath("$.data.items[0].scope").value("global"))
                .andExpect(jsonPath("$.data.items[0].createdAt").value("2026-03-10T00:00:00Z"))
                .andExpect(jsonPath("$.data.items[0].isUsed").value(true));
    }

    @Test
    void getDockerVolumeInfo_returnsContainersWithVolumeDto() throws Exception {
        InspectVolumeResponse dataVolume = createVolume("data", "/var/lib/docker/volumes/data", "Scope", "global");
        when(dockerClientUtil.inspectVolume("data")).thenReturn(dataVolume);
        when(dockerClientUtil.getVolumeContainers("data")).thenReturn(List.of(Map.of("id", "container-1", "name", "app")));

        mockMvc.perform(post("/api/v1/ops/volumes/info")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "name": "data"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("data"))
                .andExpect(jsonPath("$.data.scope").value("global"))
                .andExpect(jsonPath("$.data.containers[0].id").value("container-1"))
                .andExpect(jsonPath("$.data.containers[0].name").value("app"));

        verify(dockerClientUtil).setCurrentHost("unix:///var/run/docker.sock");
    }

    @Test
    void removeDockerVolume_returnsFailureWhenSingleRemovalFails() throws Exception {
        when(dockerClientUtil.removeVolume("data")).thenReturn(new HashMap<>(Map.of("status", "failed", "message", "volume is in use")));

        mockMvc.perform(post("/api/v1/ops/volumes/remove")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "names": ["data"]
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("volume is in use"))
                .andExpect(jsonPath("$.data.status").value("failed"));
    }

    @Test
    void backupDockerVolume_streamsArchiveAndClearsHost() throws Exception {
        when(dockerClientUtil.backupVolume("data", "/"))
                .thenReturn(new ByteArrayInputStream("tar-content".getBytes()));

        com.helmx.tutorial.docker.dto.VolumeBackupRequest request = new com.helmx.tutorial.docker.dto.VolumeBackupRequest();
        request.setHost("unix:///var/run/docker.sock");
        request.setName("data");
        request.setPath("/");

        ResponseEntity<StreamingResponseBody> response = controller.backupDockerVolume(request);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals("attachment; filename=\"data-backup.tar\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        Assertions.assertEquals("application/x-tar", response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
        Assertions.assertNotNull(response.getBody());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.getBody().writeTo(outputStream);
        Assertions.assertEquals("tar-content", outputStream.toString());

        var inOrder = inOrder(dockerClientUtil);
        inOrder.verify(dockerClientUtil).setCurrentHost("unix:///var/run/docker.sock");
        inOrder.verify(dockerClientUtil).backupVolume("data", "/");
        inOrder.verify(dockerClientUtil).clearCurrentHost();
    }

    @Test
    void cloneDockerVolume_returnsFailureWhenUtilityFails() throws Exception {
        when(dockerClientUtil.cloneVolume("source-data", "target-data", "local"))
                .thenReturn(new HashMap<>(Map.of("status", "failed", "message", "Source volume does not exist: source-data")));

        mockMvc.perform(post("/api/v1/ops/volumes/clone")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "sourceName": "source-data",
                                  "targetName": "target-data",
                                  "driver": "local"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Source volume does not exist: source-data"))
                .andExpect(jsonPath("$.data.status").value("failed"));
    }

    private InspectVolumeResponse createVolume(String name, String mountpoint, String... rawPairs) {
        InspectVolumeResponse volume = new InspectVolumeResponse();
        ReflectionTestUtils.setField(volume, "name", name);
        ReflectionTestUtils.setField(volume, "driver", "local");
        ReflectionTestUtils.setField(volume, "mountpoint", mountpoint);
        ReflectionTestUtils.setField(volume, "options", Map.of("o", "addr=server"));
        ReflectionTestUtils.setField(volume, "labels", Map.of("env", "test"));

        java.util.HashMap<String, Object> rawValues = new java.util.HashMap<>();
        for (int i = 0; i < rawPairs.length; i += 2) {
            rawValues.put(rawPairs[i], rawPairs[i + 1]);
        }
        ReflectionTestUtils.setField(volume, "rawValues", rawValues);
        return volume;
    }
}
