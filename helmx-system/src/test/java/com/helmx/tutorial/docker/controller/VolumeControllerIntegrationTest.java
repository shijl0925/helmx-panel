package com.helmx.tutorial.docker.controller;

import com.github.dockerjava.api.command.InspectVolumeResponse;
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

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        VolumeController controller = new VolumeController();
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
    void createDockerVolume_nullStatusInResult_doesNotThrowNPE() throws Exception {
        // Before the fix, status.equals("success") threw NPE when status was null
        Map<String, Object> result = new HashMap<>();
        result.put("status", null);
        result.put("message", "unexpected null status");
        when(dockerClientUtil.createVolume(any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/ops/volumes")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "name": "null-status-vol"
                                }
                                """))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void removeDockerVolume_nullStatusInResult_doesNotThrowNPE() throws Exception {
        // Before the fix, removeVolumeResult.get("status").equals("failed") threw NPE
        // when the status key was null
        Map<String, Object> nullStatusResult = new HashMap<>();
        nullStatusResult.put("status", null);
        when(dockerClientUtil.removeVolume("orphan")).thenReturn(nullStatusResult);

        mockMvc.perform(post("/api/v1/ops/volumes/remove")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "names": ["orphan"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
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
