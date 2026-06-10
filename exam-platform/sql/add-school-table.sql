SET NAMES utf8mb4;

-- =================================================
-- 学校表 — 管理全国学校信息，支持层级查询
-- 层级：省 → 市 → 县 → 学校
-- =================================================

CREATE TABLE IF NOT EXISTS `school` (
  `id`         BIGINT       NOT NULL COMMENT '主键（雪花ID）',
  `name`       VARCHAR(100) NOT NULL COMMENT '学校名称',
  `province`   VARCHAR(50)  NOT NULL COMMENT '所属省',
  `city`       VARCHAR(50)  NOT NULL COMMENT '所属市',
  `county`     VARCHAR(50)  DEFAULT NULL COMMENT '所属县/区',
  `address`    VARCHAR(200) DEFAULT NULL COMMENT '详细地址',
  `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0=禁用、1=启用',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_province` (`province`),
  KEY `idx_city` (`city`),
  KEY `idx_county` (`county`),
  UNIQUE KEY `uk_school` (`name`, `province`, `city`, `county`) COMMENT '同区域内学校名唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学校表';

-- =================================================
-- 插入种子数据：河南省部分学校
-- =================================================

INSERT INTO `school` (`id`, `name`, `province`, `city`, `county`) VALUES
-- 郑州市
(1, '郑州一中', '河南省', '郑州市', '金水区'),
(2, '郑州二中', '河南省', '郑州市', '中原区'),
(3, '郑州三中', '河南省', '郑州市', '二七区'),
(4, '郑州外国语学校', '河南省', '郑州市', '高新区'),
(5, '河南省实验中学', '河南省', '郑州市', '金水区'),

-- 洛阳市
(6, '洛阳一高', '河南省', '洛阳市', '洛龙区'),
(7, '洛阳二高', '河南省', '洛阳市', '西工区'),
(8, '洛阳理工学院附属中学', '河南省', '洛阳市', '洛龙区'),

-- 开封市
(9, '开封高中', '河南省', '开封市', '龙亭区'),
(10, '开封一中', '河南省', '开封市', '鼓楼区'),

-- 新乡市
(11, '新乡一中', '河南省', '新乡市', '红旗区'),
(12, '新乡二中', '河南省', '新乡市', '卫滨区'),

-- 安阳市
(13, '安阳一中', '河南省', '安阳市', '文峰区'),
(14, '安阳二中', '河南省', '安阳市', '北关区'),

-- 许昌市
(15, '许昌高中', '河南省', '许昌市', '魏都区'),
(16, '许昌一中', '河南省', '许昌市', '建安区'),

-- 南阳市
(17, '南阳一中', '河南省', '南阳市', '卧龙区'),
(18, '南阳二中', '河南省', '南阳市', '宛城区'),

-- 信阳市
(19, '信阳高中', '河南省', '信阳市', '浉河区'),
(20, '信阳一中', '河南省', '信阳市', '平桥区');

-- =================================================
-- 修改 user 表：添加 school_id 字段
-- =================================================

ALTER TABLE `user` ADD COLUMN `school_id` BIGINT DEFAULT NULL COMMENT '学校ID' AFTER `school`;
ALTER TABLE `user` ADD KEY `idx_school_id` (`school_id`);

-- =================================================
-- 更新现有用户的 school_id
-- =================================================

UPDATE `user` SET `school_id` = 1 WHERE `school` = '郑州一中';
UPDATE `user` SET `school_id` = 2 WHERE `school` = '郑州二中';
UPDATE `user` SET `school_id` = 6 WHERE `school` = '洛阳一高';
UPDATE `user` SET `school_id` = 9 WHERE `school` = '开封高中';
UPDATE `user` SET `school_id` = 11 WHERE `school` = '新乡一中';
UPDATE `user` SET `school_id` = 13 WHERE `school` = '安阳一中';
UPDATE `user` SET `school_id` = 10 WHERE `school` = '开封一中';

-- 验证结果
SELECT '=== 学校表 ===' as info;
SELECT id, name, province, city, county FROM school LIMIT 10;

SELECT '=== 用户关联 ===' as info;
SELECT u.id, u.real_name, u.school, s.name as school_name 
FROM `user` u 
LEFT JOIN school s ON u.school_id = s.id 
WHERE u.school_id IS NOT NULL
LIMIT 10;
