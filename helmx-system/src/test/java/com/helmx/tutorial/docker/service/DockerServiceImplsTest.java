package com.helmx.tutorial.docker.service;

import com.helmx.tutorial.docker.entity.DockerEnv;
import com.helmx.tutorial.docker.mapper.DockerEnvMapper;
import com.helmx.tutorial.docker.mapper.RegistryMapper;
import com.helmx.tutorial.docker.mapper.StackMapper;
import com.helmx.tutorial.docker.mapper.TemplateMapper;
import com.helmx.tutorial.docker.entity.Registry;
import com.helmx.tutorial.docker.entity.Stack;
import com.helmx.tutorial.docker.entity.Template;
import com.helmx.tutorial.docker.service.impl.DockerEnvServiceImpl;
import com.helmx.tutorial.docker.service.impl.RegistryServiceImpl;
import com.helmx.tutorial.docker.service.impl.StackServiceImpl;
import com.helmx.tutorial.docker.service.impl.TemplateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for the thin docker service implementations.
 * Each service is a ServiceImpl wrapper with injected mapper —
 * we verify the mapper injection and basic CRUD delegation.
 */
@ExtendWith(MockitoExtension.class)
class DockerServiceImplsTest {

    // ===================== DockerEnvServiceImpl =====================

    @Mock
    private DockerEnvMapper dockerEnvMapper;

    @InjectMocks
    private DockerEnvServiceImpl dockerEnvService;

    // ===================== RegistryServiceImpl =====================

    @Mock
    private RegistryMapper registryMapper;

    @InjectMocks
    private RegistryServiceImpl registryService;

    // ===================== StackServiceImpl =====================

    @Mock
    private StackMapper stackMapper;

    @InjectMocks
    private StackServiceImpl stackService;

    // ===================== TemplateServiceImpl =====================

    @Mock
    private TemplateMapper templateMapper;

    @InjectMocks
    private TemplateServiceImpl templateService;

    @BeforeEach
    void setUp() {
        // Inject mocks into the "baseMapper" field used by ServiceImpl
        ReflectionTestUtils.setField(dockerEnvService, "baseMapper", dockerEnvMapper);
        ReflectionTestUtils.setField(dockerEnvService, "dockerEnvMapper", dockerEnvMapper);

        ReflectionTestUtils.setField(registryService, "baseMapper", registryMapper);
        ReflectionTestUtils.setField(registryService, "registryMapper", registryMapper);

        ReflectionTestUtils.setField(stackService, "baseMapper", stackMapper);
        ReflectionTestUtils.setField(stackService, "stackMapper", stackMapper);

        ReflectionTestUtils.setField(templateService, "baseMapper", templateMapper);
        ReflectionTestUtils.setField(templateService, "templateMapper", templateMapper);
    }

    // ---- DockerEnvServiceImpl ----

    @Test
    void dockerEnvService_listAll_delegatesToMapper() {
        DockerEnv env = new DockerEnv();
        env.setId(1L);
        env.setName("local");
        env.setHost("unix:///var/run/docker.sock");
        when(dockerEnvMapper.selectList(any())).thenReturn(List.of(env));

        List<DockerEnv> results = dockerEnvService.list();

        assertEquals(1, results.size());
        assertEquals("local", results.get(0).getName());
    }

    @Test
    void dockerEnvService_getById_delegatesToMapper() {
        DockerEnv env = new DockerEnv();
        env.setId(42L);
        env.setName("remote");
        when(dockerEnvMapper.selectById(42L)).thenReturn(env);

        DockerEnv result = dockerEnvService.getById(42L);

        assertNotNull(result);
        assertEquals("remote", result.getName());
    }

    @Test
    void dockerEnvService_getById_notFound_returnsNull() {
        when(dockerEnvMapper.selectById(99L)).thenReturn(null);
        assertNull(dockerEnvService.getById(99L));
    }

    // ---- RegistryServiceImpl ----

    @Test
    void registryService_listAll_delegatesToMapper() {
        Registry registry = new Registry();
        registry.setId(1L);
        registry.setName("DockerHub");
        registry.setUrl("https://hub.docker.com");
        when(registryMapper.selectList(any())).thenReturn(List.of(registry));

        List<Registry> results = registryService.list();

        assertEquals(1, results.size());
        assertEquals("DockerHub", results.get(0).getName());
    }

    @Test
    void registryService_getById_delegatesToMapper() {
        Registry registry = new Registry();
        registry.setId(5L);
        registry.setUrl("https://registry.example.com");
        when(registryMapper.selectById(5L)).thenReturn(registry);

        Registry result = registryService.getById(5L);

        assertNotNull(result);
        assertEquals("https://registry.example.com", result.getUrl());
    }

    @Test
    void registryService_getById_notFound_returnsNull() {
        when(registryMapper.selectById(0L)).thenReturn(null);
        assertNull(registryService.getById(0L));
    }

    // ---- StackServiceImpl ----

    @Test
    void stackService_listAll_delegatesToMapper() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setName("my-stack");
        when(stackMapper.selectList(any())).thenReturn(List.of(stack));

        List<Stack> results = stackService.list();

        assertEquals(1, results.size());
        assertEquals("my-stack", results.get(0).getName());
    }

    @Test
    void stackService_getById_delegatesToMapper() {
        Stack stack = new Stack();
        stack.setId(10L);
        stack.setContent("version: '3'\nservices:\n  web:\n    image: nginx");
        when(stackMapper.selectById(10L)).thenReturn(stack);

        Stack result = stackService.getById(10L);

        assertNotNull(result);
        assertTrue(result.getContent().contains("nginx"));
    }

    @Test
    void stackService_getById_notFound_returnsNull() {
        when(stackMapper.selectById(999L)).thenReturn(null);
        assertNull(stackService.getById(999L));
    }

    // ---- TemplateServiceImpl ----

    @Test
    void templateService_listAll_delegatesToMapper() {
        Template template = new Template();
        template.setId(1L);
        template.setName("nginx-template");
        template.setType("Dockerfile");
        when(templateMapper.selectList(any())).thenReturn(List.of(template));

        List<Template> results = templateService.list();

        assertEquals(1, results.size());
        assertEquals("nginx-template", results.get(0).getName());
    }

    @Test
    void templateService_getById_delegatesToMapper() {
        Template template = new Template();
        template.setId(20L);
        template.setName("compose-template");
        template.setType("DockerCompose");
        when(templateMapper.selectById(20L)).thenReturn(template);

        Template result = templateService.getById(20L);

        assertNotNull(result);
        assertEquals("DockerCompose", result.getType());
    }

    @Test
    void templateService_getById_notFound_returnsNull() {
        when(templateMapper.selectById(404L)).thenReturn(null);
        assertNull(templateService.getById(404L));
    }
}
