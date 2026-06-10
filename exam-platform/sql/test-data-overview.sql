SET NAMES utf8mb4;
-- 测试数据概览
SELECT '=== 用户列表 ===' as info;
SELECT id, real_name, role, school FROM user ORDER BY id;

SELECT '=== 项目列表 ===' as info;
SELECT id, name, status FROM project ORDER BY id;

SELECT '=== 项目-用户关系 ===' as info;
SELECT pu.project_id, p.name as project_name, pu.user_id, u.real_name, u.role
FROM project_user pu
JOIN project p ON pu.project_id = p.id
JOIN user u ON pu.user_id = u.id
ORDER BY pu.project_id, pu.user_id;
