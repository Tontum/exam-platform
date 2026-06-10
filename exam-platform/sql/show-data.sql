SET NAMES utf8mb4;
USE exam_platform;

SELECT '=== 用户表 ===' as info;
SELECT id, username, real_name, role, school FROM user LIMIT 5;

SELECT '=== 项目表 ===' as info;
SELECT id, name, status FROM project;

SELECT '=== 项目-用户关系 ===' as info;
SELECT project_id, user_id FROM project_user LIMIT 10;

SELECT '=== 配置表 ===' as info;
SELECT project_id, tool_id, role, is_enabled FROM config LIMIT 10;
