package com.helmx.tutorial.docker.dto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContainerRequestsTest {

    // ---- ContainerCommitRequest ----

    @Test
    void commitRequest_defaultHost_isUnixSocket() {
        ContainerCommitRequest req = new ContainerCommitRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
    }

    @Test
    void commitRequest_settersAndGetters() {
        ContainerCommitRequest req = new ContainerCommitRequest();
        req.setHost("tcp://192.168.1.10:2375");
        req.setContainerId("abc123");
        req.setRepository("my-repo:latest");

        assertEquals("tcp://192.168.1.10:2375", req.getHost());
        assertEquals("abc123", req.getContainerId());
        assertEquals("my-repo:latest", req.getRepository());
    }

    @Test
    void commitRequest_equals_sameFields() {
        ContainerCommitRequest r1 = new ContainerCommitRequest();
        r1.setContainerId("c1");
        r1.setRepository("repo");

        ContainerCommitRequest r2 = new ContainerCommitRequest();
        r2.setContainerId("c1");
        r2.setRepository("repo");

        assertEquals(r1, r2);
    }

    // ---- ContainerCopyRequest ----

    @Test
    void copyRequest_defaultConstructor_fieldsAreNull() {
        ContainerCopyRequest req = new ContainerCopyRequest();
        assertNull(req.getHost());
        assertNull(req.getContainerId());
        assertNull(req.getContainerPath());
    }

    @Test
    void copyRequest_settersAndGetters() {
        ContainerCopyRequest req = new ContainerCopyRequest();
        req.setHost("unix:///var/run/docker.sock");
        req.setContainerId("def456");
        req.setContainerPath("/app/data");

        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertEquals("def456", req.getContainerId());
        assertEquals("/app/data", req.getContainerPath());
    }

    // ---- ContainerExecRequest ----

    @Test
    void execRequest_defaultBooleans_areTrue() {
        ContainerExecRequest req = new ContainerExecRequest();
        assertTrue(req.getAttachStdin());
        assertTrue(req.getAttachStdout());
        assertTrue(req.getAttachStderr());
        assertTrue(req.getTty());
    }

    @Test
    void execRequest_settersAndGetters() {
        ContainerExecRequest req = new ContainerExecRequest();
        req.setHost("unix:///var/run/docker.sock");
        req.setContainerId("ghi789");
        req.setCommand(new String[]{"ls", "-la"});
        req.setAttachStdin(false);
        req.setAttachStdout(false);
        req.setAttachStderr(false);
        req.setTty(false);

        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertEquals("ghi789", req.getContainerId());
        assertArrayEquals(new String[]{"ls", "-la"}, req.getCommand());
        assertFalse(req.getAttachStdin());
        assertFalse(req.getAttachStdout());
        assertFalse(req.getAttachStderr());
        assertFalse(req.getTty());
    }

    // ---- ContainerInfoRequest ----

    @Test
    void infoRequest_defaultValues() {
        ContainerInfoRequest req = new ContainerInfoRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertTrue(req.isSimple()); // default is true
        assertNull(req.getContainerId());
    }

    @Test
    void infoRequest_settersAndGetters() {
        ContainerInfoRequest req = new ContainerInfoRequest();
        req.setHost("tcp://10.0.0.1:2376");
        req.setContainerId("jkl012");
        req.setSimple(false);

        assertEquals("tcp://10.0.0.1:2376", req.getHost());
        assertEquals("jkl012", req.getContainerId());
        assertFalse(req.isSimple());
    }

    // ---- ContainerLogRequest ----

    @Test
    void logRequest_defaultValues() {
        ContainerLogRequest req = new ContainerLogRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertEquals(100, req.getTail());
        assertNull(req.getContainerId());
    }

    @Test
    void logRequest_settersAndGetters() {
        ContainerLogRequest req = new ContainerLogRequest();
        req.setHost("tcp://10.0.0.2:2375");
        req.setContainerId("mno345");
        req.setTail(200);

        assertEquals("tcp://10.0.0.2:2375", req.getHost());
        assertEquals("mno345", req.getContainerId());
        assertEquals(200, req.getTail());
    }

    // ---- ContainerNetworkDisconnectRequest ----

    @Test
    void networkDisconnectRequest_defaultHost() {
        ContainerNetworkDisconnectRequest req = new ContainerNetworkDisconnectRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getContainerId());
        assertNull(req.getNetwork());
    }

    @Test
    void networkDisconnectRequest_settersAndGetters() {
        ContainerNetworkDisconnectRequest req = new ContainerNetworkDisconnectRequest();
        req.setHost("tcp://localhost:2375");
        req.setContainerId("pqr678");
        req.setNetwork("my-network");

        assertEquals("tcp://localhost:2375", req.getHost());
        assertEquals("pqr678", req.getContainerId());
        assertEquals("my-network", req.getNetwork());
    }

    // ---- ContainerNetworkUpdateRequest ----

    @Test
    void networkUpdateRequest_defaultHost() {
        ContainerNetworkUpdateRequest req = new ContainerNetworkUpdateRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getContainerId());
        assertNull(req.getNetworks());
    }

    @Test
    void networkUpdateRequest_settersAndGetters() {
        ContainerNetworkUpdateRequest req = new ContainerNetworkUpdateRequest();
        req.setHost("tcp://192.168.1.5:2375");
        req.setContainerId("stu901");
        req.setNetworks(new String[]{"net1", "net2"});

        assertEquals("tcp://192.168.1.5:2375", req.getHost());
        assertEquals("stu901", req.getContainerId());
        assertArrayEquals(new String[]{"net1", "net2"}, req.getNetworks());
    }

    // ---- ContainerQueryRequest ----

    @Test
    void queryRequest_defaultValues() {
        ContainerQueryRequest req = new ContainerQueryRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getContainerId());
        assertNull(req.getName());
        assertNull(req.getState());
        assertNull(req.getFilters());
        assertNull(req.getSortBy());
        assertNull(req.getSortOrder());
        assertNull(req.getPage());
        assertNull(req.getPageSize());
    }

    @Test
    void queryRequest_settersAndGetters() {
        ContainerQueryRequest req = new ContainerQueryRequest();
        req.setHost("tcp://10.0.0.3:2375");
        req.setContainerId("vwx234");
        req.setName("my-container");
        req.setState("running");
        req.setFilters(Map.of("status", "running"));
        req.setSortBy("name");
        req.setSortOrder("asc");
        req.setPage(1);
        req.setPageSize(20);

        assertEquals("tcp://10.0.0.3:2375", req.getHost());
        assertEquals("vwx234", req.getContainerId());
        assertEquals("my-container", req.getName());
        assertEquals("running", req.getState());
        assertEquals("running", req.getFilters().get("status"));
        assertEquals("name", req.getSortBy());
        assertEquals("asc", req.getSortOrder());
        assertEquals(1, req.getPage());
        assertEquals(20, req.getPageSize());
    }

    // ---- ContainerRenameRequest ----

    @Test
    void renameRequest_defaultHost() {
        ContainerRenameRequest req = new ContainerRenameRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getContainerId());
        assertNull(req.getNewName());
    }

    @Test
    void renameRequest_settersAndGetters() {
        ContainerRenameRequest req = new ContainerRenameRequest();
        req.setHost("tcp://10.0.0.4:2375");
        req.setContainerId("yz1234");
        req.setNewName("renamed-container");

        assertEquals("tcp://10.0.0.4:2375", req.getHost());
        assertEquals("yz1234", req.getContainerId());
        assertEquals("renamed-container", req.getNewName());
    }
}
