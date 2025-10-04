package com.helmx.tutorial.security.configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.helmx.tutorial.security.security.JwtAuthenticationEntryPoint;
import com.helmx.tutorial.security.security.JwtExceptionFilter;
import com.helmx.tutorial.security.security.service.UserDetailsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security 配置类，用于配置认证、授权、JWT 编解码器、CORS 和安全过滤器链。
 */
@Configuration
@EnableMethodSecurity() // 启用方法级安全注解
public class SpringSecurityConfig {

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private JwtExceptionFilter jwtExceptionFilter;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    private static final Logger logger = LoggerFactory.getLogger(SpringSecurityConfig.class);

    private static final List<String> ALLOWED_METHODS = Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD");
    private static final List<String> ALLOWED_HEADERS = Arrays.asList("Authorization", "Content-Type");

    /**
     * 配置安全过滤器链，定义请求的认证和授权规则。
     *
     * @param httpSecurity HttpSecurity 对象，用于构建安全配置
     * @return 配置完成的 SecurityFilterChain 实例
     * @throws Exception 配置过程中可能抛出异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .cors(Customizer.withDefaults())
                // 禁用 CSRF
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 授权异常
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // 用户登录/注册/退出
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/logout").permitAll()
                        // 获取环境信息
                        .requestMatchers("/api/v1/ops/envs/all").permitAll()
                        // 容器终端
                        .requestMatchers("/api/v1/ops/containers/terminal/**").permitAll()
                        // 容器日志流
                        .requestMatchers("/api/v1/ops/containers/logs/stream").permitAll()
                        .requestMatchers("/api/v1/ops/containers/logs/stream/**").permitAll()
                        // swagger 文档
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // 静态资源等等
                        .requestMatchers("/index.html", "/static/**",  "/public/**", "/jse/**", "/css/**", "/js/**","/_app.config.js**", "/favicon.ico", "/sw.js").permitAll()
                        // 所有请求都需要认证
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .addFilterBefore(jwtExceptionFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 配置 CORS 策略，控制跨域请求的来源、方法、头部等。
     *
     * @return CorsConfigurationSource 实例，用于提供 CORS 配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        boolean isWildcard = "*".equals(allowedOrigin);
        if (isWildcard) {
            configuration.setAllowedOrigins(Collections.singletonList("*"));
        } else {
            configuration.setAllowedOrigins(Collections.singletonList(allowedOrigin));
        }

        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setAllowCredentials(!isWildcard); // 通配符时不允许凭证
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * 配置认证管理器，用于处理用户认证逻辑。
     *
     * @param http HttpSecurity 对象，用于获取共享的 AuthenticationManagerBuilder
     * @return AuthenticationManager 实例，用于执行认证操作
     * @throws Exception 配置过程中可能抛出异常
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder);
        return authenticationManagerBuilder.build();
    }

}
