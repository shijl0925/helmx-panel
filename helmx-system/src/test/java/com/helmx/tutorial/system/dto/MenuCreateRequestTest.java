package com.helmx.tutorial.system.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MenuCreateRequestTest {

    @Test
    void settersAndGetters_workCorrectly() {
        MenuCreateRequest req = new MenuCreateRequest();
        MenuMetaRequest meta = new MenuMetaRequest();
        meta.setTitle("仪表板");
        meta.setIcon("dashboard");
        meta.setSort(1);

        req.setName("Dashboard");
        req.setPid(0L);
        req.setType("menu");
        req.setAuthCode("Dashboard:View");
        req.setPath("/dashboard");
        req.setComponent("views/dashboard/index");
        req.setStatus(1);
        req.setActivePath("/dashboard");
        req.setMeta(meta);

        assertEquals("Dashboard", req.getName());
        assertEquals(0L, req.getPid());
        assertEquals("menu", req.getType());
        assertEquals("Dashboard:View", req.getAuthCode());
        assertEquals("/dashboard", req.getPath());
        assertEquals("views/dashboard/index", req.getComponent());
        assertEquals(1, req.getStatus());
        assertEquals("/dashboard", req.getActivePath());
        assertNotNull(req.getMeta());
        assertEquals("仪表板", req.getMeta().getTitle());
        assertEquals("dashboard", req.getMeta().getIcon());
        assertEquals(1, req.getMeta().getSort());
    }

    @Test
    void defaultConstructor_fieldsAreNull() {
        MenuCreateRequest req = new MenuCreateRequest();
        assertNull(req.getName());
        assertNull(req.getPid());
        assertNull(req.getMeta());
    }
}
