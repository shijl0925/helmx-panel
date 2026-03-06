package com.helmx.tutorial.system.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MenuUpdateRequestTest {

    @Test
    void settersAndGetters_workCorrectly() {
        MenuUpdateRequest req = new MenuUpdateRequest();
        MenuMetaRequest meta = new MenuMetaRequest();
        meta.setTitle("新标题");
        meta.setIcon("new-icon");
        meta.setSort(2);

        req.setName("NewName");
        req.setPid(1L);
        req.setType("button");
        req.setAuthCode("System:User:Edit");
        req.setPath("/system/users/edit");
        req.setComponent("views/system/user/edit");
        req.setStatus(0);
        req.setActivePath("/system/users");
        req.setMeta(meta);

        assertEquals("NewName", req.getName());
        assertEquals(1L, req.getPid());
        assertEquals("button", req.getType());
        assertEquals("System:User:Edit", req.getAuthCode());
        assertEquals("/system/users/edit", req.getPath());
        assertEquals("views/system/user/edit", req.getComponent());
        assertEquals(0, req.getStatus());
        assertEquals("/system/users", req.getActivePath());
        assertNotNull(req.getMeta());
        assertEquals("新标题", req.getMeta().getTitle());
    }

    @Test
    void defaultConstructor_fieldsAreNull() {
        MenuUpdateRequest req = new MenuUpdateRequest();
        assertNull(req.getName());
        assertNull(req.getPid());
        assertNull(req.getMeta());
    }
}
