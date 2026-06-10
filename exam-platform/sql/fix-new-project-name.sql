SET NAMES utf8mb4;
UPDATE project SET name = '新测试项目', description = '测试自动初始化配置' WHERE id = 2062803221124407298;
SELECT id, name, description FROM project;
