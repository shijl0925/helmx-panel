package com.helmx.tutorial.system.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserRoleTest {

    @Test
    void noArgConstructor_fieldsAreNull() {
        UserRole ur = new UserRole();
        assertNull(ur.getUserId());
        assertNull(ur.getRoleId());
    }

    @Test
    void allArgConstructor_setsFields() {
        UserRole ur = new UserRole(10L, 20L);
        assertEquals(10L, ur.getUserId());
        assertEquals(20L, ur.getRoleId());
    }

    @Test
    void setters_updateFields() {
        UserRole ur = new UserRole();
        ur.setUserId(5L);
        ur.setRoleId(7L);
        assertEquals(5L, ur.getUserId());
        assertEquals(7L, ur.getRoleId());
    }

    @Test
    void equals_sameFields_areEqual() {
        UserRole ur1 = new UserRole(1L, 2L);
        UserRole ur2 = new UserRole(1L, 2L);
        assertEquals(ur1, ur2);
    }

    @Test
    void equals_differentFields_notEqual() {
        assertNotEquals(new UserRole(1L, 2L), new UserRole(1L, 3L));
    }

    @Test
    void hashCode_sameFields_sameHashCode() {
        assertEquals(new UserRole(1L, 2L).hashCode(), new UserRole(1L, 2L).hashCode());
    }
}
