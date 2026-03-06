package com.helmx.tutorial.system.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleMenuTest {

    @Test
    void noArgConstructor_fieldsAreNull() {
        RoleMenu rm = new RoleMenu();
        assertNull(rm.getRoleId());
        assertNull(rm.getMenuId());
    }

    @Test
    void allArgConstructor_setsFields() {
        RoleMenu rm = new RoleMenu(1L, 100L);
        assertEquals(1L, rm.getRoleId());
        assertEquals(100L, rm.getMenuId());
    }

    @Test
    void setters_updateFields() {
        RoleMenu rm = new RoleMenu();
        rm.setRoleId(2L);
        rm.setMenuId(200L);
        assertEquals(2L, rm.getRoleId());
        assertEquals(200L, rm.getMenuId());
    }

    @Test
    void equals_sameFields_areEqual() {
        assertEquals(new RoleMenu(1L, 10L), new RoleMenu(1L, 10L));
    }

    @Test
    void equals_differentFields_notEqual() {
        assertNotEquals(new RoleMenu(1L, 10L), new RoleMenu(1L, 20L));
    }

    @Test
    void hashCode_sameFields_sameHashCode() {
        assertEquals(new RoleMenu(1L, 10L).hashCode(), new RoleMenu(1L, 10L).hashCode());
    }
}
