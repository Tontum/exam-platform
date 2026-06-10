SET NAMES utf8mb4;

-- 检查 principal2 的项目
SELECT '=== principal2 的项目 ===' as info;
SELECT pu.project_id, p.name, p.status 
FROM project_user pu 
JOIN project p ON pu.project_id = p.id 
WHERE pu.user_id = 2063260889874108417;

-- 检查试卷工具配置
SELECT '=== 试卷工具配置 ===' as info;
SELECT c.project_id, c.role, c.is_enabled 
FROM config c 
WHERE c.tool_id = 1 
AND c.project_id IN (
  SELECT pu.project_id FROM project_user pu WHERE pu.user_id = 2063260889874108417
);
