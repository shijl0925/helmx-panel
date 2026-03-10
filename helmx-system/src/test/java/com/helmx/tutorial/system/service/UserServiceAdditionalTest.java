package com.helmx.tutorial.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.helmx.tutorial.security.dto.SignupRequest;
import com.helmx.tutorial.system.entity.ERole;
import com.helmx.tutorial.system.entity.Role;
import com.helmx.tutorial.system.entity.User;
import com.helmx.tutorial.system.entity.UserRole;
import com.helmx.tutorial.system.mapper.RoleMapper;
import com.helmx.tutorial.system.mapper.UserMapper;
import com.helmx.tutorial.system.mapper.UserRoleMapper;
import com.helmx.tutorial.system.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Additional UserServiceImpl tests covering existsByUsername, existsByEmail,
 * existsById, isSuperAdmin, and registerUser.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceAdditionalTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    // ---- existsByUsername ----

    @Test
    void existsByUsername_nullUsername_returnsFalse() {
        assertFalse(userService.existsByUsername(null));
        verify(userMapper, never()).selectCount(any());
    }

    @Test
    void existsByUsername_emptyUsername_returnsFalse() {
        // empty string is non-null – count query will run and return 0
        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        assertFalse(userService.existsByUsername(""));
    }

    @Test
    void existsByUsername_existing_returnsTrue() {
        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        assertTrue(userService.existsByUsername("alice"));
    }

    @Test
    void existsByUsername_nonExisting_returnsFalse() {
        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        assertFalse(userService.existsByUsername("ghost"));
    }

    // ---- existsByEmail ----

    @Test
    void existsByEmail_nullEmail_returnsFalse() {
        assertFalse(userService.existsByEmail(null));
        verify(userMapper, never()).selectCount(any());
    }

    @Test
    void existsByEmail_existing_returnsTrue() {
        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        assertTrue(userService.existsByEmail("alice@example.com"));
    }

    @Test
    void existsByEmail_nonExisting_returnsFalse() {
        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        assertFalse(userService.existsByEmail("unknown@example.com"));
    }

    // ---- existsById ----

    @Test
    void existsById_nullId_returnsFalse() {
        assertFalse(userService.existsById(null));
        verify(userMapper, never()).selectCount(any());
    }

    @Test
    void existsById_existing_returnsTrue() {
        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        assertTrue(userService.existsById(1L));
    }

    @Test
    void existsById_nonExisting_returnsFalse() {
        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        assertFalse(userService.existsById(9999L));
    }

    // ---- isSuperAdmin ----

    @Test
    void isSuperAdmin_delegatesToMapper_returnsTrue() {
        when(userMapper.isSuperAdmin(1L)).thenReturn(true);
        assertTrue(userService.isSuperAdmin(1L));
        verify(userMapper).isSuperAdmin(1L);
    }

    @Test
    void isSuperAdmin_delegatesToMapper_returnsFalse() {
        when(userMapper.isSuperAdmin(2L)).thenReturn(false);
        assertFalse(userService.isSuperAdmin(2L));
    }

    // ---- registerUser ----

    @Test
    void registerUser_validRequest_insertsUserAndAssignsDefaultRole() {
        SignupRequest req = new SignupRequest();
        req.setUsername("newUser");
        req.setEmail("new@example.com");
        req.setPassword("password123");
        req.setPhone("13800000001");
        req.setNickname("New User");

        when(encoder.encode("password123")).thenReturn("hashed-password");

        // The insert sets the user's id via MyBatis-Plus – simulate this
        doAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(100L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        // Default role found
        Role defaultRole = makeRole(1L, ERole.User.name());
        when(roleMapper.selectOne(any(QueryWrapper.class))).thenReturn(defaultRole);
        when(userRoleMapper.delete(any(QueryWrapper.class))).thenReturn(0);
        when(userRoleMapper.insert(any(UserRole.class))).thenReturn(1);

        assertDoesNotThrow(() -> userService.registerUser(req));

        verify(userMapper).insert(any(User.class));
        verify(userRoleMapper).insert(argThat(ur -> ur.getUserId().equals(100L) && ur.getRoleId().equals(1L)));
    }

    @Test
    void registerUser_defaultRoleNotFound_throwsRuntimeException() {
        SignupRequest req = new SignupRequest();
        req.setUsername("bob");
        req.setEmail("bob@example.com");
        req.setPassword("pass123");

        when(encoder.encode("pass123")).thenReturn("hashed");
        doAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(200L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        when(roleMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        assertThrows(RuntimeException.class, () -> userService.registerUser(req));
    }

    @Test
    void registerUser_isTransactional() throws NoSuchMethodException {
        assertTrue(UserServiceImpl.class
                .getMethod("registerUser", SignupRequest.class)
                .isAnnotationPresent(Transactional.class));
    }

    @Test
    void updateUserRoles_isTransactional() throws NoSuchMethodException {
        assertTrue(UserServiceImpl.class
                .getMethod("updateUserRoles", Long.class, Set.class)
                .isAnnotationPresent(Transactional.class));
    }

    // helper
    private Role makeRole(Long id, String name) {
        Role r = new Role();
        r.setId(id);
        r.setName(name);
        return r;
    }
}
