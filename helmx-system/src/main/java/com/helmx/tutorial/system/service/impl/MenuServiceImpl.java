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
    @Autowired
    private MenuMapper menuMapper;

    @Override
    public boolean existsById(Long id) {
        return id != null && this.count(new QueryWrapper<Menu>().eq("id", id)) > 0;
    }

    @Override
    public List<Menu> buildMenuTree() {
        List<Menu> allMenus = menuMapper.selectList(null);
        return buildMenuTreeRecursive(allMenus, null);
    }

    @Override
    public List<Menu> buildMenuTree(List<Menu> menus) {
        return buildMenuTreeRecursive(menus, null);
    }

    /**
     * 递归构建菜单树
     * @param menus 所有菜单列表
     * @param parentId 父菜单ID
     * @return 菜单树
     */
    private List<Menu> buildMenuTreeRecursive(List<Menu> menus, Long parentId) {
        List<Menu> children = new ArrayList<>();

        if (menus == null || menus.isEmpty()) {
            return children;
        }

        for (Menu menu : menus) {
            // 查找指定父ID的子菜单
            Long menuParentId = menu.getParentId();
            boolean isMatch;
            if (parentId == null) {
                // 当查找顶层菜单时，匹配parent_id为null或0的菜单
                isMatch = menuParentId == null || menuParentId == 0;
            } else {
                // 正常比较
                isMatch = Objects.equals(menuParentId, parentId);
            }
            if (isMatch) {
                // 递归查找该菜单的子菜单
                List<Menu> subChildren = buildMenuTreeRecursive(menus, menu.getId());
                menu.setChildren(subChildren);

                Map<String, Object> meta = new HashMap<>();
                meta.put("title", menu.getTitle());
                meta.put("icon", menu.getIcon());
                meta.put("sort", menu.getSort());
                menu.setMeta(meta);

                children.add(menu);
            }
        }

        // 按order字段排序
        children.sort((m1, m2) -> {
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
        });

        return children;
    }
}
