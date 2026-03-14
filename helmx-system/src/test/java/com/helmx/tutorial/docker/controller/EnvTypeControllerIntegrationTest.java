package com.helmx.tutorial.docker.controller;

import com.helmx.tutorial.docker.entity.EnvType;
import com.helmx.tutorial.docker.mapper.EnvTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EnvTypeControllerIntegrationTest {

    @Mock
    private EnvTypeMapper envTypeMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        EnvTypeController controller = new EnvTypeController();
        ReflectionTestUtils.setField(controller, "envTypeMapper", envTypeMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── GET /api/v1/ops/env-types ────────────────────────────────────────────

    @Test
    void getAllEnvTypes_returnsOrderedList() throws Exception {
        EnvType dev  = envType(1L, "dev",  "开发环境", 1);
        EnvType prod = envType(2L, "prod", "生产环境", 4);
        when(envTypeMapper.selectList(any())).thenReturn(List.of(dev, prod));

        mockMvc.perform(get("/api/v1/ops/env-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].code").value("dev"))
                .andExpect(jsonPath("$.data[0].remark").value("开发环境"))
                .andExpect(jsonPath("$.data[1].code").value("prod"));
    }

    @Test
    void getAllEnvTypes_returnsEmptyListWhenNone() throws Exception {
        when(envTypeMapper.selectList(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/ops/env-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ── POST /api/v1/ops/env-types ───────────────────────────────────────────

    @Test
    void createEnvType_persistsNewType() throws Exception {
        when(envTypeMapper.exists(any())).thenReturn(false);
        doAnswer(invocation -> {
            EnvType et = invocation.getArgument(0);
            et.setId(10L);
            return 1;
        }).when(envTypeMapper).insert(any(EnvType.class));

        mockMvc.perform(post("/api/v1/ops/env-types")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "staging",
                                  "remark": "预发布环境",
                                  "sort": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.code").value("staging"))
                .andExpect(jsonPath("$.data.remark").value("预发布环境"))
                .andExpect(jsonPath("$.data.sort").value(3));

        ArgumentCaptor<EnvType> captor = ArgumentCaptor.forClass(EnvType.class);
        verify(envTypeMapper).insert(captor.capture());
        assertEquals("staging", captor.getValue().getCode());
        assertEquals("预发布环境", captor.getValue().getRemark());
        assertEquals(3, captor.getValue().getSort());
    }

    @Test
    void createEnvType_defaultsSortToZeroWhenAbsent() throws Exception {
        when(envTypeMapper.exists(any())).thenReturn(false);
        doAnswer(invocation -> {
            EnvType et = invocation.getArgument(0);
            et.setId(11L);
            return 1;
        }).when(envTypeMapper).insert(any(EnvType.class));

        mockMvc.perform(post("/api/v1/ops/env-types")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "qa"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sort").value(0));

        ArgumentCaptor<EnvType> captor = ArgumentCaptor.forClass(EnvType.class);
        verify(envTypeMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getSort());
    }

    @Test
    void createEnvType_rejectsDuplicateCode() throws Exception {
        when(envTypeMapper.exists(any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/ops/env-types")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "dev"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Env type code already exists"));
    }

    @Test
    void createEnvType_rejectsBlankCode() throws Exception {
        mockMvc.perform(post("/api/v1/ops/env-types")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/v1/ops/env-types/{id} ───────────────────────────────────────

    @Test
    void updateEnvType_updatesFieldsAndPersists() throws Exception {
        EnvType existing = envType(5L, "test", "测试环境", 2);
        when(envTypeMapper.selectById(5L)).thenReturn(existing);
        when(envTypeMapper.exists(any())).thenReturn(false);

        mockMvc.perform(put("/api/v1/ops/env-types/5")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "qa",
                                  "remark": "质量保证环境",
                                  "sort": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.code").value("qa"))
                .andExpect(jsonPath("$.data.remark").value("质量保证环境"))
                .andExpect(jsonPath("$.data.sort").value(5));

        verify(envTypeMapper).updateById(existing);
    }

    @Test
    void updateEnvType_returnsNotFoundWhenMissing() throws Exception {
        when(envTypeMapper.selectById(99L)).thenReturn(null);

        mockMvc.perform(put("/api/v1/ops/env-types/99")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "updated"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Env type not found"));
    }

    @Test
    void updateEnvType_rejectsDuplicateCode() throws Exception {
        EnvType existing = envType(6L, "uat", "验收环境", 3);
        when(envTypeMapper.selectById(6L)).thenReturn(existing);
        when(envTypeMapper.exists(any())).thenReturn(true);

        mockMvc.perform(put("/api/v1/ops/env-types/6")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "prod"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Env type code already exists"));
    }

    @Test
    void updateEnvType_sameCodeSkipsDuplicateCheck() throws Exception {
        EnvType existing = envType(7L, "prod", "生产环境", 4);
        when(envTypeMapper.selectById(7L)).thenReturn(existing);

        mockMvc.perform(put("/api/v1/ops/env-types/7")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "prod",
                                  "remark": "updated remark"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.remark").value("updated remark"));
    }

    // ── DELETE /api/v1/ops/env-types/{id} ────────────────────────────────────

    @Test
    void deleteEnvType_removesExistingType() throws Exception {
        EnvType existing = envType(3L, "uat", "验收环境", 3);
        when(envTypeMapper.selectById(3L)).thenReturn(existing);

        mockMvc.perform(delete("/api/v1/ops/env-types/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(envTypeMapper).deleteById(3L);
    }

    @Test
    void deleteEnvType_returnsNotFoundWhenMissing() throws Exception {
        when(envTypeMapper.selectById(99L)).thenReturn(null);

        mockMvc.perform(delete("/api/v1/ops/env-types/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Env type not found"));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private EnvType envType(Long id, String code, String remark, int sort) {
        EnvType et = new EnvType();
        et.setId(id);
        et.setCode(code);
        et.setRemark(remark);
        et.setSort(sort);
        return et;
    }
}
