SET NAMES utf8mb4;
SELECT pu.project_id, p.name, p.deleted 
FROM project_user pu 
JOIN project p ON pu.project_id = p.id 
WHERE pu.user_id = 3;
