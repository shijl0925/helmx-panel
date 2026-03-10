package com.helmx.tutorial.docker.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkAdvancedRequestsTest {

    // ---- NetworkConnectRequest ----

    @Test
    void networkConnectRequest_defaultHost() {
        NetworkConnectRequest req = new NetworkConnectRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getNetworkId());
        assertNull(req.getContainerId());
    }

    @Test
    void networkConnectRequest_settersAndGetters() {
        NetworkConnectRequest req = new NetworkConnectRequest();
        req.setHost("tcp://10.0.0.1:2375");
        req.setNetworkId("net-abc123");
        req.setContainerId("container-xyz");

        assertEquals("tcp://10.0.0.1:2375", req.getHost());
        assertEquals("net-abc123", req.getNetworkId());
        assertEquals("container-xyz", req.getContainerId());
    }

    // ---- NetworkDisconnectRequest ----

    @Test
    void networkDisconnectRequest_defaultHost() {
        NetworkDisconnectRequest req = new NetworkDisconnectRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getNetworkId());
        assertNull(req.getContainerId());
    }

    @Test
    void networkDisconnectRequest_settersAndGetters() {
        NetworkDisconnectRequest req = new NetworkDisconnectRequest();
        req.setHost("tcp://10.0.0.2:2375");
        req.setNetworkId("net-def456");
        req.setContainerId("container-abc");

        assertEquals("tcp://10.0.0.2:2375", req.getHost());
        assertEquals("net-def456", req.getNetworkId());
        assertEquals("container-abc", req.getContainerId());
    }

    // ---- NetworkSearchRequest ----

    @Test
    void networkSearchRequest_defaultValues() {
        NetworkSearchRequest req = new NetworkSearchRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getName());
        assertNull(req.getDriver());
        assertNull(req.getScope());
        assertNull(req.getSortOrder());
        assertNull(req.getPage());
        assertNull(req.getPageSize());
    }

    @Test
    void networkSearchRequest_settersAndGetters() {
        NetworkSearchRequest req = new NetworkSearchRequest();
        req.setHost("tcp://192.168.1.100:2375");
        req.setName("my-net");
        req.setDriver("bridge");
        req.setScope("local");
        req.setSortOrder("asc");
        req.setPage(2);
        req.setPageSize(20);

        assertEquals("tcp://192.168.1.100:2375", req.getHost());
        assertEquals("my-net", req.getName());
        assertEquals("bridge", req.getDriver());
        assertEquals("local", req.getScope());
        assertEquals("asc", req.getSortOrder());
        assertEquals(2, req.getPage());
        assertEquals(20, req.getPageSize());
    }

    @Test
    void networkSearchRequest_overlayDriver() {
        NetworkSearchRequest req = new NetworkSearchRequest();
        req.setDriver("overlay");
        req.setScope("swarm");
        assertEquals("overlay", req.getDriver());
        assertEquals("swarm", req.getScope());
    }

    // ---- ImageHubSearchRequest ----

    @Test
    void imageHubSearchRequest_defaultValues() {
        ImageHubSearchRequest req = new ImageHubSearchRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getTerm());
        assertEquals(25, req.getLimit());
    }

    @Test
    void imageHubSearchRequest_settersAndGetters() {
        ImageHubSearchRequest req = new ImageHubSearchRequest();
        req.setHost("tcp://10.0.0.5:2375");
        req.setTerm("nginx");
        req.setLimit(50);

        assertEquals("tcp://10.0.0.5:2375", req.getHost());
        assertEquals("nginx", req.getTerm());
        assertEquals(50, req.getLimit());
    }

    @Test
    void imageHubSearchRequest_customLimit() {
        ImageHubSearchRequest req = new ImageHubSearchRequest();
        req.setTerm("redis");
        req.setLimit(10);
        assertEquals("redis", req.getTerm());
        assertEquals(10, req.getLimit());
    }
}
