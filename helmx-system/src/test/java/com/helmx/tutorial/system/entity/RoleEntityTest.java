package com.helmx.tutorial.system.entity;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoleEntityTest {

    // ---- getters / setters ----

    @Test
    void settersAndGetters_workCorrectly() {
        Role role = new Role();
        role.setId(1L);
        role.setName("Admin");
        role.setRemark("管理员");
        role.setStatus(1);
        role.setCode("admin");

        assertEquals(1L, role.getId());
        assertEquals("Admin", role.getName());
        assertEquals("管理员", role.getRemark());
        assertEquals(1, role.getStatus());
        assertEquals("admin", role.getCode());
    }

    @Test
    void menusAndPermissionsFields_canBeSetAndRead() {
        Role role = new Role();
        Menu m = new Menu();
        m.setId(10L);
        role.setMenus(Set.of(m));
        role.setPermissions(Set.of(10L, 20L));

        assertEquals(1, role.getMenus().size());
        assertEquals(2, role.getPermissions().size());
    }

    // ---- equals / hashCode ----

    @Test
    void equals_sameId_areEqual() {
        Role r1 = makeRole(1L);
        Role r2 = makeRole(1L);
        assertEquals(r1, r2);
    }

    @Test
    void equals_differentId_notEqual() {
        Role r1 = makeRole(1L);
        Role r2 = makeRole(2L);
        assertNotEquals(r1, r2);
    }

    @Test
    void equals_null_returnsFalse() {
        assertNotEquals(null, makeRole(1L));
    }

    @Test
    void equals_sameInstance_returnsTrue() {
        Role r = makeRole(1L);
        assertEquals(r, r);
    }

    @Test
    void equals_differentType_returnsFalse() {
        assertNotEquals("Admin", makeRole(1L));
    }

    @Test
    void hashCode_equalRoles_sameHashCode() {
        assertEquals(makeRole(1L).hashCode(), makeRole(1L).hashCode());
    }

    // ---- helpers ----

    private Role makeRole(Long id) {
        Role r = new Role();
        r.setId(id);
        r.setName("Role-" + id);
        return r;
    }
}
