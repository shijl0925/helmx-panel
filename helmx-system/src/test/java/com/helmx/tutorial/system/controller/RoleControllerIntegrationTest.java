package com.helmx.tutorial.system.controller;

import com.helmx.tutorial.system.entity.Role;
import com.helmx.tutorial.system.entity.RoleMenu;
import com.helmx.tutorial.system.mapper.RoleMapper;
import com.helmx.tutorial.system.mapper.RoleMenuMapper;
import com.helmx.tutorial.system.service.MenuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
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
class RoleControllerIntegrationTest {

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private RoleMenuMapper roleMenuMapper;

    @Mock
    private MenuService menuService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RoleController controller = new RoleController();
        ReflectionTestUtils.setField(controller, "roleMapper", roleMapper);
        ReflectionTestUtils.setField(controller, "roleMenuMapper", roleMenuMapper);
        ReflectionTestUtils.setField(controller, "menuService", menuService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllRoles_returnsRolesWithResolvedPermissions() throws Exception {
        Role role = new Role();
        role.setId(3L);
        role.setName("管理员");
        role.setCode("ADMIN");
        role.setStatus(1);

        when(roleMapper.findRolesByConditions(any())).thenReturn(List.of(role));
        when(roleMapper.findMenuIdsByRoleId(3L)).thenReturn(Set.of(8L, 9L));

        mockMvc.perform(get("/api/v1/rbac/roles")
                        .param("name", "管理"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(3))
                .andExpect(jsonPath("$.data[0].name").value("管理员"))
                .andExpect(jsonPath("$.data[0].permissions", containsInAnyOrder(8, 9)));
    }

    @Test
    void createRole_persistsOnlyExistingMenuPermissions() throws Exception {
        when(menuService.existsById(1L)).thenReturn(true);
        when(menuService.existsById(2L)).thenReturn(false);
        doAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(5L);
            return 1;
        }).when(roleMapper).insert(any(Role.class));

        mockMvc.perform(post("/api/v1/rbac/roles")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "审核员",
                                  "remark": "审核权限",
                                  "status": 1,
                                  "code": "AUDITOR",
                                  "permissions": [1, 2, null]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.code").value("AUDITOR"));

        ArgumentCaptor<RoleMenu> roleMenuCaptor = ArgumentCaptor.forClass(RoleMenu.class);
        verify(roleMenuMapper).insert(roleMenuCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(5L, roleMenuCaptor.getValue().getRoleId());
        org.junit.jupiter.api.Assertions.assertEquals(1L, roleMenuCaptor.getValue().getMenuId());
    }
}
