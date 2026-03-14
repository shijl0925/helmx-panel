package com.helmx.tutorial.docker.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.helmx.tutorial.docker.entity.EnvType;
import com.helmx.tutorial.docker.mapper.EnvTypeMapper;
import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/ops/env-types")
public class EnvTypeController {

    @Autowired
    private EnvTypeMapper envTypeMapper;

    @Operation(summary = "Get all env types")
    @GetMapping("")
//    @PreAuthorize("@va.check('Ops:EnvType:List')")
    public ResponseEntity<Result> GetAllEnvTypes() {
        QueryWrapper<EnvType> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("sort", "code");
        List<EnvType> envTypes = envTypeMapper.selectList(queryWrapper);
        return ResponseUtil.success(envTypes);
    }

    @Operation(summary = "Create a new env type")
    @PostMapping("")
    @PreAuthorize("@va.check('Ops:EnvType:Create')")
    public ResponseEntity<Result> CreateEnvType(@Valid @RequestBody EnvType request) {
        LambdaQueryWrapper<EnvType> codeQuery = new LambdaQueryWrapper<>();
        codeQuery.eq(EnvType::getCode, request.getCode());
        if (envTypeMapper.exists(codeQuery)) {
            return ResponseUtil.failed(400, null, "Env type code already exists");
        }

        EnvType envType = new EnvType();
        envType.setCode(request.getCode());
        envType.setRemark(request.getRemark());
        envType.setSort(request.getSort() != null ? request.getSort() : 0);
        envTypeMapper.insert(envType);

        return ResponseUtil.success(envType);
    }

    @Operation(summary = "Update env type by ID")
    @PutMapping("/{id}")
    @PreAuthorize("@va.check('Ops:EnvType:Edit')")
    public ResponseEntity<Result> UpdateEnvTypeById(@PathVariable Long id, @RequestBody EnvType request) {
        EnvType envType = envTypeMapper.selectById(id);
        if (envType == null) {
            return ResponseUtil.failed(404, null, "Env type not found");
        }

        if (request.getCode() != null && !request.getCode().equals(envType.getCode())) {
            LambdaQueryWrapper<EnvType> codeQuery = new LambdaQueryWrapper<>();
            codeQuery.eq(EnvType::getCode, request.getCode());
            if (envTypeMapper.exists(codeQuery)) {
                return ResponseUtil.failed(400, null, "Env type code already exists");
            }
            envType.setCode(request.getCode());
        }
        if (request.getRemark() != null) {
            envType.setRemark(request.getRemark());
        }
        if (request.getSort() != null) {
            envType.setSort(request.getSort());
        }

        envTypeMapper.updateById(envType);

        return ResponseUtil.success(envType);
    }

    @Operation(summary = "Delete env type by ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("@va.check('Ops:EnvType:Delete')")
    public ResponseEntity<Result> DeleteEnvTypeById(@PathVariable Long id) {
        EnvType envType = envTypeMapper.selectById(id);
        if (envType == null) {
            return ResponseUtil.failed(404, null, "Env type not found");
        }
        envTypeMapper.deleteById(id);
        return ResponseUtil.success(null);
    }
}
