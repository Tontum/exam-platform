SET NAMES utf8mb4;
ALTER TABLE project ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除、1=已删除' AFTER `updated_at`;
SELECT id, name, deleted FROM project;
