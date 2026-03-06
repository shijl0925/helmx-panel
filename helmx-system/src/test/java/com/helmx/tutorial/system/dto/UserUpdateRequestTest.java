package com.helmx.tutorial.system.dto;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserUpdateRequestTest {

    @Test
    void settersAndGetters_workCorrectly() {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setNickName("Bob");
        req.setPhone("13900000002");
        req.setEmail("bob@example.com");
        req.setStatus(0);
        req.setRole(Set.of(2, 3));

        assertEquals("Bob", req.getNickName());
        assertEquals("13900000002", req.getPhone());
        assertEquals("bob@example.com", req.getEmail());
        assertEquals(0, req.getStatus());
        assertEquals(Set.of(2, 3), req.getRole());
    }

    @Test
    void defaultConstructor_fieldsAreNull() {
        UserUpdateRequest req = new UserUpdateRequest();
        assertNull(req.getNickName());
        assertNull(req.getPhone());
        assertNull(req.getEmail());
        assertNull(req.getStatus());
        assertNull(req.getRole());
    }
}
