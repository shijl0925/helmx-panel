package com.helmx.tutorial.system.entity;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserEntityTest {

    // ---- getters / setters ----

    @Test
    void settersAndGetters_workCorrectly() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setPassword("secret");
        user.setNickname("Alice");
        user.setPhone("13800000001");
        user.setEmail("alice@example.com");
        user.setStatus(1);
        user.setSuperAdmin(true);

        assertEquals(1L, user.getId());
        assertEquals("alice", user.getUsername());
        assertEquals("secret", user.getPassword());
        assertEquals("Alice", user.getNickname());
        assertEquals("13800000001", user.getPhone());
        assertEquals("alice@example.com", user.getEmail());
        assertEquals(1, user.getStatus());
        assertTrue(user.isSuperAdmin());
    }

    @Test
    void rolesField_canBeSetAndRead() {
        User user = new User();
        Role r = new Role();
        r.setId(1L);
        r.setName("Admin");
        user.setRoles(Set.of(r));
        assertEquals(1, user.getRoles().size());
        assertEquals("Admin", user.getRoles().iterator().next().getName());
    }

    // ---- equals / hashCode ----

    @Test
    void equals_sameIdAndUsername_areEqual() {
        User u1 = makeUser(1L, "alice");
        User u2 = makeUser(1L, "alice");
        assertEquals(u1, u2);
    }

    @Test
    void equals_differentId_notEqual() {
        User u1 = makeUser(1L, "alice");
        User u2 = makeUser(2L, "alice");
        assertNotEquals(u1, u2);
    }

    @Test
    void equals_differentUsername_notEqual() {
        User u1 = makeUser(1L, "alice");
        User u2 = makeUser(1L, "bob");
        assertNotEquals(u1, u2);
    }

    @Test
    void equals_null_returnsFalse() {
        User u = makeUser(1L, "alice");
        assertNotEquals(null, u);
    }

    @Test
    void equals_sameInstance_returnsTrue() {
        User u = makeUser(1L, "alice");
        assertEquals(u, u);
    }

    @Test
    void equals_differentType_returnsFalse() {
        User u = makeUser(1L, "alice");
        assertNotEquals("alice", u);
    }

    @Test
    void hashCode_equalObjects_sameHashCode() {
        User u1 = makeUser(1L, "alice");
        User u2 = makeUser(1L, "alice");
        assertEquals(u1.hashCode(), u2.hashCode());
    }

    @Test
    void hashCode_differentObjects_likelyDifferent() {
        User u1 = makeUser(1L, "alice");
        User u2 = makeUser(2L, "bob");
        assertNotEquals(u1.hashCode(), u2.hashCode());
    }

    // ---- helpers ----

    private User makeUser(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }
}
