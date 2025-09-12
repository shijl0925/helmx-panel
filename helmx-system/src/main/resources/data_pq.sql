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