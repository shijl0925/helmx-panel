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

        return buildMenuTreeRecursive(childrenByParentId, null);
    }

    /**
     * 递归构建菜单树
     * @param childrenByParentId 按父菜单ID分组后的菜单列表
     * @param parentId 父菜单ID
     * @return 菜单树
     */
    private List<Menu> buildMenuTreeRecursive(Map<Long, List<Menu>> childrenByParentId, Long parentId) {
        List<Menu> children = new ArrayList<>(
                childrenByParentId.getOrDefault(normalizeParentId(parentId), Collections.emptyList()));

        children.sort(MENU_SORT_COMPARATOR);
        for (Menu menu : children) {
            menu.setChildren(buildMenuTreeRecursive(childrenByParentId, menu.getId()));
        }

        return children;
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
