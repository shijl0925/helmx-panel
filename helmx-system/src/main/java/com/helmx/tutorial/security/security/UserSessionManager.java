package com.helmx.tutorial.security.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class UserSessionManager {

    private static final Map<String, Map<String, Object>> onlineUsers = new ConcurrentHashMap<>();

    private static final long SESSION_TIMEOUT = 24 * 3600 * 1000; // 24小时

    public void addUserSession(String username, String token) {
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("token", token);
        sessionData.put("loginTime", new Date());
        sessionData.put("lastAccessTime", System.currentTimeMillis());
        onlineUsers.put(username, sessionData);
    }

    public void removeUserSession(String username) {
        onlineUsers.remove(username);
    }

    public boolean isUserOnline(String username) {
        Map<String, Object> sessionData = onlineUsers.get(username);
        if (sessionData != null) {
            long lastAccessTime = (Long) sessionData.get("lastAccessTime");
            if (System.currentTimeMillis() - lastAccessTime > SESSION_TIMEOUT) {
                // 会话过期，移除用户
                onlineUsers.remove(username);
                log.info("用户 {} 的会话已过期，已移除", username);
                return false;
            }
            return true;
        }
        return false;
    }
}