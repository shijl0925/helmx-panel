package com.helmx.tutorial.docker.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistryEntityTest {

    @Test
    void settersAndGetters_workCorrectly() {
        Registry registry = new Registry();
        registry.setId(1L);
        registry.setName("DockerHub");
        registry.setUrl("https://hub.docker.com");
        registry.setUsername("user");
        registry.setPassword("pass");
        registry.setAuth(true);

        assertEquals(1L, registry.getId());
        assertEquals("DockerHub", registry.getName());
        assertEquals("https://hub.docker.com", registry.getUrl());
        assertEquals("user", registry.getUsername());
        assertEquals("pass", registry.getPassword());
        assertTrue(registry.getAuth());
    }

    @Test
    void authField_canBeNull() {
        Registry registry = new Registry();
        assertNull(registry.getAuth());
    }

    @Test
    void passwordField_canBeNulled() {
        Registry registry = new Registry();
        registry.setPassword("secret");
        registry.setPassword(null);
        assertNull(registry.getPassword());
    }

    @Test
    void usernameField_canBeNull() {
        Registry registry = new Registry();
        assertNull(registry.getUsername());
    }
}
