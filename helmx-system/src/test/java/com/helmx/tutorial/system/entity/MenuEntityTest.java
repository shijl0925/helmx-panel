package com.helmx.tutorial.system.entity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MenuEntityTest {

    // ---- getters / setters ----

    @Test
    void settersAndGetters_workCorrectly() {
        Menu menu = new Menu();
        menu.setId(1L);
        menu.setName("Dashboard");
        menu.setParentId(0L);
        menu.setType("menu");
        menu.setAuthCode("Dashboard:View");
        menu.setPath("/dashboard");
        menu.setComponent("views/dashboard/index");
        menu.setStatus(1);
        menu.setActivePath("/dashboard");
        menu.setIcon("dashboard");
        menu.setSort(1);
        menu.setTitle("仪表板");

        assertEquals(1L, menu.getId());
        assertEquals("Dashboard", menu.getName());
        assertEquals(0L, menu.getParentId());
        assertEquals("menu", menu.getType());
        assertEquals("Dashboard:View", menu.getAuthCode());
        assertEquals("/dashboard", menu.getPath());
        assertEquals("views/dashboard/index", menu.getComponent());
        assertEquals(1, menu.getStatus());
        assertEquals("/dashboard", menu.getActivePath());
        assertEquals("dashboard", menu.getIcon());
        assertEquals(1, menu.getSort());
        assertEquals("仪表板", menu.getTitle());
    }

    @Test
    void childrenField_canBeSetAndRead() {
        Menu parent = new Menu();
        parent.setId(1L);
        Menu child = new Menu();
        child.setId(2L);
        parent.setChildren(new ArrayList<>(List.of(child)));

        assertEquals(1, parent.getChildren().size());
        assertEquals(2L, parent.getChildren().get(0).getId());
    }

    @Test
    void metaField_canBeSetAndRead() {
        Menu menu = new Menu();
        Map<String, Object> meta = Map.of("title", "Test", "icon", "icon-test", "sort", 1);
        menu.setMeta(meta);

        assertEquals("Test", menu.getMeta().get("title"));
    }

    // ---- equals / hashCode ----

    @Test
    void equals_sameId_areEqual() {
        assertEquals(makeMenu(1L), makeMenu(1L));
    }

    @Test
    void equals_differentId_notEqual() {
        assertNotEquals(makeMenu(1L), makeMenu(2L));
    }

    @Test
    void equals_null_returnsFalse() {
        assertNotEquals(null, makeMenu(1L));
    }

    @Test
    void equals_sameInstance_returnsTrue() {
        Menu m = makeMenu(1L);
        assertEquals(m, m);
    }

    @Test
    void equals_differentType_returnsFalse() {
        assertNotEquals("menu", makeMenu(1L));
    }

    @Test
    void hashCode_equalMenus_sameHashCode() {
        assertEquals(makeMenu(1L).hashCode(), makeMenu(1L).hashCode());
    }

    @Test
    void hashCode_differentMenus_likelyDifferent() {
        assertNotEquals(makeMenu(1L).hashCode(), makeMenu(2L).hashCode());
    }

    // ---- helpers ----

    private Menu makeMenu(Long id) {
        Menu m = new Menu();
        m.setId(id);
        m.setName("Menu-" + id);
        return m;
    }
}
