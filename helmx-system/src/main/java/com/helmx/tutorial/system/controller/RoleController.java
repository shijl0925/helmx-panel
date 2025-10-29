package com.helmx.tutorial.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.helmx.tutorial.system.mapper.RoleMapper;
import com.helmx.tutorial.system.mapper.RoleMenuMapper;
import com.helmx.tutorial.system.service.MenuService;
import com.helmx.tutorial.system.dto.RoleCreateRequest;
import com.helmx.tutorial.system.dto.RoleUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.utils.ResponseUtil;

import com.helmx.tutorial.system.entity.Role;
import com.helmx.tutorial.system.dto.RoleQueryRequest;
import com.helmx.tutorial.system.entity.RoleMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/v1/rbac/roles")
public class RoleController {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RoleMenuMapper roleMenuMapper;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MenuService menuService;

    @Operation(summary = "Get all roles")
    @GetMapping("")
    @PreAuthorize("@va.check('System:Role:List')")
    public ResponseEntity<Result> GetAllRoles(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endTime
    ) {
//        // 方法1: 构造查询条件
//        QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
//        if (name != null && !name.isEmpty()) {
//            queryWrapper.like("name", name);
//        }
//        if (code != null && !code.isEmpty()) {
//            queryWrapper.like("code", code);
//        }
//        if (id != null && id != 0) {
//            queryWrapper.eq("id", id);
//        }
//        if (remark != null && !remark.isEmpty()) {
//            queryWrapper.like("remark", remark);
//        }
//        if (status != null) {
//            queryWrapper.eq("status", status);
//        }
//        // 添加创建时间范围筛选
//        if (startTime != null) {
//            queryWrapper.ge("created_at", startTime);
//        }
//        if (endTime != null) {
//            queryWrapper.le("created_at", endTime);
//        }
//
//        List<Role> roles = roleMapper.selectList(queryWrapper);

//        // 方法2: 构造查询参数，使用 XML 映射文件
        RoleQueryRequest criteria = new RoleQueryRequest();
        criteria.setName(name);
        criteria.setCode(code);
        criteria.setId(id);
        criteria.setRemark(remark);
        criteria.setStatus(status);
        criteria.setStartTime(startTime);
        criteria.setEndTime(endTime);
        List<Role> roles = roleMapper.findRolesByConditions(criteria);

        // 获取角色的菜单权限和接口权限
        roles.forEach(role -> {
            role.setPermissions(roleMapper.findMenuIdsByRoleId(role.getId()));
        });
        return ResponseUtil.success(roles);
    }

    @Operation(summary = "Create a new role")
    @PostMapping("")
    @PreAuthorize("@va.check('System:Role:Create')")
    public ResponseEntity<Result> CreateRole(@RequestBody RoleCreateRequest roleRequest) {
        Role role = new Role();

        role.setName(roleRequest.getName());
        role.setRemark(roleRequest.getRemark());
        role.setStatus(roleRequest.getStatus());
        role.setCode(roleRequest.getCode());

        roleMapper.insert(role);

        // 创建角色的菜单权限
        if (roleRequest.getPermissions() != null) {
            // 先删除原有权限
            roleMenuMapper.delete(new QueryWrapper<RoleMenu>().eq("role_id", role.getId()));

            Set<Integer> menuIds = roleRequest.getPermissions().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            List<RoleMenu> roleMenus = menuIds.stream()
                    .filter(Objects::nonNull)  // 过滤空值
                    .map(Integer::longValue)   // 类型安全转换
                    .filter(menuService::existsById)   // 验证菜单是否存在
                    .map(menuId -> new RoleMenu(role.getId(), menuId))
                    .toList();

            // 批量插入新权限
            if (!roleMenus.isEmpty()) {
                roleMenus.forEach(roleMenu -> roleMenuMapper.insert(roleMenu));
            }
        }

        return ResponseUtil.success(role);
    }

    @Operation(summary = "Update role by ID")
    @PutMapping("/{id}")
    @PreAuthorize("@va.check('System:Role:Edit')")
    public ResponseEntity<Result> UpdateRoleById(@PathVariable Long id, @RequestBody RoleUpdateRequest roleRequest) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            return ResponseUtil.failed(404, null, "Role not found");
        }
        if (roleRequest.getName() != null) {
            role.setName(roleRequest.getName());
        }
        if (roleRequest.getRemark() != null) {
            role.setRemark(roleRequest.getRemark());
        }
        if (roleRequest.getStatus() != null) {
            role.setStatus(roleRequest.getStatus());
        }
        if (roleRequest.getCode() != null) {
            role.setCode(roleRequest.getCode());
        }
        roleMapper.updateById(role);

        // 更新角色的菜单权限
        if (roleRequest.getPermissions() != null) {
            roleMenuMapper.delete(new QueryWrapper<RoleMenu>().eq("role_id", role.getId()));

            List<Integer> menuIds = roleRequest.getPermissions();
            menuIds.forEach(mid -> {
                Long menuId = Long.valueOf(mid);
                // 验证菜单ID是否存在
                if (menuService.existsById(menuId)) {
                    RoleMenu roleMenu = new RoleMenu(role.getId(), menuId);
                    roleMenuMapper.insert(roleMenu);
                }
            });
        }

        return ResponseUtil.success(role);
    }

    @Operation(summary = "Delete role by ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("@va.check('System:Role:Delete')")
    public ResponseEntity<Result> DeleteRoleById(@PathVariable Long id) {
        roleMapper.deleteById(id);
        return ResponseUtil.success(null);
    }
}
