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
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/ops/networks")
public class NetworkController {

    @Autowired
    private DockerClientUtil dockerClientUtil;

    @Operation(summary = "Get all Docker Networks")
    @PostMapping("/all")
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

    @Operation(summary = "Create new docker network")
    @PostMapping("")
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
    public ResponseEntity<Result> GetDockerNetworkInfo(@Valid @RequestBody NetworkInfoRequest criteria) {
        String host = criteria.getHost();
        dockerClientUtil.setCurrentHost(host);

        Network network = dockerClientUtil.inspectNetwork(criteria.getNetworkId());
        log.info("Network info: {}", network);
        return ResponseUtil.success(new NetworkDTO(network));
    }

    @Operation(summary = "Remove Docker Network")
    @PostMapping("/remove")
    public ResponseEntity<Result> removeDockerNetwork(@Valid @RequestBody removeNetworkRequest criteria) {
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
}