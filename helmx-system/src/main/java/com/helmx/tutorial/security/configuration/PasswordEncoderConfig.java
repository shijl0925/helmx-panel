package com.helmx.tutorial.security.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码编码器配置类
 * <p>
 * 该配置类用于创建和管理密码编码器Bean，是Spring Security安全框架的重要组成部分。
 * 主要负责提供BCryptPasswordEncoder实例，用于密码的加密和验证操作。
 * </p>
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 创建BCrypt密码编码器Bean
     * <p>
     * 该方法返回一个BCryptPasswordEncoder实例，用于对用户密码进行加密处理。
     * BCrypt是一种安全的密码哈希算法，具有 salt 随机化和适应性特点。
     * </p>
     *
     * @return BCryptPasswordEncoder实例，用于密码加密和验证
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
