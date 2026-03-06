package com.helmx.tutorial.system.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ERoleTest {

    @Test
    void enumValues_existWithExpectedNames() {
        ERole[] values = ERole.values();
        assertEquals(3, values.length);
    }

    @Test
    void user_value_exists() {
        ERole role = ERole.User;
        assertEquals("User", role.name());
    }

    @Test
    void admin_value_exists() {
        ERole role = ERole.Admin;
        assertEquals("Admin", role.name());
    }

    @Test
    void super_value_exists() {
        ERole role = ERole.Super;
        assertEquals("Super", role.name());
    }

    @Test
    void valueOf_returnsCorrectEnum() {
        assertEquals(ERole.User, ERole.valueOf("User"));
        assertEquals(ERole.Admin, ERole.valueOf("Admin"));
        assertEquals(ERole.Super, ERole.valueOf("Super"));
    }

    @Test
    void ordinal_isStable() {
        assertEquals(0, ERole.User.ordinal());
        assertEquals(1, ERole.Admin.ordinal());
        assertEquals(2, ERole.Super.ordinal());
    }
}
