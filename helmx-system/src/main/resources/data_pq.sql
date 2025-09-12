-- 插入部门数据
INSERT INTO tb_rbac_departs (id, name, remark, status, created_at, updated_at) VALUES
  (1, '技术部', '技术开发部门', 1, NOW(), NOW()),
  (2, '产品部', '产品管理部门', 1, NOW(), NOW()),
  (3, '运营部', '运营管理部', 1, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 插入角色数据
INSERT INTO tb_rbac_roles (id, name, remark, status, code, created_at, updated_at) VALUES
 (1, 'Super', '超级管理员', 1, 'super', NOW(), NOW()),
 (2, 'Admin', '管理员', 1, 'admin', NOW(), NOW()),
 (3, 'User', '用户', 1, 'user', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 插入用户数据
INSERT INTO tb_users (id, username, password, nickname, phone, email, status, is_super_admin, depart_id, created_at, updated_at) VALUES
 (1, 'vben', '$2a$10$UfnzZR64s0aPu8tmsFYTpei9wGEyCe7j3uPaYIIlvLcl/l/TJh3fG', 'Vben', '', 'vben@example.com', 1, TRUE, 1, NOW(), NOW()),
 (2, 'admin', '$2a$10$.DFCpdT4GgnChpdExeoxCe1ufZA4BfZxJh.op6J5icfwhyVAnNTiu', 'Admin', '', 'admin@example.com', 1, FALSE, 2, NOW(), NOW()),
 (3, 'jack', '$2a$10$N5pQz75dYqLiF8aedOvR7eNtB5XFj7xYsLC0bQrFyeaHXpnZqXoEC', 'Jack', '', 'jack@example.com', 1, FALSE, 3, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 插入用户角色关联数据
INSERT INTO tb_rbac_user_roles (user_id, role_id) VALUES
 (1, 1),
 (2, 2),
 (3, 3)
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 插入菜单数据
INSERT INTO tb_rbac_menus (id, created_at, updated_at, type, title, path, icon, parent_id, component, name, status, "order", auth_code) VALUES
  (1,NOW(), NOW(),'catalog','system.title','/system','icon-park-outline:config',0,'','System',1,0,''),
  (2,NOW(), NOW(),'menu','system.user.title','/system/user','icon-park-outline:user',1,'system/user/list','SystemUser',1,0,'System:User:List'),
  (3,NOW(), NOW(),'menu','system.role.title','/system/role','icon-park-outline:every-user',1,'system/role/list','SystemRole',1,1,'System:Role:List'),
  (4,NOW(), NOW(),'menu','system.menu.title','/system/menu','icon-park-outline:hamburger-button',1,'system/menu/list','SystemMenu',1,2,'System:Menu:List'),
  (5,NOW(), NOW(),'menu','system.resource.title','/system/resource','icon-park-outline:api',1,'system/resource/list','SystemApi',1,3,'System:Resource:List'),
  (6,NOW(), NOW(),'button','common.create','','',3,'','SystemRoleCreate',1,0,'System:Role:Create'),
  (7,NOW(), NOW(),'button','common.edit','','',3,'','SystemRoleEdit',1,0,'System:Role:Edit'),
  (8,NOW(), NOW(),'button','common.delete','','',3,'','SystemRoleDelete',1,0,'System:Role:Delete'),
  (9,NOW(), NOW(),'button','common.create','','',4,'','SystemMenuCreate',1,0,'System:Menu:Create'),
  (10,NOW(), NOW(),'button','common.edit','','',4,'','SystemMenuEdit',1,0,'System:Menu:Edit'),
  (11,NOW(), NOW(),'button','common.delete','','',4,'','SystemMenuDelete',1,0,'System:Menu:Delete'),
  (12,NOW(), NOW(),'button','common.create','','',2,'','SystemUserCreate',1,0,'System:User:Create'),
  (13,NOW(), NOW(),'button','common.edit','','',2,'','SystemUserEdit',1,0,'System:User:Edit'),
  (14,NOW(), NOW(),'button','common.delete','','',2,'','SystemUserDelete',1,0,'System:User:Delete'),
  (15,NOW(), NOW(),'button','common.create','','',5,'','SystemResourceCreate',1,0,'System:Resource:Create'),
  (16,NOW(), NOW(),'button','common.edit','','',5,'','SystemResourceEdit',1,0,'System:Resource:Edit'),
  (17,NOW(), NOW(),'button','common.delete','','',5,'','SystemResourceDelete',1,0,'System:Resource:Delete'),
  (18,NOW(), NOW(),'menu','system.operation.title','/system/operation','carbon:time',1,'/system/operation/list','OperationLog',1,5,'System:Operation:List'),
  (19,NOW(), NOW(),'menu','system.dept.title','/system/depart','carbon:departure',1,'system/dept/list','SystemDepart',1,4,'System:Depart:List'),
  (20,NOW(), NOW(),'button','common.create','','',19,'','SystemDepartCreate',1,0,'System:Depart:Create'),
  (21,NOW(), NOW(),'button','common.edit','','',19,'','SystemDepartEdit',1,0,'System:Depart:Edit'),
  (22,NOW(), NOW(),'button','common.delete','','',19,'','SystemDepartDelete',1,0,'System:Depart:Delete')
ON CONFLICT (id) DO NOTHING;

-- 插入角色菜单关联数据
INSERT INTO tb_rbac_role_menus (role_id, menu_id) VALUES
 (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15) , (1, 16), (1, 17), (1, 18), (1, 19), (1, 20), (1, 21), (1, 22), -- 超级管理员拥有所有菜单权限
 (2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 7), (2, 9), (2, 10), (2, 12), (2, 13), (2, 15) , (2, 16), (2, 18), (2, 19), (2, 20), (2, 21),  -- 管理员
 (3, 1), (3, 2), (3, 3), (3, 4), (3, 5), (3, 18), (3, 19)  -- 普通用户
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 插入资源数据
INSERT INTO tb_rbac_resources (id, created_at, updated_at, path, method, code, description, name, type, parent_id, status) VALUES
 (1,NOW(), NOW(),'','','','','用户管理','DIRECTORY',0,1),
 (2,NOW(), NOW(),'/api/v1/auth/users','GET','System:User:Read','','查询用户','API',1,1),
 (3,NOW(), NOW(),'/api/v1/auth/users','POST','System:User:Create','','创建用户','API',1,1),
 (4,NOW(), NOW(),'/api/v1/auth/users/{id}','PUT','System:User:Update','','更新用户','API',1,1),
 (5,NOW(), NOW(),'/api/v1/auth/users/{id}','DELETE','System:User:Delete','','删除用户','API',1,1),
 (6,NOW(), NOW(),'','','','','角色管理','DIRECTORY',0,1),
 (7,NOW(), NOW(),'/api/v1/rbac/roles','GET','System:Role:Read','','查询角色','API',6,1),
 (8,NOW(), NOW(),'/api/v1/rbac/roles','POST','System:Role:Create','','创建角色','API',6,1),
 (9,NOW(), NOW(),'/api/v1/rbac/roles/{id}','PUT','System:Role:Update','','更新角色','API',6,1),
 (10,NOW(), NOW(),'/api/v1/rbac/roles/{id}','DELETE','System:Role:Delete','','删除角色','API',6,1),
 (11,NOW(), NOW(),'','','','','菜单管理','DIRECTORY',0,1),
 (12,NOW(), NOW(),'/api/v1/rbac/menus','GET','System:Menu:Read','','查询菜单','API',11,1),
 (13,NOW(), NOW(),'/api/v1/rbac/menus','POST','System:Menu:Create','','创建菜单','API',11,1),
 (14,NOW(), NOW(),'/api/v1/rbac/menus/{id}','PUT','System:Menu:Update','','更新菜单','API',11,1),
 (15,NOW(), NOW(),'/api/v1/rbac/menus/{id}','DELETE','System:Menu:Delete','','删除菜单','API',11,1),
 (16,NOW(), NOW(),'','','','','接口管理','DIRECTORY',0,1),
 (17,NOW(), NOW(),'/api/v1/rbac/resources','GET','System:Resource:Read','','查询接口','API',16,1),
 (18,NOW(), NOW(),'/api/v1/rbac/resources','POST','System:Resource:Create','','创建接口','API',16,1),
 (19,NOW(), NOW(),'/api/v1/rbac/resources/{id}','PUT','System:Resource:Update','','更新接口','API',16,1),
 (20,NOW(), NOW(),'/api/v1/rbac/resources/{id}','DELETE','System:Resource:Delete','','删除接口','API',16,1),
 (21,NOW(), NOW(),'','','','','部门管理','DIRECTORY',0,1),
 (22,NOW(), NOW(),'/api/v1/rbac/departs','GET','System:Depart:Read','','查询接口','API',21,1),
 (23,NOW(), NOW(),'/api/v1/rbac/departs','POST','System:Depart:Create','','创建接口','API',21,1),
 (24,NOW(), NOW(),'/api/v1/rbac/departs/{id}','PUT','System:Depart:Update','','更新接口','API',21,1),
 (25,NOW(), NOW(),'/api/v1/rbac/departs/{id}','DELETE','System:Depart:Delete','','删除接口','API',21,1)
ON CONFLICT (id) DO NOTHING;

-- 插入角色资源关联数据
INSERT INTO tb_rbac_role_resources (role_id, resource_id) VALUES
  (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 16), (1, 17), (1, 18), (1, 19), (1, 20), (1, 21), (1, 22), (1, 23), (1, 24), (1, 25), -- 超级管理员拥有所有资源权限
  (2, 1), (2, 2), (2, 3), (2, 4), (2, 6), (2, 7), (2, 8), (2, 9), (2, 11), (2, 12), (2, 13), (2, 14), (2, 16), (2, 17), (2, 18), (2, 19), (2, 21), (2, 22), (2, 23), (2, 24), -- 管理员拥有部分资源权限
  (3, 1), (3, 2), (3, 6), (3, 7), (3, 11), (3, 12), (3, 16), (3, 17), (3, 21), (3, 22) -- 普通用户只拥有查询权限
ON CONFLICT (role_id, resource_id) DO NOTHING;
