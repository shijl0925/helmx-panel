package com.helmx.tutorial.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.helmx.tutorial.system.entity.Menu;
import com.helmx.tutorial.system.entity.Role;
import org.springframework.stereotype.Service;

import com.helmx.tutorial.security.dto.SignupRequest;
import com.helmx.tutorial.system.entity.User;

import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
public interface UserService extends IService<User> {
    /**
     * Check if user exists by username
     */
    boolean existsByUsername(String username);

    /**
     * Check if user exists by email
     */
    boolean existsByEmail(String email);

    /**
     * Register a new user
     */
    void registerUser(SignupRequest signUpRequest);

    boolean existsById(Long id);

    // 获取用户角色名称
    Set<String> getUserRoleNames(Long userId);

    // 获取用户角色ID
    Set<Long> getUserRoleIds(Long userId);

    // 批量获取用户角色名称
    Map<Long, Set<String>> getUserRoleNamesBatch(List<Long> userIds);

    // 批量获取用户角色ID
    Map<Long, Set<Long>> getUserRoleIdsBatch(List<Long> userIds);

    Set<Role> getUserRoles(Long userId);

    // 更新用户角色
    void updateUserRoles(Long userId, Set<Integer> roleIds);

    // 获取用户菜单
    Set<Menu> getUserMenus(Long userId);

    boolean isSuperAdmin(Long userId);

    // 检查用户是否具有指定权限
    boolean checkUserPermissions(Long userId, String... permissions);

    boolean checkUserPermissions(Long userId, List<String> permissions);
}