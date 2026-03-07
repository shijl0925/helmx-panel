package com.helmx.tutorial.docker.controller;

import com.helmx.tutorial.docker.dto.*;
import com.helmx.tutorial.docker.utils.DockerClientUtil;
import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.utils.ResponseUtil;
import com.github.dockerjava.api.model.Network;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/ops/networks")
public class NetworkController {

    @Autowired
    private DockerClientUtil dockerClientUtil;

    @Operation(summary = "Get all Docker Networks")
    @PostMapping("/all")
    @PreAuthorize("@va.check('Ops:Network:List')")
    public ResponseEntity<Result> SearchDockerNetworks(@Valid @RequestBody NetworkQueryRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        String name = criteria.getName();

        List<Network> networks = dockerClientUtil.listNetworks(name);
        networks.sort((o1, o2) -> o2.getCreated().compareTo(o1.getCreated()));

//        List<NetworkDTO> networkDTOS = networks.stream().map(NetworkDTO::new).toList();
        List<NetworkDTO> networkDTOS = networks.stream().map(item -> {
            NetworkDTO networkDTO = new NetworkDTO(item);
            // 判断是否正在使用
            networkDTO.setIsUsed(dockerClientUtil.isNetworkInUse(item.getId()));
            return networkDTO;
        }).toList();

        return ResponseUtil.success(networkDTOS);
    }

    @Operation(summary = "Search Docker Networks with pagination and filters")
    @PostMapping("/search")
    @PreAuthorize("@va.check('Ops:Network:List')")
    public ResponseEntity<Result> searchDockerNetworks(@Valid @RequestBody NetworkSearchRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        List<Network> networks = dockerClientUtil.listNetworks(criteria.getName());

        // 按驱动类型过滤
        String driver = criteria.getDriver();
        if (driver != null && !driver.isBlank()) {
            networks = networks.stream()
                    .filter(n -> driver.equalsIgnoreCase(n.getDriver()))
                    .collect(Collectors.toList());
        }

        // 按作用域过滤
        String scope = criteria.getScope();
        if (scope != null && !scope.isBlank()) {
            networks = networks.stream()
                    .filter(n -> scope.equalsIgnoreCase(n.getScope()))
                    .collect(Collectors.toList());
        }

        int total = networks.size();
        int page = criteria.getPage() != null ? Math.max(criteria.getPage(), 1) : 1;
        int pageSize = criteria.getPageSize() != null ? Math.max(criteria.getPageSize(), 1) : 10;

        // 排序
        boolean ascending = "asc".equalsIgnoreCase(criteria.getSortOrder());
        Comparator<Network> comparator = Comparator.comparing(this::getNetworkCreatedDate);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        networks.sort(comparator);

        int start = Math.min((page - 1) * pageSize, total);
        int end = Math.min(start + pageSize, total);
        List<Network> items = networks.subList(start, end);

        List<NetworkDTO> networkDTOS = items.stream().map(item -> {
            NetworkDTO networkDTO = new NetworkDTO(item);
            networkDTO.setIsUsed(dockerClientUtil.isNetworkInUse(item.getId()));
            return networkDTO;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("current", page);
        result.put("size", pageSize);
        result.put("pages", (int) Math.ceil((double) total / pageSize));
        result.put("items", networkDTOS);

        return ResponseUtil.success(result);
    }

    @Operation(summary = "Create new docker network")
    @PostMapping("")
    @PreAuthorize("@va.check('Ops:Network:Create')")
    public ResponseEntity<Result> CreateNewDockerNetwork(@Valid @RequestBody NetworkCreateRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        Map<String, Object> result = dockerClientUtil.createNetwork(criteria);

        String status = (String) result.get("status");
        String message = (String) result.remove("message");

        if (status.equals("success")) {
            return ResponseUtil.success(message, result);
        } else {
            log.error("Create network failed: {}", message);
            return ResponseUtil.failed(500, result, message);
        }
    }

    @Operation(summary = "Get Docker Network Info")
    @PostMapping("/info")
    @PreAuthorize("@va.check('Ops:Network:List')")
    public ResponseEntity<Result> GetDockerNetworkInfo(@Valid @RequestBody NetworkInfoRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        Network network = dockerClientUtil.inspectNetwork(criteria.getNetworkId());
        log.info("Network info: {}", network);
        return ResponseUtil.success(new NetworkDTO(network));
    }

    @Operation(summary = "Remove Docker Network")
    @PostMapping("/remove")
    @PreAuthorize("@va.check('Ops:Network:Delete')")
    public ResponseEntity<Result> removeDockerNetwork(@Valid @RequestBody RemoveNetworkRequest criteria) {
        String[] names = criteria.getNames();
        Map<String, Object> result = new HashMap<>();

        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        for (String name : names) {
            Map<String, Object> removeNetworkResult = dockerClientUtil.removeNetwork(name);
            if (removeNetworkResult.get("status").equals("failed")) {
                result.put("status", "failed");
                String message = (String) removeNetworkResult.remove("message");
                return ResponseUtil.failed(500, result, message);
            }
        }

        result.put("status", "success");
        return ResponseUtil.success("Network removed successfully!", result);
    }

    @Operation(summary = "Connect a container to a Docker Network")
    @PostMapping("/connect")
    @PreAuthorize("@va.check('Ops:Network:Connect')")
    public ResponseEntity<Result> connectContainerToNetwork(@Valid @RequestBody NetworkConnectRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        Map<String, Object> result = dockerClientUtil.connectNetwork(criteria.getNetworkId(), criteria.getContainerId());
        String status = (String) result.get("status");
        String message = (String) result.get("message");

        if ("success".equals(status)) {
            return ResponseUtil.success(message, result);
        } else {
            log.error("Connect container to network failed: {}", message);
            return ResponseUtil.failed(500, result, message);
        }
    }

    @Operation(summary = "Disconnect a container from a Docker Network")
    @PostMapping("/disconnect")
    @PreAuthorize("@va.check('Ops:Network:Disconnect')")
    public ResponseEntity<Result> disconnectContainerFromNetwork(@Valid @RequestBody NetworkDisconnectRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        Map<String, Object> result = dockerClientUtil.disconnectNetwork(criteria.getNetworkId(), criteria.getContainerId());
        String status = (String) result.get("status");
        String message = (String) result.get("message");

        if ("success".equals(status)) {
            return ResponseUtil.success(message, result);
        } else {
            log.error("Disconnect container from network failed: {}", message);
            return ResponseUtil.failed(500, result, message);
        }
    }

    @Operation(summary = "Prune unused Docker Networks")
    @PostMapping("/prune")
    @PreAuthorize("@va.check('Ops:Network:Prune')")
    public ResponseEntity<Result> pruneDockerNetworks(@Valid @RequestBody StatusRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        Map<String, Object> result = dockerClientUtil.pruneCmd("NETWORKS");
        String status = (String) result.get("status");
        String message = (String) result.get("message");

        if ("success".equals(status)) {
            return ResponseUtil.success(message, result);
        } else {
            log.error("Prune networks failed: {}", message);
            return ResponseUtil.failed(500, result, message);
        }
    }

    private java.util.Date getNetworkCreatedDate(Network network) {
        return network.getCreated() != null ? network.getCreated() : new java.util.Date(0);
    }
}