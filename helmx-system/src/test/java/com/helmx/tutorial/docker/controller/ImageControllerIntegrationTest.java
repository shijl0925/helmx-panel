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
import com.helmx.tutorial.docker.utils.ImagePushTask;
import com.helmx.tutorial.docker.utils.ImagePushTaskManager;
import com.helmx.tutorial.dto.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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

    private ImageController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new ImageController();
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
    void pullDockerImage_returnsPullResult() throws Exception {
        when(dockerClientUtil.pullImageIfNotExists("nginx:latest", false))
                .thenReturn(Map.of("status", "success", "taskId", "pull-1"));

        mockMvc.perform(post("/api/v1/ops/images/pull")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "imageName": "nginx:latest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("success"))
                .andExpect(jsonPath("$.data.taskId").value("pull-1"));

        var inOrder = inOrder(dockerClientUtil);
        inOrder.verify(dockerClientUtil).setCurrentHost("unix:///var/run/docker.sock");
        inOrder.verify(dockerClientUtil).pullImageIfNotExists("nginx:latest", false);
        inOrder.verify(dockerClientUtil).clearCurrentHost();
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
    void pushDockerImage_returnsPushResult() throws Exception {
        when(dockerClientUtil.pushImage("registry.example.com/demo/app:1.0"))
                .thenReturn(Map.of("status", "success", "taskId", "push-1"));

        mockMvc.perform(post("/api/v1/ops/images/push")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "imageName": "registry.example.com/demo/app:1.0"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("success"))
                .andExpect(jsonPath("$.data.taskId").value("push-1"));

        var inOrder = inOrder(dockerClientUtil);
        inOrder.verify(dockerClientUtil).setCurrentHost("unix:///var/run/docker.sock");
        inOrder.verify(dockerClientUtil).pushImage("registry.example.com/demo/app:1.0");
        inOrder.verify(dockerClientUtil).clearCurrentHost();
    }

    @Test
    void getDockerImagePushTaskStatus_returnsTaskMetadata() throws Exception {
        ImagePushTask task = new ImagePushTask();
        task.setTaskId("push-1");
        task.setStatus("RUNNING");
        task.setMessage("Pushing image");
        task.setStartTime(LocalDateTime.of(2026, 3, 10, 1, 10, 0));
        imagePushTaskManager.addTask("push-1", task);

        mockMvc.perform(post("/api/v1/ops/images/push/task/status")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "taskId": "push-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.message").value("Pushing image"))
                .andExpect(jsonPath("$.data.startTime[0]").value(2026))
                .andExpect(jsonPath("$.data.startTime[1]").value(3))
                .andExpect(jsonPath("$.data.startTime[2]").value(10))
                .andExpect(jsonPath("$.data.startTime[3]").value(1))
                .andExpect(jsonPath("$.data.startTime[4]").value(10));
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
    void buildDockerImage_deduplicatesTagsAndReturnsSuccess() throws Exception {
        when(dockerClientUtil.buildImage(
                eq("FROM eclipse-temurin:21"),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(Set.class),
                eq(null),
                eq(true),
                eq(false),
                eq(null),
                eq(null),
                any()
        )).thenReturn(Map.of("status", "success", "taskId", "build-2"));

        MockMultipartFile dockerfileArchive = new MockMultipartFile("files", "Dockerfile", "text/plain", "FROM base".getBytes());

        mockMvc.perform(multipart("/api/v1/ops/images/build")
                        .file(dockerfileArchive)
                        .param("host", "unix:///var/run/docker.sock")
                        .param("dockerfile", "FROM eclipse-temurin:21")
                        .param("tags", "demo/app:1.0", "demo/app:1.0", "demo/app:latest")
                        .param("pull", "true")
                        .param("noCache", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("success"))
                .andExpect(jsonPath("$.data.taskId").value("build-2"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> tagsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(dockerClientUtil).buildImage(
                eq("FROM eclipse-temurin:21"),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                tagsCaptor.capture(),
                eq(null),
                eq(true),
                eq(false),
                eq(null),
                eq(null),
                any()
        );
        assertEquals(Set.of("demo/app:1.0", "demo/app:latest"), tagsCaptor.getValue());

        var inOrder = inOrder(dockerClientUtil);
        inOrder.verify(dockerClientUtil).setCurrentHost("unix:///var/run/docker.sock");
        inOrder.verify(dockerClientUtil).buildImage(
                eq("FROM eclipse-temurin:21"),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(Set.class),
                eq(null),
                eq(true),
                eq(false),
                eq(null),
                eq(null),
                any()
        );
        inOrder.verify(dockerClientUtil).clearCurrentHost();
    }

    @Test
    void buildDockerImage_whenDockerClientThrows_stillClearsHost() {
        String dockerfilePath = null;
        String gitUrl = null;
        String branch = null;
        String username = null;
        String password = null;
        String buildArgs = null;
        String envs = null;
        Boolean pull = null;
        Boolean noCache = null;
        String labels = null;
        org.springframework.web.multipart.MultipartFile[] files = null;

        doThrow(new RuntimeException("build failed")).when(dockerClientUtil).buildImage(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );

        RuntimeException exception = assertThrows(RuntimeException.class, () -> controller.buildDockerImage(
                "unix:///var/run/docker.sock",
                "FROM eclipse-temurin:21",
                dockerfilePath,
                gitUrl,
                branch,
                username,
                password,
                buildArgs,
                envs,
                pull,
                noCache,
                labels,
                new String[]{"demo/app:1.0"},
                files
        ));

        assertEquals("build failed", exception.getMessage());
        var inOrder = inOrder(dockerClientUtil);
        inOrder.verify(dockerClientUtil).setCurrentHost("unix:///var/run/docker.sock");
        inOrder.verify(dockerClientUtil).buildImage(
                eq("FROM eclipse-temurin:21"),
                eq(dockerfilePath),
                eq(gitUrl),
                eq(branch),
                eq(username),
                eq(password),
                any(Set.class),
                eq(buildArgs),
                eq(pull),
                eq(noCache),
                eq(labels),
                eq(envs),
                eq(files)
        );
        inOrder.verify(dockerClientUtil).clearCurrentHost();
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

    @Test
    void importDockerImage_successReturnsResult() throws Exception {
        when(dockerClientUtil.importImage(any()))
                .thenReturn(Map.of("status", "success", "imageId", "sha256:imported"));

        MockMultipartFile imageTar = new MockMultipartFile("file", "image.tar", "application/x-tar", "archive".getBytes());

        mockMvc.perform(multipart("/api/v1/ops/images/import")
                        .file(imageTar)
                        .param("host", "unix:///var/run/docker.sock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("success"))
                .andExpect(jsonPath("$.data.imageId").value("sha256:imported"));
    }

    @Test
    void exportDockerImage_streamsArchiveAndClearsHost() throws Exception {
        when(dockerClientUtil.exportImage("demo/app:1.0"))
                .thenReturn(new ByteArrayInputStream("tar-content".getBytes()));

        com.helmx.tutorial.docker.dto.ExportImageRequest request = new com.helmx.tutorial.docker.dto.ExportImageRequest();
        request.setHost("unix:///var/run/docker.sock");
        request.setImageName("demo/app:1.0");
        request.setFilename("demo-app.tar");

        ResponseEntity<StreamingResponseBody> response = controller.exportDockerImage(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("attachment; filename=\"demo-app.tar\"", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals("application/x-tar", response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
        assertNotNull(response.getBody());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.getBody().writeTo(outputStream);
        assertEquals("tar-content", outputStream.toString());

        var inOrder = inOrder(dockerClientUtil);
        inOrder.verify(dockerClientUtil).setCurrentHost("unix:///var/run/docker.sock");
        inOrder.verify(dockerClientUtil).exportImage("demo/app:1.0");
        inOrder.verify(dockerClientUtil).clearCurrentHost();
    }

    private Image createImage(String id, String[] repoTags, long size, long created) {
        Image image = new Image();
        ReflectionTestUtils.setField(image, "id", id);
        ReflectionTestUtils.setField(image, "repoTags", repoTags);
        ReflectionTestUtils.setField(image, "size", size);
        ReflectionTestUtils.setField(image, "created", created);
        return image;
    }

    @Test
    void searchDockerHubImages_returnsItemsFromDockerClient() throws Exception {
        List<Map<String, Object>> hubItems = List.of(
                Map.of("name", "nginx", "description", "Official nginx", "starCount", 1000,
                        "isOfficial", true, "isTrusted", true),
                Map.of("name", "nginx-unprivileged", "description", "Rootless nginx",
                        "starCount", 200, "isOfficial", false, "isTrusted", false)
        );
        when(dockerClientUtil.searchImagesOnHub("nginx", 25)).thenReturn(hubItems);

        mockMvc.perform(post("/api/v1/ops/images/hub/search")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "term": "nginx",
                                  "limit": 25
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].name").value("nginx"))
                .andExpect(jsonPath("$.data.items[0].isOfficial").value(true))
                .andExpect(jsonPath("$.data.items[1].name").value("nginx-unprivileged"));
    }

    @Test
    void searchDockerHubImages_usesDefaultLimitWhenNotSpecified() throws Exception {
        when(dockerClientUtil.searchImagesOnHub("redis", 25)).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/ops/images/hub/search")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "term": "redis"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
