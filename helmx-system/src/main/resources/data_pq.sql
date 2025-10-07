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
INSERT INTO tb_rbac_menus (id, created_at, updated_at, type, title, path, icon, parent_id, component, name, status, sort, auth_code) VALUES
  (1,NOW(), NOW(),'catalog','system.title','/system','icon-park-outline:config',0,'','System',1,0,''),
  (2,NOW(), NOW(),'menu','system.user.title','/system/user','icon-park-outline:user',1,'system/user/list','SystemUser',1,0,'System:User:List'),
  (3,NOW(), NOW(),'menu','system.role.title','/system/role','icon-park-outline:every-user',1,'system/role/list','SystemRole',1,1,'System:Role:List'),
  (4,NOW(), NOW(),'menu','system.menu.title','/system/menu','icon-park-outline:hamburger-button',1,'system/menu/list','SystemMenu',1,2,'System:Menu:List'),
  (6,NOW(), NOW(),'button','common.create','','',3,'','SystemRoleCreate',1,0,'System:Role:Create'),
  (7,NOW(), NOW(),'button','common.edit','','',3,'','SystemRoleEdit',1,0,'System:Role:Edit'),
  (8,NOW(), NOW(),'button','common.delete','','',3,'','SystemRoleDelete',1,0,'System:Role:Delete'),
  (9,NOW(), NOW(),'button','common.create','','',4,'','SystemMenuCreate',1,0,'System:Menu:Create'),
  (10,NOW(), NOW(),'button','common.edit','','',4,'','SystemMenuEdit',1,0,'System:Menu:Edit'),
  (11,NOW(), NOW(),'button','common.delete','','',4,'','SystemMenuDelete',1,0,'System:Menu:Delete'),
  (12,NOW(), NOW(),'button','common.create','','',2,'','SystemUserCreate',1,0,'System:User:Create'),
  (13,NOW(), NOW(),'button','common.edit','','',2,'','SystemUserEdit',1,0,'System:User:Edit'),
  (14,NOW(), NOW(),'button','common.delete','','',2,'','SystemUserDelete',1,0,'System:User:Delete'),
  (18,NOW(), NOW(),'menu','system.operation.title','/system/operation','carbon:time',1,'/system/operation/list','OperationLog',1,5,'System:Operation:List'),
  (19,NOW(), NOW(),'menu','system.dept.title','/system/depart','carbon:departure',1,'system/dept/list','SystemDepart',1,4,'System:Depart:List'),
  (20,NOW(), NOW(),'button','common.create','','',19,'','SystemDepartCreate',1,0,'System:Depart:Create'),
  (21,NOW(), NOW(),'button','common.edit','','',19,'','SystemDepartEdit',1,0,'System:Depart:Edit'),
  (22,NOW(), NOW(),'button','common.delete','','',19,'','SystemDepartDelete',1,0,'System:Depart:Delete')
ON CONFLICT (id) DO NOTHING;

-- 插入角色菜单关联数据
INSERT INTO tb_rbac_role_menus (role_id, menu_id) VALUES
 (1, 1), (1, 2), (1, 3), (1, 4), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 18), (1, 19), (1, 20), (1, 21), (1, 22), -- 超级管理员拥有所有菜单权限
 (2, 1), (2, 2), (2, 3), (2, 4), (2, 6), (2, 7), (2, 9), (2, 10), (2, 12), (2, 13), (2, 18), (2, 19), (2, 20), (2, 21),  -- 管理员
 (3, 1), (3, 2), (3, 3), (3, 4), (3, 18), (3, 19)  -- 普通用户
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 插入容器环境数据
INSERT INTO tb_docker_env (id, name, remark, host, status, created_at, updated_at) VALUES
  (1, 'local', '', 'unix:///var/run/docker.sock', 1, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 插入 DockerHub 初始化数据
INSERT INTO tb_docker_registry (id, name, url, username, password, auth, created_at, updated_at) VALUES
    (1, 'DockerHub', 'https://registry.hub.docker.com', NULL, NULL, FALSE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;