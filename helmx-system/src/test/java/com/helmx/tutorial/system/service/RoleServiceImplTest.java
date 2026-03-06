package com.helmx.tutorial.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.helmx.tutorial.system.entity.Role;
import com.helmx.tutorial.system.mapper.RoleMapper;
import com.helmx.tutorial.system.service.impl.RoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private RoleServiceImpl roleService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(roleService, "baseMapper", roleMapper);
    }

    // ---- existsById ----

    @Test
    void existsById_nullId_returnsFalse() {
        boolean result = roleService.existsById(null);
        assertFalse(result);
        verify(roleMapper, never()).selectCount(any());
    }

    @Test
    void existsById_idExists_returnsTrue() {
        when(roleMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        boolean result = roleService.existsById(1L);

        assertTrue(result);
        verify(roleMapper).selectCount(any(QueryWrapper.class));
    }

    @Test
    void existsById_idDoesNotExist_returnsFalse() {
        when(roleMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);

        boolean result = roleService.existsById(999L);

        assertFalse(result);
    }

    @Test
    void existsById_zeroId_returnsFalse() {
        when(roleMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);

        boolean result = roleService.existsById(0L);

        // zero is a valid non-null id; the DB just won't find it
        assertFalse(result);
    }

    @Test
    void existsById_negativeId_returnsFalse() {
        when(roleMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);

        boolean result = roleService.existsById(-1L);

        assertFalse(result);
    }

    @Test
    void existsById_mapperReturnsMultiple_returnsTrue() {
        when(roleMapper.selectCount(any(QueryWrapper.class))).thenReturn(2L);

        boolean result = roleService.existsById(1L);

        assertTrue(result);
    }

    // helper
    private Role makeRole(Long id, String name) {
        Role r = new Role();
        r.setId(id);
        r.setName(name);
        return r;
    }
}
