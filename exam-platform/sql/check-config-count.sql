SET NAMES utf8mb4;
SELECT project_id, COUNT(*) as cnt FROM config GROUP BY project_id ORDER BY project_id;
