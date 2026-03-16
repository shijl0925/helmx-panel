-- 插入角色数据
INSERT INTO tb_rbac_roles (id, name, remark, status, code, created_at, updated_at)
SELECT id, name, remark, status, code, NOW(), NOW()
FROM (
    SELECT 1 AS id, 'Super' AS name, 'Super Admin' AS remark, 1 AS status, 'super' AS code
    UNION ALL SELECT 2, 'Admin', 'Admin', 1, 'admin'
    UNION ALL SELECT 3, 'User', 'User', 1, 'user'
) tmp
WHERE NOT EXISTS (SELECT 1 FROM tb_rbac_roles LIMIT 1);


-- 插入用户数据
INSERT INTO tb_users (id, username, password, nickname, phone, email, status, is_super_admin, created_at, updated_at)
SELECT id, username, password, nickname, phone, email, status, is_super_admin, NOW(), NOW()
FROM (
    SELECT 1 AS id, 'vben' AS username, '$2a$10$UfnzZR64s0aPu8tmsFYTpei9wGEyCe7j3uPaYIIlvLcl/l/TJh3fG' AS password, 'Vben' AS nickname, '' AS phone, 'vben@example.com' AS email, 1 AS status, TRUE AS is_super_admin
    UNION ALL SELECT 2, 'admin', '$2a$10$.DFCpdT4GgnChpdExeoxCe1ufZA4BfZxJh.op6J5icfwhyVAnNTiu', 'Admin', '', 'admin@example.com', 1, FALSE
    UNION ALL SELECT 3, 'jack', '$2a$10$N5pQz75dYqLiF8aedOvR7eNtB5XFj7xYsLC0bQrFyeaHXpnZqXoEC', 'Jack', '', 'jack@example.com', 1, FALSE
) tmp
WHERE NOT EXISTS (SELECT 1 FROM tb_users LIMIT 1);

-- 插入用户角色关联数据
INSERT INTO tb_rbac_user_roles (user_id, role_id)
SELECT user_id, role_id
FROM (
    SELECT 1 AS user_id, 1 AS role_id
    UNION ALL SELECT 2, 2
    UNION ALL SELECT 3, 3
) tmp
WHERE NOT EXISTS (SELECT 1 FROM tb_rbac_user_roles LIMIT 1);

-- 插入菜单数据
INSERT INTO tb_rbac_menus (id, created_at, updated_at, type, title, path, icon, parent_id, component, name, status, sort, auth_code)
SELECT id, NOW(), NOW(), type, title, path, icon, parent_id, component, name, status, sort, auth_code
FROM (
  -- 系统菜单
  SELECT 1 AS id, 'catalog' AS type, 'system.title' AS title, '/system' AS path, 'icon-park-outline:config' AS icon, 0 AS parent_id, '' AS component, 'System' AS name, 1 AS status, 0 AS sort, '' AS auth_code
  UNION ALL SELECT 2, 'menu', 'system.user.title', '/system/user', 'icon-park-outline:user', 1, 'system/user/list', 'SystemUser', 1, 0, 'System:User:List'
  UNION ALL SELECT 3, 'menu', 'system.role.title', '/system/role', 'icon-park-outline:every-user', 1, 'system/role/list', 'SystemRole', 1, 1, 'System:Role:List'
  UNION ALL SELECT 4, 'menu', 'system.menu.title', '/system/menu', 'icon-park-outline:hamburger-button', 1, 'system/menu/list', 'SystemMenu', 1, 2, 'System:Menu:List'
  UNION ALL SELECT 6, 'button', 'common.create', '', '', 3, '', 'SystemRoleCreate', 1, 0, 'System:Role:Create'
  UNION ALL SELECT 7, 'button', 'common.edit', '', '', 3, '', 'SystemRoleEdit', 1, 0, 'System:Role:Edit'
  UNION ALL SELECT 8, 'button', 'common.delete', '', '', 3, '', 'SystemRoleDelete', 1, 0, 'System:Role:Delete'
  UNION ALL SELECT 9, 'button', 'common.create', '', '', 4, '', 'SystemMenuCreate', 1, 0, 'System:Menu:Create'
  UNION ALL SELECT 10, 'button', 'common.edit', '', '', 4, '', 'SystemMenuEdit', 1, 0, 'System:Menu:Edit'
  UNION ALL SELECT 11, 'button', 'common.delete', '', '', 4, '', 'SystemMenuDelete', 1, 0, 'System:Menu:Delete'
  UNION ALL SELECT 12, 'button', 'common.create', '', '', 2, '', 'SystemUserCreate', 1, 0, 'System:User:Create'
  UNION ALL SELECT 13, 'button', 'common.edit', '', '', 2, '', 'SystemUserEdit', 1, 0, 'System:User:Edit'
  UNION ALL SELECT 14, 'button', 'common.delete', '', '', 2, '', 'SystemUserDelete', 1, 0, 'System:User:Delete'
  -- 添加Docker菜单
  UNION ALL SELECT 23, 'menu', 'ops.overview.title', '/ops/overview', NULL, NULL, '/ops/overview/index', 'DockerOverview', 1, NULL, 'Ops:DockerEnv:List'
  UNION ALL SELECT 24, 'catalog', 'ops.title', '/ops', NULL, NULL, NULL, 'Docker', 1, NULL, ''
  UNION ALL SELECT 25, 'menu', 'ops.docker.title', '/ops/containers', NULL, 24, '/ops/container/list', 'DockerContainer', 1, NULL, 'Ops:Container:List'
  UNION ALL SELECT 26, 'menu', 'ops.image.title', '/ops/images', NULL, 24, '/ops/image/list', 'DockerImage', 1, NULL, 'Ops:Image:List'
  UNION ALL SELECT 27, 'menu', 'ops.volume.title', '/ops/volumes', NULL, 24, '/ops/volume/list', 'DockerVolume', 1, NULL, 'Ops:Volume:List'
  UNION ALL SELECT 28, 'menu', 'ops.network.title', '/ops/networks', NULL, 24, '/ops/network/list', 'DockerNetworks', 1, NULL, 'Ops:Network:List'
  UNION ALL SELECT 29, 'menu', 'ops.management.registry', '/ops/registries', NULL, 31, '/ops/registry/index', 'DockerRegistry', 1, NULL, 'Ops:Registry:List'
  UNION ALL SELECT 30, 'menu', 'ops.management.template', '/ops/templates', NULL, 31, '/ops/template/index', 'DockerTemplate', 1, NULL, 'Ops:Template:List'
  UNION ALL SELECT 31, 'menu', 'ops.management.title', '/ops/settings', NULL, 24, '/ops/settings/index', 'DockerSettings', 1, NULL, NULL
  -- Docker 容器操作按钮
  UNION ALL SELECT 32, 'button', 'ops.docker.createContainer', '', NULL, 25, NULL, 'DockerContainerCreate', 1, NULL, 'Ops:Container:Create'
  UNION ALL SELECT 33, 'button', 'ops.docker.updateContainer', '', NULL, 25, NULL, 'DockerContainerUpdate', 1, NULL, 'Ops:Container:Edit'
  UNION ALL SELECT 34, 'button', 'ops.docker.commitContainer', '', NULL, 25, NULL, 'DockerContainerCommit', 1, NULL, 'Ops:Container:Commit'
--   UNION ALL SELECT 35, 'button', 'ops.docker.removeContainer', '', NULL, 25, NULL, 'DockerContainerDelete', 1, NULL, 'Ops:Container:Delete'
  UNION ALL SELECT 36, 'button', 'ops.docker.copyFile', '', NULL, 25, NULL, 'DockerContainerDownload', 1, NULL, 'Ops:Container:Download'
  UNION ALL SELECT 37, 'button', 'ops.docker.console', '', NULL, 25, NULL, 'DockerContainerConsole', 1, NULL, 'Ops:Container:Exec'
  UNION ALL SELECT 38, 'button', 'ops.docker.logs', '', NULL, 25, NULL, 'DockerContainerLogs', 1, NULL, 'Ops:Container:Logs'
  UNION ALL SELECT 39, 'button', 'ops.docker.operation', '', NULL, 25, NULL, 'DockerContainerOperate', 1, NULL, 'Ops:Container:Operate'
  UNION ALL SELECT 40, 'button', 'ops.docker.pruneContainers', '', NULL, 25, NULL, 'DockerContainerPrune', 1, NULL, 'Ops:Container:Prune'
  UNION ALL SELECT 41, 'button', 'ops.docker.containerStats', '', NULL, 25, NULL, 'DockerContainerStats', 1, NULL, 'Ops:Container:Stats'
  UNION ALL SELECT 42, 'button', 'ops.docker.uploadFile', '', NULL, 25, NULL, 'DockerContainerUpload', 1, NULL, 'Ops:Container:Upload'
  UNION ALL SELECT 65, 'button', 'ops.docker.inspect', '', NULL, 25, NULL, 'DockerContainerInspect', 1, NULL, 'Ops:Container:Inspect'
  -- Docker 镜像操作按钮
  UNION ALL SELECT 43, 'button', 'ops.image.buildImage', '', NULL, 26, NULL, 'DockerImageBuild', 1, NULL, 'Ops:Image:Build'
  UNION ALL SELECT 44, 'button', 'ops.image.pullImage', '', NULL, 26, NULL, 'DockerImagePull', 1, NULL, 'Ops:Image:Pull'
  UNION ALL SELECT 45, 'button', 'ops.image.pushImage', '', NULL, 26, NULL, 'DockerImagePush', 1, NULL, 'Ops:Image:Push'
  UNION ALL SELECT 46, 'button', 'ops.image.deleteImage', '', NULL, 26, NULL, 'DockerImageDelete', 1, NULL, 'Ops:Image:Delete'
  UNION ALL SELECT 47, 'button', 'ops.image.pruneImages', '', NULL, 26, NULL, 'DockerImagePrune', 1, NULL, 'Ops:Image:Prune'
  UNION ALL SELECT 48, 'button', 'ops.image.tagImage', '', NULL, 26, NULL, 'DockerImageTag', 1, NULL, 'Ops:Image:Tag'
  UNION ALL SELECT 66, 'button', 'ops.image.importImage', '', NULL, 26, NULL, 'DockerImageImport', 1, NULL, 'Ops:Image:Import'
  UNION ALL SELECT 67, 'button', 'ops.image.exportImage', '', NULL, 26, NULL, 'DockerImageExport', 1, NULL, 'Ops:Image:Export'
  -- Docker 卷操作按钮
  UNION ALL SELECT 49, 'button', 'ops.volume.createVolume', '', NULL, 27, NULL, 'DockerVolumeCreate', 1, NULL, 'Ops:Volume:Create'
  UNION ALL SELECT 50, 'button', 'ops.volume.pruneVolumes', '', NULL, 27, NULL, 'DockerVolumePrune', 1, NULL, 'Ops:Volume:Prune'
  UNION ALL SELECT 51, 'button', 'ops.volume.deleteVolume', '', NULL, 27, NULL, 'DockerVolumeDelete', 1, NULL, 'Ops:Volume:Delete'
  -- Docker 网络操作按钮
  UNION ALL SELECT 52, 'button', 'ops.network.createNetwork', '', NULL, 28, NULL, 'DockerNetworkCreate', 1, NULL, 'Ops:Network:Create'
  UNION ALL SELECT 53, 'button', 'ops.network.deleteNetwork', '', NULL, 28, NULL, 'DockerNetworkDelete', 1, NULL, 'Ops:Network:Delete'
  UNION ALL SELECT 54, 'button', 'ops.network.pruneNetworks', '', NULL, 28, NULL, 'DockerNetworkPrune', 1, NULL, 'Ops:Network:Prune'
  UNION ALL SELECT 55, 'button', 'ops.network.disconnect', '', NULL, 28, NULL, 'DockerNetworkDisconnect', 1, NULL, 'Ops:Network:Disconnect'
  -- Docker 注册表操作按钮
  UNION ALL SELECT 56, 'button', 'ops.registry.create', '', NULL, 29, NULL, 'DockerRegistryCreate', 1, NULL, 'Ops:Registry:Create'
  UNION ALL SELECT 57, 'button', 'ops.registry.edit', '', NULL, 29, NULL, 'DockerRegistryEdit', 1, NULL, 'Ops:Registry:Edit'
  UNION ALL SELECT 58, 'button', 'ops.registry.deleteRegistry', '', NULL, 29, NULL, 'DockerRegistryDelete', 1, NULL, 'Ops:Registry:Delete'
  -- Docker 模板操作按钮
  UNION ALL SELECT 59, 'button', 'ops.template.createFormTitle', '', NULL, 30, NULL, 'DockerTemplateCreate', 1, NULL, 'Ops:Template:Create'
  UNION ALL SELECT 60, 'button', 'ops.template.editFormTitle', '', NULL, 30, NULL, 'DockerTemplateEdit', 1, NULL, 'Ops:Template:Edit'
  UNION ALL SELECT 61, 'button', 'ops.template.deleteTemplate', '', NULL, 30, NULL, 'DockerTemplateDelete', 1, NULL, 'Ops:Template:Delete'
  -- Docker 环境操作按钮
  UNION ALL SELECT 62, 'button', 'ops.host.add', '', NULL, 23, NULL, 'DockerEnvCreate', 1, NULL, 'Ops:DockerEnv:Create'
  UNION ALL SELECT 63, 'button', 'ops.host.edit', '', NULL, 23, NULL, 'DockerEnvEdit', 1, NULL, 'Ops:DockerEnv:Edit'
  UNION ALL SELECT 64, 'button', 'ops.host.delete', '', NULL, 23, NULL, 'DockerEnvDelete', 1, NULL, 'Ops:DockerEnv:Delete'
) tmp
WHERE NOT EXISTS (SELECT 1 FROM tb_rbac_menus LIMIT 1);

-- 插入角色菜单关联数据
INSERT INTO tb_rbac_role_menus (role_id, menu_id)
SELECT role_id, menu_id
FROM (
  SELECT 1 AS role_id, 1 AS menu_id UNION ALL SELECT 1,2 UNION ALL SELECT 1,3 UNION ALL SELECT 1,4 UNION ALL SELECT 1,6 UNION ALL SELECT 1,7 UNION ALL SELECT 1,8 UNION ALL SELECT 1,9 UNION ALL SELECT 1,10 UNION ALL SELECT 1,11 UNION ALL SELECT 1,12 UNION ALL SELECT 1,13 UNION ALL SELECT 1,14 UNION ALL SELECT 1,23 UNION ALL SELECT 1,24 UNION ALL SELECT 1,25 UNION ALL SELECT 1,26 UNION ALL SELECT 1,27 UNION ALL SELECT 1,28 UNION ALL SELECT 1,29 UNION ALL SELECT 1,30 UNION ALL SELECT 1,31 UNION ALL SELECT 1,32 UNION ALL SELECT 1,33 UNION ALL SELECT 1,34 UNION ALL SELECT 1,35 UNION ALL SELECT 1,36 UNION ALL SELECT 1,37 UNION ALL SELECT 1,38 UNION ALL SELECT 1,39 UNION ALL SELECT 1,40 UNION ALL SELECT 1,41 UNION ALL SELECT 1,42 UNION ALL SELECT 1,43 UNION ALL SELECT 1,44 UNION ALL SELECT 1,45 UNION ALL SELECT 1,46 UNION ALL SELECT 1,47 UNION ALL SELECT 1,48 UNION ALL SELECT 1,49 UNION ALL SELECT 1,50 UNION ALL SELECT 1,51 UNION ALL SELECT 1,52 UNION ALL SELECT 1,53 UNION ALL SELECT 1,54 UNION ALL SELECT 1,55 UNION ALL SELECT 1,56 UNION ALL SELECT 1,57 UNION ALL SELECT 1,58 UNION ALL SELECT 1,59 UNION ALL SELECT 1,60 UNION ALL SELECT 1,61 UNION ALL SELECT 1,62 UNION ALL SELECT 1,63 UNION ALL SELECT 1,64 UNION ALL SELECT 1,65 UNION ALL SELECT 1,66 UNION ALL SELECT 1,67 -- 超级管理员拥有所有菜单权限
  UNION ALL SELECT 2,1 UNION ALL SELECT 2,2 UNION ALL SELECT 2,3 UNION ALL SELECT 2,4 UNION ALL SELECT 2,6 UNION ALL SELECT 2,7 UNION ALL SELECT 2,9 UNION ALL SELECT 2,10 UNION ALL SELECT 2,12 UNION ALL SELECT 2,13 UNION ALL SELECT 2,23 UNION ALL SELECT 2,24 UNION ALL SELECT 2,25 UNION ALL SELECT 2,26 UNION ALL SELECT 2,27 UNION ALL SELECT 2,28 UNION ALL SELECT 2,32 UNION ALL SELECT 2,33 UNION ALL SELECT 2,34 UNION ALL SELECT 2,35 UNION ALL SELECT 2,36 UNION ALL SELECT 2,37 UNION ALL SELECT 2,38 UNION ALL SELECT 2,39 UNION ALL SELECT 2,40 UNION ALL SELECT 2,41 UNION ALL SELECT 2,42 UNION ALL SELECT 2,43 UNION ALL SELECT 2,44 UNION ALL SELECT 2,45 UNION ALL SELECT 2,46 UNION ALL SELECT 2,47 UNION ALL SELECT 2,48 UNION ALL SELECT 2,49 UNION ALL SELECT 2,50 UNION ALL SELECT 2,51 UNION ALL SELECT 2,52 UNION ALL SELECT 2,53 UNION ALL SELECT 2,54 UNION ALL SELECT 2,55 -- 管理员
  UNION ALL SELECT 3,1 UNION ALL SELECT 3,2 UNION ALL SELECT 3,3 UNION ALL SELECT 3,4 UNION ALL SELECT 3,23 UNION ALL SELECT 3,24 UNION ALL SELECT 3,25 UNION ALL SELECT 3,26 UNION ALL SELECT 3,27 UNION ALL SELECT 3,28 -- 普通用户
) tmp
WHERE NOT EXISTS (SELECT 1 FROM tb_rbac_role_menus LIMIT 1);

-- 插入容器环境数据
INSERT INTO tb_docker_env (id, name, remark, host, status, created_at, updated_at)
SELECT 1, 'local', '', 'unix:///var/run/docker.sock', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tb_docker_env LIMIT 1);

-- 插入默认环境类型数据
INSERT INTO tb_env_type (id, code, remark, sort, created_at, updated_at)
SELECT id, code, remark, sort, NOW(), NOW()
FROM (
    SELECT 1 AS id, 'dev' AS code, '开发环境' AS remark, 1 AS sort
    UNION ALL SELECT 2, 'test', '测试环境', 2
    UNION ALL SELECT 3, 'uat',  '验收环境', 3
    UNION ALL SELECT 4, 'prod', '生产环境', 4
) tmp
WHERE NOT EXISTS (SELECT 1 FROM tb_env_type LIMIT 1);

-- 插入 DockerHub 初始化数据
INSERT INTO tb_docker_registry (id, name, url, username, password, auth, created_at, updated_at)
SELECT 1, 'DockerHub', 'https://registry.hub.docker.com', NULL, NULL, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tb_docker_registry LIMIT 1);
