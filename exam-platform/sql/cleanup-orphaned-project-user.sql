SET NAMES utf8mb4;
DELETE FROM project_user WHERE project_id NOT IN (SELECT id FROM project);
SELECT * FROM project_user;
