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
import com.helmx.tutorial.dto.PageResult;
import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.utils.ResponseUtil;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/ops/envs")
public class DockerEnvController {

    @Autowired
    private DockerEnvService dockerEnvService;

    @Autowired
    private DockerEnvMapper dockerEnvMapper;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Operation(summary = "Get all envs")
    @GetMapping("/all")
    public ResponseEntity<Result> GetAllDockerEnvs(@RequestParam(required = false) String name) {
        QueryWrapper<DockerEnv> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);

        List<DockerEnv> envs = dockerEnvMapper.selectList(queryWrapper);
        return ResponseUtil.success(envs);
    }

    @Operation(summary = "Search envs")
    @GetMapping("")
    public ResponseEntity<Result> SearchDockerEnvs(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") @ApiParam(value = "当前页码") Integer page,
            @RequestParam(defaultValue = "10") @ApiParam(value = "每页数量") Integer pageSize
    ) {
        Page<DockerEnv> pageInfo = new Page<>(page, pageSize);

        QueryWrapper<DockerEnv> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
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
    public ResponseEntity<Result> CreateDockerEnv(@RequestBody DockerEnvCreateRequest request) {
        // 检查名称是否重复
        LambdaQueryWrapper<DockerEnv> nameQuery = new LambdaQueryWrapper<>();
        nameQuery.eq(DockerEnv::getName, request.getName());
        if (dockerEnvMapper.selectCount(nameQuery) > 0) {
            return ResponseUtil.failed(400, null, "The name already exists");
        }

        // 检查主机地址是否重复
        LambdaQueryWrapper<DockerEnv> hostQuery = new LambdaQueryWrapper<>();
        hostQuery.eq(DockerEnv::getHost, request.getHost());
        if (dockerEnvMapper.selectCount(hostQuery) > 0) {
            return ResponseUtil.failed(400, null, "The url already exists");
        }

        DockerEnv env = new DockerEnv();

        env.setName(request.getName());
        env.setRemark(request.getRemark() == null ? "" : request.getRemark());
        env.setHost(request.getHost());
        // TLS设置
        env.setTlsVerify(request.getTlsVerify());
        dockerEnvMapper.insert(env);

        return ResponseUtil.success(env);
    }

    @Operation(summary = "Update env by ID")
    @PutMapping("/{id}")
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

        dockerEnvMapper.updateById(env);

        return ResponseUtil.success(env);
    }

    @Operation(summary = "Delete env by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Result> DeleteDockerEnvById(@PathVariable Long id) {
        dockerEnvMapper.deleteById(id);

        return ResponseUtil.success(null);
    }
}
