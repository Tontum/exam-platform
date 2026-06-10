# 02 - 数据库设计

> 本文档定义教师培训在线考试系统的完整数据库表结构、字段约束、索引策略和表关系。所有 SQL 建表和代码实体类必须以此文档为准。

---

## 1. ER 总览

```
┌──────────┐    1:N    ┌──────────────┐    1:N    ┌──────────┐    1:N    ┌──────────┐
│  paper   │──────────▶│   question   │──────────▶│  option   │          │  answer  │
│ (试卷表)  │           │  (题目表)     │           │ (选项表)   │          │ (答案表)  │
└────┬─────┘           └──────────────┘           └──────────┘          └────┬─────┘
     │                                                                      │
     │ 1:N                                                                  │ N:1
     ▼                                                                      │
┌──────────┐     N:1    ┌──────────┐                                       │
│ response │───────────▶│   user   │◀─────────────────────────────────────┘
│(答题记录) │            │ (用户表)  │
└──────────┘            └────┬─────┘
                             │ N:M (中间表)
                             ▼
                        ┌──────────┐
                        │  project │
                        │ (项目表)  │
                        └────┬─────┘
                             │ 1:N
                             ▼
                        ┌──────────┐     N:1    ┌──────────┐
                        │  config  │───────────▶│   tool   │
                        │ (配置表)  │            │ (工具表)  │
                        └──────────┘            └──────────┘
```

## 2. 核心业务表详细设计

### 2.1 paper（试卷表）

校长发布试卷时创建，一张试卷包含多道题目。

| 字段名 | 类型 | 长度 | 允许空 | 默认值 | 说明 |
|--------|------|------|--------|--------|------|
| id | BIGINT | 20 | 否 | 雪花ID | 主键，分布式 ID |
| title | VARCHAR | 200 | 否 | — | 试卷名称（考核名称） |
| description | VARCHAR | 500 | 是 | NULL | 试卷描述/说明 |
| paper_type | TINYINT | 1 | 否 | 1 | 试卷类型：1=普通考核、2=阶段考核 |
| total_score | DECIMAL | (5,1) | 否 | 100.0 | 试卷总分 |
| pass_score | DECIMAL | (5,1) | 否 | 60.0 | 及格分数线 |
| question_count | INT | 11 | 否 | 0 | 题目总数（冗余字段，方便展示） |
| duration_minutes | INT | 11 | 否 | 60 | 答题规定时间（分钟） |
| status | TINYINT | 1 | 否 | 0 | 0=草稿、1=已发布、2=已截止 |
| publisher_id | BIGINT | 20 | 否 | — | 发布人 user_id（校长） |
| province | VARCHAR | 50 | 是 | NULL | 试卷所属省 |
| city | VARCHAR | 50 | 是 | NULL | 试卷所属市 |
| county | VARCHAR | 50 | 是 | NULL | 试卷所属县 |
| school | VARCHAR | 100 | 是 | NULL | 试卷所属学校 |
| project_id | BIGINT | 20 | 是 | NULL | 所属项目 ID |
| created_at | DATETIME | — | 否 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | — | 否 | CURRENT_TIMESTAMP | 更新时间（自动更新） |
| deleted | TINYINT | 1 | 否 | 0 | 逻辑删除：0=未删除、1=已删除 |

**索引策略：**
- PRIMARY KEY: `id`
- INDEX `idx_publisher` (`publisher_id`)
- INDEX `idx_project` (`project_id`)
- INDEX `idx_status` (`status`)
- INDEX `idx_region` (`province`, `city`, `county`, `school`) — 层级查询
- INDEX `idx_created_at` (`created_at`)

### 2.2 question（题目表）

每张试卷下的题目信息（题干表），外键关联 paper 表。

| 字段名 | 类型 | 长度 | 允许空 | 默认值 | 说明 |
|--------|------|------|--------|--------|------|
| id | BIGINT | 20 | 否 | 雪花ID | 主键 |
| paper_id | BIGINT | 20 | 否 | — | 外键，关联 paper.id |
| title | VARCHAR | 1000 | 否 | — | 题干内容（如"1+1等于几？"） |
| question_type | TINYINT | 1 | 否 | — | 1=单选题、2=多选题、3=判断题、4=主观题（简答题） |
| score | DECIMAL | (5,1) | 否 | 0.0 | 该题分值 |
| is_required | TINYINT | 1 | 否 | 1 | 是否必答题：0=否、1=是 |
| sort_order | INT | 11 | 否 | 0 | 排序号，题目在试卷中的展示顺序 |
| analysis | VARCHAR | 2000 | 是 | NULL | 题目解析（批阅后展示） |
| created_at | DATETIME | — | 否 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | — | 否 | CURRENT_TIMESTAMP | 更新时间 |

**索引策略：**
- PRIMARY KEY: `id`
- INDEX `idx_paper_id` (`paper_id`) — 根据试卷查所有题目（高频）
- INDEX `idx_paper_type` (`paper_id`, `question_type`) — 联合索引，按题型筛选

### 2.3 option（选项表）

每道选择题/判断题的选项及正确答案标注。

| 字段名 | 类型 | 长度 | 允许空 | 默认值 | 说明 |
|--------|------|------|--------|--------|------|
| id | BIGINT | 20 | 否 | 雪花ID | 主键 |
| question_id | BIGINT | 20 | 否 | — | 外键，关联 question.id |
| paper_id | BIGINT | 20 | 否 | — | 冗余外键，关联 paper.id，提升关联查询效率 |
| option_label | VARCHAR | 10 | 否 | — | 选项标签（A、B、C、D、对、错） |
| option_content | VARCHAR | 1000 | 是 | NULL | 选项文本内容 |
| is_correct | TINYINT | 1 | 否 | 0 | 是否为正确答案：0=否、1=是 |
| sort_order | INT | 11 | 否 | 0 | 选项排序 |

**索引策略：**
- PRIMARY KEY: `id`
- INDEX `idx_question_id` (`question_id`) — 按题目查选项（高频）
- INDEX `idx_paper_id` (`paper_id`) — 冗余索引，支持跨题查询

### 2.4 response（答题记录表）

记录每位老师对每份试卷的答题状态与成绩。校长发布试卷时向此表批量插入记录。

| 字段名 | 类型 | 长度 | 允许空 | 默认值 | 说明 |
|--------|------|------|--------|--------|------|
| id | BIGINT | 20 | 否 | 雪花ID | 主键 |
| paper_id | BIGINT | 20 | 否 | — | 外键，关联 paper.id |
| user_id | BIGINT | 20 | 否 | — | 答题老师 user_id |
| status | TINYINT | 1 | 否 | 0 | 0=未答题、1=正在答题、2=已提交（答题完毕）、3=已批阅 |
| score | DECIMAL | (5,1) | 是 | NULL | 最终得分（批阅后填写） |
| is_pass | TINYINT | 1 | 是 | NULL | 是否合格：0=不合格、1=合格 |
| submit_time | DATETIME | — | 是 | NULL | 提交时间（老师点提交按钮时更新） |
| review_time | DATETIME | — | 是 | NULL | 批阅时间（校长批阅完成时更新） |
| reviewer_id | BIGINT | 20 | 是 | NULL | 批阅人 user_id（校长） |
| province | VARCHAR | 50 | 是 | NULL | 答题老师所属省（冗余，方便统计） |
| city | VARCHAR | 50 | 是 | NULL | 答题老师所属市 |
| county | VARCHAR | 50 | 是 | NULL | 答题老师所属县 |
| school | VARCHAR | 100 | 是 | NULL | 答题老师所属学校 |
| created_at | DATETIME | — | 否 | CURRENT_TIMESTAMP | 记录创建时间（即试卷分发时间） |
| updated_at | DATETIME | — | 否 | CURRENT_TIMESTAMP | 更新时间 |

**索引策略：**
- PRIMARY KEY: `id`
- UNIQUE KEY `uk_paper_user` (`paper_id`, `user_id`) — 一人一试卷仅一条记录
- INDEX `idx_user_id` (`user_id`) — 按老师查所有试卷（高频，进入试卷列表）
- INDEX `idx_user_status` (`user_id`, `status`) — 按状态筛选（未答题/已提交）
- INDEX `idx_paper_id` (`paper_id`) — 按试卷查所有老师答题情况（校长视角）
- INDEX `idx_region` (`province`, `city`, `county`, `school`) — 层级统计

### 2.5 answer（答案表）

记录老师对每道题的具体作答内容和批阅结果。

| 字段名 | 类型 | 长度 | 允许空 | 默认值 | 说明 |
|--------|------|------|--------|--------|------|
| id | BIGINT | 20 | 否 | 雪花ID | 主键 |
| response_id | BIGINT | 20 | 否 | — | 外键，关联 response.id |
| question_id | BIGINT | 20 | 否 | — | 外键，关联 question.id |
| user_id | BIGINT | 20 | 否 | — | 答题老师 user_id（冗余，方便查询） |
| answer_content | TEXT | — | 是 | NULL | 老师作答内容（选择题存选项、主观题存文本） |
| score | DECIMAL | (5,1) | 是 | NULL | 该题得分（校长批阅后填写） |
| review_comment | VARCHAR | 1000 | 是 | NULL | 校长批阅评语 |
| is_correct | TINYINT | 1 | 是 | NULL | 客观题自动判分：0=错误、1=正确 |
| created_at | DATETIME | — | 否 | CURRENT_TIMESTAMP | 作答时间 |
| updated_at | DATETIME | — | 否 | CURRENT_TIMESTAMP | 更新时间 |

> **设计要点**：answer 表的 `question_id` + `response_id` 组合唯一确定一条记录。每题的得分记录在 answer 表中，response 表的 score 是各题得分之和。这样既解决"问题2"（每道题多少分），也方便统计每道题的正确率。

**索引策略：**
- PRIMARY KEY: `id`
- UNIQUE KEY `uk_response_question` (`response_id`, `question_id`) — 一人一题仅一条记录
- INDEX `idx_question_id` (`question_id`) — 按题目统计答题情况
- INDEX `idx_user_id` (`user_id`) — 按老师查询

---

## 3. 系统支撑表设计

### 3.1 user（用户表）

| 字段名 | 类型 | 长度 | 允许空 | 默认值 | 说明 |
|--------|------|------|--------|--------|------|
| id | BIGINT | 20 | 否 | 雪花ID | 主键 |
| username | VARCHAR | 50 | 否 | — | 登录账号 |
| password | VARCHAR | 255 | 否 | — | 加密密码 |
| real_name | VARCHAR | 50 | 否 | — | 真实姓名 |
| role | TINYINT | 1 | 否 | 3 | 1=管理员、2=校长（管理端）、3=老师/学员 |
| phone | VARCHAR | 20 | 是 | NULL | 手机号 |
| email | VARCHAR | 100 | 是 | NULL | 邮箱 |
| province | VARCHAR | 50 | 是 | NULL | 所属省 |
| city | VARCHAR | 50 | 是 | NULL | 所属市 |
| county | VARCHAR | 50 | 是 | NULL | 所属县 |
| school | VARCHAR | 100 | 是 | NULL | 所属学校 |
| status | TINYINT | 1 | 否 | 1 | 0=禁用、1=启用 |
| created_at | DATETIME | — | 否 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | — | 否 | CURRENT_TIMESTAMP | 更新时间 |

**索引：** UNIQUE(`username`), INDEX(`role`), INDEX(`province`, `city`, `county`, `school`)

### 3.2 project（项目表）

| 字段名 | 类型 | 长度 | 说明 |
|--------|------|------|------|
| id | BIGINT | 20 | 主键 |
| name | VARCHAR | 200 | 项目名称 |
| description | VARCHAR | 500 | 项目描述 |
| creator_id | BIGINT | 20 | 创建人（管理员） |
| province | VARCHAR | 50 | 所属省 |
| city | VARCHAR | 50 | 所属市 |
| status | TINYINT | 1 | 0=未开始、1=进行中、2=已结束 |
| created_at | DATETIME | — | 创建时间 |
| updated_at | DATETIME | — | 更新时间 |

### 3.3 project_user（项目-用户关联表，N:M 中间表）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| project_id | BIGINT | 项目 ID |
| user_id | BIGINT | 用户 ID |
| joined_at | DATETIME | 加入时间 |

**唯一约束：** UNIQUE(`project_id`, `user_id`)

### 3.4 tool（工具/功能模块表）

项目可配置的功能模块清单。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| tool_code | VARCHAR | 50 | 工具编码（paper、article、live、qa、homework） |
| tool_name | VARCHAR | 50 | 工具名称（试题、文章、直播、问答、作业） |
| description | VARCHAR | 200 | 工具描述 |

### 3.5 config（配置表）

以项目为维度，配置各工具的功能开关和规则。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| project_id | BIGINT | 项目 ID |
| tool_id | BIGINT | 工具 ID |
| role | TINYINT | 1 | 角色：1=管理员、2=校长、3=老师 |
| is_enabled | TINYINT | 1 | 是否启用该工具（控制左侧菜单是否显示） |
| allow_publish | TINYINT | 1 | 是否允许发布 |
| allow_delete | TINYINT | 1 | 是否允许删除 |
| allow_review | TINYINT | 1 | 是否允许批阅（针对试题工具） |
| require_pass_score | TINYINT | 1 | 是否必须设置合格分 |
| auto_score | TINYINT | 1 | 主观题是否自动给分 |
| publish_time_start | TIME | — | 允许发布时间段-起始 |
| publish_time_end | TIME | — | 允许发布时间段-截止 |
| score_per_submit | INT | 11 | 每次提交试卷获得的考核加分 |
| created_at | DATETIME | — | 创建时间 |
| updated_at | DATETIME | — | 更新时间 |

**唯一约束：** UNIQUE(`project_id`, `tool_id`, `role`)

### 3.6 permission（权限表）

RBAC 权限控制，角色-功能-按钮级别。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| role | TINYINT | 角色 |
| tool_id | BIGINT | 工具 ID |
| permission_code | VARCHAR | 100 | 权限编码（如 paper:publish、paper:delete） |
| permission_name | VARCHAR | 100 | 权限名称 |

---

## 4. 核心业务数据流与表操作

### 4.1 校长发布试卷流程

```
1. 填写试卷信息 → INSERT INTO paper
2. 添加题目     → INSERT INTO question（外键 paper_id）
3. 添加选项     → INSERT INTO option（外键 question_id, paper_id）
4. 选择老师发布 → 按层级查询 user 表，获取 user_id 列表
5. 异步批量插入 → INSERT INTO response（paper_id + 每位 user_id）
   说明：第5步通过 Kafka 异步处理，不阻塞发布接口返回
```

### 4.2 老师答题流程

```
1. 进入试卷列表 → SELECT * FROM response WHERE user_id = ? 获取所有试卷状态
2. 点击某试卷   → SELECT * FROM question WHERE paper_id = ? 获取所有题目
                 → SELECT * FROM option WHERE paper_id = ? 获取所有选项
3. 提交单题答案 → INSERT / UPDATE answer（按 response_id + question_id 唯一）
4. 提交整张试卷 → UPDATE response SET status=2, submit_time=NOW()
5. 自动批阅客观题 → 比对 answer 与 option.is_correct，写入 answer.score
```

### 4.3 校长批阅试卷流程

```
1. 查看待批阅列表 → SELECT * FROM response WHERE status=2 AND paper_id=?
2. 查看某老师答案 → SELECT * FROM answer WHERE response_id=?
3. 逐题批阅      → UPDATE answer SET score=?, review_comment=?
4. 确认批阅完成  → UPDATE response SET status=3, score=SUM(answer.score),
                    is_pass=(score >= paper.pass_score), review_time=NOW(), reviewer_id=?
```

### 4.4 数据补偿流程（新增老师）

```
老师点击试卷按钮 → 查 response 是否有该 user_id 的记录
  ├── 有 → 正常展示
  └── 没有 → 从 paper 表按层级（省市县校）查出应分配的试卷
           → INSERT INTO response（补齐缺失的试卷记录）
```

---

## 5. 设计原则总结

| 原则 | 说明 |
|------|------|
| 分布式 ID | 所有主键使用雪花算法（Snowflake），避免自增 ID 在分布式环境下的冲突 |
| 逻辑删除 | 核心表使用 `deleted` 字段，不物理删除 |
| 冗余字段 | option.paper_id 和 response 层级字段允许冗余，牺牲范式提升查询效率 |
| 唯一约束 | paper+user 唯一（response）、response+question 唯一（answer），防止重复数据 |
| 审计字段 | 所有表包含 created_at、updated_at，关键表包含创建人/更新人 |
| 索引策略 | 以实际查询场景建索引，联合索引优先于多个单列索引 |
