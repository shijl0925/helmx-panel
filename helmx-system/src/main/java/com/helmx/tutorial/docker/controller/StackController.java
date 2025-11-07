package com.helmx.tutorial.docker.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.helmx.tutorial.docker.dto.ImageTaskStatusRequest;
import com.helmx.tutorial.docker.dto.StackUpdateRequest;
import com.helmx.tutorial.docker.entity.Stack;
import com.helmx.tutorial.docker.mapper.StackMapper;
import com.helmx.tutorial.docker.service.StackService;
import com.helmx.tutorial.docker.utils.ComposeBuildTask;
import com.helmx.tutorial.docker.utils.ComposeBuildTaskManager;
import com.helmx.tutorial.docker.utils.DockerCompose;
import com.helmx.tutorial.dto.PageResult;
import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.utils.ResponseUtil;
import com.helmx.tutorial.docker.dto.StackCreateRequest;
import com.helmx.tutorial.docker.dto.StackDeployRequest;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/ops/stacks")
public class StackController {

    @Autowired
    private DockerCompose dockerCompose;

    @Autowired
    private StackService stackService;

    @Autowired
    private StackMapper stackMapper;

    @Autowired
    private ComposeBuildTaskManager composeBuildTaskManager;

    @Operation(summary = "搜索编排")
    @GetMapping("")
    @PreAuthorize("@va.check('Ops:Stack:List')")
    public ResponseEntity<Result> SearchStacks(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") @ApiParam(value = "当前页码") Integer page,
            @RequestParam(defaultValue = "10") @ApiParam(value = "每页数量") Integer pageSize
    ) {
        Page<Stack> pageInfo = new Page<>(page, pageSize);

        QueryWrapper<Stack> queryWrapper = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }

        Page<Stack> resultPage = stackMapper.selectPage(pageInfo, queryWrapper);

        PageResult<Stack> pageResult = new PageResult<>(resultPage);

        return ResponseUtil.success(pageResult);
    }

    @Operation(summary = "创建新编排")
    @PostMapping("")
    @PreAuthorize("@va.check('Ops:Stack:Create')")
    public ResponseEntity<Result> CreateStack(@Valid @RequestBody StackCreateRequest request) {
        // 检查名称是否重复
        LambdaQueryWrapper<Stack> nameQuery = new LambdaQueryWrapper<>();
        nameQuery.eq(Stack::getName, request.getName());
        if (stackMapper.selectCount(nameQuery) > 0) {
            return ResponseUtil.failed(400, null, "Stack name already exists");
        }

        Stack stack = new Stack();
        stack.setName(request.getName());
        stack.setContent(request.getContent());

        stackMapper.insert(stack);

        return ResponseUtil.success(stack);
    }

    @Operation(summary = "根据ID获取编排详情")
    @GetMapping("/{id}")
    @PreAuthorize("@va.check('Ops:Stack:List')")
    public ResponseEntity<Result> GetStackById(@PathVariable Long id) {
        Stack stack = stackService.getById(id);
        if (stack == null) {
            return ResponseUtil.failed(404, null, "Stack does not exist");
        }

        return ResponseUtil.success(stack);
    }

    @Operation(summary = "根据ID编辑编排")
    @PutMapping("/{id}")
    @PreAuthorize("@va.check('Ops:Stack:Edit')")
    public ResponseEntity<Result> updateStackById(@PathVariable Long id, @Valid @RequestBody StackUpdateRequest request) {
        Stack stack = stackService.getById(id);
        if (stack == null) {
            return ResponseUtil.failed(404, null, "Stack does not exist");
        }
        if (stack.getName() != null) {
            stack.setName(request.getName());
        }

        if (stack.getContent() != null) {
            stack.setContent(request.getContent());
        }
        stackMapper.updateById(stack);

        return ResponseUtil.success(stack);
    }

    @Operation(summary = "根据ID删除编排")
    @DeleteMapping("/{id}")
    @PreAuthorize("@va.check('Ops:Stack:Delete')")
    public ResponseEntity<Result> DeleteStackById(@PathVariable Long id) {
        Stack stack = stackService.getById(id);
        if (stack == null) {
            return ResponseUtil.failed(404, null, "Stack does not exist");
        }

        stackMapper.deleteById(id);

        return ResponseUtil.success(null);
    }

    @Operation(summary = "部署编排")
    @PostMapping("/{id}/deploy")
    @PreAuthorize("@va.check('Ops:Stack:Deploy')")
    public ResponseEntity<Result> DeployStack(@PathVariable Long id, @Valid @RequestBody StackDeployRequest request) {
        Stack stack = stackService.getById(id);
        if (stack == null) {
            return ResponseUtil.failed(404, null, "Stack does not exist");
        }

        // 如果请求中提供了content，则更新stack的content字段
        if (request.getContent() != null && !request.getContent().isEmpty()) {
            stack.setContent(request.getContent());
            stackMapper.updateById(stack);
        }
        Map<String, String> result = dockerCompose.deployCompose(stack.getId(), stack.getContent());
        return ResponseUtil.success(result);
    }

    @Operation(summary = "获取编排任务状态")
    @PostMapping("/deploy/task/status")
    public ResponseEntity<Result> getComposeBuildTaskStatus(@RequestBody ImageTaskStatusRequest criteria) {
        String taskId = criteria.getTaskId();
        ComposeBuildTask task = composeBuildTaskManager.getTask(taskId);
        if (task == null) {
            return ResponseUtil.success("Task not found", null);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", task.getStatus());
        result.put("message", task.getMessage());
        result.put("startTime", task.getStartTime());
        result.put("endTime", task.getEndTime());

        return ResponseUtil.success(result);
    }

    @Operation(summary = "编排日志")
    @PostMapping("/{id}/logs")
    @PreAuthorize("@va.check('Ops:Stack:Deploy')")
    public ResponseEntity<Result> getComposeLogs(@PathVariable Long id) {
        Stack stack = stackService.getById(id);
        if (stack == null) {
            return ResponseUtil.failed(404, null, "Stack does not exist");
        }

        Map<String, String> result = new HashMap<>();
        String logs = dockerCompose.getComposeLogs(stack.getId(), stack.getContent(), false, -1);
        result.put("status", "success");
        result.put("logs", logs);
        return ResponseUtil.success(result);
    }
}