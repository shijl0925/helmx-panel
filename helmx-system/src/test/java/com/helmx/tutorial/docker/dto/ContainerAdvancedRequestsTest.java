package com.helmx.tutorial.docker.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContainerAdvancedRequestsTest {

    // ---- ContainerExportRequest ----

    @Test
    void containerExportRequest_defaultHost() {
        ContainerExportRequest req = new ContainerExportRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getContainerId());
        assertNull(req.getFilename());
    }

    @Test
    void containerExportRequest_settersAndGetters() {
        ContainerExportRequest req = new ContainerExportRequest();
        req.setHost("tcp://10.0.0.1:2375");
        req.setContainerId("abc123def456");
        req.setFilename("my-container-export.tar");

        assertEquals("tcp://10.0.0.1:2375", req.getHost());
        assertEquals("abc123def456", req.getContainerId());
        assertEquals("my-container-export.tar", req.getFilename());
    }

    @Test
    void containerExportRequest_noFilename_isNull() {
        ContainerExportRequest req = new ContainerExportRequest();
        req.setContainerId("container-xyz");
        assertNull(req.getFilename());
    }

    @Test
    void containerExportRequest_equals_sameFields() {
        ContainerExportRequest r1 = new ContainerExportRequest();
        r1.setContainerId("abc");
        r1.setFilename("abc.tar");

        ContainerExportRequest r2 = new ContainerExportRequest();
        r2.setContainerId("abc");
        r2.setFilename("abc.tar");

        assertEquals(r1, r2);
    }
}
