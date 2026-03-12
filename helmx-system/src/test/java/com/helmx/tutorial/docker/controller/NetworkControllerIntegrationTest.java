package com.helmx.tutorial.docker.controller;

import com.github.dockerjava.api.model.Network;
import com.helmx.tutorial.docker.utils.DockerClientUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NetworkControllerIntegrationTest {

    @Mock
    private DockerClientUtil dockerClientUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        NetworkController controller = new NetworkController();
        ReflectionTestUtils.setField(controller, "dockerClientUtil", dockerClientUtil);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void searchDockerNetworks_returnsSortedDtosWithUsageState() throws Exception {
        Network newer = createNetwork("network-2", "beta", Date.from(Instant.parse("2026-03-10T00:00:02Z")));
        Network older = createNetwork("network-1", "alpha", Date.from(Instant.parse("2026-03-10T00:00:01Z")));

        when(dockerClientUtil.listNetworks("app")).thenReturn(new ArrayList<>(List.of(older, newer)));
        when(dockerClientUtil.isNetworkInUse("network-1")).thenReturn(false);
        when(dockerClientUtil.isNetworkInUse("network-2")).thenReturn(true);

        mockMvc.perform(post("/api/v1/ops/networks/all")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "name": "app"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value("network-2"))
                .andExpect(jsonPath("$.data[0].name").value("beta"))
                .andExpect(jsonPath("$.data[0].isUsed").value(true))
                .andExpect(jsonPath("$.data[1].id").value("network-1"))
                .andExpect(jsonPath("$.data[1].name").value("alpha"))
                .andExpect(jsonPath("$.data[1].isUsed").value(false));

        verify(dockerClientUtil).setCurrentHost("unix:///var/run/docker.sock");
        verify(dockerClientUtil).listNetworks("app");
    }

    @Test
    void createDockerNetwork_returnsServerErrorWhenBackendFails() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "failed");
        result.put("message", "network create failed");
        result.put("reason", "driver error");
        when(dockerClientUtil.createNetwork(any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/ops/networks")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "name": "backend-net",
                                  "driver": "bridge"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("network create failed"))
                .andExpect(jsonPath("$.data.status").value("failed"))
                .andExpect(jsonPath("$.data.reason").value("driver error"));
    }

    @Test
    void removeDockerNetwork_stopsAndReturnsFailureWhenAnyDeleteFails() throws Exception {
        when(dockerClientUtil.removeNetwork("alpha")).thenReturn(new HashMap<>(Map.of("status", "success")));
        when(dockerClientUtil.removeNetwork("beta")).thenReturn(new HashMap<>(Map.of("status", "failed", "message", "network beta is in use")));

        mockMvc.perform(post("/api/v1/ops/networks/remove")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "names": ["alpha", "beta"]
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("network beta is in use"))
                .andExpect(jsonPath("$.data.status").value("failed"));

        verify(dockerClientUtil).removeNetwork("alpha");
        verify(dockerClientUtil).removeNetwork("beta");
    }

    @Test
    void createDockerNetwork_nullStatusInResult_doesNotThrowNPE() throws Exception {
        // Before the fix, status.equals("success") threw NPE when status was null
        Map<String, Object> result = new HashMap<>();
        result.put("status", null);
        result.put("message", "unexpected null status");
        when(dockerClientUtil.createNetwork(any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/ops/networks")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "name": "null-status-net",
                                  "driver": "bridge"
                                }
                                """))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void removeDockerNetwork_nullStatusInResult_doesNotThrowNPE() throws Exception {
        // Before the fix, removeNetworkResult.get("status").equals("failed") threw NPE
        // when the status key was missing (null value)
        Map<String, Object> nullStatusResult = new HashMap<>();
        nullStatusResult.put("status", null);
        when(dockerClientUtil.removeNetwork("orphan")).thenReturn(nullStatusResult);

        mockMvc.perform(post("/api/v1/ops/networks/remove")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock",
                                  "names": ["orphan"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private Network createNetwork(String id, String name, Date created) {
        Network.Ipam ipam = new Network.Ipam()
                .withDriver("default")
                .withConfig(new Network.Ipam.Config()
                        .withSubnet("172.20.0.0/16")
                        .withGateway("172.20.0.1"));

        Network network = new Network();
        ReflectionTestUtils.setField(network, "id", id);
        ReflectionTestUtils.setField(network, "name", name);
        ReflectionTestUtils.setField(network, "driver", "bridge");
        ReflectionTestUtils.setField(network, "created", created);
        ReflectionTestUtils.setField(network, "ipam", ipam);
        ReflectionTestUtils.setField(network, "options", Map.of("com.docker.network.bridge.name", "br0"));
        ReflectionTestUtils.setField(network, "containers", Map.of());
        ReflectionTestUtils.setField(network, "attachable", true);
        network.labels = Map.of("env", "test");
        return network;
    }
}
