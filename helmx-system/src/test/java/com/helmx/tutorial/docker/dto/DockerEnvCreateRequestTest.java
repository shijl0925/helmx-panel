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
        req.setSshEnabled(true);
        req.setSshPort(22);
        req.setSshUsername("root");
        req.setSshPassword("secret");
        req.setSshHostKeyFingerprint("SHA256:host");

        assertEquals("remote-server", req.getName());
        assertEquals("Production Docker", req.getRemark());
        assertEquals("tcp://192.168.1.100:2376", req.getHost());
        assertTrue(req.getTlsVerify());
        assertTrue(req.getSshEnabled());
        assertEquals(22, req.getSshPort());
        assertEquals("root", req.getSshUsername());
        assertEquals("secret", req.getSshPassword());
        assertEquals("SHA256:host", req.getSshHostKeyFingerprint());
    }

    @Test
    void defaultTlsVerify_isFalse() {
        DockerEnvCreateRequest req = new DockerEnvCreateRequest();
        assertFalse(req.getTlsVerify());
        assertFalse(req.getSshEnabled());
        assertEquals(22, req.getSshPort());
    }

    @Test
    void remarkField_canBeNull() {
        DockerEnvCreateRequest req = new DockerEnvCreateRequest();
        req.setName("local");
        req.setHost("unix:///var/run/docker.sock");
        assertNull(req.getRemark());
    }
}
