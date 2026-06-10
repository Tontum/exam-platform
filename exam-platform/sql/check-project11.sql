SET NAMES utf8mb4;

-- 检查项目 11 的试卷工具配置
SELECT '=== 项目 11 的配置 ===' as info;
SELECT c.tool_id, c.role, c.is_enabled 
FROM config c 
WHERE c.project_id = 2063262276926246914;
