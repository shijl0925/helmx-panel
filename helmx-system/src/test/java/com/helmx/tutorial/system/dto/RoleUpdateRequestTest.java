package com.helmx.tutorial.system.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoleUpdateRequestTest {

    @Test
    void settersAndGetters_workCorrectly() {
        RoleUpdateRequest req = new RoleUpdateRequest();
        req.setName("Viewer");
        req.setRemark("只读角色");
        req.setStatus(1);
        req.setCode("viewer");
        req.setPermissions(List.of(100, 200, 300));

        assertEquals("Viewer", req.getName());
        assertEquals("只读角色", req.getRemark());
        assertEquals(1, req.getStatus());
        assertEquals("viewer", req.getCode());
        assertEquals(List.of(100, 200, 300), req.getPermissions());
    }

    @Test
    void defaultConstructor_fieldsAreNull() {
        RoleUpdateRequest req = new RoleUpdateRequest();
        assertNull(req.getName());
        assertNull(req.getPermissions());
    }

    @Test
    void equals_sameFields_areEqual() {
        RoleUpdateRequest r1 = new RoleUpdateRequest();
        r1.setName("X");
        r1.setCode("x");

        RoleUpdateRequest r2 = new RoleUpdateRequest();
        r2.setName("X");
        r2.setCode("x");

        assertEquals(r1, r2);
    }
}
