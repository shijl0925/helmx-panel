package com.helmx.tutorial.docker.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.helmx.tutorial.docker.dto.RegistryConnectRequest;
import com.helmx.tutorial.docker.dto.RegistryCreateRequest;
import com.helmx.tutorial.docker.entity.Registry;
import com.helmx.tutorial.docker.mapper.RegistryMapper;
import com.helmx.tutorial.docker.utils.PasswordUtil;
import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    @Autowired
    private PasswordUtil passwordUtil;

    @Operation(summary = "Get all registries")
    @GetMapping("")
    @PreAuthorize("@va.check('Ops:Registry:List')")
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
    @PreAuthorize("@va.check('Ops:Registry:Create')")
    public ResponseEntity<Result> CreateRegistry(@Valid @RequestBody RegistryCreateRequest request) {
        // 检查名称是否重复
        LambdaQueryWrapper<Registry> nameQuery = new LambdaQueryWrapper<>();
        nameQuery.eq(Registry::getName, request.getName());
        if (registryMapper.exists(nameQuery)) {
            return ResponseUtil.failed(400, null, "Registry name already exists");
        }

        // 检查URL是否重复
        LambdaQueryWrapper<Registry> urlQuery = new LambdaQueryWrapper<>();
        urlQuery.eq(Registry::getUrl, request.getUrl());
        if (registryMapper.exists(urlQuery)) {
            return ResponseUtil.failed(400, null, "Registry url already exists");
        }

        Registry registry = new Registry();

        registry.setName(request.getName());
        registry.setUrl(request.getUrl());
        if (request.getAuth() != null && request.getAuth()) {
            registry.setAuth(true);
            registry.setUsername(request.getUsername());
            registry.setPassword(passwordUtil.encrypt(request.getPassword()));
        }
        registryMapper.insert(registry);

        // Redact sensitive credentials before responding
        registry.setPassword(null);

        return ResponseUtil.success(registry);
    }

    @Operation(summary = "Test registry connect")
    @PostMapping("/test_connect")
    public ResponseEntity<Result> TestConnectRegistry(@Valid @RequestBody RegistryConnectRequest request) throws IOException {
        Map<String, Object> result = new HashMap<>();
        try {
            String registryUrl = request.getUrl();
            String username = request.getUsername();
            String password = request.getPassword();

            // 构建基础认证头
            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            int responseCode = getResponseCode(registryUrl, encodedAuth);
            if (responseCode == 200) {
                // 连接成功
                result.put("status", "success");
                result.put("message", "Registry connection successful");
                return ResponseUtil.success("Registry connection successful", result);
            } else {
                // 连接失败
                String message = (responseCode == 401 || responseCode == 403)
                        ? "Registry authentication failed"
                        : "Registry connection failed";
                result.put("status", "failed");
                result.put("message", message);
                log.debug("Registry connection failed with HTTP code: {}", responseCode);
                return ResponseUtil.failed(500, result, message);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Registry connection test rejected due to malformed registry URL");
            result.put("status", "failed");
            result.put("message", "Invalid registry URL format");
            return ResponseUtil.failed(400, result, "Invalid registry URL format");
        } catch (Exception e) {
            String message = "Registry connection test failed";
            log.error("Registry connection test failed", e);
            result.put("status", "failed");
            result.put("message", message);
            return ResponseUtil.failed(500, result, message);
        }
    }

    private static int getResponseCode(String registryUrl, String encodedAuth) throws IOException {
        URI uri = URI.create(normalizeRegistryUrl(registryUrl) + "/v2/");
        URL url = uri.toURL();

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000); // 设置超时为15秒
            connection.setReadTimeout(15000); // 添加读取超时
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");

            if (encodedAuth != null && !encodedAuth.isEmpty()) {
                // 尝试标准Basic Auth
                connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
            }

            // 禁止自动重定向，防止跳转到恶意网站
            connection.setInstanceFollowRedirects(false);

            int responseCode = connection.getResponseCode();

            // 如果是401未授权，可能需要特殊处理
            if (responseCode == 401) {
                // 获取WWW-Authenticate头，可能需要OAuth2流程
                String authHeader = connection.getHeaderField("WWW-Authenticate");
                if (authHeader != null && authHeader.startsWith("Bearer")) {
                    // 这里可以实现Bearer Token获取逻辑
                    log.warn("Registry requires Bearer token authentication: {}", authHeader);
                    throw new IOException("Registry requires Bearer token authentication");
                }
            }

            return responseCode;
        } finally {
            connection.disconnect();
        }
    }

    @Operation(summary = "Get registry by ID")
    @GetMapping("/{id}")
    @PreAuthorize("@va.check('Ops:Registry:List')")
    public ResponseEntity<Result> GetRegistry(@PathVariable Long id) {
        Registry registry = registryMapper.selectById(id);
        if (registry == null) {
            return ResponseUtil.failed(404, null, "Registry not found");
        }
        registry.setPassword(null);

        return ResponseUtil.success(registry);
    }

    @Operation(summary = "Update registry by ID")
    @PutMapping("/{id}")
    @PreAuthorize("@va.check('Ops:Registry:Edit')")
    public ResponseEntity<Result> UpdateRegistryById(@PathVariable Long id, @Valid @RequestBody RegistryCreateRequest request) {
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
            registry.setPassword(passwordUtil.encrypt(request.getPassword()));
        } else if (Boolean.FALSE.equals(request.getAuth())) {
            registry.setAuth(false);
            registry.setUsername(null);
            registry.setPassword(null);
        }
        registryMapper.updateById(registry);

        // Redact sensitive credentials before responding
        registry.setPassword(null);

        return ResponseUtil.success(registry);
    }

    @Operation(summary = "Delete registry by ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("@va.check('Ops:Registry:Delete')")
    public ResponseEntity<Result> DeleteRegistry(@PathVariable Long id) {
        registryMapper.deleteById(id);

        return ResponseUtil.success(null);
    }

    @Operation(summary = "Browse image repositories and tags in a registry")
    @GetMapping("/{id}/catalog")
    @PreAuthorize("@va.check('Ops:Registry:List')")
    public ResponseEntity<Result> GetRegistryCatalog(@PathVariable Long id) {
        Registry registry = registryMapper.selectById(id);
        if (registry == null) {
            return ResponseUtil.failed(404, null, "Registry not found");
        }

        String encodedAuth = null;
        if (Boolean.TRUE.equals(registry.getAuth())
                && registry.getUsername() != null && registry.getPassword() != null) {
            String plainPassword = passwordUtil.decrypt(registry.getPassword());
            String auth = registry.getUsername() + ":" + plainPassword;
            encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        }

        try {
            // Step 1: fetch repository list from /v2/_catalog
            List<String> repositories = fetchRegistryCatalog(registry.getUrl(), encodedAuth);

            // Step 2: fetch tags for each repository
            List<Map<String, Object>> catalog = new ArrayList<>();
            for (String repo : repositories) {
                List<String> tags = fetchRepositoryTags(registry.getUrl(), repo, encodedAuth);
                Map<String, Object> repoEntry = new HashMap<>();
                repoEntry.put("name", repo);
                repoEntry.put("tags", tags);
                catalog.add(repoEntry);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("total", catalog.size());
            result.put("repositories", catalog);
            return ResponseUtil.success(result);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid registry URL format for registry {}: {}", id, registry.getUrl());
            return ResponseUtil.failed(400, null, "Invalid registry URL format");
        } catch (Exception e) {
            log.error("Failed to fetch catalog for registry {}: {}", id, e.getMessage());
            return ResponseUtil.failed(500, null, "Failed to fetch registry catalog: " + e.getMessage());
        }
    }

    private List<String> fetchRegistryCatalog(String registryUrl, String encodedAuth) throws IOException {
        List<String> allRepositories = new ArrayList<>();
        String path = "/v2/_catalog";
        while (path != null) {
            String[] response = callRegistryApiWithLink(registryUrl, path, encodedAuth);
            JSONObject json = JSON.parseObject(response[0]);
            List<String> page = json.getList("repositories", String.class);
            if (page != null) {
                allRepositories.addAll(page);
            }
            path = extractNextPath(response[1]);
        }
        return allRepositories;
    }

    private List<String> fetchRepositoryTags(String registryUrl, String repository, String encodedAuth)
            throws IOException {
        try {
            List<String> allTags = new ArrayList<>();
            String path = "/v2/" + repository + "/tags/list";
            while (path != null) {
                String[] response = callRegistryApiWithLink(registryUrl, path, encodedAuth);
                JSONObject json = JSON.parseObject(response[0]);
                List<String> page = json.getList("tags", String.class);
                if (page != null) {
                    allTags.addAll(page);
                }
                path = extractNextPath(response[1]);
            }
            return allTags;
        } catch (Exception e) {
            log.warn("Failed to fetch tags for repository {}: {}", repository, e.getMessage());
            return new ArrayList<>();
        }
    }

    private String callRegistryApi(String registryUrl, String path, String encodedAuth) throws IOException {
        return callRegistryApiWithLink(registryUrl, path, encodedAuth)[0];
    }

    /**
     * Calls the Docker Registry V2 HTTP API and returns a two-element array:
     * {@code [responseBody, linkHeader]} where {@code linkHeader} may be {@code null}.
     * The registry URL is normalized (trailing slashes stripped) before use.
     */
    private String[] callRegistryApiWithLink(String registryUrl, String path, String encodedAuth) throws IOException {
        URI uri = URI.create(normalizeRegistryUrl(registryUrl) + path);
        URL url = uri.toURL();

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setInstanceFollowRedirects(false);
            if (encodedAuth != null) {
                connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
            }

            int code = connection.getResponseCode();
            if (code == 200) {
                String body;
                try (InputStream in = connection.getInputStream()) {
                    body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                return new String[]{body, connection.getHeaderField("Link")};
            } else {
                throw new IOException("Registry API returned HTTP " + code + " for " + path);
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Parses a {@code Link} response header (RFC 5988) and returns the path+query of the
     * {@code rel="next"} URL, or {@code null} if there is no next page.
     * <p>Example header value: {@code </v2/_catalog?last=repo100&n=100>; rel="next"}
     * <p>Package-visible so that {@code RegistryControllerCatalogTest} can unit-test it directly.
     */
    static String extractNextPath(String linkHeader) {
        if (linkHeader == null) return null;
        int start = linkHeader.indexOf('<');
        int end = linkHeader.indexOf('>');
        if (start < 0 || end <= start) return null;
        String url = linkHeader.substring(start + 1, end);
        try {
            URI uri = URI.create(url);
            String p = uri.getPath();
            String query = uri.getQuery();
            return query != null ? p + "?" + query : p;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Strips any trailing slash from a registry URL before appending an API path. */
    private static String normalizeRegistryUrl(String registryUrl) {
        return registryUrl.endsWith("/") ? registryUrl.substring(0, registryUrl.length() - 1) : registryUrl;
    }
}
