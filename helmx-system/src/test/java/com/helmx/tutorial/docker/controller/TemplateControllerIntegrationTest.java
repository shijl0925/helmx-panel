package com.helmx.tutorial.docker.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.helmx.tutorial.docker.entity.Template;
import com.helmx.tutorial.docker.mapper.TemplateMapper;
import com.helmx.tutorial.docker.service.TemplateService;
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
class TemplateControllerIntegrationTest {

    @Mock
    private TemplateService templateService;

    @Mock
    private TemplateMapper templateMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TemplateController controller = new TemplateController();
        ReflectionTestUtils.setField(controller, "templateService", templateService);
        ReflectionTestUtils.setField(controller, "templateMapper", templateMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void searchTemplates_returnsPagedTemplateDtos() throws Exception {
        Template template = createTemplate(5L, "nginx", "Nginx image", "FROM nginx:alpine", "Dockerfile");
        when(templateMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<Template> page = invocation.getArgument(0);
            page.setRecords(List.of(template));
            page.setTotal(1);
            return page;
        });

        mockMvc.perform(get("/api/v1/ops/templates")
                        .param("name", "ng")
                        .param("type", "Dockerfile")
                        .param("page", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("nginx"))
                .andExpect(jsonPath("$.data.items[0].type").value("Dockerfile"));
    }

    @Test
    void createTemplate_rejectsDuplicateTemplateName() throws Exception {
        when(templateMapper.exists(any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/ops/templates")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "nginx",
                                  "remark": "dup",
                                  "content": "FROM nginx:alpine",
                                  "type": "Dockerfile"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Template name already exists"));
    }

    @Test
    void updateTemplate_updatesFieldsWhenTemplateExists() throws Exception {
        Template template = createTemplate(6L, "node", "Node image", "FROM node:20", "Dockerfile");
        when(templateService.getById(6L)).thenReturn(template);

        mockMvc.perform(put("/api/v1/ops/templates/6")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "Updated template",
                                  "content": "FROM node:22",
                                  "type": "Dockerfile"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(6))
                .andExpect(jsonPath("$.data.remark").value("Updated template"))
                .andExpect(jsonPath("$.data.content").value("FROM node:22"));

        ArgumentCaptor<Template> captor = ArgumentCaptor.forClass(Template.class);
        verify(templateMapper).updateById(captor.capture());
        assertEquals("Updated template", captor.getValue().getRemark());
        assertEquals("FROM node:22", captor.getValue().getContent());
    }

    @Test
    void deleteTemplate_returnsNotFoundWhenTemplateDoesNotExist() throws Exception {
        when(templateService.getById(77L)).thenReturn(null);

        mockMvc.perform(delete("/api/v1/ops/templates/77"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Template does not exist"));
    }

    private Template createTemplate(Long id, String name, String remark, String content, String type) {
        Template template = new Template();
        template.setId(id);
        template.setName(name);
        template.setRemark(remark);
        template.setContent(content);
        template.setType(type);
        return template;
    }
}
