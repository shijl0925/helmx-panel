package com.helmx.tutorial.docker.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.helmx.tutorial.docker.dto.TemplateCreateRequest;
import com.helmx.tutorial.docker.dto.TemplateDTO;
import com.helmx.tutorial.docker.dto.TemplateUpdateRequest;
import com.helmx.tutorial.docker.entity.Template;
import com.helmx.tutorial.docker.mapper.TemplateMapper;
import com.helmx.tutorial.docker.service.TemplateService;
import com.helmx.tutorial.dto.PageResult;
import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.utils.ResponseUtil;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/ops/templates")
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    @Autowired
    private TemplateMapper templateMapper;

    @Operation(summary = "搜索模板")
    @GetMapping("")
    public ResponseEntity<Result> SearchTemplates(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @ApiParam(value = "模板类型") String type,
            @RequestParam(defaultValue = "1") @ApiParam(value = "当前页码") Integer page,
            @RequestParam(defaultValue = "10") @ApiParam(value = "每页数量") Integer pageSize
    ) {
        Page<Template> pageInfo = new Page<>(page, pageSize);

        QueryWrapper<Template> queryWrapper = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }

        if (type != null && !type.isEmpty()) {
            queryWrapper.eq("type", type);
        }

        Page<Template> resultPage = templateMapper.selectPage(pageInfo, queryWrapper);
        List<TemplateDTO> templateDTOs = resultPage.getRecords().stream()
                .map(TemplateDTO::new)
                .collect(Collectors.toList());

        Page<TemplateDTO> dtoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        dtoPage.setRecords(templateDTOs);

        PageResult<TemplateDTO> pageResult = new PageResult<>(dtoPage);

        return ResponseUtil.success(pageResult);
    }

    @Operation(summary = "创建新模板")
    @PostMapping("")
    public ResponseEntity<Result> CreateTemplate(@RequestBody TemplateCreateRequest request) {
        // 检查名称是否重复
        LambdaQueryWrapper<Template> nameQuery = new LambdaQueryWrapper<>();
        nameQuery.eq(Template::getName, request.getName());
        if (templateMapper.selectCount(nameQuery) > 0) {
            return ResponseUtil.failed(400, null, "模板名称已存在");
        }

        Template template = new Template();
        template.setName(request.getName());
        template.setRemark(request.getRemark());
        template.setContent(request.getContent());
        template.setType(request.getType());

        templateMapper.insert(template);

        return ResponseUtil.success(new TemplateDTO(template));
    }

    @Operation(summary = "根据ID更新模板")
    @PutMapping("/{id}")
    public ResponseEntity<Result> UpdateTemplateById(@PathVariable Long id, @RequestBody TemplateUpdateRequest request) {
        Template template = templateService.getById(id);
        if (template == null) {
            return ResponseUtil.failed(404, null, "模板不存在");
        }

        if (request.getName() != null) {
            template.setName(request.getName());
        }
        if (request.getRemark() != null) {
            template.setRemark(request.getRemark());
        }
        if (request.getContent() != null) {
            template.setContent(request.getContent());
        }
        if (request.getType() != null) {
            template.setType(request.getType());
        }

        templateMapper.updateById(template);

        return ResponseUtil.success(new TemplateDTO(template));
    }

    @Operation(summary = "根据ID删除模板")
    @DeleteMapping("/{id}")
    public ResponseEntity<Result> DeleteTemplateById(@PathVariable Long id) {
        Template template = templateService.getById(id);
        if (template == null) {
            return ResponseUtil.failed(404, null, "模板不存在");
        }

        templateMapper.deleteById(id);

        return ResponseUtil.success(null);
    }

    @Operation(summary = "根据ID获取模板详情")
    @GetMapping("/{id}")
    public ResponseEntity<Result> GetTemplateById(@PathVariable Long id) {
        Template template = templateService.getById(id);
        if (template == null) {
            return ResponseUtil.failed(404, null, "模板不存在");
        }

        return ResponseUtil.success(new TemplateDTO(template));
    }
}