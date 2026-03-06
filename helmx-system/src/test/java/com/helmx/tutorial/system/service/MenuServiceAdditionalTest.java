package com.helmx.tutorial.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.helmx.tutorial.system.entity.Menu;
import com.helmx.tutorial.system.mapper.MenuMapper;
import com.helmx.tutorial.system.service.impl.MenuServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceAdditionalTest {

    @Mock
    private MenuMapper menuMapper;

    @InjectMocks
    private MenuServiceImpl menuService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(menuService, "baseMapper", menuMapper);
        ReflectionTestUtils.setField(menuService, "menuMapper", menuMapper);
    }

    // ---- existsById ----

    @Test
    void existsById_nullId_returnsFalse() {
        assertFalse(menuService.existsById(null));
        verify(menuMapper, never()).selectCount(any());
    }

    @Test
    void existsById_existing_returnsTrue() {
        when(menuMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        assertTrue(menuService.existsById(1L));
        verify(menuMapper).selectCount(any(QueryWrapper.class));
    }

    @Test
    void existsById_nonExisting_returnsFalse() {
        when(menuMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        assertFalse(menuService.existsById(999L));
    }

    // ---- buildMenuTree() — mapper-based variant ----

    @Test
    void buildMenuTree_noArgs_usesMapper_returnsTree() {
        Menu root = makeMenu(1L, "Dashboard", null, 1);
        Menu child = makeMenu(2L, "Overview", 1L, 1);
        when(menuMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(root, child)));

        List<Menu> result = menuService.buildMenuTree();

        verify(menuMapper).selectList(isNull());
        assertEquals(1, result.size());
        assertEquals("Dashboard", result.get(0).getName());
        assertEquals(1, result.get(0).getChildren().size());
        assertEquals("Overview", result.get(0).getChildren().get(0).getName());
    }

    @Test
    void buildMenuTree_noArgs_emptyMapper_returnsEmpty() {
        when(menuMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<Menu> result = menuService.buildMenuTree();

        assertTrue(result.isEmpty());
    }

    @Test
    void buildMenuTree_noArgs_nullMapper_returnsEmpty() {
        when(menuMapper.selectList(any())).thenReturn(null);

        List<Menu> result = menuService.buildMenuTree();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void buildMenuTree_noArgs_multipleRoots_allReturnedSorted() {
        Menu m1 = makeMenu(1L, "B-Root", null, 2);
        Menu m2 = makeMenu(2L, "A-Root", null, 1);
        when(menuMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(m1, m2)));

        List<Menu> result = menuService.buildMenuTree();

        assertEquals(2, result.size());
        assertEquals("A-Root", result.get(0).getName()); // sorted by sort field
        assertEquals("B-Root", result.get(1).getName());
    }

    // helper
    private Menu makeMenu(Long id, String name, Long parentId, Integer sort) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setName(name);
        menu.setParentId(parentId);
        menu.setSort(sort);
        menu.setChildren(new ArrayList<>());
        return menu;
    }
}
