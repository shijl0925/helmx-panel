-- 修正菜单 title 错误

-- 环境类型菜单应遵循管理子菜单命名规范（与 ops.management.registry / ops.management.template 一致）
UPDATE tb_rbac_menus
SET title = 'ops.management.envType'
WHERE id = 82 AND title = 'ops.envType.title';

-- Stack 删除按钮应使用复合形式（与其他删除按钮命名规范一致：deleteImage / deleteVolume / deleteNetwork / deleteRegistry / deleteTemplate）
UPDATE tb_rbac_menus
SET title = 'ops.stack.deleteStack'
WHERE id = 71 AND title = 'ops.stack.delete';
