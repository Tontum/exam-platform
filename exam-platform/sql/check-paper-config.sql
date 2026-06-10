SET NAMES utf8mb4;
SELECT c.project_id, c.tool_id, c.role, c.is_enabled 
FROM config c 
WHERE c.project_id IN (
  SELECT pu.project_id 
  FROM project_user pu 
  WHERE pu.user_id IN (SELECT id FROM user WHERE role = 2)
)
AND c.tool_id = 1
AND c.role = 2;
