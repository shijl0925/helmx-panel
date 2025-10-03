package com.helmx.tutorial.docker.controller;

import com.helmx.tutorial.docker.dto.*;
import com.helmx.tutorial.docker.utils.*;
import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.utils.ResponseUtil;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/v1/ops/images")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private DockerClientUtil dockerClientUtil;

    @Autowired
    private ImagePullTaskManager imagePullTaskManager;

    @Autowired
    private ImagePushTaskManager imagePushTaskManager;

    @Autowired
    private ImageBuildTaskManager imageBuildTaskManager;

    @Operation(summary = "Get all Docker images")
    @PostMapping("/all")
    public ResponseEntity<Result> GetAllDockerImages(@RequestBody ImageQueryRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);
        List<Image> images = dockerClientUtil.listImages();

        List<ImageDTO> imageDTOS = images.stream().map(ImageDTO::new).toList();

        return ResponseUtil.success(imageDTOS);
    }

    @Operation(summary = "Search Docker images")
    @PostMapping("/search")
    public ResponseEntity<Result> SearchDockerImages(@RequestBody ImageQueryRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        List<Image> images = dockerClientUtil.listImages();
        String name = criteria.getName();
        if (name != null && !name.isEmpty()) {
            images = images.stream().filter(
                    image -> image.getId().contains(name) ||
                            Arrays.stream(image.getRepoTags()).allMatch(tag -> tag.contains(name))
            ).toList();
        }

        int total = images.size();

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);

        int page = criteria.getPage() != null ? criteria.getPage() : 1;
        int pageSize = criteria.getPageSize() != null ? criteria.getPageSize() : 10;

        // 参数校验，防止越界
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1) {
            pageSize = 10;
        }

        result.put("current", page);
        result.put("size", pageSize);
        result.put("pages", (int) Math.ceil((double) total / pageSize));

        // 防止起始位置超出总数量
        int start = Math.min((page - 1) * pageSize, total);
        int end = Math.min(start + pageSize, total);

//        // 添加排序方向支持
//        boolean ascending = "asc".equalsIgnoreCase(criteria.getSortOrder());
//        Comparator<Image> comparator = Comparator.comparing(Image::getId);
//        if (!ascending) {
//            comparator = comparator.reversed();
//        }
//        images.sort(comparator);

        // 获取所有容器
        List<Container> containers = dockerClientUtil.listContainers();
        // 获取所有容器的镜像ID
        List<String> imageIds = containers.stream().map(Container::getImageId).toList();

        List<Image> items = images.subList(start, end);

        List<ImageDTO> imageDTOS = new ArrayList<>();
        for (Image image : items) {
            ImageDTO imageDTO = new ImageDTO(image);
            // 判断镜像是否正在使用
            imageDTO.setIsUsed(imageIds.contains(image.getId()));
            imageDTOS.add(imageDTO);
        }

        result.put("items", imageDTOS);

        return ResponseUtil.success(result);
    }

    @Operation(summary = "Get Docker Image Info")
    @PostMapping("/info")
    public ResponseEntity<Result> GetDockerImageInfo(@RequestBody ImageInfoRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        InspectImageResponse image = dockerClientUtil.inspectImage(criteria.getImageId());
        ImageInfo imageInfo = new ImageInfo(image);

        imageInfo.setHistory(dockerClientUtil.getImageHistory(criteria.getImageId()));

        return ResponseUtil.success(imageInfo);
    }

    @Operation(summary = "Pull Docker Image")
    @PostMapping("/pull")
    public ResponseEntity<Result> PullDockerImage(@RequestBody ImagePullRequest criteria) throws InterruptedException {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        Map<String, String> result = dockerClientUtil.pullImageIfNotExists(criteria.getImageName());

        return ResponseUtil.success(result);
    }

    @Operation(summary = "Get Docker Image pull task status")
    @PostMapping("/pull/task/status")
    public ResponseEntity<Result> getDockerImagePullTaskStatus(@RequestBody ImageTaskStatusRequest criteria) {
        String taskId = criteria.getTaskId();
        ImagePullTask task = imagePullTaskManager.getTask(taskId);

        if (task == null) {
            return ResponseUtil.success("Task not found", null);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", task.getStatus());
        result.put("message", task.getMessage());
        result.put("startTime", task.getStartTime());
        result.put("endTime", task.getEndTime());

        return ResponseUtil.success(result);
    }

    @Operation(summary = "Push Docker Image")
    @PostMapping("/push")
    public ResponseEntity<Result> PushDockerImage(@RequestBody ImagePushRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        Map<String, String> result = dockerClientUtil.pushImage(criteria.getImageName());

        return ResponseUtil.success(result);
    }

    @Operation(summary = "Get Docker Image push task status")
    @PostMapping("/push/task/status")
    public ResponseEntity<Result> getDockerImagePushTaskStatus(@RequestBody ImageTaskStatusRequest criteria) {
        String taskId = criteria.getTaskId();
        ImagePushTask task = imagePushTaskManager.getTask(taskId);

        if (task == null) {
            return ResponseUtil.success("Task not found", null);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", task.getStatus());
        result.put("message", task.getMessage());
        result.put("startTime", task.getStartTime());
        result.put("endTime", task.getEndTime());

        return ResponseUtil.success(result);
    }


    @Operation(summary = "Add tag for image")
    @PostMapping("/add_tag")
    public ResponseEntity<Result> tagImage(@RequestBody ImageTagRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        Map<String, Object> result = dockerClientUtil.tagImage(criteria.getImageId(), criteria.getImageName());
        return ResponseUtil.success(result);
    }

    @Operation(summary = "Remove Docker Image")
    @PostMapping("/remove")
    public ResponseEntity<Result> removeDockerImage(@RequestBody removeImageRequest criteria) {
        String imageId = criteria.getImageId();
        Boolean force = criteria.getForce();

        Map<String, Object> result = new HashMap<>();

        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        try {
            dockerClientUtil.removeImage(imageId, force);
            result.put("status", "success");
            return ResponseUtil.success("Container removed successfully!", result);
        } catch (Exception e) {
            result.put("status", "failed");
            result.put("message", e.getMessage());
            return ResponseUtil.success("Image removed failed!" + e.getMessage(), result);
        }
    }

    @Operation(summary = "Build Docker Image")
    @PostMapping(value = "/build")
    public ResponseEntity<Result> buildDockerImage(
            @RequestParam() String host,
            @RequestParam() String dockerfile,
            @RequestParam(required = false) String buildArgs,
            @RequestParam(required = false) String envs,
            @RequestParam(required = false) Boolean pull,
            @RequestParam(required = false) Boolean noCache,
            @RequestParam(required = false) String labels,
            @RequestParam() String[] tags,
            @RequestParam(value = "files", required = false) MultipartFile[] files
    ) {
        dockerClientUtil.setCurrentHost(host);

        Set<String> tagSet = new HashSet<>(Arrays.asList(tags));
        Map<String, String> result = dockerClientUtil.buildImage(dockerfile, tagSet, buildArgs, pull, noCache, labels, envs, files);
        return ResponseUtil.success(result);
    }

    @Operation(summary = "Get Docker Image build task status")
    @PostMapping("/build/task/status")
    public ResponseEntity<Result> getDockerImageBuildTaskStatus(@RequestBody ImageTaskStatusRequest criteria) {
        String taskId = criteria.getTaskId();
        ImageBuildTask task = imageBuildTaskManager.getTask(taskId);

        if (task == null) {
            return ResponseUtil.success("Task not found", null);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", task.getStatus());
        result.put("message", task.getMessage());
        result.put("startTime", task.getStartTime());
        result.put("endTime", task.getEndTime());
        result.put("stream", task.getStream());

        return ResponseUtil.success(result);
    }
}
