package com.helmx.tutorial.docker.controller;

import com.helmx.tutorial.docker.utils.DockerClientUtil;
import com.helmx.tutorial.docker.utils.ImageBuildTaskManager;
import com.helmx.tutorial.docker.utils.ImagePullTaskManager;
import com.helmx.tutorial.docker.utils.ImagePushTaskManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
class ImageControllerBulkRemoveTest {

    @Mock
    private DockerClientUtil dockerClientUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ImageController controller = new ImageController();
        ReflectionTestUtils.setField(controller, "dockerClientUtil", dockerClientUtil);
        ReflectionTestUtils.setField(controller, "imagePullTaskManager", new ImagePullTaskManager());
        ReflectionTestUtils.setField(controller, "imagePushTaskManager", new ImagePushTaskManager());
        ReflectionTestUtils.setField(controller, "imageBuildTaskManager", new ImageBuildTaskManager());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void bulkRemoveImages_returnsSuccessWhenAllRemoved() throws Exception {
        List<Map<String, Object>> results = List.of(
                Map.of("imageId", "sha256:aaa", "status", "success", "message", "Image removed successfully"),
                Map.of("imageId", "sha256:bbb", "status", "success", "message", "Image removed successfully")
        );
        when(dockerClientUtil.bulkRemoveImages(any(), eq(false))).thenReturn(results);

        mockMvc.perform(post("/api/v1/ops/images/bulk/remove")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "imageIds": ["sha256:aaa", "sha256:bbb"],
                                  "force": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("All images removed successfully"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.succeeded").value(2))
                .andExpect(jsonPath("$.data.failed").value(0))
                .andExpect(jsonPath("$.data.items[0].imageId").value("sha256:aaa"))
                .andExpect(jsonPath("$.data.items[1].imageId").value("sha256:bbb"));

        verify(dockerClientUtil).setCurrentHost("unix:///var/run/docker.sock");
        verify(dockerClientUtil).bulkRemoveImages(List.of("sha256:aaa", "sha256:bbb"), false);
    }

    @Test
    void bulkRemoveImages_returnsPartialErrorWhenSomeRemovalsFail() throws Exception {
        List<Map<String, Object>> results = List.of(
                Map.of("imageId", "sha256:aaa", "status", "success", "message", "Image removed successfully"),
                Map.of("imageId", "sha256:bbb", "status", "failed", "message", "image is being used by running container")
        );
        when(dockerClientUtil.bulkRemoveImages(any(), eq(false))).thenReturn(results);

        mockMvc.perform(post("/api/v1/ops/images/bulk/remove")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "imageIds": ["sha256:aaa", "sha256:bbb"],
                                  "force": false
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("1 image(s) failed to remove"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.succeeded").value(1))
                .andExpect(jsonPath("$.data.failed").value(1));
    }

    @Test
    void bulkRemoveImages_requiresNonEmptyImageIds() throws Exception {
        mockMvc.perform(post("/api/v1/ops/images/bulk/remove")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "imageIds": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bulkRemoveImages_passesForceParameterToUtil() throws Exception {
        List<Map<String, Object>> results = List.of(
                Map.of("imageId", "sha256:aaa", "status", "success", "message", "Image removed successfully")
        );
        when(dockerClientUtil.bulkRemoveImages(any(), eq(true))).thenReturn(results);

        mockMvc.perform(post("/api/v1/ops/images/bulk/remove")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "imageIds": ["sha256:aaa"],
                                  "force": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.succeeded").value(1));

        verify(dockerClientUtil).bulkRemoveImages(List.of("sha256:aaa"), true);
    }
}
