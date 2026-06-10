SET NAMES utf8mb4;
UPDATE project SET name = '测试项目一', description = '测试项目描述', province = '河南省', city = '郑州市' WHERE id = 2062779682925215746;
UPDATE project SET name = '测试项目二', description = '测试自动初始化配置' WHERE id = 2062781695058669570;
SELECT id, name, description, province, city FROM project;
