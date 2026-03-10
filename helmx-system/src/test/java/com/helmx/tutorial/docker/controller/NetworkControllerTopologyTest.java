package com.helmx.tutorial.docker.controller;

import com.helmx.tutorial.docker.dto.NetworkTopologyNode;
import com.helmx.tutorial.docker.utils.DockerClientUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NetworkControllerTopologyTest {

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
    void getNetworkTopology_returnsAllNetworksWithContainers() throws Exception {
        NetworkTopologyNode bridge = new NetworkTopologyNode();
        bridge.setNetworkId("net-bridge-id");
        bridge.setName("bridge");
        bridge.setDriver("bridge");
        bridge.setScope("local");
        bridge.setSubnet("172.17.0.0/16");
        bridge.setGateway("172.17.0.1");
        bridge.setContainers(List.of(
                Map.of("containerId", "abc123456789", "name", "web", "ipv4Address", "172.17.0.2", "macAddress", "02:42:ac:11:00:02")
        ));

        NetworkTopologyNode appNet = new NetworkTopologyNode();
        appNet.setNetworkId("net-app-id");
        appNet.setName("app-network");
        appNet.setDriver("bridge");
        appNet.setScope("local");
        appNet.setSubnet("172.20.0.0/16");
        appNet.setGateway("172.20.0.1");
        appNet.setContainers(List.of());

        when(dockerClientUtil.getNetworkTopology()).thenReturn(List.of(bridge, appNet));

        mockMvc.perform(post("/api/v1/ops/networks/topology")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].name").value("bridge"))
                .andExpect(jsonPath("$.data[0].driver").value("bridge"))
                .andExpect(jsonPath("$.data[0].subnet").value("172.17.0.0/16"))
                .andExpect(jsonPath("$.data[0].gateway").value("172.17.0.1"))
                .andExpect(jsonPath("$.data[0].containers[0].name").value("web"))
                .andExpect(jsonPath("$.data[0].containers[0].ipv4Address").value("172.17.0.2"))
                .andExpect(jsonPath("$.data[1].name").value("app-network"))
                .andExpect(jsonPath("$.data[1].containers").isEmpty());

        verify(dockerClientUtil).setCurrentHost("unix:///var/run/docker.sock");
        verify(dockerClientUtil).getNetworkTopology();
    }

    @Test
    void getNetworkTopology_returnsEmptyListWhenNoNetworks() throws Exception {
        when(dockerClientUtil.getNetworkTopology()).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/ops/networks/topology")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "host": "unix:///var/run/docker.sock"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
