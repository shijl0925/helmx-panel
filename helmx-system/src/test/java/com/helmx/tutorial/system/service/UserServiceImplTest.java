package com.helmx.tutorial.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.helmx.tutorial.system.entity.*;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // ServiceImpl stores the mapper in a protected 'baseMapper' field;
        // inject our mock there so that inherited methods (count, getById, …) work.
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    // ---- getUserRoles ----

    @Test
    void getUserRoles_nullUserId_returnsEmptySet() {
        Set<Role> result = userService.getUserRoles(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(roleMapper);
    }

    @Test
    void getUserRoles_validUserId_delegatesToRoleMapper() {
        Role r = makeRole(1L, "Admin");
        when(roleMapper.findRolesByUserId(42L)).thenReturn(new HashSet<>(Set.of(r)));

        Set<Role> result = userService.getUserRoles(42L);

        assertEquals(1, result.size());
        assertEquals("Admin", result.iterator().next().getName());
    }

    @Test
    void getUserRoles_mapperReturnsNull_returnsEmptySet() {
        when(roleMapper.findRolesByUserId(anyLong())).thenReturn(null);

        Set<Role> result = userService.getUserRoles(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ---- getUserRoleNames ----

    @Test
    void getUserRoleNames_returnsNamesFromRoles() {
        Role r1 = makeRole(1L, "Admin");
        Role r2 = makeRole(2L, "User");
        when(roleMapper.findRolesByUserId(1L)).thenReturn(new HashSet<>(Set.of(r1, r2)));

        Set<String> names = userService.getUserRoleNames(1L);

        assertEquals(Set.of("Admin", "User"), names);
    }

    // ---- getUserRoleIds ----

    @Test
    void getUserRoleIds_returnsIdsFromRoles() {
        Role r1 = makeRole(10L, "Admin");
        Role r2 = makeRole(20L, "User");
        when(roleMapper.findRolesByUserId(5L)).thenReturn(new HashSet<>(Set.of(r1, r2)));

        Set<Long> ids = userService.getUserRoleIds(5L);

        assertEquals(Set.of(10L, 20L), ids);
    }

    // ---- getUserMenus ----

    @Test
    void getUserMenus_nullUserId_returnsEmptySet() {
        Set<Menu> result = userService.getUserMenus(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(roleMapper);
    }

    @Test
    void getUserMenus_userHasNoRoles_returnsEmptySet() {
        when(roleMapper.findRolesByUserId(1L)).thenReturn(Collections.emptySet());

        Set<Menu> result = userService.getUserMenus(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getUserMenus_userHasRoles_returnsMenusFromAllRoles() {
        Role role = makeRole(1L, "Admin");
        when(roleMapper.findRolesByUserId(1L)).thenReturn(Set.of(role));

        Menu m1 = makeMenu(100L, "Users");
        Menu m2 = makeMenu(101L, "Roles");
        when(roleMapper.findMenusByRoleIds(Set.of(1L))).thenReturn(new HashSet<>(Set.of(m1, m2)));

        Set<Menu> result = userService.getUserMenus(1L);

        assertEquals(2, result.size());
        verify(roleMapper).findMenusByRoleIds(Set.of(1L));
        verify(roleMapper, never()).findMenusByRoleId(anyLong());
    }

    @Test
    void getUserMenus_roleHasNoMenus_returnsEmptySet() {
        Role role = makeRole(1L, "User");
        when(roleMapper.findRolesByUserId(1L)).thenReturn(Set.of(role));
        when(roleMapper.findMenusByRoleIds(Set.of(1L))).thenReturn(Collections.emptySet());

        Set<Menu> result = userService.getUserMenus(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getUserMenus_multipleRoles_queriesMenusOnce() {
        Role admin = makeRole(1L, "Admin");
        Role user = makeRole(2L, "User");
        Menu sharedMenu = makeMenu(100L, "Dashboard");
        when(roleMapper.findRolesByUserId(1L)).thenReturn(Set.of(admin, user));
        when(roleMapper.findMenusByRoleIds(Set.of(1L, 2L))).thenReturn(Set.of(sharedMenu));

        Set<Menu> result = userService.getUserMenus(1L);

        assertEquals(1, result.size());
        verify(roleMapper).findMenusByRoleIds(Set.of(1L, 2L));
        verify(roleMapper, never()).findMenusByRoleId(anyLong());
    }

    // ---- updateUserRoles ----

    @Test
    void updateUserRoles_nullUserId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.updateUserRoles(null, null));
    }

    @Test
    void updateUserRoles_noRoleIds_assignsDefaultUserRole() {
        Role defaultRole = makeRole(1L, ERole.User.name());
        when(roleMapper.selectOne(any(QueryWrapper.class))).thenReturn(defaultRole);
        when(userRoleMapper.delete(any(QueryWrapper.class))).thenReturn(1);
        when(userRoleMapper.insert(any(UserRole.class))).thenReturn(1);

        assertDoesNotThrow(() -> userService.updateUserRoles(10L, null));

        verify(userRoleMapper).delete(any(QueryWrapper.class));
        verify(userRoleMapper).insert(any(UserRole.class));
    }

    @Test
    void updateUserRoles_emptyRoleIds_assignsDefaultUserRole() {
        Role defaultRole = makeRole(1L, ERole.User.name());
        when(roleMapper.selectOne(any(QueryWrapper.class))).thenReturn(defaultRole);
        when(userRoleMapper.delete(any(QueryWrapper.class))).thenReturn(1);
        when(userRoleMapper.insert(any(UserRole.class))).thenReturn(1);

        assertDoesNotThrow(() -> userService.updateUserRoles(10L, Collections.emptySet()));

        verify(userRoleMapper).delete(any(QueryWrapper.class));
    }

    @Test
    void updateUserRoles_defaultRoleNotFound_throwsRuntimeException() {
        when(roleMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        assertThrows(RuntimeException.class,
                () -> userService.updateUserRoles(10L, null));
    }

    @Test
    void updateUserRoles_withRoleIds_assignsGivenRoles() {
        Role adminRole = makeRole(2L, "Admin");
        when(roleMapper.selectById(2)).thenReturn(adminRole);
        when(userRoleMapper.delete(any(QueryWrapper.class))).thenReturn(1);
        when(userRoleMapper.insert(any(UserRole.class))).thenReturn(1);

        assertDoesNotThrow(() -> userService.updateUserRoles(10L, Set.of(2)));

        verify(roleMapper).selectById(2);
        verify(userRoleMapper).insert(argThat(ur -> ur.getUserId().equals(10L) && ur.getRoleId().equals(2L)));
    }

    @Test
    void updateUserRoles_roleIdNotFound_throwsRuntimeException() {
        when(roleMapper.selectById(anyInt())).thenReturn(null);

        assertThrows(RuntimeException.class,
                () -> userService.updateUserRoles(10L, Set.of(99)));
    }

    // ---- getUserRoleNamesBatch ----

    @Test
    void getUserRoleNamesBatch_nullInput_returnsEmptyMap() {
        Map<Long, Set<String>> result = userService.getUserRoleNamesBatch(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(userRoleMapper, roleMapper);
    }

    @Test
    void getUserRoleNamesBatch_emptyInput_returnsEmptyMap() {
        Map<Long, Set<String>> result = userService.getUserRoleNamesBatch(Collections.emptyList());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getUserRoleNamesBatch_noUserRolesFound_returnsEmptySetPerUser() {
        when(userRoleMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

        Map<Long, Set<String>> result = userService.getUserRoleNamesBatch(List.of(1L, 2L));

        assertEquals(2, result.size());
        assertTrue(result.get(1L).isEmpty());
        assertTrue(result.get(2L).isEmpty());
    }

    @Test
    void getUserRoleNamesBatch_withUserRoles_returnsCorrectMapping() {
        UserRole ur1 = new UserRole(1L, 10L);
        UserRole ur2 = new UserRole(2L, 20L);
        when(userRoleMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(ur1, ur2));

        Role r1 = makeRole(10L, "Admin");
        Role r2 = makeRole(20L, "User");
        when(roleMapper.selectBatchIds(anyCollection())).thenReturn(List.of(r1, r2));

        Map<Long, Set<String>> result = userService.getUserRoleNamesBatch(List.of(1L, 2L));

        assertEquals(Set.of("Admin"), result.get(1L));
        assertEquals(Set.of("User"), result.get(2L));
    }

    // ---- getUserRoleIdsBatch ----

    @Test
    void getUserRoleIdsBatch_nullInput_returnsEmptyMap() {
        Map<Long, Set<Long>> result = userService.getUserRoleIdsBatch(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(userRoleMapper);
    }

    @Test
    void getUserRoleIdsBatch_emptyInput_returnsEmptyMap() {
        Map<Long, Set<Long>> result = userService.getUserRoleIdsBatch(Collections.emptyList());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getUserRoleIdsBatch_withUserRoles_returnsCorrectMapping() {
        UserRole ur1 = new UserRole(1L, 10L);
        UserRole ur2 = new UserRole(1L, 20L);
        UserRole ur3 = new UserRole(2L, 30L);
        when(userRoleMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(ur1, ur2, ur3));

        Map<Long, Set<Long>> result = userService.getUserRoleIdsBatch(List.of(1L, 2L));

        assertEquals(Set.of(10L, 20L), result.get(1L));
        assertEquals(Set.of(30L), result.get(2L));
    }

    @Test
    void getUserRoleIdsBatch_noRolesForUser_returnsEmptySetForThatUser() {
        when(userRoleMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

        Map<Long, Set<Long>> result = userService.getUserRoleIdsBatch(List.of(5L));

        assertEquals(1, result.size());
        assertTrue(result.get(5L).isEmpty());
    }

    // ---- helpers ----

    private Role makeRole(Long id, String name) {
        Role r = new Role();
        r.setId(id);
        r.setName(name);
        return r;
    }

    private Menu makeMenu(Long id, String name) {
        Menu m = new Menu();
        m.setId(id);
        m.setName(name);
        return m;
    }
}
