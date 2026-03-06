package com.helmx.tutorial.system.dto;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class RoleQueryRequestTest {

    @Test
    void settersAndGetters_workCorrectly() {
        RoleQueryRequest req = new RoleQueryRequest();
        Date now = new Date();
        req.setName("Admin");
        req.setCode("admin");
        req.setId(1);
        req.setRemark("管理员");
        req.setStatus(1);
        req.setStartTime(now);
        req.setEndTime(now);

        assertEquals("Admin", req.getName());
        assertEquals("admin", req.getCode());
        assertEquals(1, req.getId());
        assertEquals("管理员", req.getRemark());
        assertEquals(1, req.getStatus());
        assertEquals(now, req.getStartTime());
        assertEquals(now, req.getEndTime());
    }

    @Test
    void defaultConstructor_fieldsAreNull() {
        RoleQueryRequest req = new RoleQueryRequest();
        assertNull(req.getName());
        assertNull(req.getCode());
        assertNull(req.getId());
        assertNull(req.getRemark());
        assertNull(req.getStatus());
        assertNull(req.getStartTime());
        assertNull(req.getEndTime());
    }

    @Test
    void equals_sameFields_areEqual() {
        RoleQueryRequest r1 = new RoleQueryRequest();
        r1.setName("Admin");
        r1.setCode("admin");

        RoleQueryRequest r2 = new RoleQueryRequest();
        r2.setName("Admin");
        r2.setCode("admin");

        assertEquals(r1, r2);
    }
}
