package com.helmx.tutorial.docker.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.helmx.tutorial.docker.dto.RegistryConnectRequest;
import com.helmx.tutorial.docker.dto.RegistryCreateRequest;
import com.helmx.tutorial.docker.entity.Registry;
import com.helmx.tutorial.docker.mapper.RegistryMapper;
import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/ops/registries")
public class RegistryController {

    @Autowired
    private RegistryMapper registryMapper;

    @Operation(summary = "Get all registries")
    @GetMapping("")
    public ResponseEntity<Result> GetAllRegistries(@RequestParam(required = false) String name) {
        QueryWrapper<Registry> queryWrapper = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        // 排除密码字段
        queryWrapper.select(Registry.class, info -> !info.getColumn().equals("password"));

        List<Registry> registries = registryMapper.selectList(queryWrapper);
        return ResponseUtil.success(registries);
    }

    @Operation(summary = "Create a new registry")
    @PostMapping("")
    public ResponseEntity<Result> CreateRegistry(@RequestBody RegistryCreateRequest request) {
        // 检查名称是否重复
        LambdaQueryWrapper<Registry> nameQuery = new LambdaQueryWrapper<>();
        nameQuery.eq(Registry::getName, request.getName());
        if (registryMapper.selectCount(nameQuery) > 0) {
            return ResponseUtil.failed(400, null, "Registry name already exists");
        }

        // 检查URL是否重复
        LambdaQueryWrapper<Registry> urlQuery = new LambdaQueryWrapper<>();
        urlQuery.eq(Registry::getUrl, request.getUrl());
        if (registryMapper.selectCount(urlQuery) > 0) {
            return ResponseUtil.failed(400, null, "Registry url already exists");
        }

        Registry registry = new Registry();

        registry.setName(request.getName());
        registry.setUrl(request.getUrl());
        if (request.getAuth() != null && request.getAuth()) {
            registry.setAuth(true);
            registry.setUsername(request.getUsername());
            registry.setPassword(request.getPassword());
        }
        registryMapper.insert(registry);

        return ResponseUtil.success(registry);
    }

    @Operation(summary = "Test registry connect")
    @PostMapping("/test_connect")
    public ResponseEntity<Result> TestConnectRegistry(@RequestBody RegistryConnectRequest request) throws IOException {
        Map<String, Object> result = new HashMap<>();
        try {
            String registryUrl = request.getUrl();
            String username = request.getUsername();
            String password = request.getPassword();

            // 构建基础认证头
            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            int responseCode = getResponseCode(registryUrl, encodedAuth);
            if (responseCode == 200) {
                // 连接成功
                result.put("status", "success");
                result.put("message", "Registry connection successful");
                return ResponseUtil.success("Registry connection successful", result);
            } else {
                // 连接失败
                String message = "Registry connection failed with code: " + responseCode;
                result.put("status", "failed");
                result.put("message", message);
                return ResponseUtil.failed(500, result, message);
            }
        } catch (Exception e) {
            String message = "Registry connection test failed: " + e.getMessage();
            result.put("status", "failed");
            result.put("message", message);
            return ResponseUtil.failed(500, result, message);
        }
    }

    private static int getResponseCode(String registryUrl, String encodedAuth) throws IOException {
        URL url = new URL(registryUrl + "/v2/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000); // 设置超时为15秒
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);

        return connection.getResponseCode();
    }

    @Operation(summary = "Get registry by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Result> GetRegistry(@PathVariable Long id) {
        Registry registry = registryMapper.selectById(id);

        return ResponseUtil.success(registry);
    }

    @Operation(summary = "Update registry by ID")
    @PutMapping("/{id}")
    public ResponseEntity<Result> UpdateRegistryById(@PathVariable Long id, @RequestBody RegistryCreateRequest request) {
        Registry registry = registryMapper.selectById(id);

        if (registry == null) {
            return ResponseUtil.failed(404, null, "Registry not found");
        }

        if (request.getName() != null) {
            registry.setName(request.getName());
        }
        if (request.getUrl() != null) {
            registry.setUrl(request.getUrl());
        }
        if (request.getAuth() != null && request.getAuth()) {
            registry.setAuth(true);
            registry.setUsername(request.getUsername());
            registry.setPassword(request.getPassword());
        } else {
            registry.setAuth(false);
            registry.setUsername(null);
            registry.setPassword(null);
        }
        registryMapper.updateById(registry);

        return ResponseUtil.success(registry);
    }

    @Operation(summary = "Delete registry by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Result> DeleteRegistry(@PathVariable Long id) {
        registryMapper.deleteById(id);

        return ResponseUtil.success(null);
    }
}
