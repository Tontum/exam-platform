SET NAMES utf8mb4;
DELETE FROM config WHERE project_id NOT IN (SELECT id FROM project);
SELECT project_id, COUNT(*) as cnt FROM config GROUP BY project_id ORDER BY project_id;
