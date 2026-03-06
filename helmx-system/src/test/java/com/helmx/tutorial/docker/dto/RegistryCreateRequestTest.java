package com.helmx.tutorial.docker.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistryCreateRequestTest {

    @Test
    void settersAndGetters_workCorrectly() {
        RegistryCreateRequest req = new RegistryCreateRequest();
        req.setName("DockerHub");
        req.setUrl("https://hub.docker.com");
        req.setUsername("myuser");
        req.setPassword("mypass");
        req.setAuth(true);

        assertEquals("DockerHub", req.getName());
        assertEquals("https://hub.docker.com", req.getUrl());
        assertEquals("myuser", req.getUsername());
        assertEquals("mypass", req.getPassword());
        assertTrue(req.getAuth());
    }

    @Test
    void defaultConstructor_authIsNull() {
        RegistryCreateRequest req = new RegistryCreateRequest();
        assertNull(req.getAuth());
    }

    @Test
    void noAuth_usernameAndPasswordCanBeNull() {
        RegistryCreateRequest req = new RegistryCreateRequest();
        req.setName("Public");
        req.setUrl("https://registry.example.com");
        req.setAuth(false);

        assertNull(req.getUsername());
        assertNull(req.getPassword());
        assertFalse(req.getAuth());
    }
}
