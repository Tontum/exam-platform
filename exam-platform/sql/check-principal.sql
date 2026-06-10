SET NAMES utf8mb4;
SELECT u.id, u.username, u.real_name, u.role, u.school_id 
FROM user u 
WHERE u.role = 2 AND u.status = 1;

SELECT pu.project_id, p.name, p.status 
FROM project_user pu 
JOIN project p ON pu.project_id = p.id 
WHERE pu.user_id IN (SELECT id FROM user WHERE role = 2);
