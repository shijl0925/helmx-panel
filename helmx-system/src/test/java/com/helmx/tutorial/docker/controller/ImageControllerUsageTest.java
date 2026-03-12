package com.helmx.tutorial.docker.controller;

import com.helmx.tutorial.docker.dto.ImageUsageItem;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ImageControllerUsageTest {

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
    void getImageDiskUsage_returnsItemsWithSizeSummary() throws Exception {
        ImageUsageItem nginx = new ImageUsageItem();
        nginx.setId("abc123456789");
        nginx.setFullId("sha256:abc123456789abcdef");
        nginx.setRepoTags(List.of("nginx:latest"));
        nginx.setSize(54000000L);
        nginx.setSizeHuman("51.50 MB");
        nginx.setVirtualSize(54000000L);
        nginx.setVirtualSizeHuman("51.50 MB");
        nginx.setIsUsed(true);

        ImageUsageItem postgres = new ImageUsageItem();
        postgres.setId("def987654321");
        postgres.setFullId("sha256:def987654321fedcba");
        postgres.setRepoTags(List.of("postgres:15", "postgres:latest"));
        postgres.setSize(395000000L);
        postgres.setSizeHuman("376.70 MB");
        postgres.setVirtualSize(395000000L);
        postgres.setVirtualSizeHuman("376.70 MB");
        postgres.setIsUsed(false);

        when(dockerClientUtil.getImageDiskUsage()).thenReturn(List.of(postgres, nginx));

        mockMvc.perform(post("/api/v1/ops/images/usage")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.totalSize").value(449000000))
                .andExpect(jsonPath("$.data.items[0].repoTags[0]").value("postgres:15"))
                .andExpect(jsonPath("$.data.items[0].sizeHuman").value("376.70 MB"))
                .andExpect(jsonPath("$.data.items[0].isUsed").value(false))
                .andExpect(jsonPath("$.data.items[1].repoTags[0]").value("nginx:latest"))
                .andExpect(jsonPath("$.data.items[1].isUsed").value(true));

        verify(dockerClientUtil).setCurrentHost("unix:///var/run/docker.sock");
        verify(dockerClientUtil).getImageDiskUsage();
    }

    @Test
    void getImageDiskUsage_returnsZeroTotalsForEmptyList() throws Exception {
        when(dockerClientUtil.getImageDiskUsage()).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/ops/images/usage")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.totalSize").value(0))
                .andExpect(jsonPath("$.data.totalSizeHuman").value("0 B"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }
}
