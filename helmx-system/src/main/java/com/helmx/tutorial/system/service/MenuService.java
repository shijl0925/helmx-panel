package com.helmx.tutorial.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

import com.helmx.tutorial.system.entity.Menu;

import java.util.List;

@Service
public interface MenuService extends IService<Menu> {
    boolean existsById(Long id);

    List<Menu> buildMenuTree();

    List<Menu> buildMenuTree(List<Menu> menus);
}
