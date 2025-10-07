package com.helmx.tutorial.security.configuration;

//import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
//import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.helmx.tutorial.system.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

//import com.helmx.tutorial.modules.system.entity.Resource;
//import com.helmx.tutorial.modules.system.entity.User;
import com.helmx.tutorial.utils.SecurityUtils;

import java.util.*;
//import java.util.stream.Collectors;

@Slf4j
@Service(value = "va")
public class AuthorityConfig {

    private final UserService userService;

    public AuthorityConfig(UserService userService) {
        this.userService = userService;
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

//        // 方法1：通过用户ID查询用户接口权限，然后判断
//        Set<Resource> resources = userService.getUserResources(userId);
//        if (resources == null || resources.isEmpty()) {
//            return false;
//        }
//        Set<String> resourceCodes = resources.stream()
//                .map(Resource::getCode)
//                .filter(Objects::nonNull)
//                .filter(StringUtils::isNotBlank)
//                .collect(Collectors.toSet());
//        log.info("resourceCodes：{}", resourceCodes);
//
//        return resourceCodes.containsAll(Arrays.asList(permissions));

        // 方法2: 使用 UserService 的 checkUserPermissions 方法
        return userService.checkUserPermissions(userId, permissions);
    }
}
