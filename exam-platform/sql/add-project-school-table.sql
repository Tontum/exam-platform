SET NAMES utf8mb4;

-- =================================================
-- 项目-学校关联表
-- 记录项目覆盖的学校，管理员创建项目时选择
-- =================================================

CREATE TABLE IF NOT EXISTS `project_school` (
  `id`         BIGINT   NOT NULL COMMENT '主键（雪花ID）',
  `project_id` BIGINT   NOT NULL COMMENT '项目 ID',
  `school_id`  BIGINT   NOT NULL COMMENT '学校 ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_school` (`project_id`, `school_id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_school_id` (`school_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目-学校关联表';

-- =================================================
-- 修改 project 表：添加项目类型字段
-- type: 1=省级项目（管理员创建）、2=校级项目（校长创建）
-- =================================================

ALTER TABLE `project` ADD COLUMN `type` TINYINT NOT NULL DEFAULT 1 COMMENT '项目类型：1=省级项目、2=校级项目' AFTER `status`;
ALTER TABLE `project` ADD COLUMN `school_id` BIGINT DEFAULT NULL COMMENT '校级项目所属学校ID' AFTER `type`;

SELECT '表创建完成' as status;
