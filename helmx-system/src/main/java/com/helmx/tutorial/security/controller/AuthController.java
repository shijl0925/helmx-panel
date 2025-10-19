package com.helmx.tutorial.security.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.helmx.tutorial.security.dto.LoginRequest;
import com.helmx.tutorial.security.dto.SignupRequest;
import com.helmx.tutorial.security.security.UserSessionManager;
import com.helmx.tutorial.security.security.service.JWTService;
import com.helmx.tutorial.system.entity.Menu;
import com.helmx.tutorial.system.entity.User;
import com.helmx.tutorial.system.mapper.UserMapper;
import com.helmx.tutorial.system.service.MenuService;
import com.helmx.tutorial.system.service.UserService;
import com.helmx.tutorial.utils.RequestHolder;
import com.helmx.tutorial.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
    private MenuService menuService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private HttpSession session;

    @Autowired
    private UserSessionManager userSessionManager;

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

            // 记录登录状态到 Session
            session.setAttribute("username", username);
            session.setAttribute("loginTime", new Date());

            // 获取request
            HttpServletRequest request = RequestHolder.getHttpServletRequest();
            // 获取IP地址并记录到 Session
            session.setAttribute("ipAddress", request.getRemoteAddr());

            Map<String, Object> jwtInfo = new HashMap<>();
            jwtInfo.put("accessToken", jwt);

            userSessionManager.addUserSession(username, jwt);

            return ResponseUtil.success( jwtInfo);
        } catch (org.springframework.security.authentication.DisabledException e) {
            logger.warn("Login failed: Account is disabled - {}", username);
            return ResponseUtil.failed(401, null, "Account is disabled");
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            logger.warn("Login failed: Username or password is incorrect - {}", username);
            return ResponseUtil.failed(401, null, "Username or password is incorrect");
        } catch (org.springframework.security.core.AuthenticationException e) {
            logger.warn("Login failed: Authentication exception - {}: {}", username, e.getMessage());
            return ResponseUtil.failed(401, null, "Login failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Login failed: {}", e.getMessage());
            return ResponseUtil.failed(500, null, "Login failed");
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

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");

        return ResponseUtil.success("User registered successfully!", result);
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
                return ResponseUtil.failed(401, null, "Account does not exist or has been disabled");
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

            // 从数据库中查询用户菜单权限
            Set<Menu> menuSets = userService.getUserMenus(user.getId());

            if (user.isSuperAdmin()) {
                userInfo.put("permissions", List.of("*"));
            } else {
                Set<String> authCodes = menuSets.stream().map(Menu::getAuthCode)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toSet());
                userInfo.put("permissions", authCodes);
            }

            List<Menu> menus = menuSets.stream().filter(menu -> !Objects.equals(menu.getType(), "button")).toList();
            userInfo.put("menus", menuService.buildMenuTree(menus));

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
        String username;

        // 尝试从 JWT token 中提取用户名
        username = SecurityUtils.getCurrentUsername();

        // 如果没有从 JWT token 中提取到用户名，尝试从 SecurityContext 中获取
        if (username == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                username = authentication.getName();
            }
        }

        // 清除安全上下文
        SecurityContextHolder.clearContext();

        // 移除用户会话
        if (username != null) {
            logger.info("Removing user session for: {}", username);
            userSessionManager.removeUserSession(username);
        } else {
            logger.info("No user to logout");
        }
        logger.info("User logged out successfully!");

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");

        return ResponseUtil.success("User logged out successfully!", result);
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
