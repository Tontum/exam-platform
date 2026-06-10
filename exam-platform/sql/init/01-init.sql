-- =================================================================
-- 教师培训在线考试平台 — 数据库初始化脚本
-- 基于 docs/02-database-design.md，MySQL 8.0
-- 按依赖顺序创建：先支撑表，后业务表
-- =================================================================

SET NAMES utf8mb4;
SET CHARACTER_SET_CLIENT = utf8mb4;
SET CHARACTER_SET_CONNECTION = utf8mb4;

CREATE DATABASE IF NOT EXISTS exam_platform
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE exam_platform;

-- =================================================================
-- 一、系统支撑表
-- =================================================================

-- 1. user（用户表）— 管理员、校长、老师
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`         BIGINT      NOT NULL COMMENT '主键（雪花ID）',
  `username`   VARCHAR(50) NOT NULL COMMENT '登录账号',
  `password`   VARCHAR(255) NOT NULL COMMENT '加密密码',
  `real_name`  VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `role`       TINYINT     NOT NULL DEFAULT 3 COMMENT '角色：1=管理员、2=校长、3=老师',
  `scope`      VARCHAR(20)     NULL DEFAULT NULL COMMENT '管理员权限范围：ALL=全国、PROVINCE=省级',
  `province`   VARCHAR(50)     NULL COMMENT '管理员所属省份（scope=PROVINCE时必填）',
  `phone`      VARCHAR(20)     NULL COMMENT '手机号',
  `email`      VARCHAR(100)    NULL COMMENT '邮箱',
  `school_id`  BIGINT          NULL COMMENT '学校ID（关联 school 表）',
  `status`     TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：0=禁用、1=启用',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_school_id` (`school_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. school（学校表）
DROP TABLE IF EXISTS `school`;
CREATE TABLE `school` (
  `id`         BIGINT       NOT NULL COMMENT '主键',
  `name`       VARCHAR(100) NOT NULL COMMENT '学校名称',
  `province`   VARCHAR(50)      NULL COMMENT '所属省',
  `city`       VARCHAR(50)      NULL COMMENT '所属市',
  `county`     VARCHAR(50)      NULL COMMENT '所属县/区',
  `address`    VARCHAR(200)     NULL COMMENT '详细地址',
  `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0=禁用、1=启用',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_province` (`province`, `city`, `county`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学校表';

-- 3. project（项目表）
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
  `id`          BIGINT        NOT NULL COMMENT '主键（雪花ID）',
  `name`        VARCHAR(200)  NOT NULL COMMENT '项目名称',
  `description` VARCHAR(500)      NULL COMMENT '项目描述',
  `creator_id`  BIGINT        NOT NULL COMMENT '创建人（管理员/校长）',
  `province`    VARCHAR(50)       NULL COMMENT '所属省',
  `city`        VARCHAR(50)       NULL COMMENT '所属市',
  `status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '0=未开始、1=进行中、2=已结束',
  `type`        TINYINT       NOT NULL DEFAULT 1 COMMENT '项目类型：1=省级项目、2=校级项目',
  `school_id`   BIGINT            NULL COMMENT '校级项目所属学校ID（省级项目为NULL）',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_creator` (`creator_id`),
  KEY `idx_status` (`status`),
  KEY `idx_type` (`type`),
  KEY `idx_school_id` (`school_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- 4. project_user（项目-用户关联表）
DROP TABLE IF EXISTS `project_user`;
CREATE TABLE `project_user` (
  `id`         BIGINT   NOT NULL COMMENT '主键（雪花ID）',
  `project_id` BIGINT   NOT NULL COMMENT '项目 ID',
  `user_id`    BIGINT   NOT NULL COMMENT '用户 ID',
  `joined_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_user` (`project_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目-用户关联表';

-- 4. project_school（项目-学校关联表）
DROP TABLE IF EXISTS `project_school`;
CREATE TABLE `project_school` (
  `id`         BIGINT   NOT NULL COMMENT '主键',
  `project_id` BIGINT   NOT NULL COMMENT '项目 ID',
  `school_id`  BIGINT   NOT NULL COMMENT '学校 ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_school` (`project_id`, `school_id`),
  KEY `idx_school_id` (`school_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目-学校关联表';

-- 5. tool（工具/功能模块表）
DROP TABLE IF EXISTS `tool`;
CREATE TABLE `tool` (
  `id`          BIGINT       NOT NULL COMMENT '主键',
  `tool_code`   VARCHAR(50)  NOT NULL COMMENT '工具编码（paper、article、chat、qa、homework、live、video）',
  `tool_name`   VARCHAR(50)  NOT NULL COMMENT '工具名称',
  `description` VARCHAR(200)     NULL COMMENT '工具描述',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tool_code` (`tool_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具/功能模块表';

-- 6. config（配置表）— 按项目+工具+角色维度配置规则
DROP TABLE IF EXISTS `config`;
CREATE TABLE `config` (
  `id`                  BIGINT   NOT NULL COMMENT '主键',
  `project_id`          BIGINT   NOT NULL COMMENT '项目 ID',
  `tool_id`             BIGINT   NOT NULL COMMENT '工具 ID',
  `role`                TINYINT  NOT NULL DEFAULT 1 COMMENT '角色：1=管理员、2=校长、3=老师',
  `is_enabled`          TINYINT  NOT NULL DEFAULT 0 COMMENT '是否启用该工具菜单',
  `allow_publish`       TINYINT  NOT NULL DEFAULT 0 COMMENT '是否允许发布',
  `allow_delete`        TINYINT  NOT NULL DEFAULT 0 COMMENT '是否允许删除',
  `allow_review`        TINYINT  NOT NULL DEFAULT 0 COMMENT '是否允许批阅',
  `require_pass_score`  TINYINT  NOT NULL DEFAULT 0 COMMENT '是否必须设置合格分',
  `auto_score`          TINYINT  NOT NULL DEFAULT 0 COMMENT '主观题是否自动给分',
  `publish_time_start`  TIME         NULL COMMENT '发布时间段-起始',
  `publish_time_end`    TIME         NULL COMMENT '发布时间段-截止',
  `score_per_submit`    INT      NOT NULL DEFAULT 0 COMMENT '每次提交加考核分',
  `created_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_tool_role` (`project_id`, `tool_id`, `role`),
  KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置表';

-- 7. permission（权限表）— RBAC 按钮级别
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission` (
  `id`              BIGINT       NOT NULL COMMENT '主键',
  `role`            TINYINT      NOT NULL COMMENT '角色',
  `tool_id`         BIGINT       NOT NULL COMMENT '工具 ID',
  `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码',
  `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
  PRIMARY KEY (`id`),
  KEY `idx_role_tool` (`role`, `tool_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- =================================================================
-- 二、核心业务表
-- =================================================================

-- 8. paper（试卷表）
DROP TABLE IF EXISTS `paper`;
CREATE TABLE `paper` (
  `id`               BIGINT        NOT NULL COMMENT '主键（雪花ID）',
  `title`            VARCHAR(200)  NOT NULL COMMENT '试卷名称',
  `description`      VARCHAR(500)      NULL COMMENT '试卷描述',
  `paper_type`       TINYINT       NOT NULL DEFAULT 1 COMMENT '类型：1=普通考核、2=阶段考核',
  `total_score`      DECIMAL(5,1)  NOT NULL DEFAULT 100.0 COMMENT '试卷总分',
  `pass_score`       DECIMAL(5,1)  NOT NULL DEFAULT 60.0 COMMENT '及格分数线',
  `question_count`   INT           NOT NULL DEFAULT 0 COMMENT '题目总数（冗余）',
  `duration_minutes` INT           NOT NULL DEFAULT 60 COMMENT '答题时间（分钟）',
  `status`           TINYINT       NOT NULL DEFAULT 0 COMMENT '0=草稿、1=已发布、2=已截止',
  `publisher_id`     BIGINT        NOT NULL COMMENT '发布人（校长）',
  `province`         VARCHAR(50)       NULL COMMENT '所属省',
  `city`             VARCHAR(50)       NULL COMMENT '所属市',
  `county`           VARCHAR(50)       NULL COMMENT '所属县',
  `school`           VARCHAR(100)      NULL COMMENT '所属学校',
  `project_id`       BIGINT            NULL COMMENT '所属项目 ID',
  `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`          TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除、1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_publisher` (`publisher_id`),
  KEY `idx_project` (`project_id`),
  KEY `idx_status` (`status`),
  KEY `idx_region` (`province`, `city`, `county`, `school`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷表';

-- 9. question（题目表）
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question` (
  `id`            BIGINT         NOT NULL COMMENT '主键（雪花ID）',
  `paper_id`      BIGINT         NOT NULL COMMENT '外键，关联 paper.id',
  `title`         VARCHAR(1000)  NOT NULL COMMENT '题干内容',
  `question_type` TINYINT        NOT NULL COMMENT '1=单选、2=多选、3=判断、4=主观题',
  `score`         DECIMAL(5,1)   NOT NULL DEFAULT 0.0 COMMENT '该题分值',
  `is_required`   TINYINT        NOT NULL DEFAULT 1 COMMENT '是否必答：0=否、1=是',
  `sort_order`    INT            NOT NULL DEFAULT 0 COMMENT '排序号',
  `analysis`      VARCHAR(2000)      NULL COMMENT '题目解析',
  `created_at`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_paper_id` (`paper_id`),
  KEY `idx_paper_type` (`paper_id`, `question_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- 10. option（选项表）
DROP TABLE IF EXISTS `option`;
CREATE TABLE `option` (
  `id`             BIGINT        NOT NULL COMMENT '主键（雪花ID）',
  `question_id`    BIGINT        NOT NULL COMMENT '外键，关联 question.id',
  `paper_id`       BIGINT        NOT NULL COMMENT '冗余外键，关联 paper.id',
  `option_label`   VARCHAR(10)   NOT NULL COMMENT '选项标签（A/B/C/D/对/错）',
  `option_content` VARCHAR(1000)     NULL COMMENT '选项文本',
  `is_correct`     TINYINT       NOT NULL DEFAULT 0 COMMENT '是否正确答案：0=否、1=是',
  `sort_order`     INT           NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_question_id` (`question_id`),
  KEY `idx_paper_id` (`paper_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选项表';

-- 11. response（答题记录表）
DROP TABLE IF EXISTS `response`;
CREATE TABLE `response` (
  `id`          BIGINT       NOT NULL COMMENT '主键（雪花ID）',
  `paper_id`    BIGINT       NOT NULL COMMENT '外键，关联 paper.id',
  `user_id`     BIGINT       NOT NULL COMMENT '答题老师 user_id',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '0=未答题、1=正在答题、2=已提交、3=已批阅',
  `score`       DECIMAL(5,1)     NULL COMMENT '最终得分',
  `is_pass`     TINYINT          NULL COMMENT '是否合格：0=否、1=是',
  `submit_time` DATETIME         NULL COMMENT '提交时间',
  `review_time` DATETIME         NULL COMMENT '批阅时间',
  `reviewer_id` BIGINT           NULL COMMENT '批阅人 user_id',
  `province`    VARCHAR(50)      NULL COMMENT '老师所属省（冗余）',
  `city`        VARCHAR(50)      NULL COMMENT '所属市',
  `county`      VARCHAR(50)      NULL COMMENT '所属县',
  `school`      VARCHAR(100)     NULL COMMENT '所属学校',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间（分发时间）',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_paper_user` (`paper_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_paper_id` (`paper_id`),
  KEY `idx_region` (`province`, `city`, `county`, `school`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题记录表';

-- 12. answer（答案表）
DROP TABLE IF EXISTS `answer`;
CREATE TABLE `answer` (
  `id`             BIGINT        NOT NULL COMMENT '主键（雪花ID）',
  `response_id`    BIGINT        NOT NULL COMMENT '外键，关联 response.id',
  `question_id`    BIGINT        NOT NULL COMMENT '外键，关联 question.id',
  `user_id`        BIGINT        NOT NULL COMMENT '答题老师 user_id（冗余）',
  `answer_content` TEXT              NULL COMMENT '作答内容',
  `score`          DECIMAL(5,1)      NULL COMMENT '该题得分',
  `review_comment` VARCHAR(1000)     NULL COMMENT '批阅评语',
  `is_correct`     TINYINT           NULL COMMENT '客观题自动判分：0=错、1=对',
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作答时间',
  `updated_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_response_question` (`response_id`, `question_id`),
  KEY `idx_question_id` (`question_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答案表';


-- =================================================================
-- 三、种子数据（开发/演示用）
-- =================================================================

-- 工具模块
INSERT INTO `tool` (`id`, `tool_code`, `tool_name`, `description`) VALUES
(1, 'paper',      '试题工具', '试卷发布、答题、批阅'),
(2, 'article',    '文章工具', '文章发布、评论、问卷'),
(3, 'chat',       '交流工具', '讨论区/论坛'),
(4, 'qa',         '问答工具', '类似知乎的问答系统'),
(5, 'homework',   '作业工具', '作业布置与提交'),
(6, 'live',       '直播工具', '在线直播培训'),
(7, 'video',      '视频工具', '录播视频观看'),
(8, 'statistics', '统计工具', '分数统计与数据分析');

-- 用户
INSERT INTO `user` (`id`, `username`, `password`, `real_name`, `role`, `scope`, `province`, `school_id`) VALUES
(1, 'admin',      'e10adc3949ba59abbe56e057f20f883e', '系统管理员', 1, 'ALL', NULL, NULL),
(2, 'principal1',  'e10adc3949ba59abbe56e057f20f883e', '李校长',   2, NULL, NULL, 51),
(3, 'teacher1',   'e10adc3949ba59abbe56e057f20f883e', '张老师',   3, NULL, NULL, 51),
(4, 'teacher2',   'e10adc3949ba59abbe56e057f20f883e', '王老师',   3, NULL, NULL, 52),
(5, 'teacher3',   'e10adc3949ba59abbe56e057f20f883e', '赵老师',   3, NULL, NULL, 56),
(6, 'teacher4',   'e10adc3949ba59abbe56e057f20f883e', '李老师',   3, NULL, NULL, 56),
(7, 'principal_bj', 'e10adc3949ba59abbe56e057f20f883e', '王校长', 2, NULL, NULL, 1),
(8, 'teacher_bj1', 'e10adc3949ba59abbe56e057f20f883e', '刘老师',   3, NULL, NULL, 1),
(9, 'teacher_bj2', 'e10adc3949ba59abbe56e057f20f883e', '陈老师',   3, NULL, NULL, 2),
(10, 'principal_sh', 'e10adc3949ba59abbe56e057f20f883e', '周校长', 2, NULL, NULL, 4),
(11, 'teacher_sh1', 'e10adc3949ba59abbe56e057f20f883e', '吴老师',   3, NULL, NULL, 4),
(12, 'principal_gd', 'e10adc3949ba59abbe56e057f20f883e', '黄校长', 2, NULL, NULL, 7),
(13, 'teacher_gd1', 'e10adc3949ba59abbe56e057f20f883e', '林老师',   3, NULL, NULL, 7),
(14, 'teacher_gd2', 'e10adc3949ba59abbe56e057f20f883e', '何老师',   3, NULL, NULL, 8),
(15, 'principal_sc', 'e10adc3949ba59abbe56e057f20f883e', '杨校长', 2, NULL, NULL, 24),
(16, 'teacher_sc1', 'e10adc3949ba59abbe56e057f20f883e', '罗老师',   3, NULL, NULL, 24);

-- 学校（覆盖全国主要省市）
INSERT INTO `school` (`id`, `name`, `province`, `city`, `county`, `status`) VALUES
-- 北京市
(1,  '北京市第四中学',     '北京市', '北京市', '西城区', 1),
(2,  '中国人民大学附属中学', '北京市', '北京市', '海淀区', 1),
(3,  '北京市第二中学',     '北京市', '北京市', '东城区', 1),
-- 上海市
(4,  '上海中学',           '上海市', '上海市', '徐汇区', 1),
(5,  '华东师范大学第二附属中学', '上海市', '上海市', '浦东新区', 1),
(6,  '复旦大学附属中学',   '上海市', '上海市', '杨浦区', 1),
-- 广东省
(7,  '华南师范大学附属中学', '广东省', '广州市', '天河区', 1),
(8,  '深圳中学',           '广东省', '深圳市', '罗湖区', 1),
(9,  '广州市执信中学',     '广东省', '广州市', '越秀区', 1),
(10, '佛山市第一中学',     '广东省', '佛山市', '禅城区', 1),
-- 江苏省
(11, '南京外国语学校',     '江苏省', '南京市', '鼓楼区', 1),
(12, '江苏省常州高级中学', '江苏省', '常州市', '天宁区', 1),
(13, '苏州中学',           '江苏省', '苏州市', '姑苏区', 1),
(14, '无锡市第一中学',     '江苏省', '无锡市', '梁溪区', 1),
-- 浙江省
(15, '杭州第二中学',       '浙江省', '杭州市', '上城区', 1),
(16, '宁波镇海中学',       '浙江省', '宁波市', '镇海区', 1),
(17, '温州中学',           '浙江省', '温州市', '鹿城区', 1),
-- 湖北省
(18, '华中师范大学第一附属中学', '湖北省', '武汉市', '洪山区', 1),
(19, '武汉市第二中学',     '湖北省', '武汉市', '江岸区', 1),
(20, '襄阳市第四中学',     '湖北省', '襄阳市', '襄城区', 1),
-- 湖南省
(21, '湖南师范大学附属中学', '湖南省', '长沙市', '岳麓区', 1),
(22, '长沙市第一中学',     '湖南省', '长沙市', '开福区', 1),
(23, '雅礼中学',           '湖南省', '长沙市', '雨花区', 1),
-- 四川省
(24, '成都市第七中学',     '四川省', '成都市', '武侯区', 1),
(25, '成都外国语学校',     '四川省', '成都市', '郫都区', 1),
(26, '绵阳中学',           '四川省', '绵阳市', '涪城区', 1),
-- 山东省
(27, '山东省实验中学',     '山东省', '济南市', '历下区', 1),
(28, '青岛第二中学',       '山东省', '青岛市', '崂山区', 1),
(29, '烟台第一中学',       '山东省', '烟台市', '芝罘区', 1),
-- 河北省
(30, '石家庄市第二中学',   '河北省', '石家庄市', '新华区', 1),
(31, '衡水中学',           '河北省', '衡水市', '桃城区', 1),
(32, '唐山市第一中学',     '河北省', '唐山市', '路北区', 1),
-- 陕西省
(33, '西北工业大学附属中学', '陕西省', '西安市', '碑林区', 1),
(34, '西安高新第一中学',   '陕西省', '西安市', '雁塔区', 1),
(35, '西安市铁一中学',     '陕西省', '西安市', '新城区', 1),
-- 四川省 重庆
(36, '重庆巴蜀中学',       '重庆市', '重庆市', '渝中区', 1),
(37, '重庆南开中学',       '重庆市', '重庆市', '沙坪坝区', 1),
-- 辽宁省
(38, '东北育才学校',       '辽宁省', '沈阳市', '浑南区', 1),
(39, '大连市第二十四中学', '辽宁省', '大连市', '中山区', 1),
-- 吉林省
(40, '东北师范大学附属中学', '吉林省', '长春市', '朝阳区', 1),
-- 黑龙江省
(41, '哈尔滨市第三中学',   '黑龙江省', '哈尔滨市', '南岗区', 1),
-- 安徽省
(42, '合肥市第一中学',     '安徽省', '合肥市', '包河区', 1),
(43, '马鞍山市第二中学',   '安徽省', '马鞍山市', '雨山区', 1),
-- 福建省
(44, '福州第一中学',       '福建省', '福州市', '鼓楼区', 1),
(45, '厦门第一中学',       '福建省', '厦门市', '思明区', 1),
-- 江西省
(46, '江西师范大学附属中学', '江西省', '南昌市', '东湖区', 1),
-- 山西省
(47, '山西省实验中学',     '山西省', '太原市', '小店区', 1),
-- 甘肃省
(48, '西北师范大学附属中学', '甘肃省', '兰州市', '安宁区', 1),
-- 云南省
(49, '云南师范大学附属中学', '云南省', '昆明市', '五华区', 1),
-- 贵州省
(50, '贵阳市第一中学',     '贵州省', '贵阳市', '观山湖区', 1),
-- 河南省（保留原有数据）
(51, '郑州一中',           '河南省', '郑州市', '金水区', 1),
(52, '郑州二中',           '河南省', '郑州市', '中原区', 1),
(53, '郑州外国语',         '河南省', '郑州市', '高新区', 1),
(54, '洛阳一高',           '河南省', '洛阳市', '洛龙区', 1),
(55, '开封高中',           '河南省', '开封市', '鼓楼区', 1),
(56, '开封一中',           '河南省', '开封市', '龙亭区', 1),
(57, '新乡一中',           '河南省', '新乡市', '牧野区', 1),
(58, '安阳一中',           '河南省', '安阳市', '文峰区', 1);

-- 项目
INSERT INTO `project` (`id`, `name`, `description`, `creator_id`, `province`, `city`, `status`, `type`) VALUES
(1, '2025年度河南省教师素质提升培训', '全省中小学教师在线考核，涵盖教育理论、学科知识、教学技能三大模块', 1, '河南省', '郑州市', 1, 1),
(2, '郑州市春季学期第二阶段考核', '市级教师考核，重点考察新课程标准的实施能力', 1, '河南省', '郑州市', 1, 1),
(3, '2024年度教师年终考核', '年终综合考核，已结束', 1, '河南省', '洛阳市', 2, 1);

-- 项目-用户关联（河南省项目覆盖河南老师 + 部分外省老师）
INSERT INTO `project_user` (`id`, `project_id`, `user_id`) VALUES
(1, 1, 3),(2, 1, 4),(3, 1, 5),(4, 1, 6),
(5, 1, 8),(6, 1, 11),(7, 1, 13),(8, 1, 16),
(9, 2, 3),(10, 2, 4),(11, 2, 5),
(12, 3, 3),(13, 3, 5);

-- 项目-学校关联（河南省项目覆盖多个省份学校）
INSERT INTO `project_school` (`id`, `project_id`, `school_id`) VALUES
(1, 1, 51),(2, 1, 52),(3, 1, 53),(4, 1, 54),
(5, 1, 1),(6, 1, 4),(7, 1, 7),(8, 1, 24),
(9, 2, 51),(10, 2, 52),(11, 2, 53),
(12, 3, 51),(13, 3, 54);

-- 试卷（草稿、已发布、已截止）
INSERT INTO `paper` (`id`, `title`, `description`, `paper_type`, `total_score`, `pass_score`, `question_count`, `duration_minutes`, `status`, `publisher_id`, `province`, `city`, `project_id`) VALUES
(1, '教育理论基础知识', '考察教育学、心理学基础理论', 1, 100, 60, 5, 60, 0, 2, '河南省', '郑州市', 1),
(2, '学科专业知识考核', '学科知识深度考察', 1, 150, 90, 80, 90, 1, 2, '河南省', '郑州市', 1),
(3, '课堂教学技能测试', '课堂教学设计与实施能力', 1, 80, 48, 30, 45, 1, 2, '河南省', '郑州市', 2),
(4, '暑期培训结业考核', '暑期培训课程结业考试', 1, 100, 60, 40, 60, 2, 2, '河南省', '洛阳市', 3);

-- 题目（试卷1下的 5 道题）
INSERT INTO `question` (`id`, `paper_id`, `title`, `question_type`, `score`, `is_required`, `sort_order`) VALUES
(1, 1, '教育的本质属性是？', 1, 2, 1, 1),
(2, 1, '以下哪些属于教学原则？', 2, 4, 1, 2),
(3, 1, '教师职业道德的核心是爱岗敬业。', 3, 2, 1, 3),
(4, 1, '请结合实际教学案例，谈谈你对"以学生为中心"教育理念的理解。', 4, 15, 1, 4),
(5, 1, '新课程改革的核心目标是？', 1, 2, 1, 5);

-- 选项
INSERT INTO `option` (`id`, `question_id`, `paper_id`, `option_label`, `option_content`, `is_correct`, `sort_order`) VALUES
-- 第1题 单选题
(1,  1, 1, 'A', '社会性',             0, 1),
(2,  1, 1, 'B', '培养人的社会活动',   1, 2),
(3,  1, 1, 'C', '阶级性',             0, 3),
(4,  1, 1, 'D', '生产力',             0, 4),
-- 第2题 多选题
(5,  2, 1, 'A', '启发性原则',         1, 1),
(6,  2, 1, 'B', '巩固性原则',         1, 2),
(7,  2, 1, 'C', '因材施教原则',       1, 3),
(8,  2, 1, 'D', '循序渐进原则',       1, 4),
-- 第3题 判断题
(9,  3, 1, 'A', '正确',               1, 1),
(10, 3, 1, 'B', '错误',               0, 2),
-- 第5题 单选题
(11, 5, 1, 'A', '减轻学生负担',       0, 1),
(12, 5, 1, 'B', '促进学生的全面发展', 1, 2),
(13, 5, 1, 'C', '提高升学率',         0, 3),
(14, 5, 1, 'D', '加强师资建设',       0, 4);

-- 答题记录（模拟多个老师的答题状态，覆盖不同省份）
INSERT INTO `response` (`id`, `paper_id`, `user_id`, `status`, `score`, `is_pass`, `submit_time`, `review_time`, `reviewer_id`, `province`, `city`, `county`, `school`) VALUES
(1, 1, 3, 0, NULL, NULL, NULL, NULL, NULL, '河南省', '郑州市', '金水区', '郑州一中'),
(2, 2, 3, 1, NULL, NULL, NULL, NULL, NULL, '河南省', '郑州市', '金水区', '郑州一中'),
(3, 3, 3, 2, NULL, NULL, '2025-03-12 15:30:00', NULL, NULL, '河南省', '郑州市', '金水区', '郑州一中'),
(4, 4, 3, 3, 78, 1, '2025-02-15 10:00:00', '2025-02-16 14:00:00', 2, '河南省', '郑州市', '金水区', '郑州一中'),
(5, 2, 4, 3, 65, 1, '2025-03-11 10:20:00', '2025-03-12 09:00:00', 2, '河南省', '郑州市', '中原区', '郑州二中'),
(6, 2, 5, 2, NULL, NULL, '2025-03-12 09:15:00', NULL, NULL, '河南省', '洛阳市', '洛龙区', '洛阳一高'),
(7, 1, 8, 2, NULL, NULL, '2025-03-13 10:00:00', NULL, NULL, '北京市', '北京市', '西城区', '北京市第四中学'),
(8, 1, 11, 2, NULL, NULL, '2025-03-13 11:00:00', NULL, NULL, '上海市', '上海市', '徐汇区', '上海中学'),
(9, 1, 13, 2, NULL, NULL, '2025-03-13 14:00:00', NULL, NULL, '广东省', '广州市', '天河区', '华南师范大学附属中学'),
(10, 1, 16, 2, NULL, NULL, '2025-03-13 15:00:00', NULL, NULL, '四川省', '成都市', '武侯区', '成都市第七中学');

-- 配置（项目1 — 试题工具各角色权限）
INSERT INTO `config` (`id`, `project_id`, `tool_id`, `role`, `is_enabled`, `allow_publish`, `allow_delete`, `allow_review`, `require_pass_score`, `auto_score`, `score_per_submit`) VALUES
(1, 1, 1, 1, 1, 0, 0, 0, 1, 0, 0),
(2, 1, 1, 2, 1, 1, 1, 1, 1, 0, 0),
(3, 1, 1, 3, 1, 0, 0, 0, 0, 0, 5),
(4, 1, 2, 2, 1, 1, 1, 0, 0, 0, 0),
(5, 1, 2, 3, 1, 0, 0, 0, 0, 0, 1);

-- 权限
INSERT INTO `permission` (`id`, `role`, `tool_id`, `permission_code`, `permission_name`) VALUES
(1, 2, 1, 'paper:create',  '创建试卷'),
(2, 2, 1, 'paper:publish', '发布试卷'),
(3, 2, 1, 'paper:edit',    '编辑试卷'),
(4, 2, 1, 'paper:delete',  '删除试卷'),
(5, 2, 1, 'paper:review',  '批阅试卷'),
(6, 3, 1, 'paper:view',    '查看试卷'),
(7, 3, 1, 'paper:submit',  '提交试卷'),
(8, 2, 2, 'article:create','发布文章'),
(9, 2, 2, 'article:delete','删除文章'),
(10, 3, 2, 'article:view', '查看文章'),
(11, 3, 2, 'article:comment','评论');
