package com.helmx.tutorial.security.configuration;

import com.helmx.tutorial.system.mapper.UserMapper;
import com.helmx.tutorial.system.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.helmx.tutorial.utils.SecurityUtils;

import java.util.*;

@Slf4j
@Service(value = "va")
public class AuthorityConfig {

    private final UserService userService;

    private final UserMapper userMapper;

    public AuthorityConfig(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    /**
     * 判断接口是否有权限
     * @param permissions 权限
     * @return /
     */
    public Boolean check(String ...permissions){
        // 处理边界情况：权限数组为空时直接返回true
        if (permissions == null || permissions.length == 0) {
            return true;
        }

        UserDetails userDetails = SecurityUtils.getCurrentUser();
        if (userDetails == null) {
            return false;
        }

        // 获取当前用户的id
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("userId：{}", userId);
        if (userId == null) {
            return false;
        }

        // 获取当前用户的角色名称
        List<String> roleNames = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        log.info("roleNames：{}", roleNames);

        // 超级管理员直接返回true
        if (userService.isSuperAdmin(userId)) {
            return true;
        }

        // 直接使用 UserMapper 查询用户权限
        Set<String> userPermissions = userMapper.selectUserPermissions(userId);
        return userPermissions.containsAll(Arrays.asList(permissions));
    }
}
