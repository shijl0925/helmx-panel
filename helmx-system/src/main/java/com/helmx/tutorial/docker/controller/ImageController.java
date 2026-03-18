package com.helmx.tutorial.docker.controller;

import com.helmx.tutorial.docker.dto.*;
import com.helmx.tutorial.docker.utils.*;
import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.logging.annotation.Log;
import com.helmx.tutorial.utils.ResponseUtil;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
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

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("dockerTaskExecutor")
    private java.util.concurrent.Executor dockerTaskExecutor;

    @Operation(summary = "Get all Docker images")
    @PostMapping("/all")
    @PreAuthorize("@va.check('Ops:Image:List')")
    public ResponseEntity<Result> GetAllDockerImages(@Valid @RequestBody ImageQueryRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);
        List<Image> images = dockerClientUtil.listImages().stream()
                .filter(image -> image.getRepoTags() != null &&
                        Arrays.stream(image.getRepoTags()).noneMatch("<none>:<none>"::equals)).toList();

        List<ImageDTO> imageDTOS = images.stream().map(ImageDTO::new).toList();

        return ResponseUtil.success(imageDTOS);
    }

    @Operation(summary = "Search Docker images")
    @PostMapping("/search")
    @PreAuthorize("@va.check('Ops:Image:List')")
    public ResponseEntity<Result> SearchDockerImages(@RequestBody ImageQueryRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        List<Image> images = dockerClientUtil.listImages().stream()
                .filter(image -> image.getRepoTags() != null &&
                        Arrays.stream(image.getRepoTags()).noneMatch("<none>:<none>"::equals)).toList();

        String name = criteria.getName();
        if (name != null && !name.isEmpty()) {
            images = images.stream().filter(
                    image -> image.getId().contains(name) ||
                            Arrays.stream(image.getRepoTags()).anyMatch(tag -> tag.contains(name))
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
    @PreAuthorize("@va.check('Ops:Image:List')")
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
    @PreAuthorize("@va.check('Ops:Image:Pull')")
    @Log(value = "拉取镜像", resourceName = "#criteria.imageName")
    public ResponseEntity<Result> PullDockerImage(@Valid @RequestBody ImagePullRequest criteria) throws InterruptedException {
        String host = criteria.getHost();
        try {
            dockerClientUtil.setCurrentHost(host);
            Map<String, String> result = dockerClientUtil.pullImageIfNotExists(criteria.getImageName(), false);
            return ResponseUtil.success(result);
        } finally {
            dockerClientUtil.clearCurrentHost();
        }
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
    @PreAuthorize("@va.check('Ops:Image:Push')")
    @Log(value = "推送镜像", resourceName = "#criteria.imageName")
    public ResponseEntity<Result> PushDockerImage(@Valid @RequestBody ImagePushRequest criteria) {
        String host = criteria.getHost();
        try {
            dockerClientUtil.setCurrentHost(host);
            Map<String, String> result = dockerClientUtil.pushImage(criteria.getImageName());
            return ResponseUtil.success(result);
        } finally {
            dockerClientUtil.clearCurrentHost();
        }
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
    @PreAuthorize("@va.check('Ops:Image:Tag')")
    @Log(value = "添加镜像标签", resourceName = "#criteria.imageId")
    public ResponseEntity<Result> tagImage(@Valid @RequestBody ImageTagRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        Map<String, Object> result = dockerClientUtil.tagImage(criteria.getImageId(), criteria.getImageName());
        return ResponseUtil.success(result);
    }

    @Operation(summary = "Remove Docker Image")
    @PostMapping("/remove")
    @PreAuthorize("@va.check('Ops:Image:Delete')")
    @Log(value = "删除镜像", resourceName = "#criteria.imageId")
    public ResponseEntity<Result> removeDockerImage(@Valid @RequestBody RemoveImageRequest criteria) {
        String imageId = criteria.getImageId();
        Boolean force = criteria.getForce();

        Map<String, Object> result = new HashMap<>();

        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        try {
            dockerClientUtil.removeImage(imageId, force);
            result.put("status", "success");
            return ResponseUtil.success("Image removed successfully!", result);
        } catch (Exception e) {
            result.put("status", "failed");
            result.put("message", e.getMessage());
            return ResponseUtil.failed(500, result, "Image removed failed! " + e.getMessage());
        }
    }

    @Operation(summary = "Prune stopped Docker Images to reclaim disk space")
    @PostMapping("/prune")
    @PreAuthorize("@va.check('Ops:Image:Prune')")
    @Log(value = "清理未使用镜像")
    public ResponseEntity<Result> pruneImages(@Valid @RequestBody StatusRequest request) {
        String host = request.getHost();
        dockerClientUtil.setCurrentHost(host);

        Map<String, Object> result = dockerClientUtil.pruneCmd("IMAGES");
        String status = (String) result.get("status");
        String message = (String) result.get("message");

        if ("success".equals(status)) {
            return ResponseUtil.success(message, result);
        } else {
            log.error("Prune images failed: {}", message);
            return ResponseUtil.failed(500, result, message);
        }
    }

    @Operation(summary = "Build Docker Image")
    @PostMapping(value = "/build")
    @PreAuthorize("@va.check('Ops:Image:Build')")
    @Log(value = "构建镜像", resourceName = "#tags")
    public ResponseEntity<Result> buildDockerImage(
            @RequestParam() String host,
            @RequestParam(required = false) String dockerfile,
            @RequestParam(required = false) String dockerfilePath,
            @RequestParam(required = false) String gitUrl,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String buildArgs,
            @RequestParam(required = false) String envs,
            @RequestParam(required = false) Boolean pull,
            @RequestParam(required = false) Boolean noCache,
            @RequestParam(required = false) String labels,
            @RequestParam() String[] tags,
            @RequestParam(value = "files", required = false) MultipartFile[] files
    ) {
        try {
            dockerClientUtil.setCurrentHost(host);
            Set<String> tagSet = new HashSet<>(Arrays.asList(tags));
            Map<String, String> result = dockerClientUtil.buildImage(
                    dockerfile,
                    dockerfilePath,
                    gitUrl,
                    branch,
                    username,
                    password,
                    tagSet,
                    buildArgs,
                    pull,
                    noCache,
                    labels,
                    envs,
                    files
            );
            return ResponseUtil.success(result);
        } finally {
            dockerClientUtil.clearCurrentHost();
        }
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

    /**
     * 通过 SSE（Server-Sent Events）实时推送镜像构建日志与状态，无需客户端轮询。
     * <p>
     * 客户端订阅后会依次收到以下类型的事件：
     * <ul>
     *   <li>{@code stream} — 构建过程中的每一行日志输出</li>
     *   <li>{@code status} — 任务状态变化（PENDING → RUNNING → SUCCESS/FAILED）</li>
     *   <li>{@code complete} — 构建结束后的最终摘要（含状态、消息和时间戳），之后连接关闭</li>
     * </ul>
     */
    @Operation(summary = "Stream Docker Image build output via SSE")
    @GetMapping(value = "/build/stream/{taskId}", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamDockerImageBuildStatus(
            @PathVariable String taskId) {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter =
                new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(30 * 60 * 1000L);

        ImageBuildTask task = imageBuildTaskManager.getTask(taskId);
        if (task == null) {
            try {
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                        .name("error")
                        .data("Task not found: " + taskId));
            } catch (java.io.IOException e) {
                log.error("Error sending SSE error event for taskId: {}", taskId, e);
            }
            emitter.complete();
            return emitter;
        }

        emitter.onTimeout(emitter::complete);
        emitter.onError(t -> emitter.complete());

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                String lastStatus = null;
                while (true) {
                    // 推送新增日志行
                    String line;
                    while ((line = task.getLogQueue().poll()) != null) {
                        emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                .name("stream")
                                .data(line));
                    }

                    // 推送状态变化
                    String currentStatus = task.getStatus();
                    if (!java.util.Objects.equals(currentStatus, lastStatus)) {
                        lastStatus = currentStatus;
                        emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                .name("status")
                                .data(currentStatus));
                    }

                    // 任务已完成且队列已排空，发送终止事件并关闭连接
                    if (task.isCompleted() && task.getLogQueue().isEmpty()) {
                        Map<String, Object> summary = new HashMap<>();
                        summary.put("status", task.getStatus());
                        summary.put("message", task.getMessage());
                        summary.put("startTime", task.getStartTime());
                        summary.put("endTime", task.getEndTime());
                        emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                .name("complete")
                                .data(summary));
                        emitter.complete();
                        return;
                    }

                    Thread.sleep(200);
                }
            } catch (IllegalStateException e) {
                // 客户端断开连接时 SseEmitter 已经关闭
                log.debug("SSE emitter closed for taskId: {}", taskId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                emitter.completeWithError(e);
            } catch (java.io.IOException e) {
                log.error("Error sending SSE event for taskId: {}", taskId, e);
                emitter.completeWithError(e);
            }
        }, dockerTaskExecutor);

        return emitter;
    }


    @PostMapping("/import")
    @PreAuthorize("@va.check('Ops:Image:Import')")
    @Log(value = "导入镜像", resourceName = "#imageTarFile.originalFilename")
    public ResponseEntity<Result> importDockerImage(
            @RequestParam String host,
            @RequestParam("file") MultipartFile imageTarFile) {

        dockerClientUtil.setCurrentHost(host);

        try {
            try (InputStream inputStream = imageTarFile.getInputStream()) {
                Map<String, Object> result = dockerClientUtil.importImage(inputStream);

                if ("success".equals(result.get("status"))) {
                    return ResponseUtil.success(result);
                } else {
                    return ResponseUtil.failed(500, result, (String) result.get("message"));
                }
            }
        } catch (Exception e) {
            log.error("Failed to import image", e);
            return ResponseUtil.failed(500, null, "镜像导入失败: " + e.getMessage());
        }
    }

    @Operation(summary = "Export Docker Image to tar file")
    @PostMapping("/export")
    @PreAuthorize("@va.check('Ops:Image:Export')")
    @Log(value = "导出镜像", resourceName = "#criteria.imageName")
    public ResponseEntity<StreamingResponseBody> exportDockerImage(@Valid @RequestBody ExportImageRequest criteria) {
        String host = criteria.getHost();
        String imageName = criteria.getImageName();
        String filename = criteria.getFilename();

        // 将 host 信息传递到异步线程中
        final String hostForAsync = host;
        final String imageNameForAsync = imageName;

        StreamingResponseBody stream = outputStream -> {
            dockerClientUtil.setCurrentHost(hostForAsync);
            try (InputStream imageInputStream = dockerClientUtil.exportImage(imageNameForAsync)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = imageInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            } catch (Exception e) {
                log.error("Error streaming image data", e);
                throw e;
            } finally {
                dockerClientUtil.clearCurrentHost();
            }
        };

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type", "application/x-tar")
                .body(stream);
    }

    @Operation(summary = "Search Docker Hub for images")
    @PostMapping("/hub/search")
    @PreAuthorize("@va.check('Ops:Image:List')")
    public ResponseEntity<Result> searchDockerHubImages(@Valid @RequestBody ImageHubSearchRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        int limit = criteria.getLimit() != null && criteria.getLimit() > 0 ? criteria.getLimit() : 25;
        List<Map<String, Object>> items = dockerClientUtil.searchImagesOnHub(criteria.getTerm(), limit);

        Map<String, Object> result = new HashMap<>();
        result.put("total", items.size());
        result.put("items", items);

        return ResponseUtil.success(result);
    }

    @Operation(summary = "Bulk remove Docker images")
    @PostMapping("/bulk/remove")
    @PreAuthorize("@va.check('Ops:Image:Delete')")
    @Log(value = "批量删除镜像", resourceName = "#criteria.imageIds")
    public ResponseEntity<Result> bulkRemoveDockerImages(@Valid @RequestBody BulkImageRemoveRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        List<Map<String, Object>> results = dockerClientUtil.bulkRemoveImages(criteria.getImageIds(), criteria.getForce());

        long failedCount = results.stream().filter(r -> "failed".equals(r.get("status"))).count();

        Map<String, Object> result = new HashMap<>();
        result.put("total", results.size());
        result.put("failed", failedCount);
        result.put("succeeded", results.size() - failedCount);
        result.put("items", results);

        if (failedCount > 0) {
            log.error("Bulk remove images: {} out of {} failed", failedCount, results.size());
            return ResponseUtil.failed(500, result, failedCount + " image(s) failed to remove");
        }
        return ResponseUtil.success("All images removed successfully", result);
    }

    @Operation(summary = "Get image disk usage summary")
    @PostMapping("/usage")
    @PreAuthorize("@va.check('Ops:Image:List')")
    public ResponseEntity<Result> getImageDiskUsage(@Valid @RequestBody ImageUsageRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        List<ImageUsageItem> items = dockerClientUtil.getImageDiskUsage();

        long totalSize = items.stream().mapToLong(ImageUsageItem::getSize).sum();
        long totalVirtualSize = items.stream().mapToLong(ImageUsageItem::getVirtualSize).sum();

        Map<String, Object> result = new HashMap<>();
        result.put("total", items.size());
        result.put("totalSize", totalSize);
        result.put("totalSizeHuman", com.helmx.tutorial.docker.utils.ByteUtils.formatBytes(totalSize));
        result.put("totalVirtualSize", totalVirtualSize);
        result.put("totalVirtualSizeHuman", com.helmx.tutorial.docker.utils.ByteUtils.formatBytes(totalVirtualSize));
        result.put("items", items);

        return ResponseUtil.success(result);
    }
}
