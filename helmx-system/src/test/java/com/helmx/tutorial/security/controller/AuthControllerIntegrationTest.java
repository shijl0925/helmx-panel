package com.helmx.tutorial.security.controller;

import com.helmx.tutorial.security.security.UserSessionManager;
import com.helmx.tutorial.security.security.service.JWTService;
import com.helmx.tutorial.system.entity.Menu;
import com.helmx.tutorial.system.entity.User;
import com.helmx.tutorial.system.mapper.UserMapper;
import com.helmx.tutorial.system.service.MenuService;
import com.helmx.tutorial.system.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerIntegrationTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private MenuService menuService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JWTService jwtService;

    @Mock
    private HttpSession session;

    @Mock
    private UserSessionManager userSessionManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "authenticationManager", authenticationManager);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "menuService", menuService);
        ReflectionTestUtils.setField(controller, "userMapper", userMapper);
        ReflectionTestUtils.setField(controller, "jwtService", jwtService);
        ReflectionTestUtils.setField(controller, "session", session);
        ReflectionTestUtils.setField(controller, "userSessionManager", userSessionManager);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void login_returnsTokenAndTracksSessionState() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken("demo", "secret");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken(authentication)).thenReturn("jwt-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(remoteAddress("127.0.0.1"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "demo",
                                  "password": "secret"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"));

        verify(session).setAttribute("username", "demo");
        verify(session).setAttribute(eq("loginTime"), any());
        verify(session).setAttribute("ipAddress", "127.0.0.1");
        verify(userSessionManager).addUserSession("demo", "jwt-token");
    }

    @Test
    void register_rejectsDuplicateUsername() throws Exception {
        when(userService.existsByUsername("demo")).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "demo",
                                  "email": "demo@example.com",
                                  "password": "secret12",
                                  "phone": "13800138000",
                                  "nickname": "Demo"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Username is already taken!"))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getUserInfo_returnsUserProfilePermissionsAndMenus() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("demo")
                .claim("userId", "9")
                .claim("scope", "ADMIN USER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        User user = new User();
        user.setId(9L);
        user.setUsername("demo");
        user.setEmail("demo@example.com");
        user.setNickname("Demo");
        user.setStatus(1);
        user.setSuperAdmin(false);

        Menu rootMenu = new Menu();
        rootMenu.setId(100L);
        rootMenu.setName("Dashboard");
        rootMenu.setType("menu");
        rootMenu.setAuthCode("System:Dashboard:View");

        Menu actionMenu = new Menu();
        actionMenu.setId(101L);
        actionMenu.setName("Create");
        actionMenu.setType("button");
        actionMenu.setAuthCode("System:Dashboard:Create");

        when(userMapper.selectOne(any())).thenReturn(user);
        when(userService.getUserMenus(9L)).thenReturn(Set.of(rootMenu, actionMenu));
        when(menuService.buildMenuTree(anyList())).thenReturn(List.of(rootMenu));

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(jwt, null, Collections.emptyList());

        mockMvc.perform(get("/api/v1/auth/user/info")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userName").value("demo"))
                .andExpect(jsonPath("$.data.userId").value(9))
                .andExpect(jsonPath("$.data.realName").value("Demo"))
                .andExpect(jsonPath("$.data.email").value("demo@example.com"))
                .andExpect(jsonPath("$.data.roles", contains("ADMIN", "USER")))
                .andExpect(jsonPath("$.data.permissions", containsInAnyOrder("System:Dashboard:View", "System:Dashboard:Create")))
                .andExpect(jsonPath("$.data.menus", hasSize(1)))
                .andExpect(jsonPath("$.data.menus[0].name").value("Dashboard"));
    }

    @Test
    void refreshToken_returnsNewAccessToken() throws Exception {
        when(jwtService.refreshToken("expired-token")).thenReturn("new-token");

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .header("Authorization", "Bearer expired-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("new-token"));
    }

    @Test
    void logout_clearsCurrentUserSession() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("demo", "secret"));

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User logged out successfully!"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("success"));

        verify(userSessionManager).removeUserSession("demo");
    }

    private RequestPostProcessor remoteAddress(String remoteAddress) {
        return request -> {
            request.setRemoteAddr(remoteAddress);
            return request;
        };
    }

    private RequestPostProcessor authentication(Authentication authentication) {
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            request.setUserPrincipal(authentication);
            return request;
        };
    }
}
