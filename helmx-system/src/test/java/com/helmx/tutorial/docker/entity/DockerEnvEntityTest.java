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
        env.setSshEnabled(true);
        env.setSshPort(22);
        env.setSshUsername("root");
        env.setSshPassword("encrypted");
        env.setSshHostKeyFingerprint("SHA256:host");
        env.setEnvType("dev");
        env.setClusterName("cluster-1");

        assertEquals(1L, env.getId());
        assertEquals("local", env.getName());
        assertEquals("Local Docker daemon", env.getRemark());
        assertEquals("unix:///var/run/docker.sock", env.getHost());
        assertEquals(1, env.getStatus());
        assertFalse(env.getTlsVerify());
        assertTrue(env.getSshEnabled());
        assertEquals(22, env.getSshPort());
        assertEquals("root", env.getSshUsername());
        assertEquals("encrypted", env.getSshPassword());
        assertEquals("SHA256:host", env.getSshHostKeyFingerprint());
        assertEquals("dev", env.getEnvType());
        assertEquals("cluster-1", env.getClusterName());
    }

    @Test
    void defaultTlsVerify_isFalse() {
        // The field defaults to false at declaration site
        DockerEnv env = new DockerEnv();
        assertFalse(env.getTlsVerify());
        assertFalse(env.getSshEnabled());
        assertEquals(22, env.getSshPort());
        assertNull(env.getEnvType());
        assertNull(env.getClusterName());
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
