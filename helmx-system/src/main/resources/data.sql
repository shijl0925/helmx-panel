-- 插入角色数据
INSERT IGNORE INTO tb_rbac_roles (id, name, remark, status, code, created_at, updated_at) VALUES
 (1, 'Super', 'Super Admin', 1, 'super', NOW(), NOW()),
 (2, 'Admin', 'Admin', 1, 'admin', NOW(), NOW()),
 (3, 'User', 'User', 1, 'user', NOW(), NOW());


-- 插入用户数据
INSERT IGNORE INTO tb_users (id, username, password, nickname, phone, email, status, is_super_admin, created_at, updated_at) VALUES
 (1, 'vben', '$2a$10$UfnzZR64s0aPu8tmsFYTpei9wGEyCe7j3uPaYIIlvLcl/l/TJh3fG', 'Vben', '', 'vben@example.com', 1, TRUE, NOW(), NOW()),
 (2, 'admin', '$2a$10$.DFCpdT4GgnChpdExeoxCe1ufZA4BfZxJh.op6J5icfwhyVAnNTiu', 'Admin', '', 'admin@example.com', 1, FALSE, NOW(), NOW()),
 (3, 'jack', '$2a$10$N5pQz75dYqLiF8aedOvR7eNtB5XFj7xYsLC0bQrFyeaHXpnZqXoEC', 'Jack', '', 'jack@example.com', 1, FALSE, NOW(), NOW());

-- 插入用户角色关联数据
INSERT IGNORE INTO tb_rbac_user_roles (user_id, role_id) VALUES
 (1, 1),
 (2, 2),
 (3, 3);

-- 插入菜单数据
INSERT IGNORE INTO tb_rbac_menus (id, created_at, updated_at, type, title, path, icon, parent_id, component, name, status, sort, auth_code) VALUES
  -- 系统菜单
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

  -- 添加Docker菜单
  (23,NOW(), NOW(),'menu','ops.overview.title','/ops/overview',NULL,NULL,'/ops/overview/index','DockerOverview',1,NULL,'Ops:DockerEnv:List'),
  (24,NOW(), NOW(),'catalog','ops.title','/ops',NULL,NULL,NULL,'Docker',1,NULL,''),
  (25,NOW(), NOW(),'menu','ops.docker.title','/ops/containers',NULL,24,'/ops/container/list','DockerContainer',1,NULL,'Ops:Container:List'),
  (26,NOW(), NOW(),'menu','ops.image.title','/ops/images',NULL,24,'/ops/image/list','DockerImage',1,NULL,'Ops:Image:List'),
  (27,NOW(), NOW(),'menu','ops.volume.title','/ops/volumes',NULL,24,'/ops/volume/list','DockerVolume',1,NULL,'Ops:Volume:List'),
  (28,NOW(), NOW(),'menu','ops.network.title','/ops/networks',NULL,24,'/ops/network/list','DockerNetworks',1,NULL,'Ops:Network:List'),
  (29,NOW(), NOW(),'menu','ops.management.registry','/ops/registries',NULL,31,'/ops/registry/index','DockerRegistry',1,NULL,'Ops:Registry:List'),
  (30,NOW(), NOW(),'menu','ops.management.template','/ops/templates',NULL,31,'/ops/template/index','DockerTemplate',1,NULL,'Ops:Template:List'),
  (31,NOW(), NOW(),'menu','ops.management.title','/ops/settings',NULL,24,'/ops/settings/index','DockerSettings',1,NULL,NULL),

  -- Docker 容器操作按钮
  (32,NOW(), NOW(),'button','ops.docker.createContainer','',NULL,25,NULL,'DockerContainerCreate',1,NULL,'Ops:Container:Create'),
  (33,NOW(), NOW(),'button','ops.docker.updateContainer','',NULL,25,NULL,'DockerContainerUpdate',1,NULL,'Ops:Container:Edit'),
  (34,NOW(), NOW(),'button','ops.docker.commitContainer','',NULL,25,NULL,'DockerContainerCommit',1,NULL,'Ops:Container:Commit'),
  (35,NOW(), NOW(),'button','ops.docker.removeContainer','',NULL,25,NULL,'DockerContainerDelete',1,NULL,'Ops:Container:Delete'),
  (36,NOW(), NOW(),'button','ops.docker.copyFile','',NULL,25,NULL,'DockerContainerDownload',1,NULL,'Ops:Container:Download'),
  (37,NOW(), NOW(),'button','ops.docker.console','',NULL,25,NULL,'DockerContainerConsole',1,NULL,'Ops:Container:Exec'),
  (38,NOW(), NOW(),'button','ops.docker.logs','',NULL,25,NULL,'DockerContainerLogs',1,NULL,'Ops:Container:Logs'),
  (39,NOW(), NOW(),'button','ops.docker.operation','',NULL,25,NULL,'DockerContainerOperate',1,NULL,'Ops:Container:Operate'),
  (40,NOW(), NOW(),'button','ops.docker.pruneContainers','',NULL,25,NULL,'DockerContainerPrune',1,NULL,'Ops:Container:Prune'),
  (41,NOW(), NOW(),'button','ops.docker.containerStats','',NULL,25,NULL,'DockerContainerStats',1,NULL,'Ops:Container:Stats'),
  (42,NOW(), NOW(),'button','ops.docker.uploadFile','',NULL,25,NULL,'DockerContainerUpload',1,NULL,'Ops:Container:Upload'),

  -- Docker 镜像操作按钮
  (43,NOW(), NOW(),'button','ops.image.buildImage','',NULL,26,NULL,'DockerImageBuild',1,NULL,'Ops:Image:Build'),
  (44,NOW(), NOW(),'button','ops.image.pullImage','',NULL,26,NULL,'DockerImagePull',1,NULL,'Ops:Image:Pull'),
  (45,NOW(), NOW(),'button','ops.image.pushImage','',NULL,26,NULL,'DockerImagePush',1,NULL,'Ops:Image:Push'),
  (46,NOW(), NOW(),'button','ops.image.deleteImage','',NULL,26,NULL,'DockerImageDelete',1,NULL,'Ops:Image:Delete'),
  (47,NOW(), NOW(),'button','ops.image.pruneImages','',NULL,26,NULL,'DockerImagePrune',1,NULL,'Ops:Image:Prune'),
  (48,NOW(), NOW(),'button','ops.image.tagImage','',NULL,26,NULL,'DockerImageTag',1,NULL,'Ops:Image:Tag'),

  -- Docker 卷操作按钮
  (49,NOW(), NOW(),'button','ops.volume.createVolume','',NULL,27,NULL,'DockerVolumeCreate',1,NULL,'Ops:Volume:Create'),
  (50,NOW(), NOW(),'button','ops.volume.pruneVolumes','',NULL,27,NULL,'DockerVolumePrune',1,NULL,'Ops:Volume:Prune'),
  (51,NOW(), NOW(),'button','ops.volume.deleteVolume','',NULL,27,NULL,'DockerVolumeDelete',1,NULL,'Ops:Volume:Delete'),

  -- Docker 网络操作按钮
  (52,NOW(), NOW(),'button','ops.network.createNetwork','',NULL,28,NULL,'DockerNetworkCreate',1,NULL,'Ops:Network:Create'),
  (53,NOW(), NOW(),'button','ops.network.deleteNetwork','',NULL,28,NULL,'DockerNetworkDelete',1,NULL,'Ops:Network:Delete'),
  (54,NOW(), NOW(),'button','ops.network.pruneNetworks','',NULL,28,NULL,'DockerNetworkPrune',1,NULL,'Ops:Network:Prune'),
  (55,NOW(), NOW(),'button','ops.network.disconnect','',NULL,28,NULL,'DockerNetworkDisconnect',1,NULL,'Ops:Network:Disconnect'),

  -- Docker 注册表操作按钮
  (56,NOW(), NOW(),'button','ops.registry.create','',NULL,29,NULL,'DockerRegistryCreate',1,NULL,'Ops:Registry:Create'),
  (57,NOW(), NOW(),'button','ops.registry.edit','',NULL,29,NULL,'DockerRegistryEdit',1,NULL,'Ops:Registry:Edit'),
  (58,NOW(), NOW(),'button','ops.registry.deleteRegistry','',NULL,29,NULL,'DockerRegistryDelete',1,NULL,'Ops:Registry:Delete'),

  -- Docker 模板操作按钮
  (59,NOW(), NOW(),'button','ops.template.createFormTitle','',NULL,30,NULL,'DockerTemplateCreate',1,NULL,'Ops:Template:Create'),
  (60,NOW(), NOW(),'button','ops.template.editFormTitle','',NULL,30,NULL,'DockerTemplateEdit',1,NULL,'Ops:Template:Edit'),
  (61,NOW(), NOW(),'button','ops.template.deleteTemplate','',NULL,30,NULL,'DockerTemplateDelete',1,NULL,'Ops:Template:Delete'),

  -- Docker 环境操作按钮
  (62,NOW(), NOW(),'button','ops.host.add','',NULL,23,NULL,'DockerEnvCreate',1,NULL,'Ops:DockerEnv:Create'),
  (63,NOW(), NOW(),'button','ops.host.edit','',NULL,23,NULL,'DockerEnvEdit',1,NULL,'Ops:DockerEnv:Edit'),
  (64,NOW(), NOW(),'button','ops.host.delete','',NULL,23,NULL,'DockerEnvDelete',1,NULL,'Ops:DockerEnv:Delete');

-- 插入角色菜单关联数据
INSERT IGNORE INTO tb_rbac_role_menus (role_id, menu_id) VALUES
 (1, 1), (1, 2), (1, 3), (1, 4), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 23), (1, 24), (1, 25), (1, 26), (1, 27), (1, 28), (1, 29), (1, 30), (1, 31), (1, 32), (1, 33), (1, 34), (1, 35), (1, 36), (1, 37), (1, 38), (1, 39), (1, 40), (1, 41), (1, 42), (1, 43), (1, 44), (1, 45), (1, 46), (1, 47), (1, 48), (1, 49), (1, 50), (1, 51), (1, 52), (1, 53), (1, 54), (1, 55), (1, 56), (1, 57), (1, 58), (1, 59), (1, 60), (1, 61), (1, 62), (1, 63), (1, 64), -- 超级管理员拥有所有菜单权限
 (2, 1), (2, 2), (2, 3), (2, 4), (2, 6), (2, 7), (2, 9), (2, 10), (2, 12), (2, 13), (2, 23), (2, 24), (2, 25), (2, 26), (2, 27), (2, 28), (2, 32), (2, 33), (2, 34), (2, 35), (2, 36), (2, 37), (2, 38), (2, 39), (2, 40), (2, 41), (2, 42), (2, 43), (2, 44), (2, 45), (2, 46), (2, 47), (2, 48), (2, 49), (2, 50), (2, 51), (2, 52), (2, 53), (2, 54), (2, 55), -- 管理员
 (3, 1), (3, 2), (3, 3), (3, 4), (3, 23), (3, 24), (3, 25), (3, 26), (3, 27), (3, 28);  -- 普通用户

-- 插入容器环境数据
INSERT IGNORE INTO tb_docker_env (id, name, remark, host, status, created_at, updated_at) VALUES
  (1, 'local', '', 'unix:///var/run/docker.sock', 1, NOW(), NOW());

-- 插入 DockerHub 初始化数据
INSERT IGNORE INTO tb_docker_registry (id, name, url, username, password, auth, created_at, updated_at) VALUES
  (1, 'DockerHub', 'https://registry.hub.docker.com', NULL, NULL, FALSE, NOW(), NOW());