package com.helmx.tutorial.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.helmx.tutorial.system.entity.*;
import com.helmx.tutorial.system.mapper.*;
import com.helmx.tutorial.security.dto.SignupRequest;
import com.helmx.tutorial.system.service.UserService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private PasswordEncoder encoder;

    @Override
    public boolean existsByUsername(String username) {
        return username != null && this.count(new QueryWrapper<User>().eq("username", username)) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        return email != null && this.count(new QueryWrapper<User>().eq("email", email)) > 0;
    }

    @Override
    public void registerUser(SignupRequest signUpRequest) {

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setPhone(signUpRequest.getPhone());
        user.setStatus(1);
        user.setSuperAdmin(false);
        user.setNickname(signUpRequest.getNickName());

        // 保存用户到数据库，获取生成的用户ID
        userMapper.insert(user);
        Long userId = user.getId();

        Set<Integer> strRoles = signUpRequest.getRole();
        this.updateUserRoles(userId, strRoles);
    }

    @Override
    public void updateUserRoles(Long userId, Set<Integer> roleIds) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Set<Role> roles = new HashSet<>();

        if (roleIds == null || roleIds.isEmpty()) {
            // 如果没有指定角色，则默认为普通用户
            Role userRole = roleMapper.selectOne(new QueryWrapper<Role>().eq("name", ERole.User.name()));
            if (userRole == null) {
                throw new RuntimeException("Error: Role is not found.");
            }
            roles.add(userRole);
        } else {
            // 处理传入的角色
            for (Integer roleId : roleIds) {
                // 直接使用传入的角色ID进行查询
                Role foundRole = roleMapper.selectById(roleId);
                if (foundRole == null) {
                    throw new RuntimeException("Error: Role is not found.");
                }
                roles.add(foundRole);
            }
        }

        // 先删除旧的角色关联
        userRoleMapper.delete(new QueryWrapper<UserRole>().eq("user_id", userId));
        // 或者: userRoleMapper.deleteByUserId(userId);

        // 添加新的角色关联
        roles.forEach(role -> {
            UserRole userRole = new UserRole(userId, role.getId());
            userRoleMapper.insert(userRole);
            // 或者: userRoleMapper.insertUserRole(userId, role.getId());
        });
    }

    @Override
    public boolean existsById(Long id) {
        return id != null && this.count(new QueryWrapper<User>().eq("id", id)) > 0;
    }

    @Override
    public Set<String> getUserRoleNames(Long userId) {
        return this.getUserRoles(userId).stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getUserRoleIds(Long userId) {
        return this.getUserRoles(userId).stream()
                .map(Role::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Role> getUserRoles(Long userId) {
        if (userId == null) {
            return new HashSet<>();
        }

        Set<Role> roles = roleMapper.findRolesByUserId(userId);
        return roles != null ? roles : new HashSet<>();
    }

    @Override
    public Map<Long, Set<String>> getUserRoleNamesBatch(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }

        // 批量查询用户角色关联
        List<UserRole> userRoles = userRoleMapper.selectList(
                new QueryWrapper<UserRole>().in("user_id", userIds)
        );

        if (userRoles.isEmpty()) {
            return userIds.stream().collect(Collectors.toMap(
                    userId -> userId,
                    userId -> new HashSet<>()
            ));
        }

        // 获取所有相关的角色ID
        Set<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toSet());

        // 批量查询角色信息
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        Map<Long, String> roleIdToNameMap = roles.stream()
                .collect(Collectors.toMap(Role::getId, Role::getName));

        // 构建用户ID到角色名称集合的映射
        Map<Long, Set<String>> result = new HashMap<>();
        userIds.forEach(userId -> result.put(userId, new HashSet<>()));

        // 填充每个用户的角色名称
        for (UserRole userRole : userRoles) {
            Long userId = userRole.getUserId();
            Long roleId = userRole.getRoleId();
            String roleName = roleIdToNameMap.get(roleId);

            if (roleName != null) {
                result.get(userId).add(roleName);
            }
        }

        return result;
    }

    @Override
    public Map<Long, Set<Long>> getUserRoleIdsBatch(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }

        // 批量查询用户角色关联
        List<UserRole> userRoles = userRoleMapper.selectList(
                new QueryWrapper<UserRole>().in("user_id", userIds)
        );

        // 构建用户ID到角色ID集合的映射
        Map<Long, Set<Long>> result = new HashMap<>();
        userIds.forEach(userId -> result.put(userId, new HashSet<>()));

//        //或者:
//        Map<Long, Set<Long>> result = userIds.stream()
//                .collect(Collectors.toMap(
//                        userId -> userId,
//                        userId -> new HashSet<>()
//                ));

        // 填充每个用户的角色ID
        for (UserRole userRole : userRoles) {
            Long userId = userRole.getUserId();
            Long roleId = userRole.getRoleId();
            result.get(userId).add(roleId);
        }

        return result;
    }

    /**
     * 检查用户是否具有指定权限
     *
     * @param userId 用户ID
     * @param permissions 权限代码数组
     * @return 如果用户具有所有指定权限返回true，否则返回false
     */
    public boolean checkUserPermissions(Long userId, String... permissions) {
        if (userId == null || permissions == null || permissions.length == 0) {
            return false;
        }

        List<String> permissionList = Arrays.asList(permissions);
        return userMapper.checkUserPermissions(userId, permissionList);
    }

    /**
     * 检查用户是否具有指定权限（使用列表参数）
     *
     * @param userId 用户ID
     * @param permissions 权限代码列表
     * @return 如果用户具有所有指定权限返回true，否则返回false
     */
    public boolean checkUserPermissions(Long userId, List<String> permissions) {
        if (userId == null || permissions == null || permissions.isEmpty()) {
            return false;
        }

        return userMapper.checkUserPermissions(userId, permissions);
    }
}
