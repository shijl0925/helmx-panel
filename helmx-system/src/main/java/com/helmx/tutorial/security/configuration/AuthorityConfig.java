package com.helmx.tutorial.security.configuration;

import com.helmx.tutorial.security.security.service.UserPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.helmx.tutorial.utils.SecurityUtils;

@Slf4j
@Service(value = "va")
public class AuthorityConfig {

    private final UserPermissionService userPermissionService;

    public AuthorityConfig(UserPermissionService userPermissionService) {
        this.userPermissionService = userPermissionService;
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
        return userPermissionService.hasAllPermissions(userId, permissions);
    }
}
