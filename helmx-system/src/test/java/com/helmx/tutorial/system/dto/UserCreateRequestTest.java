package com.helmx.tutorial.system.dto;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserCreateRequestTest {

    @Test
    void settersAndGetters_workCorrectly() {
        UserCreateRequest req = new UserCreateRequest();
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("secret123");
        req.setNickName("Alice");
        req.setPhone("13800000001");
        req.setStatus(1);
        req.setRole(Set.of(1, 2));

        assertEquals("alice", req.getUsername());
        assertEquals("alice@example.com", req.getEmail());
        assertEquals("secret123", req.getPassword());
        assertEquals("Alice", req.getNickName());
        assertEquals("13800000001", req.getPhone());
        assertEquals(1, req.getStatus());
        assertEquals(Set.of(1, 2), req.getRole());
    }

    @Test
    void defaultConstructor_fieldsAreNull() {
        UserCreateRequest req = new UserCreateRequest();
        assertNull(req.getUsername());
        assertNull(req.getEmail());
        assertNull(req.getPassword());
        assertNull(req.getRole());
    }
}
