package com.helmx.tutorial.docker.dto;

import com.helmx.tutorial.docker.entity.DockerEnv;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DockerEnvDTOTest {

    @Test
    void constructor_mapsAllFields() {
        DockerEnv env = new DockerEnv();
        env.setId(1L);
        env.setName("local");
        env.setRemark("Local Docker");
        env.setHost("unix:///var/run/docker.sock");
        env.setStatus(1);
        env.setTlsVerify(false);
        env.setSshEnabled(true);
        env.setSshPort(22);
        env.setSshUsername("root");
        env.setSshPassword("encrypted");
        env.setSshHostKeyFingerprint("SHA256:host");
        env.setEnvType("prod");
        env.setClusterName("cluster-a");

        DockerEnvDTO dto = new DockerEnvDTO(env);

        assertEquals(1L,     dto.getId());
        assertEquals("local", dto.getName());
        assertEquals("Local Docker", dto.getRemark());
        assertEquals("unix:///var/run/docker.sock", dto.getHost());
        assertEquals(1,      dto.getStatus());
        assertFalse(dto.getTlsVerify());
        assertTrue(dto.getSshEnabled());
        assertEquals(22, dto.getSshPort());
        assertEquals("root", dto.getSshUsername());
        assertEquals("SHA256:host", dto.getSshHostKeyFingerprint());
        assertTrue(dto.getSshPasswordConfigured());
        assertEquals("prod", dto.getEnvType());
        assertEquals("cluster-a", dto.getClusterName());
    }

    @Test
    void constructor_tlsVerifyTrue_mappedCorrectly() {
        DockerEnv env = new DockerEnv();
        env.setId(2L);
        env.setName("remote");
        env.setTlsVerify(true);
        env.setHost("tcp://192.168.1.100:2376");
        env.setSshEnabled(true);

        DockerEnvDTO dto = new DockerEnvDTO(env);

        assertTrue(dto.getTlsVerify());
        assertEquals("remote", dto.getName());
        assertTrue(dto.getSshEnabled());
    }

    @Test
    void constructor_nullFields_mappedAsNull() {
        DockerEnv env = new DockerEnv();
        env.setId(3L);
        env.setName("minimal");
        env.setHost("unix:///var/run/docker.sock");
        // remark, status and tlsVerify intentionally left null/default

        DockerEnvDTO dto = new DockerEnvDTO(env);

        assertNull(dto.getRemark());
        assertNull(dto.getStatus());
    }

    @Test
    void constructor_defaultTlsVerify_isFalse() {
        // DockerEnv sets tlsVerify = false by default
        DockerEnv env = new DockerEnv();
        env.setId(4L);
        env.setName("defaults");
        env.setHost("unix:///var/run/docker.sock");

        DockerEnvDTO dto = new DockerEnvDTO(env);

        assertFalse(dto.getTlsVerify());
        assertFalse(dto.getSshEnabled());
        assertFalse(dto.getSshPasswordConfigured());
        assertNull(dto.getEnvType());
        assertNull(dto.getClusterName());
    }

    @Test
    void constructor_envTypeAndClusterName_mappedCorrectly() {
        DockerEnv env = new DockerEnv();
        env.setId(5L);
        env.setName("dev-host-1");
        env.setHost("tcp://dev1:2376");
        env.setEnvType("dev");
        env.setClusterName("cluster-dev");

        DockerEnvDTO dto = new DockerEnvDTO(env);

        assertEquals("dev", dto.getEnvType());
        assertEquals("cluster-dev", dto.getClusterName());
    }
}
