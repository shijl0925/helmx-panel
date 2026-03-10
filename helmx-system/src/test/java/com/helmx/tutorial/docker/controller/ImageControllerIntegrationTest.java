package com.helmx.tutorial.docker.controller;

import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.RootFS;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.Image;
import com.helmx.tutorial.docker.dto.ImageHistoryItem;
import com.helmx.tutorial.docker.utils.DockerClientUtil;
import com.helmx.tutorial.docker.utils.ImageBuildTask;
import com.helmx.tutorial.docker.utils.ImageBuildTaskManager;
import com.helmx.tutorial.docker.utils.ImagePullTask;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ImageControllerIntegrationTest {

    @Mock
    private DockerClientUtil dockerClientUtil;

    private final ImagePullTaskManager imagePullTaskManager = new ImagePullTaskManager();
    private final ImagePushTaskManager imagePushTaskManager = new ImagePushTaskManager();
    private final ImageBuildTaskManager imageBuildTaskManager = new ImageBuildTaskManager();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ImageController controller = new ImageController();
        ReflectionTestUtils.setField(controller, "dockerClientUtil", dockerClientUtil);
        ReflectionTestUtils.setField(controller, "imagePullTaskManager", imagePullTaskManager);
        ReflectionTestUtils.setField(controller, "imagePushTaskManager", imagePushTaskManager);
        ReflectionTestUtils.setField(controller, "imageBuildTaskManager", imageBuildTaskManager);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllDockerImages_filtersDanglingImages() throws Exception {
        Image dangling = createImage("sha256:unused", new String[]{"<none>:<none>"}, 512L, 10L);
        Image tagged = createImage("sha256:used", new String[]{"nginx:latest"}, 2048L, 20L);
        when(dockerClientUtil.listImages()).thenReturn(List.of(dangling, tagged));

        mockMvc.perform(post("/api/v1/ops/images/all")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value("used"))
                .andExpect(jsonPath("$.data[0].tags[0]").value("nginx:latest"))
                .andExpect(jsonPath("$.data[0].size").value("2.00 KB"));
    }

    @Test
    void searchDockerImages_returnsPagedResultsWithUsageFlag() throws Exception {
        Image appImage = createImage("sha256:app1", new String[]{"demo/app:1.0"}, 4096L, 30L);
        Image otherImage = createImage("sha256:other2", new String[]{"demo/other:1.0"}, 1024L, 40L);
        Container runningContainer = new Container();
        ReflectionTestUtils.setField(runningContainer, "imageId", "sha256:app1");

        when(dockerClientUtil.listImages()).thenReturn(List.of(appImage, otherImage));
        when(dockerClientUtil.listContainers()).thenReturn(List.of(runningContainer));

        mockMvc.perform(post("/api/v1/ops/images/search")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "name": "demo/app",
                                  "page": 1,
                                  "pageSize": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value("app1"))
                .andExpect(jsonPath("$.data.items[0].isUsed").value(true));
    }

    @Test
    void getDockerImageInfo_returnsMappedInspectResponseAndHistory() throws Exception {
        InspectImageResponse response = new InspectImageResponse()
                .withId("sha256:image-123")
                .withAuthor("helmx")
                .withComment("base image")
                .withRepoTags(List.of("demo/app:1.0"))
                .withSize(2048L)
                .withCreated("2026-03-10T00:00:00Z")
                .withArch("amd64")
                .withOs("linux")
                .withConfig(new ContainerConfig()
                        .withCmd(new String[]{"java", "-jar", "app.jar"})
                        .withEntrypoint(new String[]{"/bin/sh", "-c"})
                        .withEnv(new String[]{"SPRING_PROFILES_ACTIVE=prod"})
                        .withLabels(Map.of("maintainer", "helmx")))
                .withRootFS(new RootFS().withLayers(List.of("layer-1", "layer-2")));
        when(dockerClientUtil.inspectImage("sha256:image-123")).thenReturn(response);
        when(dockerClientUtil.getImageHistory("sha256:image-123"))
                .thenReturn(List.of(new ImageHistoryItem("layer-1", "2026-03-10T00:00:00Z", "RUN echo hi", "1 KB", List.of("demo/app:1.0"), "created")));

        mockMvc.perform(post("/api/v1/ops/images/info")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "imageId": "sha256:image-123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value("image-123"))
                .andExpect(jsonPath("$.data.author").value("helmx"))
                .andExpect(jsonPath("$.data.repoTags[0]").value("demo/app:1.0"))
                .andExpect(jsonPath("$.data.build").value("amd64,linux"))
                .andExpect(jsonPath("$.data.history[0].id").value("layer-1"));
    }

    @Test
    void getDockerImagePullTaskStatus_returnsTaskMetadata() throws Exception {
        ImagePullTask task = new ImagePullTask();
        task.setTaskId("pull-1");
        task.setStatus("RUNNING");
        task.setMessage("Pulling image");
        task.setStartTime(LocalDateTime.of(2026, 3, 10, 1, 0, 0));
        imagePullTaskManager.addTask("pull-1", task);

        mockMvc.perform(post("/api/v1/ops/images/pull/task/status")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "taskId": "pull-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.message").value("Pulling image"))
                .andExpect(jsonPath("$.data.startTime[0]").value(2026))
                .andExpect(jsonPath("$.data.startTime[1]").value(3))
                .andExpect(jsonPath("$.data.startTime[2]").value(10))
                .andExpect(jsonPath("$.data.startTime[3]").value(1))
                .andExpect(jsonPath("$.data.startTime[4]").value(0));
    }

    @Test
    void getDockerImageBuildTaskStatus_returnsStreamData() throws Exception {
        ImageBuildTask task = new ImageBuildTask();
        task.setTaskId("build-1");
        task.setStatus("SUCCESS");
        task.setMessage("Build complete");
        task.setStartTime(LocalDateTime.of(2026, 3, 10, 1, 0, 0));
        task.setEndTime(LocalDateTime.of(2026, 3, 10, 1, 5, 0));
        task.setStream("Step 1/2\nStep 2/2");
        imageBuildTaskManager.addTask("build-1", task);

        mockMvc.perform(post("/api/v1/ops/images/build/task/status")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "taskId": "build-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.stream").value("Step 1/2\nStep 2/2"))
                .andExpect(jsonPath("$.data.endTime[0]").value(2026))
                .andExpect(jsonPath("$.data.endTime[1]").value(3))
                .andExpect(jsonPath("$.data.endTime[2]").value(10))
                .andExpect(jsonPath("$.data.endTime[3]").value(1))
                .andExpect(jsonPath("$.data.endTime[4]").value(5));
    }

    @Test
    void tagImage_returnsDockerClientResult() throws Exception {
        when(dockerClientUtil.tagImage("sha256:image-123", "demo/app:stable"))
                .thenReturn(Map.of("status", "success", "tag", "demo/app:stable"));

        mockMvc.perform(post("/api/v1/ops/images/add_tag")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "imageId": "sha256:image-123",
                                  "imageName": "demo/app:stable"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("success"))
                .andExpect(jsonPath("$.data.tag").value("demo/app:stable"));
    }

    @Test
    void removeDockerImage_whenDockerClientThrowsReturnsServerError() throws Exception {
        doThrow(new RuntimeException("image in use")).when(dockerClientUtil).removeImage("sha256:image-123", true);

        mockMvc.perform(post("/api/v1/ops/images/remove")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "imageId": "sha256:image-123",
                                  "force": true
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Image removed failed! image in use"))
                .andExpect(jsonPath("$.data.status").value("failed"))
                .andExpect(jsonPath("$.data.message").value("image in use"));
    }

    private Image createImage(String id, String[] repoTags, long size, long created) {
        Image image = new Image();
        ReflectionTestUtils.setField(image, "id", id);
        ReflectionTestUtils.setField(image, "repoTags", repoTags);
        ReflectionTestUtils.setField(image, "size", size);
        ReflectionTestUtils.setField(image, "created", created);
        return image;
    }
}
