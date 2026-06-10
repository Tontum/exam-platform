SET NAMES utf8mb4;

-- =================================================
-- 清理所有测试数据，保留表结构
-- =================================================

-- 先删除有外键依赖的数据
DELETE FROM answer;
DELETE FROM response;
DELETE FROM `option`;
DELETE FROM question;
DELETE FROM paper;
DELETE FROM config;
DELETE FROM permission;
DELETE FROM project_user;

-- 删除业务数据
DELETE FROM project;
DELETE FROM `user`;
DELETE FROM school;
DELETE FROM tool;

SELECT '清理完成' as status;
