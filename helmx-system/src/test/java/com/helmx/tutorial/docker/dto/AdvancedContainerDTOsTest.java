package com.helmx.tutorial.docker.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedContainerDTOsTest {

    // ---- ContainerFilesRequest ----

    @Test
    void filesRequest_defaultValues() {
        ContainerFilesRequest req = new ContainerFilesRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertEquals("/", req.getPath());
        assertNull(req.getContainerId());
    }

    @Test
    void filesRequest_settersAndGetters() {
        ContainerFilesRequest req = new ContainerFilesRequest();
        req.setHost("tcp://192.168.1.10:2375");
        req.setContainerId("abc123");
        req.setPath("/etc");

        assertEquals("tcp://192.168.1.10:2375", req.getHost());
        assertEquals("abc123", req.getContainerId());
        assertEquals("/etc", req.getPath());
    }

    @Test
    void filesRequest_equals_sameFields() {
        ContainerFilesRequest r1 = new ContainerFilesRequest();
        r1.setContainerId("c1");
        r1.setPath("/var");

        ContainerFilesRequest r2 = new ContainerFilesRequest();
        r2.setContainerId("c1");
        r2.setPath("/var");

        assertEquals(r1, r2);
    }

    @Test
    void filesRequest_toString_containsFields() {
        ContainerFilesRequest req = new ContainerFilesRequest();
        req.setContainerId("myContainer");
        req.setPath("/app");

        String str = req.toString();
        assertTrue(str.contains("myContainer"));
        assertTrue(str.contains("/app"));
    }

    // ---- BulkContainerOperationRequest ----

    @Test
    void bulkRequest_defaultValues() {
        BulkContainerOperationRequest req = new BulkContainerOperationRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertFalse(req.isForce());
        assertNull(req.getContainerIds());
        assertNull(req.getOperation());
    }

    @Test
    void bulkRequest_settersAndGetters() {
        BulkContainerOperationRequest req = new BulkContainerOperationRequest();
        req.setHost("tcp://192.168.1.1:2375");
        req.setContainerIds(List.of("c1", "c2", "c3"));
        req.setOperation("stop");
        req.setForce(true);

        assertEquals("tcp://192.168.1.1:2375", req.getHost());
        assertEquals(List.of("c1", "c2", "c3"), req.getContainerIds());
        assertEquals("stop", req.getOperation());
        assertTrue(req.isForce());
    }

    @Test
    void bulkRequest_equals_sameFields() {
        BulkContainerOperationRequest r1 = new BulkContainerOperationRequest();
        r1.setContainerIds(List.of("c1", "c2"));
        r1.setOperation("start");

        BulkContainerOperationRequest r2 = new BulkContainerOperationRequest();
        r2.setContainerIds(List.of("c1", "c2"));
        r2.setOperation("start");

        assertEquals(r1, r2);
    }

    @Test
    void bulkRequest_toString_containsFields() {
        BulkContainerOperationRequest req = new BulkContainerOperationRequest();
        req.setOperation("restart");
        req.setContainerIds(List.of("abc"));

        String str = req.toString();
        assertTrue(str.contains("restart"));
        assertTrue(str.contains("abc"));
    }

    // ---- ContainerResourceUpdateRequest ----

    @Test
    void resourceUpdateRequest_defaultValues() {
        ContainerResourceUpdateRequest req = new ContainerResourceUpdateRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getContainerId());
        assertNull(req.getCpuShares());
        assertNull(req.getCpuQuota());
        assertNull(req.getCpuPeriod());
        assertNull(req.getMemory());
        assertNull(req.getMemorySwap());
        assertNull(req.getMemoryReservation());
        assertNull(req.getBlkioWeight());
    }

    @Test
    void resourceUpdateRequest_settersAndGetters() {
        ContainerResourceUpdateRequest req = new ContainerResourceUpdateRequest();
        req.setHost("tcp://192.168.1.1:2375");
        req.setContainerId("myContainer");
        req.setCpuShares(512);
        req.setCpuQuota(50000L);
        req.setCpuPeriod(100000L);
        req.setMemory(536870912L);        // 512 MB
        req.setMemorySwap(1073741824L);   // 1 GB
        req.setMemoryReservation(268435456L); // 256 MB
        req.setBlkioWeight(500);

        assertEquals("tcp://192.168.1.1:2375", req.getHost());
        assertEquals("myContainer", req.getContainerId());
        assertEquals(512, req.getCpuShares());
        assertEquals(50000L, req.getCpuQuota());
        assertEquals(100000L, req.getCpuPeriod());
        assertEquals(536870912L, req.getMemory());
        assertEquals(1073741824L, req.getMemorySwap());
        assertEquals(268435456L, req.getMemoryReservation());
        assertEquals(500, req.getBlkioWeight());
    }

    @Test
    void resourceUpdateRequest_equals_sameFields() {
        ContainerResourceUpdateRequest r1 = new ContainerResourceUpdateRequest();
        r1.setContainerId("c1");
        r1.setMemory(512L * 1024 * 1024);

        ContainerResourceUpdateRequest r2 = new ContainerResourceUpdateRequest();
        r2.setContainerId("c1");
        r2.setMemory(512L * 1024 * 1024);

        assertEquals(r1, r2);
    }

    @Test
    void resourceUpdateRequest_toString_containsFields() {
        ContainerResourceUpdateRequest req = new ContainerResourceUpdateRequest();
        req.setContainerId("resourceContainer");
        req.setCpuShares(1024);

        String str = req.toString();
        assertTrue(str.contains("resourceContainer"));
        assertTrue(str.contains("1024"));
    }
}
