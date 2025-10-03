package com.helmx.tutorial.docker.controller;

import com.alibaba.fastjson2.JSONObject;
import com.helmx.tutorial.docker.dto.*;
import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.utils.ResponseUtil;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.helmx.tutorial.docker.utils.DockerClientUtil;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/ops/containers")
public class ContainerController {

    @Autowired
    private DockerClientUtil dockerClientUtil;

    @Operation(summary = "Create Docker Container")
    @PostMapping("")
    public ResponseEntity<Result> CreateDockerContainer(@RequestBody ContainerCreateRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        Map<String, Object> result = dockerClientUtil.createContainer(criteria);
        String status = (String) result.get("status");
        String message = (String) result.get("message");

        if ("success".equals(status)) {
            String containerId = (String) result.get("containerId");
            log.info("Create container ID: {}", containerId);
            return ResponseUtil.success(message, result);
        } else {
            log.error("Failed to create container: {}", message);
            return ResponseUtil.failed(500, result, message);
        }
    }

    @Operation(summary = "Search Docker Containers")
    @PostMapping("/search")
    public ResponseEntity<Result> SearchDockerContainers(@RequestBody ContainerQueryRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host); // "tcp://"+host+":2375"

        List<Container> containers = dockerClientUtil.searchContainers(criteria);
        int total = containers.size();

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

        // 添加排序方向支持
        boolean ascending = "asc".equalsIgnoreCase(criteria.getSortOrder());
        String orderBy = criteria.getSortBy() != null ? criteria.getSortBy() : "created";

        Comparator<Container> comparator;

        switch (orderBy) {
            case "state":
                comparator = Comparator.comparing(Container::getState, Comparator.nullsFirst(String::compareTo));
                break;
            case "name":
                java.util.function.Function<Container, String> nameExtractor = container -> {
                    List<String> names = List.of(container.getNames());
                    return !names.isEmpty() ? names.getFirst() : "";
                };
                comparator = Comparator.comparing(nameExtractor, Comparator.nullsFirst(String::compareTo));
                break;
            default:
                comparator = Comparator.comparingLong(Container::getCreated);
                break;
        }

        // 倒序
        if (!ascending) {
            comparator = comparator.reversed();
        }

        containers.sort(comparator);

        List<Container> items = containers.subList(start, end);

//        // 使用 Project Loom 虚拟线程
//        List<ContainerDTO> containerDTOs;
//        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
//            containerDTOs = items.stream()
//                    .map(container -> CompletableFuture.supplyAsync(() -> {
//                        ContainerDTO dto = new ContainerDTO(container);
//                        try {
//                            JSONObject stats = dockerClientUtil.getContainerStats(container.getId(), false);
//                            ContainerStats containerStats = new ContainerStats(stats);
//                            dto.setStats(containerStats);
//                        } catch (Exception e) {
//                            log.warn("Failed to get stats for container: {}", container.getId(), e);
//                        }
//                        return dto;
//                    }, executor))
//                    .toList()
//                    .parallelStream()
//                    .map(CompletableFuture::join)
//                    .toList();
//        }
        List<ContainerDTO> containerDTOs = items.stream().map(ContainerDTO::new).toList();

        result.put("items", containerDTOs);

        return ResponseUtil.success(result);
    }

    @Operation(summary = "Get Docker Container info")
    @PostMapping("/info")
    public ResponseEntity<Result> GetDockerContainerInfo(@RequestBody ContainerInfoRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        try {
            InspectContainerResponse containerResponse = dockerClientUtil.inspectContainer(criteria.getContainerId());
            ContainerOperate result = new ContainerOperate(containerResponse);
            return ResponseUtil.success(result);
        } catch (Exception e) {
            return ResponseUtil.failed(500, null, e.getMessage());
        }
    }

    @Operation(summary = "Get Docker Container stats")
    @PostMapping("/stats")
    public ResponseEntity<Result> GetDockerContainerStats(@RequestBody ContainerInfoRequest criteria) {
        String containerId = criteria.getContainerId();

        try {
            String host = criteria.getHost();
            dockerClientUtil.setCurrentHost(host);

            JSONObject stats = dockerClientUtil.getContainerStats(containerId, false);
            if (criteria.isSimple()) {
                ContainerStats containerStats = new ContainerStats(stats);
                return ResponseUtil.success(containerStats);
            } else {
                return ResponseUtil.success(stats);
            }
        } catch (Exception e) {
            log.warn("Failed to get stats for container: {}", containerId, e);
            return ResponseUtil.failed(500, null, "Failed to get stats for container: " + containerId);
        }
    }

    @Operation(summary = "Get Docker Container top")
    @PostMapping("/top")
    public ResponseEntity<Result> GetDockerContainerTop(@RequestBody ContainerInfoRequest criteria) {
        String containerId = criteria.getContainerId();

        try {
            String host = criteria.getHost();
            dockerClientUtil.setCurrentHost(host);

            JSONObject top = dockerClientUtil.getContainerTop(containerId);
            return ResponseUtil.success(top);
        } catch (Exception e) {
            log.warn("Failed to get top for container: {}", containerId, e);
            return ResponseUtil.failed(500, null, "Failed to get top for container: " + containerId);
        }
    }

     @Operation(summary = "Copy file from container")
     @PostMapping("/copy/from")
     public ResponseEntity<?> copyFileFromContainer(@RequestBody ContainerCopyRequest request) {
         try {
             String host = request.getHost();
             dockerClientUtil.setCurrentHost(host);

             String containerId = request.getContainerId();
             String containerPath = request.getContainerPath();

             byte[] fileContent = dockerClientUtil.copyFileFromContainer(containerId, containerPath);

             // 提取文件名
             String fileName = containerPath.substring(containerPath.lastIndexOf("/") + 1);

             // 设置响应头，使浏览器能够下载文件
             HttpHeaders headers = new HttpHeaders();
             headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
             headers.setContentDispositionFormData("attachment", fileName);
             headers.setContentLength(fileContent.length);

             return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
         } catch (Exception e) {
             log.error("Failed to copy file from container: {}", request.getContainerId(), e);
             return ResponseUtil.failed(500, null, e.getMessage());
         }
     }

     @Operation(summary = "Copy file to container")
     @PostMapping("/copy/to")
     public ResponseEntity<Result> copyFileToContainer(
             @RequestParam("file") MultipartFile file,
             @RequestParam() String host,
             @RequestParam() String containerId,
             @RequestParam() String containerPath
     ) {
         try {
             dockerClientUtil.setCurrentHost(host);
             dockerClientUtil.copyFileToContainer(containerId, containerPath, file);
             return ResponseUtil.success("File copied successfully to container");
         } catch (Exception e) {
             log.error("Failed to copy file to container: {}", containerId, e);
             return ResponseUtil.failed(500, null, "Failed to copy file to container: " + e.getMessage());
         }
     }

    @Operation(summary = "Operate Docker Container")
    @PostMapping("/operate")
    public ResponseEntity<Result> OperateDockerContainer(@RequestBody ContainerOperation criteria) {
        String operation = criteria.getOperation();
        String containerId = criteria.getContainerId();
        Map<String, Object> result = new HashMap<>();

        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        switch (operation) {
            case "start" -> result = dockerClientUtil.startContainer(containerId);
            case "stop" -> result = dockerClientUtil.stopContainer(containerId);
            case "restart" -> result = dockerClientUtil.restartContainer(containerId);
            case "remove" -> result = dockerClientUtil.removeContainer(containerId);
            case "kill" -> result = dockerClientUtil.killContainer(containerId);
            case "pause" -> result = dockerClientUtil.pauseContainer(containerId);
            case "unpause" -> result = dockerClientUtil.unpauseContainer(containerId);
        }
        String status = (String) result.get("status");
        String message = (String) result.remove("message");

        if (status.equals("success")) {
            return ResponseUtil.success(message, result);
        } else {
            log.error("Operate container failed: {}", message);
            return ResponseUtil.failed(500, result, message);
        }
    }

    @Operation(summary = "Get container logs")
    @PostMapping("/logs")
    public ResponseEntity<Result> GetContainerLogs(@RequestBody ContainerLogRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        String containerId = criteria.getContainerId();
        int tail = criteria.getTail();

        String logs = dockerClientUtil.getContainerLogs(containerId, tail);
        return ResponseUtil.success("Get container logs successfully!", logs);
    }

    @Operation(summary = "Rename Docker Container")
    @PostMapping("/rename")
    public ResponseEntity<Result> RenameDockerContainer(@RequestBody ContainerRenameRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        Map<String, Object> result = dockerClientUtil.renameContainer(criteria);

        String status = (String) result.get("status");
        String message = (String) result.remove("message");

        if (status.equals("success")) {
            return ResponseUtil.success(message, result);
        } else {
            return ResponseUtil.failed(500, result, message);
        }
    }

    @Operation(summary = "Get Docker Container inspect")
    @PostMapping("/inspect")
    public ResponseEntity<Result> GetDockerContainerInspect(@RequestBody ContainerInfoRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        InspectContainerResponse inspect = dockerClientUtil.inspectContainer(criteria.getContainerId());
        JSONObject jsonObject = DockerClientUtil.toJSON(inspect);

        return ResponseUtil.success(jsonObject);
    }

    @Operation(summary = "Update Docker Container")
    @PostMapping("/update")
    public ResponseEntity<Result> UpdateDockerContainer(@RequestBody ContainerCreateRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        Map<String, Object> result = dockerClientUtil.updateContainer(criteria);

        String status = (String) result.get("status");
        String message = (String) result.get("message");
        if ("success".equals(status)) {
            String newContainerId = (String) result.get("newContainerId");
            log.info("Update container with ID: {}, new container ID: {}", criteria.getContainerId(), newContainerId);
            return ResponseUtil.success(message, result);
        } else {
            log.error("Update container failed: {}", message);
            return ResponseUtil.failed(500, result, message);
        }
    }

    @Operation(summary = "Commit Docker Container")
    @PostMapping("/commit")
    public ResponseEntity<Result> CommitDockerContainer(@RequestBody ContainerCommitRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        Map<String, Object> result = dockerClientUtil.commitContainer(criteria.getContainerId(), criteria.getRepository());
        return ResponseUtil.success(result);
    }

    @Operation(summary = "Update container networks")
    @PostMapping("/networks")
    public ResponseEntity<Result> updateContainerNetworks(@RequestBody ContainerNetworkUpdateRequest request) {
        String containerId = request.getContainerId();

        String host = request.getHost();
        dockerClientUtil.setCurrentHost(host);

        // 获取当前容器连接的网络
        Set<String> currentNetworks = dockerClientUtil.getContainerNetworks(containerId);

        // 断开不再需要的网络连接
        for (String network : currentNetworks) {
            if (!Arrays.asList(request.getNetworks()).contains(network)) {
                Map<String, Object> result = dockerClientUtil.disconnectNetwork(network, containerId);
                if ("failed".equals(result.get("status"))) {
                    return ResponseUtil.failed(500, result, (String) result.get("message"));
                }
            }
        }

        // 连接新网络
        for (String networkName : request.getNetworks()) {
            if (!currentNetworks.contains(networkName)) {
                Map<String, Object> result = dockerClientUtil.connectNetwork(networkName, containerId);
                if ("failed".equals(result.get("status"))) {
                    return ResponseUtil.failed(500, result, (String) result.get("message"));
                }
            }
        }

        return ResponseUtil.success("Networks updated successfully");
    }

    @Operation(summary = "Execute command in Docker Container")
    @PostMapping("/exec")
    public ResponseEntity<Result> ExecuteCommandInContainer(@RequestBody ContainerExecRequest request) {
        try {
            ContainerExecResponse response = dockerClientUtil.execCommand(request);

            if ("success".equals(response.getStatus())) {
                return ResponseUtil.success("Command executed successfully", response);
            } else {
                return ResponseUtil.failed(500, response, response.getError());
            }
        } catch (Exception e) {
            log.error("Failed to execute command in container: {}", request.getContainerId(), e);
            return ResponseUtil.failed(500, null, "Failed to execute command: " + e.getMessage());
        }
    }
}
