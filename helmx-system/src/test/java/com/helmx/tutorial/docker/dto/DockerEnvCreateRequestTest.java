package com.helmx.tutorial.docker.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DockerEnvCreateRequestTest {

    @Test
    void settersAndGetters_workCorrectly() {
        DockerEnvCreateRequest req = new DockerEnvCreateRequest();
        req.setName("remote-server");
        req.setRemark("Production Docker");
        req.setHost("tcp://192.168.1.100:2376");
        req.setTlsVerify(true);

        assertEquals("remote-server", req.getName());
        assertEquals("Production Docker", req.getRemark());
        assertEquals("tcp://192.168.1.100:2376", req.getHost());
        assertTrue(req.getTlsVerify());
    }

    @Test
    void defaultTlsVerify_isFalse() {
        DockerEnvCreateRequest req = new DockerEnvCreateRequest();
        assertFalse(req.getTlsVerify());
    }

    @Test
    void remarkField_canBeNull() {
        DockerEnvCreateRequest req = new DockerEnvCreateRequest();
        req.setName("local");
        req.setHost("unix:///var/run/docker.sock");
        assertNull(req.getRemark());
    }
}
