-- 补充缺失的环境类型和操作日志菜单权限数据

-- 添加系统日志（操作日志）菜单及按钮
-- 添加环境类型管理菜单及按钮
INSERT INTO tb_rbac_menus (id, created_at, updated_at, type, title, path, icon, parent_id, component, name, status, sort, auth_code) VALUES
  -- 系统日志菜单（System catalog 下）
  (80,NOW(), NOW(),'menu','system.log.title','/system/log',NULL,1,'system/log/list','SystemLog',1,NULL,'System:Log:List'),
  (81,NOW(), NOW(),'button','common.delete','',NULL,80,NULL,'SystemLogDelete',1,NULL,'System:Log:Delete'),
  -- 环境类型管理菜单（DockerSettings 下）
  (82,NOW(), NOW(),'menu','ops.envType.title','/ops/env-types',NULL,31,'/ops/env-type/index','OpsEnvType',1,NULL,'Ops:EnvType:List'),
  (83,NOW(), NOW(),'button','common.create','',NULL,82,NULL,'OpsEnvTypeCreate',1,NULL,'Ops:EnvType:Create'),
  (84,NOW(), NOW(),'button','common.edit','',NULL,82,NULL,'OpsEnvTypeEdit',1,NULL,'Ops:EnvType:Edit'),
  (85,NOW(), NOW(),'button','common.delete','',NULL,82,NULL,'OpsEnvTypeDelete',1,NULL,'Ops:EnvType:Delete')
ON CONFLICT (id) DO NOTHING;

-- 更新角色菜单关联数据
INSERT INTO tb_rbac_role_menus (role_id, menu_id) VALUES
  -- 超级管理员拥有所有新增菜单权限
  (1, 80), (1, 81), (1, 82), (1, 83), (1, 84), (1, 85),
  -- 管理员拥有新增菜单权限
  (2, 80), (2, 81), (2, 82), (2, 83), (2, 84), (2, 85)
ON CONFLICT (role_id, menu_id) DO NOTHING;
