package com.helmx.tutorial.docker.controller;

import com.helmx.tutorial.docker.dto.*;
import com.github.dockerjava.api.command.InspectVolumeResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.helmx.tutorial.docker.utils.DockerClientUtil;
import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.utils.ResponseUtil;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/ops/volumes")
public class VolumeController {

    @Autowired
    private DockerClientUtil dockerClientUtil;

    @Operation(summary = "List all Docker Volumes")
    @PostMapping("/all")
    public ResponseEntity<Result> listDockerVolumes(@RequestBody VolumeQueryRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        List<InspectVolumeResponse> volumes = dockerClientUtil.listVolumed();

        String name = criteria.getName();
        if (name != null) {
            volumes.removeIf(volume -> !volume.getName().contains(name));
        }

        // 按照名称排序
        volumes.sort((v1, v2) -> v2.getName().compareTo(v1.getName()));

        List<VolumeDTO> volumeDTOS = volumes.stream()
                .map(VolumeDTO::new)
                .toList();

        return ResponseUtil.success(volumeDTOS);
    }

    @Operation(summary = "Search docker volumes")
    @PostMapping("/search")
    public ResponseEntity<Result> SearchDockerVolumes(@RequestBody VolumeQueryRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        List<InspectVolumeResponse> volumes = dockerClientUtil.searchVolumed(criteria);
        int total = volumes.size();

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
        // volumes.sort((v1, v2) -> v2.getName().compareTo(v1.getName()));
        boolean ascending = "asc".equalsIgnoreCase(criteria.getSortOrder());
        Comparator<InspectVolumeResponse> comparator = Comparator.comparing(InspectVolumeResponse::getName);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        volumes.sort(comparator);

        List<InspectVolumeResponse> items = volumes.subList(start, end);

        List<VolumeDTO> volumeDTOS = items.stream().map(item -> {
            VolumeDTO volumeDTO = new VolumeDTO(item);

            InspectVolumeResponse detail = dockerClientUtil.inspectVolume(item.getName());
            if (detail != null) {
                volumeDTO.setScope((String) detail.getRawValues().get("Scope"));
                volumeDTO.setCreatedAt((String) detail.getRawValues().get("CreatedAt"));
            }
            volumeDTO.setIsUsed(dockerClientUtil.isVolumeInUse(item.getName()));

            return volumeDTO;
        }).collect(Collectors.toList());

        result.put("items", volumeDTOS);

        return ResponseUtil.success(result);
    }

    @Operation(summary = "Create new docker volume")
    @PostMapping("")
    public ResponseEntity<Result> CreateNewDockerVolume(@RequestBody VolumeCreateRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        Map<String, Object> result = dockerClientUtil.createVolume(criteria);

        String status = (String) result.get("status");
        String message = (String) result.remove("message");

        if (status.equals("success")) {
            return ResponseUtil.success(message, result);
        } else {
            log.error("Create volume failed: {}", message);
            return ResponseUtil.failed(500, result, message);
        }
    }

    @Operation(summary = "Get Docker Volume Info")
    @PostMapping("/info")
    public ResponseEntity<Result> GetDockerVolumeInfo(@RequestBody VolumeInfoRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        InspectVolumeResponse volume = dockerClientUtil.inspectVolume(criteria.getName());
        log.info("volumeDTO: {}", volume.getRawValues());

        VolumeDTO volumeDTO = new VolumeDTO(volume);
        volumeDTO.setContainers(dockerClientUtil.getVolumeContainers(volume.getName()));
        return ResponseUtil.success(volumeDTO);
    }

    @Operation(summary = "Remove Docker Volume")
    @PostMapping("/remove")
    public ResponseEntity<Result> removeDockerVolume(@RequestBody removeVolumeRequest criteria) {
        String[] names = criteria.getNames();
        Map<String, Object> result = new HashMap<>();

        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        for (String name : names) {
            Map<String, Object> removeVolumeResult = dockerClientUtil.removeVolume(name);
            if (removeVolumeResult.get("status").equals("failed")) {
                result.put("status", "failed");
                String message = (String) removeVolumeResult.remove("message");
                return ResponseUtil.failed(500, result, message);
            }
        }

        result.put("status", "success");
        return ResponseUtil.success("Volume removed successfully!", result);
    }
}
