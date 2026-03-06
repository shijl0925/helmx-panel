package com.helmx.tutorial.system.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoleCreateRequestTest {

    @Test
    void settersAndGetters_workCorrectly() {
        RoleCreateRequest req = new RoleCreateRequest();
        req.setName("Editor");
        req.setRemark("内容编辑");
        req.setStatus(1);
        req.setCode("editor");
        req.setPermissions(List.of(1, 2, 3));
        req.setResources(List.of(10, 20));

        assertEquals("Editor", req.getName());
        assertEquals("内容编辑", req.getRemark());
        assertEquals(1, req.getStatus());
        assertEquals("editor", req.getCode());
        assertEquals(List.of(1, 2, 3), req.getPermissions());
        assertEquals(List.of(10, 20), req.getResources());
    }

    @Test
    void defaultConstructor_fieldsAreNull() {
        RoleCreateRequest req = new RoleCreateRequest();
        assertNull(req.getName());
        assertNull(req.getPermissions());
        assertNull(req.getResources());
    }
}
