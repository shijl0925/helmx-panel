package com.helmx.tutorial.system.service;

import com.helmx.tutorial.system.entity.Menu;
import com.helmx.tutorial.system.service.impl.MenuServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MenuServiceImplTest {

    private MenuServiceImpl menuService;

    @BeforeEach
    void setUp() {
        menuService = new MenuServiceImpl();
    }

    // ---- buildMenuTree(List<Menu>) ----

    @Test
    void buildMenuTree_nullList_returnsEmpty() {
        List<Menu> result = menuService.buildMenuTree((List<Menu>) null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void buildMenuTree_emptyList_returnsEmpty() {
        List<Menu> result = menuService.buildMenuTree(Collections.emptyList());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void buildMenuTree_singleTopLevelMenu_nullParentId() {
        Menu menu = makeMenu(1L, "Dashboard", null, 1);
        List<Menu> result = menuService.buildMenuTree(List.of(menu));

        assertEquals(1, result.size());
        assertEquals("Dashboard", result.get(0).getName());
        assertTrue(result.get(0).getChildren().isEmpty());
    }

    @Test
    void buildMenuTree_singleTopLevelMenu_zeroParentId() {
        // parentId == 0 should also be treated as a root item
        Menu menu = makeMenu(2L, "Home", 0L, 1);
        List<Menu> result = menuService.buildMenuTree(List.of(menu));

        assertEquals(1, result.size());
        assertEquals("Home", result.get(0).getName());
    }

    @Test
    void buildMenuTree_nestedMenus_buildsCorrectHierarchy() {
        Menu parent = makeMenu(10L, "System",  null, 1);
        Menu child1  = makeMenu(11L, "Users",   10L,  1);
        Menu child2  = makeMenu(12L, "Roles",   10L,  2);

        List<Menu> result = menuService.buildMenuTree(Arrays.asList(parent, child1, child2));

        assertEquals(1, result.size());
        Menu root = result.get(0);
        assertEquals("System", root.getName());
        assertEquals(2, root.getChildren().size());
    }

    @Test
    void buildMenuTree_multipleRootMenus_allReturnedAtTopLevel() {
        Menu m1 = makeMenu(1L, "A", null, 2);
        Menu m2 = makeMenu(2L, "B", null, 1);
        Menu m3 = makeMenu(3L, "C", null, 3);

        List<Menu> result = menuService.buildMenuTree(Arrays.asList(m1, m2, m3));

        assertEquals(3, result.size());
    }

    @Test
    void buildMenuTree_sortsByOrderField_ascending() {
        Menu m1 = makeMenu(1L, "Z", null, 3);
        Menu m2 = makeMenu(2L, "A", null, 1);
        Menu m3 = makeMenu(3L, "M", null, 2);

        List<Menu> result = menuService.buildMenuTree(Arrays.asList(m1, m2, m3));

        assertEquals(3, result.size());
        assertEquals("A", result.get(0).getName());
        assertEquals("M", result.get(1).getName());
        assertEquals("Z", result.get(2).getName());
    }

    @Test
    void buildMenuTree_nullSortField_sortedLast() {
        Menu noSort  = makeMenu(1L, "NoSort", null, null);
        Menu withSort = makeMenu(2L, "WithSort", null, 1);

        List<Menu> result = menuService.buildMenuTree(Arrays.asList(noSort, withSort));

        assertEquals(2, result.size());
        assertEquals("WithSort", result.get(0).getName());
        assertEquals("NoSort",   result.get(1).getName());
    }

    @Test
    void buildMenuTree_populatesMetaWithTitleIconSort() {
        Menu menu = makeMenu(1L, "Dashboard", null, 5);
        menu.setTitle("仪表板");
        menu.setIcon("dashboard-icon");

        List<Menu> result = menuService.buildMenuTree(List.of(menu));

        Map<String, Object> meta = result.get(0).getMeta();
        assertNotNull(meta);
        assertEquals("仪表板", meta.get("title"));
        assertEquals("dashboard-icon", meta.get("icon"));
        assertEquals(5, meta.get("sort"));
    }

    @Test
    void buildMenuTree_deeplyNestedMenus_buildsFullTree() {
        Menu level1 = makeMenu(1L, "L1", null, 1);
        Menu level2 = makeMenu(2L, "L2", 1L,  1);
        Menu level3 = makeMenu(3L, "L3", 2L,  1);

        List<Menu> result = menuService.buildMenuTree(Arrays.asList(level1, level2, level3));

        assertEquals(1, result.size());
        Menu l1 = result.get(0);
        assertEquals(1, l1.getChildren().size());

        Menu l2 = l1.getChildren().get(0);
        assertEquals("L2", l2.getName());
        assertEquals(1, l2.getChildren().size());

        Menu l3 = l2.getChildren().get(0);
        assertEquals("L3", l3.getName());
        assertTrue(l3.getChildren().isEmpty());
    }

    @Test
    void buildMenuTree_childrenSortedWithinEachLevel() {
        Menu parent = makeMenu(1L, "Parent", null, 1);
        Menu c1     = makeMenu(2L, "C-order2", 1L, 2);
        Menu c2     = makeMenu(3L, "C-order1", 1L, 1);

        List<Menu> result = menuService.buildMenuTree(Arrays.asList(parent, c1, c2));

        List<Menu> children = result.get(0).getChildren();
        assertEquals(2, children.size());
        assertEquals("C-order1", children.get(0).getName());
        assertEquals("C-order2", children.get(1).getName());
    }

    // ---- helpers ----

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
