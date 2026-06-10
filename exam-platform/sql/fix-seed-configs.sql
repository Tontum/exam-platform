SET NAMES utf8mb4;

-- 为项目 2 补全配置（当前只有 3 条，需要 24 条）
INSERT INTO config (id, project_id, tool_id, role, is_enabled, allow_publish, allow_delete, allow_review, require_pass_score, auto_score, score_per_submit)
SELECT 
  (@row_num := @row_num + 1) + 1000 as id,
  2 as project_id,
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
  WHERE c.project_id = 2 AND c.tool_id = t.id AND c.role = r.role
);

-- 为项目 3 补全配置（当前没有配置）
INSERT INTO config (id, project_id, tool_id, role, is_enabled, allow_publish, allow_delete, allow_review, require_pass_score, auto_score, score_per_submit)
SELECT 
  (@row_num := @row_num + 1) + 2000 as id,
  3 as project_id,
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
  WHERE c.project_id = 3 AND c.tool_id = t.id AND c.role = r.role
);

-- 验证结果
SELECT project_id, COUNT(*) as cnt FROM config GROUP BY project_id ORDER BY project_id;
