package com.helmx.tutorial.docker.controller;

import com.helmx.tutorial.docker.utils.DockerClientUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VolumeControllerAdvancedTest {

    @Mock
    private DockerClientUtil dockerClientUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        VolumeController controller = new VolumeController();
        ReflectionTestUtils.setField(controller, "dockerClientUtil", dockerClientUtil);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ─── Volume Backup ──────────────────────────────────────────────────────────

    @Test
    void backupDockerVolume_streamsContentWithCorrectHeaders() throws Exception {
        byte[] tarBytes = "fake-tar-content".getBytes();
        InputStream tarStream = new ByteArrayInputStream(tarBytes);
        when(dockerClientUtil.backupVolume("mydata", "/")).thenReturn(tarStream);

        mockMvc.perform(post("/api/v1/ops/volumes/backup")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "name": "mydata",
                                  "path": "/"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"mydata-backup.tar\""))
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(content().bytes(tarBytes));

        verify(dockerClientUtil).setCurrentHost("unix:///var/run/docker.sock");
        verify(dockerClientUtil).backupVolume("mydata", "/");
    }

    @Test
    void backupDockerVolume_usesDefaultPathWhenNotProvided() throws Exception {
        byte[] tarBytes = new byte[]{1, 2, 3};
        InputStream tarStream = new ByteArrayInputStream(tarBytes);
        when(dockerClientUtil.backupVolume(eq("logs"), any())).thenReturn(tarStream);

        mockMvc.perform(post("/api/v1/ops/volumes/backup")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "name": "logs"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"logs-backup.tar\""));

        verify(dockerClientUtil).backupVolume(eq("logs"), any());
    }

    // ─── Volume Clone ───────────────────────────────────────────────────────────

    @Test
    void cloneDockerVolume_returnsSuccessWhenCloneSucceeds() throws Exception {
        Map<String, Object> cloneResult = new HashMap<>();
        cloneResult.put("status", "success");
        cloneResult.put("message", "Volume cloned successfully");
        when(dockerClientUtil.cloneVolume("source-vol", "target-vol", "local")).thenReturn(cloneResult);

        mockMvc.perform(post("/api/v1/ops/volumes/clone")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "sourceName": "source-vol",
                                  "targetName": "target-vol",
                                  "driver": "local"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("Volume cloned successfully"));

        verify(dockerClientUtil).setCurrentHost("unix:///var/run/docker.sock");
        verify(dockerClientUtil).cloneVolume("source-vol", "target-vol", "local");
    }

    @Test
    void cloneDockerVolume_returnsServerErrorWhenCloneFails() throws Exception {
        Map<String, Object> cloneResult = new HashMap<>();
        cloneResult.put("status", "failed");
        cloneResult.put("message", "Failed to create target volume: already exists");
        when(dockerClientUtil.cloneVolume("source-vol", "target-vol", "local")).thenReturn(cloneResult);

        mockMvc.perform(post("/api/v1/ops/volumes/clone")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "sourceName": "source-vol",
                                  "targetName": "target-vol",
                                  "driver": "local"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Failed to create target volume: already exists"));
    }

    @Test
    void cloneDockerVolume_requiresSourceAndTargetNames() throws Exception {
        mockMvc.perform(post("/api/v1/ops/volumes/clone")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "sourceName": "",
                                  "targetName": "target-vol"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
