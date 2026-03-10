package com.helmx.tutorial.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.helmx.tutorial.system.entity.Menu;
import com.helmx.tutorial.system.mapper.MenuMapper;
import com.helmx.tutorial.system.service.MenuService;

import java.util.*;

@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {
    private static final Comparator<Menu> MENU_SORT_COMPARATOR = (m1, m2) -> {
        Integer order1 = m1.getSort();
        Integer order2 = m2.getSort();

        if (order1 == null && order2 == null) {
            return 0;
        }
        if (order1 == null) {
            return 1;
        }
        if (order2 == null) {
            return -1;
        }
        return order1.compareTo(order2);
    };

    @Autowired
    private MenuMapper menuMapper;

    @Override
    public boolean existsById(Long id) {
        return id != null && this.count(new QueryWrapper<Menu>().eq("id", id)) > 0;
    }

    @Override
    public List<Menu> buildMenuTree() {
        List<Menu> allMenus = menuMapper.selectList(null);
        return buildMenuTree(allMenus);
    }

    @Override
    public List<Menu> buildMenuTree(List<Menu> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, List<Menu>> childrenByParentId = new HashMap<>();
        for (Menu menu : menus) {
            menu.setChildren(new ArrayList<>());
            menu.setMeta(buildMeta(menu));
            childrenByParentId.computeIfAbsent(normalizeParentId(menu.getParentId()), key -> new ArrayList<>())
                    .add(menu);
        }

        List<Menu> menuTree = new ArrayList<>();
        Set<Menu> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        addRootMenus(menuTree, childrenByParentId.getOrDefault(null, Collections.emptyList()), childrenByParentId, visited);
        addDisconnectedMenus(menuTree, menus, childrenByParentId, visited);

        return menuTree;
    }

    /**
     * 递归构建菜单树
     * @param childrenByParentId 按父菜单ID分组后的菜单列表
     * @param parentId 父菜单ID
     * @return 菜单树
     */
    private void addRootMenus(List<Menu> menuTree,
                              List<Menu> roots,
                              Map<Long, List<Menu>> childrenByParentId,
                              Set<Menu> visited) {
        List<Menu> sortedRoots = new ArrayList<>(roots);
        sortedRoots.sort(MENU_SORT_COMPARATOR);
        for (Menu root : sortedRoots) {
            if (visited.add(root)) {
                populateChildren(root, childrenByParentId, visited, Collections.newSetFromMap(new IdentityHashMap<>()));
                menuTree.add(root);
            }
        }
    }

    private void addDisconnectedMenus(List<Menu> menuTree,
                                      List<Menu> menus,
                                      Map<Long, List<Menu>> childrenByParentId,
                                      Set<Menu> visited) {
        List<Menu> remainingMenus = new ArrayList<>(menus);
        remainingMenus.sort(MENU_SORT_COMPARATOR);
        for (Menu menu : remainingMenus) {
            if (!visited.add(menu)) {
                continue;
            }
            populateChildren(menu, childrenByParentId, visited, Collections.newSetFromMap(new IdentityHashMap<>()));
            menuTree.add(menu);
        }
    }

    private void populateChildren(Menu menu,
                                  Map<Long, List<Menu>> childrenByParentId,
                                  Set<Menu> visited,
                                  Set<Menu> visiting) {
        if (!visiting.add(menu)) {
            return;
        }

        List<Menu> children = new ArrayList<>(
                childrenByParentId.getOrDefault(normalizeParentId(menu.getId()), Collections.emptyList()));
        children.sort(MENU_SORT_COMPARATOR);

        List<Menu> safeChildren = new ArrayList<>();
        for (Menu child : children) {
            if (visiting.contains(child)) {
                continue;
            }
            visited.add(child);
            populateChildren(child, childrenByParentId, visited, visiting);
            safeChildren.add(child);
        }
        menu.setChildren(safeChildren);
        visiting.remove(menu);
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId == 0L ? null : parentId;
    }

    private Map<String, Object> buildMeta(Menu menu) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("title", menu.getTitle());
        meta.put("icon", menu.getIcon());
        meta.put("sort", menu.getSort());
        return meta;
    }
}
