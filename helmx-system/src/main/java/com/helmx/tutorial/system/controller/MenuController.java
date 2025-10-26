package com.helmx.tutorial.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.helmx.tutorial.system.mapper.MenuMapper;
import com.helmx.tutorial.system.service.MenuService;
import com.helmx.tutorial.system.dto.MenuCreateRequest;
import com.helmx.tutorial.system.dto.MenuUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.helmx.tutorial.utils.ResponseUtil;

import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.system.entity.Menu;
import com.helmx.tutorial.system.dto.MenuMetaRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Tag(name = "菜单管理")
@RestController
@RequestMapping("/api/v1/rbac/menus")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @Autowired
    private MenuMapper menuMapper;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Operation(summary = "Get all menus")
    @GetMapping("")
    // @PreAuthorize("@va.check('System:Menu:Read')")
    public ResponseEntity<Result> GetAllMenus() {
        List<Menu> menuTree = menuService.buildMenuTree();
        return ResponseUtil.success(menuTree);
    }

    @Operation(summary = "Create a new menu")
    @PostMapping("")
    // @PreAuthorize("@va.check('System:Menu:Create')")
    public ResponseEntity<Result> CreateMenu(@RequestBody MenuCreateRequest menuRequest) {
        Menu menu = new Menu();
        menu.setName(menuRequest.getName());
        menu.setParentId(menuRequest.getPid());
        menu.setType(menuRequest.getType());
        menu.setAuthCode(menuRequest.getAuthCode());
        menu.setPath(menuRequest.getPath());
        menu.setComponent(menuRequest.getComponent());
        menu.setStatus(menuRequest.getStatus());
        menu.setActivePath(menuRequest.getActivePath());

        // 从meta中获取图标、排序和标题信息
        MenuMetaRequest meta = menuRequest.getMeta();
        if (meta != null) {
            menu.setIcon(meta.getIcon());
            menu.setSort(meta.getSort());
            menu.setTitle(meta.getTitle());
        }

        menuMapper.insert(menu);

        return ResponseUtil.success(menu);
    }

    @Operation(summary = "Check Menu Name exists")
    @GetMapping("/name-exists")
    public ResponseEntity<Result> checkMenuNameExists(
            @RequestParam(required = true) String name,
            @RequestParam(required = false) Integer id
    ) {
        QueryWrapper<Menu> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("name", name);
        if (id != null && id != 0) {
            queryWrapper.ne("id", id);
        }

        boolean exists = menuMapper.selectCount(queryWrapper) > 0;

        return ResponseUtil.success(exists);
    }

    @Operation(summary = "Check Menu path exists")
    @GetMapping("/path-exists")
    public ResponseEntity<Result> checkMenuPathExists(
            @RequestParam(required = true) String path,
            @RequestParam(required = false) Integer id
    ) {
        QueryWrapper<Menu> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("path", path);
        if (id != null && id != 0) {
            queryWrapper.ne("id", id);
        }

        boolean exists = menuMapper.selectCount(queryWrapper) > 0;

        return ResponseUtil.success(exists);
    }

    @Operation(summary = "Get menu by ID")
    @GetMapping("/{id}")
    // @PreAuthorize("@va.check('System:Menu:Read')")
    public ResponseEntity<Result> GetMenuById(@PathVariable Long id) {
        Menu menu = menuService.getById(id);
        return menu != null ? ResponseUtil.success(menu) : ResponseUtil.failed(404, null, "Menu not found");
    }

    @Operation(summary = "Update menu by ID")
    @PutMapping("/{id}")
    // @PreAuthorize("@va.check('System:Menu:Update')")
    public ResponseEntity<Result> UpdateMenuById(@PathVariable Long id, @RequestBody MenuUpdateRequest menuRequest) {
        Menu menu = menuService.getById(id);
        if (menu == null) {
            return ResponseUtil.failed(404, null, "Menu not found");
        }
        if (menuRequest.getName() != null) {
            menu.setName(menuRequest.getName());
        }
        if (menuRequest.getPid() != null) {
            menu.setParentId(menuRequest.getPid());
        }
        if (menuRequest.getAuthCode() != null) {
            menu.setAuthCode(menuRequest.getAuthCode());
        }
        if (menuRequest.getPath() != null) {
            menu.setPath(menuRequest.getPath());
        }
        if (menuRequest.getComponent() != null) {
            menu.setComponent(menuRequest.getComponent());
        }
        if (menuRequest.getStatus() != null) {
            menu.setStatus(menuRequest.getStatus());
        }
        if (menuRequest.getActivePath() != null) {
            menu.setActivePath(menuRequest.getActivePath());
        }
        if (menuRequest.getType() != null) {
            menu.setType(menuRequest.getType());
        }

        // 从meta中获取图标、排序和标题信息
        MenuMetaRequest meta = menuRequest.getMeta();
        if (meta != null) {
            if (meta.getTitle() != null) {
                menu.setTitle(meta.getTitle());
            }
            if (meta.getIcon() != null) {
                menu.setIcon(meta.getIcon());
            }
            if (meta.getSort() != null) {
                menu.setSort(meta.getSort());
            }
        }
        menuMapper.updateById(menu);

        return ResponseUtil.success(menu);
    }

    @Operation(summary = "Delete menu by ID")
    @DeleteMapping("/{id}")
    // @PreAuthorize("@va.check('System:Menu:Delete')")
    public ResponseEntity<Result> DeleteMenuById(@PathVariable Long id) {
        menuMapper.deleteById(id);
        return ResponseUtil.success(null);
    }
}
