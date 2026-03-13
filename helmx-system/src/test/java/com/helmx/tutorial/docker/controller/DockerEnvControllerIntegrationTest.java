package com.helmx.tutorial.docker.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.helmx.tutorial.docker.entity.DockerEnv;
import com.helmx.tutorial.docker.mapper.DockerEnvMapper;
import com.helmx.tutorial.docker.service.DockerEnvService;
import com.helmx.tutorial.docker.utils.PasswordUtil;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DockerEnvControllerIntegrationTest {

    @Mock
    private DockerEnvService dockerEnvService;

    @Mock
    private DockerEnvMapper dockerEnvMapper;

    @Mock
    private PasswordUtil passwordUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DockerEnvController controller = new DockerEnvController();
        ReflectionTestUtils.setField(controller, "dockerEnvService", dockerEnvService);
        ReflectionTestUtils.setField(controller, "dockerEnvMapper", dockerEnvMapper);
        ReflectionTestUtils.setField(controller, "passwordUtil", passwordUtil);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllDockerEnvs_returnsActiveEnvironmentList() throws Exception {
        DockerEnv env = createEnv(1L, "prod", "Production", "tcp://docker:2376", 1, true);
        when(dockerEnvMapper.selectList(any())).thenReturn(List.of(env));

        mockMvc.perform(get("/api/v1/ops/envs/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("prod"))
                .andExpect(jsonPath("$.data[0].tlsVerify").value(true));
    }

    @Test
    void searchDockerEnvs_returnsPagedDtos() throws Exception {
        DockerEnv env = createEnv(2L, "staging", "Staging", "tcp://staging:2376", 1, false);
        when(dockerEnvMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<DockerEnv> page = invocation.getArgument(0);
            page.setRecords(List.of(env));
            page.setTotal(1);
            return page;
        });

        mockMvc.perform(get("/api/v1/ops/envs")
                        .param("name", "stag")
                        .param("page", "2")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.current").value(2))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("staging"))
                .andExpect(jsonPath("$.data.items[0].host").value("tcp://staging:2376"));
    }

    @Test
    void searchDockerEnvs_normalizesInvalidPagination() throws Exception {
        when(dockerEnvMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(get("/api/v1/ops/envs")
                        .param("page", "0")
                        .param("pageSize", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10));

        mockMvc.perform(get("/api/v1/ops/envs")
                        .param("page", "-2")
                        .param("pageSize", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10));

        ArgumentCaptor<Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(dockerEnvMapper, org.mockito.Mockito.times(2)).selectPage(captor.capture(), any());
        assertEquals(1L, captor.getAllValues().get(0).getCurrent());
        assertEquals(10L, captor.getAllValues().get(0).getSize());
        assertEquals(1L, captor.getAllValues().get(1).getCurrent());
        assertEquals(10L, captor.getAllValues().get(1).getSize());
    }

    @Test
    void createDockerEnv_persistsTlsVerifyAndDefaultRemark() throws Exception {
        when(dockerEnvMapper.exists(any())).thenReturn(false);
        when(passwordUtil.encrypt("secret")).thenReturn("encrypted-secret");
        doAnswer(invocation -> {
            DockerEnv env = invocation.getArgument(0);
            env.setId(9L);
            return 1;
        }).when(dockerEnvMapper).insert(any(DockerEnv.class));

        mockMvc.perform(post("/api/v1/ops/envs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "dev",
                                  "host": "tcp://dev:2376",
                                  "tlsVerify": true,
                                  "sshEnabled": true,
                                  "sshPort": 2222,
                                  "sshUsername": "root",
                                  "sshPassword": "secret",
                                  "sshHostKeyFingerprint": "SHA256:host"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(9))
                .andExpect(jsonPath("$.data.name").value("dev"))
                .andExpect(jsonPath("$.data.tlsVerify").value(true))
                .andExpect(jsonPath("$.data.remark").value(""))
                .andExpect(jsonPath("$.data.sshEnabled").value(true))
                .andExpect(jsonPath("$.data.sshPort").value(2222))
                .andExpect(jsonPath("$.data.sshUsername").value("root"))
                .andExpect(jsonPath("$.data.sshPasswordConfigured").value(true));

        ArgumentCaptor<DockerEnv> captor = ArgumentCaptor.forClass(DockerEnv.class);
        verify(dockerEnvMapper).insert(captor.capture());
        assertEquals(true, captor.getValue().getTlsVerify());
        assertEquals("", captor.getValue().getRemark());
        assertEquals(true, captor.getValue().getSshEnabled());
        assertEquals(2222, captor.getValue().getSshPort());
        assertEquals("root", captor.getValue().getSshUsername());
        assertEquals("encrypted-secret", captor.getValue().getSshPassword());
        assertEquals("SHA256:host", captor.getValue().getSshHostKeyFingerprint());
    }

    @Test
    void updateDockerEnv_returnsNotFoundWhenMissing() throws Exception {
        when(dockerEnvService.getById(99L)).thenReturn(null);

        mockMvc.perform(put("/api/v1/ops/envs/99")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "missing"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Env not found"));
    }

    private DockerEnv createEnv(Long id, String name, String remark, String host, Integer status, Boolean tlsVerify) {
        DockerEnv env = new DockerEnv();
        env.setId(id);
        env.setName(name);
        env.setRemark(remark);
        env.setHost(host);
        env.setStatus(status);
        env.setTlsVerify(tlsVerify);
        env.setSshEnabled(true);
        env.setSshPort(22);
        env.setSshUsername("root");
        env.setSshPassword("encrypted");
        env.setSshHostKeyFingerprint("SHA256:host");
        return env;
    }

    @Test
    void getAllDockerEnvs_filtersByEnvType() throws Exception {
        DockerEnv prodEnv = createEnvWithType(10L, "prod-1", "tcp://prod1:2376", "prod", "cluster-prod");
        when(dockerEnvMapper.selectList(any())).thenReturn(List.of(prodEnv));

        mockMvc.perform(get("/api/v1/ops/envs/all").param("envType", "prod"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].envType").value("prod"))
                .andExpect(jsonPath("$.data[0].clusterName").value("cluster-prod"));
    }

    @Test
    void getDockerEnvsGrouped_returnsMapKeyedByEnvType() throws Exception {
        DockerEnv devEnv  = createEnvWithType(20L, "dev-1",  "tcp://dev1:2376",  "dev",  "cluster-dev");
        DockerEnv prodEnv = createEnvWithType(21L, "prod-1", "tcp://prod1:2376", "prod", "cluster-prod");
        DockerEnv noType  = createEnvWithType(22L, "bare",   "tcp://bare:2376",  null,   null);
        when(dockerEnvMapper.selectList(any())).thenReturn(List.of(devEnv, prodEnv, noType));

        mockMvc.perform(get("/api/v1/ops/envs/grouped"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.dev[0].name").value("dev-1"))
                .andExpect(jsonPath("$.data.prod[0].name").value("prod-1"))
                .andExpect(jsonPath("$.data.default[0].name").value("bare"));
    }

    @Test
    void createDockerEnv_persistsEnvTypeAndClusterName() throws Exception {
        when(dockerEnvMapper.exists(any())).thenReturn(false);
        doAnswer(invocation -> {
            DockerEnv env = invocation.getArgument(0);
            env.setId(30L);
            return 1;
        }).when(dockerEnvMapper).insert(any(DockerEnv.class));

        mockMvc.perform(post("/api/v1/ops/envs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "uat-host",
                                  "host": "tcp://uat:2376",
                                  "envType": "uat",
                                  "clusterName": "cluster-uat"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.envType").value("uat"))
                .andExpect(jsonPath("$.data.clusterName").value("cluster-uat"));

        ArgumentCaptor<DockerEnv> captor = ArgumentCaptor.forClass(DockerEnv.class);
        verify(dockerEnvMapper).insert(captor.capture());
        assertEquals("uat", captor.getValue().getEnvType());
        assertEquals("cluster-uat", captor.getValue().getClusterName());
    }

    @Test
    void updateDockerEnv_updatesEnvTypeAndClusterName() throws Exception {
        DockerEnv existing = createEnvWithType(40L, "test-host", "tcp://test:2376", "test", "cluster-old");
        when(dockerEnvService.getById(40L)).thenReturn(existing);

        mockMvc.perform(put("/api/v1/ops/envs/40")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "envType": "prod",
                                  "clusterName": "cluster-new"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.envType").value("prod"))
                .andExpect(jsonPath("$.data.clusterName").value("cluster-new"));
    }

    private DockerEnv createEnvWithType(Long id, String name, String host, String envType, String clusterName) {
        DockerEnv env = createEnv(id, name, "", host, 1, false);
        env.setEnvType(envType);
        env.setClusterName(clusterName);
        return env;
    }
}
