package com.helmx.tutorial.logging.controller;

import com.helmx.tutorial.logging.dto.SysLogQueryCriteria;
import com.helmx.tutorial.logging.entity.SysLog;
import com.helmx.tutorial.logging.service.SysLogService;
import com.helmx.tutorial.dto.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.sql.Timestamp;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SysLogControllerIntegrationTest {

    @Mock
    private SysLogService sysLogService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SysLogController controller = new SysLogController();
        ReflectionTestUtils.setField(controller, "sysLogService", sysLogService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllSysLogs_returnsPagedLogs() throws Exception {
        SysLog log1 = new SysLog("INFO", 120L);
        log1.setUsername("admin");
        log1.setDescription("查询用户列表");
        log1.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        Page<SysLog> page = new Page<>(1, 10);
        page.setRecords(List.of(log1));
        page.setTotal(1);

        PageResult<SysLog> pageResult = new PageResult<>(page);

        when(sysLogService.queryAll(any(SysLogQueryCriteria.class), any(Page.class)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/operation_logs")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].username").value("admin"))
                .andExpect(jsonPath("$.data.items[0].logType").value("INFO"));
    }

    @Test
    void getAllSysLogs_withUsernameFilter_returnsFilteredLogs() throws Exception {
        Page<SysLog> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0);

        PageResult<SysLog> emptyResult = new PageResult<>(emptyPage);

        when(sysLogService.queryAll(any(SysLogQueryCriteria.class), any(Page.class)))
                .thenReturn(emptyResult);

        mockMvc.perform(get("/api/v1/operation_logs")
                        .param("username", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
