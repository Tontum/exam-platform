SET NAMES utf8mb4;

-- 为项目 1 补全缺失的配置
INSERT INTO config (id, project_id, tool_id, role, is_enabled, allow_publish, allow_delete, allow_review, require_pass_score, auto_score, score_per_submit)
SELECT 
  (@row_num := @row_num + 1) + 3000 as id,
  1 as project_id,
  t.id as tool_id,
  r.role,
  1 as is_enabled,
  CASE WHEN r.role = 2 THEN 1 ELSE 0 END as allow_publish,
  CASE WHEN r.role = 2 THEN 1 ELSE 0 END as allow_delete,
  CASE WHEN r.role = 2 THEN 1 ELSE 0 END as allow_review,
  0 as require_pass_score,
  0 as auto_score,
  0 as score_per_submit
FROM tool t
CROSS JOIN (SELECT 1 as role UNION SELECT 2 UNION SELECT 3) r
CROSS JOIN (SELECT @row_num := 0) init
WHERE NOT EXISTS (
  SELECT 1 FROM config c 
  WHERE c.project_id = 1 AND c.tool_id = t.id AND c.role = r.role
);

-- 验证结果
SELECT project_id, COUNT(*) as cnt FROM config GROUP BY project_id ORDER BY project_id;
