package com.helmx.tutorial.security.security.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
// import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

/**
 * JWTService 提供 JWT 令牌的生成功能。
 * 该服务使用 Spring Security 的 JwtEncoder 来创建基于认证信息的 JWT 令牌。
 */
@Service
public class JWTService {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Value("${app.jwt.expiration-hours:24}") // JWT 令牌有效期，默认为 24 小时(注解和@Data注解类似, 区别在于配置文件读取的属性，@Value注解会自动将属性值注入到变量中)
    private long expirationHours;

    private static final Logger logger = LoggerFactory.getLogger(JWTService.class);

    /**
     * 根据认证信息生成 JWT 令牌。
     *
     * @param authentication 用户认证信息，包含用户名和权限信息。
     *                       不能为 null，且必须包含有效的用户名。
     * @return 生成的 JWT 令牌字符串。
     * @throws IllegalArgumentException 如果 authentication 为 null 或者用户名为空。
     * @throws RuntimeException         如果在生成 JWT 令牌过程中发生异常。
     */
    public String generateToken(Authentication authentication) {
        // 验证认证对象是否为空
        if (authentication == null) {
            logger.error("Authentication object is null");
            throw new IllegalArgumentException("Authentication object cannot be null");
        }

        // 获取并验证用户名
        String username = authentication.getName();
        if (username == null || username.isEmpty()) {
            logger.error("Authentication name is null or empty");
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        Long userId = 0L;
        if (authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            userId = userDetails.getId();
        }

        Instant now = Instant.now();

        // 构建用户权限范围字符串
        String scope = authentication.getAuthorities().stream()
                .map(authority -> {
                    if (authority == null || authority.getAuthority() == null) {
                        logger.warn("Found null authority in authentication for user: {}", username);
                        return "";
                    }
                    return authority.getAuthority();
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));

        // 构造 JWT 声明集
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(expirationHours, ChronoUnit.HOURS))
                .subject(username)
                .claim("scope", scope);

        // 添加用户ID到声明集中
        if (userId != 0L) {
            claimsBuilder.claim("userId", userId);
        }

        JwtClaimsSet claims = claimsBuilder.build();

        // 编码并返回 JWT 令牌
        try {
            String token = this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
            logger.info("Successfully generated JWT token for user: {}", username);
            return token;
        } catch (Exception e) {
            logger.error("Failed to generate JWT token for user: {}", username, e);
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    public String refreshToken(String expiredToken) {
        try {
            // 解析过期的token获取用户信息
            Jwt decodedJwt = this.jwtDecoder.decode(expiredToken);

            String username = decodedJwt.getSubject();
            //List<String> roles = decodedJwt.getClaimAsStringList("scope");

            // 创建新的认证对象
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            // 生成新的token
            return generateToken(authentication);
        } catch (Exception e) {
            logger.error("Failed to refresh token: {}", e.getMessage());
            return null;
        }
    }
}
