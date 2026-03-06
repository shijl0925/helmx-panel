package com.helmx.tutorial.system.dto;

import com.helmx.tutorial.system.entity.User;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class UserDTOTest {

    @Test
    void constructor_mapsBasicFields() {
        User user = makeUser(1L, "alice", "alice@example.com");

        UserDTO dto = new UserDTO(user);

        assertEquals(1L, dto.getId());
        assertEquals("alice", dto.getUsername());
        assertEquals("alice@example.com", dto.getEmail());
        assertEquals("alice-nickname", dto.getNickname());
        assertEquals("13800000000", dto.getPhone());
        assertEquals(1, dto.getStatus());
    }

    @Test
    void constructor_nullTimestamps_leavesDateFieldsNull() {
        User user = makeUser(2L, "bob", "bob@example.com");
        user.setCreatedAt(null);
        user.setUpdatedAt(null);

        UserDTO dto = new UserDTO(user);

        assertNull(dto.getCreatedAt());
        assertNull(dto.getUpdatedAt());
    }

    @Test
    void constructor_withTimestamps_convertsToLocalDateTime() {
        User user = makeUser(3L, "carol", "carol@example.com");
        Timestamp now = Timestamp.from(Instant.now());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        UserDTO dto = new UserDTO(user);

        assertNotNull(dto.getCreatedAt());
        assertNotNull(dto.getUpdatedAt());
        assertEquals(now.toLocalDateTime(), dto.getCreatedAt());
        assertEquals(now.toLocalDateTime(), dto.getUpdatedAt());
    }

    @Test
    void rolesAndRoleFields_defaultToNull() {
        User user = makeUser(4L, "dave", "dave@example.com");
        UserDTO dto = new UserDTO(user);

        assertNull(dto.getRoles());
        assertNull(dto.getRole());
        assertNull(dto.getOnline());
    }

    @Test
    void setRoles_updatesField() {
        User user = makeUser(5L, "eve", "eve@example.com");
        UserDTO dto = new UserDTO(user);
        dto.setRoles(java.util.Set.of("Admin", "User"));

        assertEquals(2, dto.getRoles().size());
    }

    // ---- helpers ----

    private User makeUser(Long id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setNickname(username + "-nickname");
        user.setPhone("13800000000");
        user.setStatus(1);
        return user;
    }
}
