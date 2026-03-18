-- 修正菜单 title 错误（根据前端翻译文件 en-US 规范）

-- ID 65: ops.docker.inspect → ops.docker.containerInspect
UPDATE tb_rbac_menus SET title = 'ops.docker.containerInspect'
WHERE id = 65 AND title = 'ops.docker.inspect';

-- ID 71: ops.stack.delete → ops.stack.title
UPDATE tb_rbac_menus SET title = 'ops.stack.title'
WHERE id = 71 AND title IN ('ops.stack.delete', 'ops.stack.deleteStack');

-- ID 73: ops.docker.bulkOperate → ops.docker.bulkOps.title
UPDATE tb_rbac_menus SET title = 'ops.docker.bulkOps.title'
WHERE id = 73 AND title = 'ops.docker.bulkOperate';

-- ID 74: ops.docker.diff → ops.docker.diff.title
UPDATE tb_rbac_menus SET title = 'ops.docker.diff.title'
WHERE id = 74 AND title = 'ops.docker.diff';

-- ID 76: ops.docker.fileBrowse → ops.docker.fileBrowser.title
UPDATE tb_rbac_menus SET title = 'ops.docker.fileBrowser.title'
WHERE id = 76 AND title = 'ops.docker.fileBrowse';

-- ID 78: ops.docker.updateResources → ops.docker.resourceUpdate.title
UPDATE tb_rbac_menus SET title = 'ops.docker.resourceUpdate.title'
WHERE id = 78 AND title = 'ops.docker.updateResources';

-- ID 79: ops.network.connect → ops.network.connectContainer
UPDATE tb_rbac_menus SET title = 'ops.network.connectContainer'
WHERE id = 79 AND title = 'ops.network.connect';

-- ID 82: ops.envType.title → ops.management.envTypes
UPDATE tb_rbac_menus SET title = 'ops.management.envTypes'
WHERE id = 82 AND title IN ('ops.envType.title', 'ops.management.envType');
