package com.helmx.tutorial.security.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.helmx.tutorial.security.dto.LoginRequest;
import com.helmx.tutorial.security.dto.SignupRequest;
import com.helmx.tutorial.security.security.service.JWTService;
import com.helmx.tutorial.system.entity.User;
import com.helmx.tutorial.system.mapper.UserMapper;
import com.helmx.tutorial.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.utils.ResponseUtil;

import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Auth", description = "Auth management endpoints")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JWTService jwtService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Operation(summary = "Log in a user", description = "This operation logs in a user with the provided details and returns a token.")
    @PostMapping(value = "/login")
    public ResponseEntity<Result> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername(); // only for vben
        String password = loginRequest.getPassword();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtService.generateToken(authentication);

            Map<String, Object> jwtInfo = new HashMap<>();
            jwtInfo.put("accessToken", jwt);
            return ResponseUtil.success( jwtInfo);
        } catch (org.springframework.security.authentication.DisabledException e) {
            logger.warn("登录失败：账户已被禁用 - {}", username);
            return ResponseUtil.failed(401, null, "账户已被禁用");
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            logger.warn("登录失败：用户名或密码错误 - {}", username);
            return ResponseUtil.failed(401, null, "用户名或密码错误");
        } catch (org.springframework.security.core.AuthenticationException e) {
            logger.warn("登录失败：认证异常 - {}: {}", username, e.getMessage());
            return ResponseUtil.failed(401, null, "登录失败: " + e.getMessage());
        }catch (Exception e) {
            logger.error("登录失败：{}", e.getMessage());
            return ResponseUtil.failed(500, null, "登录失败");
        }
    }

    @Operation(summary = "Register a new user", description = "This operation registers a new user with the provided details and returns the registered user.")
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {

        if (userService.existsByUsername(signUpRequest.getUsername())) {
            return ResponseUtil.failed(400, null, "Error: Username is already taken!");
        }

        if (userService.existsByEmail(signUpRequest.getEmail())) {
                return ResponseUtil.failed(400, null, "Error: Email is already in use!");
        }

        userService.registerUser(signUpRequest);
        return ResponseUtil.success("User registered successfully!", null);
    }

    @Operation(summary = "Get user information", description = "This operation returns the user information.")
    @GetMapping("/user/info")
    public ResponseEntity<Result> getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseUtil.failed(401, null, "Unauthorized");
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            Map<String, Object> userInfo = new HashMap<>();
            String username = jwt.getSubject();
            userInfo.put("userName", username);

            User user = userMapper.selectOne(new QueryWrapper<User>()
                    .last("LIMIT 1") // 仅返回一条记录
                    .eq("username", username)
                    .eq("status", 1) // 只查找启用状态的用户
            );
//            或者: User user = userService.getOne(new QueryWrapper<User>().eq("username", username));
//            或者: User user = userMapper.findByUsername(username);

            // 检查用户是否存在且为启用状态
            if (user == null) {
                return ResponseUtil.failed(401, null, "账户不存在或已被禁用");
            }

            userInfo.put("userId", user.getId());
            userInfo.put("email", user.getEmail());
            userInfo.put("realName", user.getNickname());

            String userId = jwt.getClaimAsString("id");
            logger.info("username: {}, id: {}", username, userId);

            // 从 scope 中提取角色信息
            String scope = jwt.getClaimAsString("scope");
            if (scope != null) {
                userInfo.put("roles", Arrays.asList(scope.split(" ")));
            }

            return ResponseUtil.success(userInfo);
        }

        return ResponseUtil.failed(401, null, "Unable to extract user information");
    }


    @GetMapping("/user/codes")
    public ResponseEntity<Result> getUserCodes() {
        List<String> codes = Arrays.asList("AC_100100", "AC_100110", "AC_100120", "AC_100010");
        return ResponseUtil.success(codes);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        SecurityContextHolder.clearContext();
        logger.info("User logged out successfully!");
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "Refresh JWT token", description = "This operation refreshes an expired JWT token.")
    @PostMapping("/refresh-token")
    public ResponseEntity<Result> refreshToken(HttpServletRequest request) {
        try {
            // 从请求头中获取过期的token
            String expiredToken = extractTokenFromRequest(request);
            if (expiredToken == null) {
                return ResponseUtil.failed(400, null, "Missing token");
            }

            // 验证并刷新token
            String newToken = jwtService.refreshToken(expiredToken);
            if (newToken != null) {
                Map<String, Object> jwtInfo = new HashMap<>();
                jwtInfo.put("accessToken", newToken);
                return ResponseUtil.success("Token refreshed successfully", jwtInfo);
            } else {
                return ResponseUtil.failed(401, null, "Unable to refresh token");
            }
        } catch (Exception e) {
            logger.error("Token refresh failed: {}", e.getMessage());
            return ResponseUtil.failed(500, null, "Token refresh failed");
        }
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
