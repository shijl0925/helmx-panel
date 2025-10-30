package com.helmx.tutorial.docker.controller;

import com.github.dockerjava.api.model.Info;
import com.helmx.tutorial.docker.dto.PruneRequest;
import com.helmx.tutorial.docker.dto.StatusRequest;
import com.helmx.tutorial.docker.utils.ByteUtils;
import com.helmx.tutorial.docker.utils.DockerClientUtil;
import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.system.mapper.UserMapper;
import com.helmx.tutorial.system.service.UserService;
import com.helmx.tutorial.utils.ResponseUtil;
import com.helmx.tutorial.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/v1/ops")
public class HomeController {

    @Autowired
    private DockerClientUtil dockerClientUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    // 添加健康检查端点
    @Operation(summary = "Health check endpoint")
    @GetMapping("/health")
    public ResponseEntity<Result> healthCheck() {
        return ResponseUtil.success("OK", Map.of("status", "healthy"));
    }

    @Operation(summary = "Get Docker status")
    @PostMapping("/docker_status")
    public ResponseEntity<Result> GetDockerStatus(@Valid @RequestBody StatusRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        Map<String, Object> result = new HashMap<>();
        result.put("online", false);

        // 获取Docker连接状态
        try {
            boolean online = dockerClientUtil.isConnectionHealthy();
            result.put("online", online);
            if (!online) {
                log.warn("Docker connection failed for host: {}", host);
                return ResponseUtil.success( "Docker connection failed", result);
            }
        } catch (Exception e) {
            log.error("Docker connection failed for host: {}", host, e);
            return ResponseUtil.failed(500, result, "Docker connection failed: " + e.getMessage());
        }

        // 获取Docker信息(CPU核数/内存)
        try {
            Info info = dockerClientUtil.getInfo();
            if (info != null) {
                Integer ncpu = info.getNCPU();
                result.put("ncpu", ncpu);

                long memTotal = info.getMemTotal() != null ? info.getMemTotal() : 0L;
                result.put("memTotal", ByteUtils.formatBytes(memTotal));
            } else {
                result.put("ncpu", 0);
                result.put("memTotal", "0B");
            }
        } catch (Exception e) {
            log.error("Failed to get Docker info for host: {}", host, e);
            result.put("ncpu", 0);
            result.put("memTotal", "0B");
        }

        // 获取Docker状态
        try {
            result.putAll(dockerClientUtil.loadStatus());
        } catch (Exception e) {
            log.error("Failed to load Docker status for host: {}", host, e);
            result.put("status", new HashMap<>());
        }

        return ResponseUtil.success("success", result);
    }

    @Operation(summary = "Docker System Prune")
    @PostMapping("/prune")
    public ResponseEntity<Result> DockerSystemPrune(@Valid @RequestBody PruneRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        String pruneType = criteria.getPruneType();

        // 获取当前用户名
        Long userId = SecurityUtils.getCurrentUserId();
        if (!checkPermission(userId, pruneType)) {
            log.warn("User {} does not have permission to prune {}", userId, pruneType);
            return ResponseUtil.failed(403, null, "Forbidden");
        }

        Map<String, Object> result = dockerClientUtil.pruneCmd(pruneType);
        String status = (String) result.get("status");
        String message = (String) result.get("message");

        if ("success".equals(status)) {
            return ResponseUtil.success(message, result);
        } else {
            log.error("Prune failed: {}", message);
            return ResponseUtil.failed(500, result, message);
        }
    }

    private boolean checkPermission(Long userId, String pruneType) {
        String permission = switch (pruneType) {
            case "BUILD" -> "Ops:Build:Prune";
            case "CONTAINERS" -> "Ops:Container:Prune";
            case "IMAGES" -> "Ops:Image:Prune";
            case "NETWORKS" -> "Ops:Network:Prune";
            case "VOLUMES" -> "Ops:Volume:Prune";
            default -> "";
        };
        log.info("Checking permission for user {} and prune type {}, Permission: {}", userId, pruneType, permission);

        if (userId != null && !pruneType.isEmpty()) {
            if (userService.isSuperAdmin(userId)) {
                return true;
            }

            Set<String> userPermissions = userMapper.selectUserPermissions(userId);
            return userPermissions.contains(permission);
        }
        return false;
    }
}
