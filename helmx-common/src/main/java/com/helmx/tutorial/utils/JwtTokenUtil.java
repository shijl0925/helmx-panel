package com.helmx.tutorial.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
public class JwtTokenUtil {
    private record CachedJwt(String token, Jwt jwt) {}

    private final JwtDecoder jwtDecoder;
    private final ThreadLocal<CachedJwt> cachedJwtHolder = new ThreadLocal<>();

    public JwtTokenUtil(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    /**
     * 解析 JWT token
     *
     * @param token JWT token 字符串
     * @return 解析后的 Jwt 对象
     * @throws RuntimeException 如果解析失败
     */
    public Jwt parseToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                throw new IllegalArgumentException("Token cannot be null or empty");
            }
            CachedJwt cachedJwt = cachedJwtHolder.get();
            if (cachedJwt != null && token.equals(cachedJwt.token())) {
                return cachedJwt.jwt();
            }

            Jwt jwt = jwtDecoder.decode(token);
            cachedJwtHolder.set(new CachedJwt(token, jwt));
            return jwt;
        } catch (Exception e) {
            cachedJwtHolder.remove();
            log.error("Failed to parse JWT token", e);
            throw new RuntimeException("Failed to parse JWT token", e);
        }
    }

    /**
     * 从 JWT token 中获取用户名
     *
     * @param token JWT token 字符串
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        try {
            Jwt jwt = parseToken(token);
            return jwt.getSubject();
        } catch (Exception e) {
            log.error("Failed to get username from token", e);
            return null;
        }
    }

    /**
     * 验证 JWT token 是否过期
     *
     * @param token JWT token 字符串
     * @return 如果未过期返回 true，否则返回 false
     */
    public boolean validateToken(String token) {
        try {
            Jwt jwt = parseToken(token);
            Instant expiryDate = jwt.getExpiresAt();
            return expiryDate != null && !expiryDate.isBefore(Instant.now());
        } catch (Exception e) {
            log.error("Failed to validate token", e);
            return false;
        }
    }

    /**
     * 从 JWT token 中获取指定的 claim 值
     *
     * @param token JWT token 字符串
     * @param claimName claim 名称
     * @return claim 值
     */
    public Object getClaimFromToken(String token, String claimName) {
        try {
            Jwt jwt = parseToken(token);
            return jwt.getClaim(claimName);
        } catch (Exception e) {
            log.error("Failed to get claim from token", e);
            return null;
        }
    }

    public Long getUserIdFromToken(String token) {
        Object userIdClaim = getClaimFromToken(token, "userId");
        if (userIdClaim instanceof Number number) {
            return number.longValue();
        }
        if (userIdClaim != null) {
            try {
                return Long.valueOf(userIdClaim.toString());
            } catch (NumberFormatException e) {
                log.warn("Invalid userId claim in token: {}", userIdClaim);
            }
        }
        return null;
    }
}
