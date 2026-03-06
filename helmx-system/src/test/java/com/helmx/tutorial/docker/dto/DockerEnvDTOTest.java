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

        DockerEnvDTO dto = new DockerEnvDTO(env);

        assertEquals(1L,     dto.getId());
        assertEquals("local", dto.getName());
        assertEquals("Local Docker", dto.getRemark());
        assertEquals("unix:///var/run/docker.sock", dto.getHost());
        assertEquals(1,      dto.getStatus());
        assertFalse(dto.getTlsVerify());
    }

    @Test
    void constructor_tlsVerifyTrue_mappedCorrectly() {
        DockerEnv env = new DockerEnv();
        env.setId(2L);
        env.setName("remote");
        env.setTlsVerify(true);
        env.setHost("tcp://192.168.1.100:2376");

        DockerEnvDTO dto = new DockerEnvDTO(env);

        assertTrue(dto.getTlsVerify());
        assertEquals("remote", dto.getName());
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
    }
}
