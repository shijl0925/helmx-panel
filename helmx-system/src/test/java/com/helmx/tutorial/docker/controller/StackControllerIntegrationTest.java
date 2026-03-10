package com.helmx.tutorial.docker.controller;

import com.helmx.tutorial.docker.entity.Stack;
import com.helmx.tutorial.docker.mapper.StackMapper;
import com.helmx.tutorial.docker.service.StackService;
import com.helmx.tutorial.docker.utils.ComposeBuildTaskManager;
import com.helmx.tutorial.docker.utils.DockerCompose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StackControllerIntegrationTest {

    @Mock
    private DockerCompose dockerCompose;

    @Mock
    private StackService stackService;

    @Mock
    private StackMapper stackMapper;

    @Mock
    private ComposeBuildTaskManager composeBuildTaskManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StackController controller = new StackController();
        ReflectionTestUtils.setField(controller, "dockerCompose", dockerCompose);
        ReflectionTestUtils.setField(controller, "stackService", stackService);
        ReflectionTestUtils.setField(controller, "stackMapper", stackMapper);
        ReflectionTestUtils.setField(controller, "composeBuildTaskManager", composeBuildTaskManager);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void updateStackById_withPartialPayload_preservesExistingValues() throws Exception {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setName("existing-name");
        stack.setContent("original-content");

        when(stackService.getById(1L)).thenReturn(stack);
        when(stackMapper.updateById(any(Stack.class))).thenReturn(1);

        mockMvc.perform(put("/api/v1/ops/stacks/{id}", 1)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "updated-content"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("existing-name"))
                .andExpect(jsonPath("$.data.content").value("updated-content"));

        ArgumentCaptor<Stack> stackCaptor = ArgumentCaptor.forClass(Stack.class);
        verify(stackMapper).updateById(stackCaptor.capture());
        assertEquals("existing-name", stackCaptor.getValue().getName());
        assertEquals("updated-content", stackCaptor.getValue().getContent());
    }

    @Test
    void updateStackById_whenStackMissing_returnsNotFound() throws Exception {
        when(stackService.getById(99L)).thenReturn(null);

        mockMvc.perform(put("/api/v1/ops/stacks/{id}", 99)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "any",
                                  "content": "content"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Stack does not exist"));

        verify(stackMapper, never()).updateById(any());
    }

    @Test
    void createStack_whenNameDuplicate_returnsBadRequest() throws Exception {
        when(stackMapper.exists(any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/ops/stacks")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "duplicate",
                                  "content": "version: '3'"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", is("Stack name already exists")));

        verify(stackMapper, never()).insert(any(Stack.class));
    }
}
