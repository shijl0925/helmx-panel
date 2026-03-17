-- 补充缺失的菜单权限数据

-- 添加 Stack（编排管理）菜单及按钮
INSERT INTO tb_rbac_menus (id, created_at, updated_at, type, title, path, icon, parent_id, component, name, status, sort, auth_code) VALUES
  (68,NOW(), NOW(),'menu','ops.stack.title','/ops/stacks',NULL,24,'/ops/stack/list','DockerStack',1,NULL,'Ops:Stack:List'),
  (69,NOW(), NOW(),'button','ops.stack.create','',NULL,68,NULL,'DockerStackCreate',1,NULL,'Ops:Stack:Create'),
  (70,NOW(), NOW(),'button','ops.stack.edit','',NULL,68,NULL,'DockerStackEdit',1,NULL,'Ops:Stack:Edit'),
  (71,NOW(), NOW(),'button','ops.stack.delete','',NULL,68,NULL,'DockerStackDelete',1,NULL,'Ops:Stack:Delete'),
  (72,NOW(), NOW(),'button','ops.stack.deploy','',NULL,68,NULL,'DockerStackDeploy',1,NULL,'Ops:Stack:Deploy'),
  -- 补充容器操作按钮
  (73,NOW(), NOW(),'button','ops.docker.bulkOperate','',NULL,25,NULL,'DockerContainerBulkOperate',1,NULL,'Ops:Container:BulkOperate'),
  (74,NOW(), NOW(),'button','ops.docker.diff','',NULL,25,NULL,'DockerContainerDiff',1,NULL,'Ops:Container:Diff'),
  (75,NOW(), NOW(),'button','ops.docker.exportContainer','',NULL,25,NULL,'DockerContainerExport',1,NULL,'Ops:Container:Export'),
  (76,NOW(), NOW(),'button','ops.docker.fileBrowse','',NULL,25,NULL,'DockerContainerFileBrowse',1,NULL,'Ops:Container:FileBrowse'),
  (77,NOW(), NOW(),'button','ops.docker.fileEdit','',NULL,25,NULL,'DockerContainerFileEdit',1,NULL,'Ops:Container:FileEdit'),
  (78,NOW(), NOW(),'button','ops.docker.updateResources','',NULL,25,NULL,'DockerContainerUpdateResources',1,NULL,'Ops:Container:UpdateResources'),
  -- 补充网络操作按钮
  (79,NOW(), NOW(),'button','ops.network.connect','',NULL,28,NULL,'DockerNetworkConnect',1,NULL,'Ops:Network:Connect')
ON CONFLICT (id) DO NOTHING;

-- 更新角色菜单关联数据
INSERT INTO tb_rbac_role_menus (role_id, menu_id) VALUES
  -- 超级管理员拥有所有新增菜单权限
  (1, 68), (1, 69), (1, 70), (1, 71), (1, 72),
  (1, 73), (1, 74), (1, 75), (1, 76), (1, 77), (1, 78),
  (1, 79),
  -- 管理员拥有新增菜单权限
  (2, 68), (2, 69), (2, 70), (2, 71), (2, 72),
  (2, 73), (2, 74), (2, 75), (2, 76), (2, 77), (2, 78),
  (2, 79),
  -- 普通用户仅拥有 Stack 查看权限
  (3, 68)
ON CONFLICT (role_id, menu_id) DO NOTHING;
