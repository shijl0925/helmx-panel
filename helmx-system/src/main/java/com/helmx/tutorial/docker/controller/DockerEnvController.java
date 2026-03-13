package com.helmx.tutorial.docker.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.helmx.tutorial.docker.dto.DockerEnvCreateRequest;
import com.helmx.tutorial.docker.dto.DockerEnvDTO;
import com.helmx.tutorial.docker.dto.DockerEnvUpdateRequest;
import com.helmx.tutorial.docker.entity.DockerEnv;
import com.helmx.tutorial.docker.mapper.DockerEnvMapper;
import com.helmx.tutorial.docker.service.DockerEnvService;
import com.helmx.tutorial.docker.utils.PasswordUtil;
import com.helmx.tutorial.dto.PageResult;
import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.utils.ResponseUtil;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/ops/envs")
public class DockerEnvController {

    @Autowired
    private DockerEnvService dockerEnvService;

    @Autowired
    private DockerEnvMapper dockerEnvMapper;

    @Autowired
    private PasswordUtil passwordUtil;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Operation(summary = "Get all envs")
    @GetMapping("/all")
//    @PreAuthorize("@va.check('Ops:DockerEnv:List')")
    public ResponseEntity<Result> GetAllDockerEnvs(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String envType,
            @RequestParam(required = false) String clusterName
    ) {
        QueryWrapper<DockerEnv> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        if (envType != null && !envType.isEmpty()) {
            queryWrapper.eq("env_type", envType);
        }
        if (clusterName != null && !clusterName.isEmpty()) {
            queryWrapper.eq("cluster_name", clusterName);
        }

        List<DockerEnvDTO> envs = dockerEnvMapper.selectList(queryWrapper).stream()
                .map(DockerEnvDTO::new)
                .toList();
        return ResponseUtil.success(envs);
    }

    @Operation(summary = "Get all envs grouped by environment type")
    @GetMapping("/grouped")
//    @PreAuthorize("@va.check('Ops:DockerEnv:List')")
    public ResponseEntity<Result> GetDockerEnvsGrouped() {
        QueryWrapper<DockerEnv> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.orderByAsc("env_type", "cluster_name", "name");

        Map<String, List<DockerEnvDTO>> grouped = dockerEnvMapper.selectList(queryWrapper).stream()
                .map(DockerEnvDTO::new)
                .collect(Collectors.groupingBy(
                        this::groupingKey,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        return ResponseUtil.success(grouped);
    }

    private String groupingKey(DockerEnvDTO dto) {
        return dto.getEnvType() != null && !dto.getEnvType().isBlank() ? dto.getEnvType() : "default";
    }

    @Operation(summary = "Search envs")
    @GetMapping("")
    @PreAuthorize("@va.check('Ops:DockerEnv:List')")
    public ResponseEntity<Result> SearchDockerEnvs(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String envType,
            @RequestParam(required = false) String clusterName,
            @RequestParam(defaultValue = "1") @ApiParam(value = "当前页码") Integer page,
            @RequestParam(defaultValue = "10") @ApiParam(value = "每页数量") Integer pageSize
    ) {
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }

        Page<DockerEnv> pageInfo = new Page<>(page, pageSize);

        QueryWrapper<DockerEnv> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        if (envType != null && !envType.isEmpty()) {
            queryWrapper.eq("env_type", envType);
        }
        if (clusterName != null && !clusterName.isEmpty()) {
            queryWrapper.eq("cluster_name", clusterName);
        }

        Page<DockerEnv> resultPage = dockerEnvMapper.selectPage(pageInfo, queryWrapper);
        List<DockerEnvDTO> envDTOs = resultPage.getRecords().stream()
                .map(DockerEnvDTO::new)
                .toList();

        Page<DockerEnvDTO> dtoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        dtoPage.setRecords(envDTOs);

        // 使用自定义分页结果类
        PageResult<DockerEnvDTO> pageResult = new PageResult<>(dtoPage);

        return ResponseUtil.success(pageResult);
    }

    @Operation(summary = "Create a new env")
    @PostMapping("")
    @PreAuthorize("@va.check('Ops:DockerEnv:Create')")
    public ResponseEntity<Result> CreateDockerEnv(@Valid @RequestBody DockerEnvCreateRequest request) {
        // 检查名称是否重复
        LambdaQueryWrapper<DockerEnv> nameQuery = new LambdaQueryWrapper<>();
        nameQuery.eq(DockerEnv::getName, request.getName());
        if (dockerEnvMapper.exists(nameQuery)) {
            return ResponseUtil.failed(400, null, "The name already exists");
        }

        // 检查主机地址是否重复
        LambdaQueryWrapper<DockerEnv> hostQuery = new LambdaQueryWrapper<>();
        hostQuery.eq(DockerEnv::getHost, request.getHost());
        if (dockerEnvMapper.exists(hostQuery)) {
            return ResponseUtil.failed(400, null, "The url already exists");
        }

        DockerEnv env = new DockerEnv();

        env.setName(request.getName());
        env.setRemark(request.getRemark() == null ? "" : request.getRemark());
        env.setHost(request.getHost());
        // TLS设置
        env.setTlsVerify(request.getTlsVerify());
        env.setSshEnabled(request.getSshEnabled());
        env.setSshPort(request.getSshPort());
        env.setSshUsername(request.getSshUsername());
        env.setSshHostKeyFingerprint(request.getSshHostKeyFingerprint());
        if (request.getSshPassword() != null && !request.getSshPassword().isBlank()) {
            env.setSshPassword(passwordUtil.encrypt(request.getSshPassword()));
        }
        env.setEnvType(request.getEnvType());
        env.setClusterName(request.getClusterName());
        dockerEnvMapper.insert(env);

        return ResponseUtil.success(new DockerEnvDTO(env));
    }

    @Operation(summary = "Update env by ID")
    @PutMapping("/{id}")
    @PreAuthorize("@va.check('Ops:DockerEnv:Edit')")
    public ResponseEntity<Result> UpdateDockerEnvById(@PathVariable Long id, @RequestBody DockerEnvUpdateRequest request) {
        DockerEnv env = dockerEnvService.getById(id);
        if (env == null) {
            return ResponseUtil.failed(404, null, "Env not found");
        }
        if (request.getName() != null) {
            env.setName(request.getName());
        }
        if (request.getRemark() != null) {
            env.setRemark(request.getRemark());
        }
        if (request.getHost() != null) {
            env.setHost(request.getHost());
        }
        if (request.getStatus() != null) {
            env.setStatus(request.getStatus());
        }
        if (request.getTlsVerify() != null) {
            env.setTlsVerify(request.getTlsVerify());
        }
        if (request.getSshEnabled() != null) {
            env.setSshEnabled(request.getSshEnabled());
        }
        if (request.getSshPort() != null) {
            env.setSshPort(request.getSshPort());
        }
        if (request.getSshUsername() != null) {
            env.setSshUsername(request.getSshUsername());
        }
        if (request.getSshHostKeyFingerprint() != null) {
            env.setSshHostKeyFingerprint(request.getSshHostKeyFingerprint());
        }
        if (request.getSshPassword() != null) {
            env.setSshPassword(request.getSshPassword().isBlank() ? null : passwordUtil.encrypt(request.getSshPassword()));
        }
        if (request.getEnvType() != null) {
            env.setEnvType(request.getEnvType().isBlank() ? null : request.getEnvType());
        }
        if (request.getClusterName() != null) {
            env.setClusterName(request.getClusterName().isBlank() ? null : request.getClusterName());
        }

        dockerEnvMapper.updateById(env);

        return ResponseUtil.success(new DockerEnvDTO(env));
    }

    @Operation(summary = "Delete env by ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("@va.check('Ops:DockerEnv:Delete')")
    public ResponseEntity<Result> DeleteDockerEnvById(@PathVariable Long id) {
        dockerEnvMapper.deleteById(id);

        return ResponseUtil.success(null);
    }
}
