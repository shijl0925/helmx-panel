package com.helmx.tutorial.docker.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DockerEnvEntityTest {

    @Test
    void settersAndGetters_workCorrectly() {
        DockerEnv env = new DockerEnv();
        env.setId(1L);
        env.setName("local");
        env.setRemark("Local Docker daemon");
        env.setHost("unix:///var/run/docker.sock");
        env.setStatus(1);
        env.setTlsVerify(false);

        assertEquals(1L, env.getId());
        assertEquals("local", env.getName());
        assertEquals("Local Docker daemon", env.getRemark());
        assertEquals("unix:///var/run/docker.sock", env.getHost());
        assertEquals(1, env.getStatus());
        assertFalse(env.getTlsVerify());
    }

    @Test
    void defaultTlsVerify_isFalse() {
        // The field defaults to false at declaration site
        DockerEnv env = new DockerEnv();
        assertFalse(env.getTlsVerify());
    }

    @Test
    void tlsVerify_canBeSetToTrue() {
        DockerEnv env = new DockerEnv();
        env.setTlsVerify(true);
        assertTrue(env.getTlsVerify());
    }

    @Test
    void statusField_canBeNull() {
        DockerEnv env = new DockerEnv();
        assertNull(env.getStatus());
    }

    @Test
    void remarkField_canBeNull() {
        DockerEnv env = new DockerEnv();
        assertNull(env.getRemark());
    }
}
