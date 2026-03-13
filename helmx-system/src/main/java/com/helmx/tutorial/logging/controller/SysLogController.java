package com.helmx.tutorial.logging.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.logging.dto.SysLogQueryCriteria;
import com.helmx.tutorial.logging.entity.SysLog;
import com.helmx.tutorial.logging.service.SysLogService;
import com.helmx.tutorial.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "日志管理")
@RestController
@RequestMapping("/api/v1/operation_logs")
public class SysLogController {

    @Autowired
    private SysLogService sysLogService;

    @Operation(summary = "日志查询")
    @GetMapping("")
    @PreAuthorize("@va.check('System:Log:List')")
    public ResponseEntity<Result> GetAllSysLogs(SysLogQueryCriteria criteria) {
        Page<SysLog> page = new Page<>(criteria.getPage(), criteria.getSize());
        return ResponseUtil.success(sysLogService.queryAll(criteria, page));
    }

    @Operation(summary = "删除所有日志")
    @DeleteMapping("")
    @PreAuthorize("@va.check('System:Log:Delete')")
    public ResponseEntity<Result> DeleteAllSysLogs() {
        sysLogService.remove(null);
        return ResponseUtil.success(null);
    }
}
