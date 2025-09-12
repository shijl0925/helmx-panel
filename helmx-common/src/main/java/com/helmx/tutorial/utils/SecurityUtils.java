package com.helmx.tutorial.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

@Slf4j
@Component
public class SecurityUtils {
    /**
     * 获取当前登录的用户
     * @return UserDetails
     */
    public static UserDetails getCurrentUser() {
        UserDetailsService userDetailsService = SpringBeanHolder.getBean(UserDetailsService.class);
        return userDetailsService.loadUserByUsername(getCurrentUsername());
    }

    /**
     * 获取系统用户名称
     *
     * @return 系统用户名称
     */
    public static String getCurrentUsername() {
        return getCurrentUsername(getToken());
    }

    /**
     * 获取系统用户名称
     *
     * @return 系统用户名称
     */
    public static String getCurrentUsername(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return null;
            }

            JwtTokenUtil jwtTokenUtil = SpringBeanHolder.getBean(JwtTokenUtil.class);
            return jwtTokenUtil.getUsernameFromToken(token);
        } catch (Exception e) {
            log.error("Failed to get username from token", e);
            return null;
        }
    }

    /**
     * 获取当前用户ID
     * @return 用户ID
     */
    public static Long getCurrentUserId() {
        return getCurrentUserId(getToken());
    }
    /**
     * 从token中获取用户ID
     * @param token JWT token
     * @return 用户ID
     */
    public static Long getCurrentUserId(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return null;
            }

            JwtTokenUtil jwtTokenUtil = SpringBeanHolder.getBean(JwtTokenUtil.class);
            Object userIdClaim = jwtTokenUtil.getClaimFromToken(token, "userId");

            if (userIdClaim != null) {
                if (userIdClaim instanceof Number) {
                    return ((Number) userIdClaim).longValue();
                } else {
                    return Long.valueOf(userIdClaim.toString());
                }
            }
        } catch (Exception e) {
            log.error("Failed to get user ID from token", e);
        }
        return null;
    }

    /**
     * 获取Token
     * @return /
     */
    public static String getToken() {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder
                .getRequestAttributes())).getRequest();

        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            // 去掉令牌前缀
            return bearerToken.replace("Bearer ", "");
        } else {
            log.debug("非法Token：{}", bearerToken);
        }
        return null;
    }
}
