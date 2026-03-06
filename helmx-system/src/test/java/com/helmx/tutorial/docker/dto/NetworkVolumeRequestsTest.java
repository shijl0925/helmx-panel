package com.helmx.tutorial.docker.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkVolumeRequestsTest {

    // ---- NetworkCreateRequest ----

    @Test
    void networkCreateRequest_defaultValues() {
        NetworkCreateRequest req = new NetworkCreateRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getName());
        assertNull(req.getDriver());
        assertNull(req.getDriverOpts());
        assertNull(req.getEnableIpv4());
        assertNull(req.getSubnet());
        assertNull(req.getGateway());
        assertNull(req.getIpRange());
        assertNull(req.getEnableIpv6());
        assertNull(req.getSubnetV6());
        assertNull(req.getGatewayV6());
        assertNull(req.getIpRangeV6());
        assertNull(req.getLabels());
    }

    @Test
    void networkCreateRequest_settersAndGetters() {
        NetworkCreateRequest req = new NetworkCreateRequest();
        req.setHost("tcp://10.0.0.1:2375");
        req.setName("my-net");
        req.setDriver("bridge");
        req.setDriverOpts(new String[]{"opt1=val1"});
        req.setEnableIpv4(true);
        req.setSubnet("172.20.0.0/16");
        req.setGateway("172.20.0.1");
        req.setIpRange("172.20.10.0/24");
        req.setEnableIpv6(false);
        req.setSubnetV6("fd00::/64");
        req.setGatewayV6("fd00::1");
        req.setIpRangeV6("fd00::10/64");
        req.setLabels(new String[]{"env=test"});

        assertEquals("tcp://10.0.0.1:2375", req.getHost());
        assertEquals("my-net", req.getName());
        assertEquals("bridge", req.getDriver());
        assertArrayEquals(new String[]{"opt1=val1"}, req.getDriverOpts());
        assertTrue(req.getEnableIpv4());
        assertEquals("172.20.0.0/16", req.getSubnet());
        assertEquals("172.20.0.1", req.getGateway());
        assertEquals("172.20.10.0/24", req.getIpRange());
        assertFalse(req.getEnableIpv6());
        assertEquals("fd00::/64", req.getSubnetV6());
        assertEquals("fd00::1", req.getGatewayV6());
        assertEquals("fd00::10/64", req.getIpRangeV6());
        assertArrayEquals(new String[]{"env=test"}, req.getLabels());
    }

    // ---- NetworkInfoRequest ----

    @Test
    void networkInfoRequest_defaultHost() {
        NetworkInfoRequest req = new NetworkInfoRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getNetworkId());
    }

    @Test
    void networkInfoRequest_settersAndGetters() {
        NetworkInfoRequest req = new NetworkInfoRequest();
        req.setHost("tcp://10.0.0.2:2375");
        req.setNetworkId("net-abc123");

        assertEquals("tcp://10.0.0.2:2375", req.getHost());
        assertEquals("net-abc123", req.getNetworkId());
    }

    // ---- NetworkQueryRequest ----

    @Test
    void networkQueryRequest_defaultValues() {
        NetworkQueryRequest req = new NetworkQueryRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getName());
    }

    @Test
    void networkQueryRequest_settersAndGetters() {
        NetworkQueryRequest req = new NetworkQueryRequest();
        req.setHost("tcp://10.0.0.3:2375");
        req.setName("bridge");

        assertEquals("tcp://10.0.0.3:2375", req.getHost());
        assertEquals("bridge", req.getName());
    }

    // ---- PruneRequest ----

    @Test
    void pruneRequest_defaultValues() {
        PruneRequest req = new PruneRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getPruneType());
    }

    @Test
    void pruneRequest_settersAndGetters() {
        for (String type : new String[]{"BUILD", "CONTAINERS", "IMAGES", "NETWORKS", "VOLUMES"}) {
            PruneRequest req = new PruneRequest();
            req.setPruneType(type);
            assertEquals(type, req.getPruneType());
        }
    }

    // ---- RegistryConnectRequest ----

    @Test
    void registryConnectRequest_defaultConstructor_allNull() {
        RegistryConnectRequest req = new RegistryConnectRequest();
        assertNull(req.getUrl());
        assertNull(req.getUsername());
        assertNull(req.getPassword());
    }

    @Test
    void registryConnectRequest_settersAndGetters() {
        RegistryConnectRequest req = new RegistryConnectRequest();
        req.setUrl("https://registry.example.com");
        req.setUsername("admin");
        req.setPassword("s3cret");

        assertEquals("https://registry.example.com", req.getUrl());
        assertEquals("admin", req.getUsername());
        assertEquals("s3cret", req.getPassword());
    }

    // ---- RestartPolicyRequest ----

    @Test
    void restartPolicyRequest_defaultConstructor_allNull() {
        RestartPolicyRequest req = new RestartPolicyRequest();
        assertNull(req.getName());
        assertNull(req.getMaximumRetryCount());
    }

    @Test
    void restartPolicyRequest_settersAndGetters() {
        RestartPolicyRequest req = new RestartPolicyRequest();
        req.setName("on-failure");
        req.setMaximumRetryCount(5);

        assertEquals("on-failure", req.getName());
        assertEquals(5, req.getMaximumRetryCount());
    }

    @Test
    void restartPolicyRequest_alwaysPolicy() {
        RestartPolicyRequest req = new RestartPolicyRequest();
        req.setName("always");
        req.setMaximumRetryCount(0);

        assertEquals("always", req.getName());
        assertEquals(0, req.getMaximumRetryCount());
    }

    // ---- StackDeployRequest ----

    @Test
    void stackDeployRequest_defaultConstructor_contentNull() {
        StackDeployRequest req = new StackDeployRequest();
        assertNull(req.getContent());
    }

    @Test
    void stackDeployRequest_settersAndGetters() {
        StackDeployRequest req = new StackDeployRequest();
        req.setContent("version: '3'\nservices:\n  web:\n    image: nginx:latest");
        assertEquals("version: '3'\nservices:\n  web:\n    image: nginx:latest", req.getContent());
    }

    // ---- StackUpdateRequest ----

    @Test
    void stackUpdateRequest_defaultConstructor_allNull() {
        StackUpdateRequest req = new StackUpdateRequest();
        assertNull(req.getName());
        assertNull(req.getContent());
    }

    @Test
    void stackUpdateRequest_settersAndGetters() {
        StackUpdateRequest req = new StackUpdateRequest();
        req.setName("my-stack-v2");
        req.setContent("version: '3'\nservices:\n  web:\n    image: nginx:1.25");

        assertEquals("my-stack-v2", req.getName());
        assertEquals("version: '3'\nservices:\n  web:\n    image: nginx:1.25", req.getContent());
    }

    // ---- StatusRequest ----

    @Test
    void statusRequest_defaultHost_isUnixSocket() {
        StatusRequest req = new StatusRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
    }

    @Test
    void statusRequest_customHost() {
        StatusRequest req = new StatusRequest();
        req.setHost("tcp://192.168.1.200:2376");
        assertEquals("tcp://192.168.1.200:2376", req.getHost());
    }

    @Test
    void statusRequest_equals_sameHost() {
        StatusRequest r1 = new StatusRequest();
        r1.setHost("tcp://10.0.0.1:2375");

        StatusRequest r2 = new StatusRequest();
        r2.setHost("tcp://10.0.0.1:2375");

        assertEquals(r1, r2);
    }
}
