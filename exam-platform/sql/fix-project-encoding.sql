SET NAMES utf8mb4;
UPDATE project SET name = '2025年度河南省教师素质提升培训', description = '全省中小学教师在线考核，涵盖教育理论、学科知识、教学技能三大模块' WHERE id = 1;
UPDATE project SET name = '郑州市春季学期第二阶段考核', description = '市级教师考核，重点考察新课程标准的实施能力' WHERE id = 2;
UPDATE project SET name = '2024年度教师年终考核', description = '年终综合考核，已结束' WHERE id = 3;
SELECT id, name, description FROM project;
