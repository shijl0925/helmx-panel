package com.helmx.tutorial.system.controller;

import com.helmx.tutorial.system.entity.Menu;
import com.helmx.tutorial.system.mapper.MenuMapper;
import com.helmx.tutorial.system.service.MenuService;
import com.helmx.tutorial.system.service.impl.MenuServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MenuControllerIntegrationTest {

    @Mock
    private MenuService menuService;

    @Mock
    private MenuMapper menuMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MenuController controller = new MenuController();
        ReflectionTestUtils.setField(controller, "menuService", menuService);
        ReflectionTestUtils.setField(controller, "menuMapper", menuMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllMenus_returnsMenuTree() throws Exception {
        Menu menu = new Menu();
        menu.setId(11L);
        menu.setName("系统管理");
        menu.setPath("/system");
        when(menuService.buildMenuTree()).thenReturn(List.of(menu));

        mockMvc.perform(get("/api/v1/rbac/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("系统管理"));
    }

    @Test
    void createMenu_mapsMetaFieldsIntoPersistedEntity() throws Exception {
        doAnswer(invocation -> {
            Menu menu = invocation.getArgument(0);
            menu.setId(12L);
            return 1;
        }).when(menuMapper).insert(any(Menu.class));

        mockMvc.perform(post("/api/v1/rbac/menus")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "用户管理",
                                  "pid": 1,
                                  "type": "menu",
                                  "authCode": "System:User:List",
                                  "path": "/users",
                                  "component": "system/users/index",
                                  "status": 1,
                                  "activePath": "/users",
                                  "meta": {
                                    "title": "用户管理",
                                    "icon": "mdi:account",
                                    "sort": 20
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.data.name").value("用户管理"))
                .andExpect(jsonPath("$.data.pid").value(1));

        ArgumentCaptor<Menu> menuCaptor = ArgumentCaptor.forClass(Menu.class);
        verify(menuMapper).insert(menuCaptor.capture());
        Menu persistedMenu = menuCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("用户管理", persistedMenu.getTitle());
        org.junit.jupiter.api.Assertions.assertEquals("mdi:account", persistedMenu.getIcon());
        org.junit.jupiter.api.Assertions.assertEquals(20, persistedMenu.getSort());
    }

    @Test
    void updateMenu_returnsNotFoundWhenMenuDoesNotExist() throws Exception {
        when(menuService.getById(99L)).thenReturn(null);

        mockMvc.perform(put("/api/v1/rbac/menus/99")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "missing"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Menu not found"));
    }

    @Test
    void getAllMenus_includesOrphanAndSelfReferencingMenusAsStableTopLevelItems() throws Exception {
        MenuServiceImpl realMenuService = new MenuServiceImpl();
        ReflectionTestUtils.setField(realMenuService, "menuMapper", menuMapper);

        MenuController controller = new MenuController();
        ReflectionTestUtils.setField(controller, "menuService", realMenuService);
        ReflectionTestUtils.setField(controller, "menuMapper", menuMapper);
        MockMvc localMockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        Menu root = new Menu();
        root.setId(1L);
        root.setName("Root");
        root.setSort(1);

        Menu orphan = new Menu();
        orphan.setId(2L);
        orphan.setName("Orphan");
        orphan.setParentId(999L);
        orphan.setSort(2);

        Menu selfReferencing = new Menu();
        selfReferencing.setId(3L);
        selfReferencing.setName("Self");
        selfReferencing.setParentId(3L);
        selfReferencing.setSort(3);

        when(menuMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(root, orphan, selfReferencing)));

        localMockMvc.perform(get("/api/v1/rbac/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].name").value("Root"))
                .andExpect(jsonPath("$.data[1].name").value("Orphan"))
                .andExpect(jsonPath("$.data[2].name").value("Self"))
                .andExpect(jsonPath("$.data[1].children", hasSize(0)))
                .andExpect(jsonPath("$.data[2].children", hasSize(0)));
    }
}
