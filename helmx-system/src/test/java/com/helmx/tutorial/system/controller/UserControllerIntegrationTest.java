package com.helmx.tutorial.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.helmx.tutorial.security.security.UserSessionManager;
import com.helmx.tutorial.system.entity.User;
import com.helmx.tutorial.system.mapper.UserMapper;
import com.helmx.tutorial.system.service.UserService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerIntegrationTest {

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserSessionManager userSessionManager;

    @Mock
    private PasswordEncoder encoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        if (TableInfoHelper.getTableInfo(User.class) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        }
        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "userMapper", userMapper);
        ReflectionTestUtils.setField(controller, "userSessionManager", userSessionManager);
        ReflectionTestUtils.setField(controller, "encoder", encoder);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllUsers_appliesPagingDefaultsAndReturnsDtos() throws Exception {
        User user = new User();
        user.setId(7L);
        user.setUsername("demo");
        user.setNickname("Demo");
        user.setEmail("demo@example.com");
        user.setPhone("13800138000");
        user.setStatus(1);

        when(userService.page(any(Page.class), any())).thenAnswer(invocation -> {
            Page<User> page = invocation.getArgument(0);
            page.setRecords(java.util.List.of(user));
            page.setTotal(1);
            return page;
        });
        when(userService.getUserRoleNamesBatch(java.util.List.of(7L))).thenReturn(Map.of(7L, Set.of("ADMIN")));
        when(userService.getUserRoleIdsBatch(java.util.List.of(7L))).thenReturn(Map.of(7L, Set.of(2L)));
        when(userSessionManager.isUserOnline("demo")).thenReturn(true);

        mockMvc.perform(get("/api/v1/auth/users")
                        .param("page", "0")
                        .param("pageSize", "200")
                        .param("username", "de"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].username").value("demo"))
                .andExpect(jsonPath("$.data.items[0].nickName").value("Demo"))
                .andExpect(jsonPath("$.data.items[0].online").value(true))
                .andExpect(jsonPath("$.data.items[0].roles", containsInAnyOrder("ADMIN")))
                .andExpect(jsonPath("$.data.items[0].role", containsInAnyOrder(2)));
    }

    @Test
    void createUser_encodesPasswordPersistsUserAndLinksRoles() throws Exception {
        when(userService.existsByUsername("demo")).thenReturn(false);
        when(userService.existsByEmail("demo@example.com")).thenReturn(false);
        when(encoder.encode("secret123")).thenReturn("encoded-secret");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(9L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        mockMvc.perform(post("/api/v1/auth/users")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "demo",
                                  "email": "demo@example.com",
                                  "password": "secret123",
                                  "nickName": "Demo",
                                  "phone": "13800138000",
                                  "role": [1, 2]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(9))
                .andExpect(jsonPath("$.data.username").value("demo"))
                .andExpect(jsonPath("$.data.nickName").value("Demo"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        verify(userService).updateUserRoles(9L, Set.of(1, 2));

        User persistedUser = userCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("encoded-secret", persistedUser.getPassword());
        org.junit.jupiter.api.Assertions.assertEquals("demo@example.com", persistedUser.getEmail());
    }

    @Test
    void getUserById_returnsUserDtoWithRoles() throws Exception {
        User user = new User();
        user.setId(7L);
        user.setUsername("demo");
        user.setNickname("Demo");
        user.setEmail("demo@example.com");
        user.setPhone("13800138000");
        user.setStatus(1);

        when(userService.existsById(7L)).thenReturn(true);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(userService.getUserRoleNames(7L)).thenReturn(Set.of("ADMIN"));
        when(userService.getUserRoleIds(7L)).thenReturn(Set.of(2L));

        mockMvc.perform(get("/api/v1/auth/users/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.username").value("demo"))
                .andExpect(jsonPath("$.data.roles", containsInAnyOrder("ADMIN")))
                .andExpect(jsonPath("$.data.role", containsInAnyOrder(2)));
    }
}
