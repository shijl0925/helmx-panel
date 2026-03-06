package com.helmx.tutorial.system.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MenuMetaRequestTest {

    @Test
    void settersAndGetters_workCorrectly() {
        MenuMetaRequest req = new MenuMetaRequest();
        req.setTitle("系统管理");
        req.setIcon("setting");
        req.setSort(10);

        assertEquals("系统管理", req.getTitle());
        assertEquals("setting", req.getIcon());
        assertEquals(10, req.getSort());
    }

    @Test
    void defaultConstructor_fieldsAreNull() {
        MenuMetaRequest req = new MenuMetaRequest();
        assertNull(req.getTitle());
        assertNull(req.getIcon());
        assertNull(req.getSort());
    }

    @Test
    void equals_sameFields_areEqual() {
        MenuMetaRequest r1 = new MenuMetaRequest();
        r1.setTitle("Title");
        r1.setIcon("icon");
        r1.setSort(5);

        MenuMetaRequest r2 = new MenuMetaRequest();
        r2.setTitle("Title");
        r2.setIcon("icon");
        r2.setSort(5);

        assertEquals(r1, r2);
    }

    @Test
    void equals_differentTitle_notEqual() {
        MenuMetaRequest r1 = new MenuMetaRequest();
        r1.setTitle("A");

        MenuMetaRequest r2 = new MenuMetaRequest();
        r2.setTitle("B");

        assertNotEquals(r1, r2);
    }
}
