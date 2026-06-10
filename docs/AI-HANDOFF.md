# AI 开发接手指南

> **最后更新：2026-06-10**
> **当前版本：commit 77505ed**
> **本文档目的：让任意 AI 可以在 5 分钟内理解项目全貌并开始开发**

---

## 一、项目概述

**教师培训在线考试系统** — 面向全国 40 万教师的在线培训与考核平台。

### 三端架构

| 端 | 路由前缀 | 角色 | 说明 |
|---|---------|------|------|
| 学员端 | `/teacher` | teacher (role=3) | 老师答题、查成绩 |
| 管理端 | `/principal` | principal (role=2) | 校长发布试卷、批阅 |
| 管理后台 | `/admin` | admin (role=1) | 管理员配置项目、管理用户 |

### 权限层级

```
超级管理员 (role=1, scope=ALL)
  └── 看全国所有数据，可创建省级管理员

省级管理员 (role=1, scope=PROVINCE, province=某省)
  └── 只看本省数据，可创建校长和老师

校长 (role=2, school_id=某学校)
  └── 管本校老师，创建校级项目，查看本校统计

老师 (role=3, school_id=某学校)
  └── 参与项目，答题，查成绩
```

---

## 二、技术栈

| 层 | 技术 | 版本 |
|---|------|------|
| 前端 | Vue 3 + TypeScript + Element Plus + Pinia + Axios | Vite 5 |
| 后端 | Spring Boot 3.2 + MyBatis-Plus + JDK 17 | Maven |
| 数据库 | MySQL 8.0 (端口 13306) | Docker |
| 缓存 | Redis 7.2 (端口 16379) | Docker |
| 消息 | Kafka 3.6 (端口 19092) | Docker (未使用) |
| 搜索 | Elasticsearch 7.17 (端口 19200) | Docker (未使用) |

---

## 三、项目结构

```
exam-platform/
├── pom.xml                          # 父 POM
├── docker-compose.yml               # 基础设施（MySQL/Redis/Kafka/ES）
├── sql/
│   └── init/
│       ├── 01-init.sql              # 建表 + 种子数据（必须执行）
│       ├── 02-test-data.sql         # 补充测试数据
│       └── 03-school-data.sql       # 全国学校数据
│
├── exam-common/                     # 公共模块
│   └── src/main/java/com/exam/common/
│       ├── common/                  # Result, BusinessException, BaseEntity
│       ├── entity/                  # 12 个实体类（对应 12 张表）
│       ├── dto/                     # 公共 DTO
│       ├── vo/                      # 公共 VO
│       └── utils/                   # JwtUtils, SnowflakeUtil
│
├── exam-project-service/            # 【唯一有业务代码的服务】
│   └── src/main/java/com/exam/project/
│       ├── controller/              # 6 个 Controller
│       │   ├── AuthController       # 登录/注册
│       │   ├── ProjectController    # 项目 CRUD
│       │   ├── UserController       # 用户管理
│       │   ├── PaperController      # 试卷管理
│       │   ├── AnswerController     # 答题/提交/成绩
│       │   └── ConfigController     # 配置管理
│       ├── service/impl/            # 5 个 Service 实现
│       ├── mapper/                  # 12 个 Mapper 接口
│       ├── model/dto/               # 模块 DTO
│       ├── model/vo/                # 模块 VO
│       ├── interceptor/             # JWT 拦截器 + 项目权限拦截器
│       └── config/                  # WebMvc 配置
│
├── exam-frontend/                   # Vue 3 前端
│   └── src/
│       ├── api/                     # API 封装（index.ts, paper.ts, project.ts）
│       ├── stores/user.ts           # 用户状态（Pinia）
│       ├── router/index.ts          # 路由（三端分离）
│       ├── components/              # AppLayout + SideMenu
│       └── views/
│           ├── teacher/             # 5 页：项目列表→试卷→答题→成绩
│           ├── principal/           # 7 页：试卷管理→创建→题目→批阅→统计
│           └── admin/               # 5 页：项目管理→用户管理→配置
│
├── exam-gateway/                    # API 网关（空壳，未实现）
├── exam-user-service/               # 用户服务（空壳，逻辑在 project-service）
├── exam-paper-service/              # 试卷服务（空壳，逻辑在 project-service）
├── exam-answer-service/             # 答题服务（空壳，逻辑在 project-service）
├── exam-statistics-service/         # 统计服务（空壳，未实现）
└── exam-search-service/             # 搜索服务（空壳，未实现）
```

---

## 四、数据库（12 张表）

```sql
-- 支撑表
user           -- 用户（id, username, password, real_name, role, scope, province, school_id, status）
school         -- 学校（id, name, province, city, county, status）
project        -- 项目（id, name, description, creator_id, province, city, status, type, school_id）
project_user   -- 项目-用户关联（id, project_id, user_id）
project_school -- 项目-学校关联（id, project_id, school_id）
tool           -- 工具模块（id, tool_code, tool_name）
config         -- 配置（id, project_id, tool_id, role, is_enabled, allow_publish, ...）
permission     -- 权限（id, role, tool_id, permission_code）

-- 业务表
paper          -- 试卷（id, title, total_score, pass_score, status, publisher_id, project_id）
question       -- 题目（id, paper_id, title, question_type, score, sort_order）
option         -- 选项（id, question_id, paper_id, option_label, is_correct）
response       -- 答题记录（id, paper_id, user_id, status, score, is_pass, submit_time, review_time）
answer         -- 答案（id, response_id, question_id, user_id, answer_content, score, is_correct, review_comment）
```

**关键外键关系：**
- `user.school_id` → `school.id`
- `project_user.project_id` → `project.id`
- `project_user.user_id` → `user.id`
- `project_school.project_id` → `project.id`
- `project_school.school_id` → `school.id`
- `paper.project_id` → `project.id`
- `question.paper_id` → `paper.id`
- `option.question_id` → `question.id`
- `response.paper_id` → `paper.id`, `response.user_id` → `user.id`
- `answer.response_id` → `response.id`, `answer.question_id` → `question.id`

---

## 五、已实现的 API

### Auth（认证）
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录，返回 token + 用户信息 |
| POST | `/api/auth/register` | 老师注册（自动绑定学校） |

### Project（项目）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/project/list` | 项目列表（省级管理员自动过滤省份） |
| GET | `/api/project/my` | 当前用户参与的项目 |
| GET | `/api/project/{id}` | 项目详情 |
| POST | `/api/project` | 创建项目（省级管理员自动绑定省份） |
| PUT | `/api/project/{id}` | 编辑项目 |
| DELETE | `/api/project/{id}` | 删除项目 |
| POST | `/api/project/{id}/join` | 加入项目 |
| POST | `/api/project/{id}/users` | 批量添加用户到项目 |

### User（用户）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/list` | 用户列表（省级管理员过滤本省） |
| POST | `/api/user` | 创建用户（省级管理员不能创建管理员） |
| PUT | `/api/user/{id}/status` | 启用/禁用用户 |
| PUT | `/api/user/{id}/password` | 重置密码 |
| GET | `/api/user/teachers` | 本校老师列表（校长用） |

### Paper（试卷）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/paper/list` | 试卷列表 |
| GET | `/api/paper/my` | 我的试卷 |
| GET | `/api/paper/{id}` | 试卷详情 |
| GET | `/api/paper/{id}/exam` | 答题页试卷数据（不含答案） |
| POST | `/api/paper` | 创建试卷 |
| POST | `/api/paper/{id}/question` | 添加题目 |
| POST | `/api/paper/{id}/publish` | 发布试卷 |
| POST | `/api/paper/{id}/close` | 下线试卷 |

### Answer（答题）
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/answer/{paperId}/start` | 开始答题 |
| POST | `/api/answer/{paperId}/save` | 保存进度到 Redis |
| POST | `/api/answer/{paperId}/submit` | 提交试卷（自动批阅客观题） |
| GET | `/api/answer/{paperId}/result` | 查看成绩详情（已批阅） |

### Config（配置）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/project/{projectId}/config` | 项目配置列表 |
| PUT | `/api/project/{projectId}/config` | 更新配置 |

### School（学校）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/school/list` | 学校列表（按省市县筛选） |
| GET | `/api/school/provinces` | 省份列表 |
| GET | `/api/school/cities` | 城市列表 |
| GET | `/api/school/counties` | 区县列表 |

---

## 六、未实现的功能（按优先级）

### P0 — 考试链路断裂点（必须实现）
| 功能 | 前端页面 | 后端 API | 说明 |
|------|---------|---------|------|
| 批阅列表 | `principal/ReviewList.vue` | `GET /api/review/pending` | 校长查看待批阅试卷 |
| 批阅详情 | `principal/ReviewDetail.vue` | `GET /api/review/{responseId}/detail` | 查看学员答卷 |
| 主观题评分 | 前端已有 | `PUT /api/review/{responseId}/question/{questionId}/score` | 给主观题打分 |
| 完成批阅 | 前端已有 | `PUT /api/review/{responseId}/complete` | 计算总分，更新状态 |

### P1 — 统计分析
| 功能 | 前端页面 | 后端 API |
|------|---------|---------|
| 试卷统计 | `principal/Statistics.vue` | `GET /api/statistics/paper/{paperId}` |
| 区域统计 | 前端已有 | `GET /api/statistics/region` |

### P2 — 基础设施
| 功能 | 说明 |
|------|------|
| Kafka 异步分发 | 发布试卷时异步创建 response 记录（当前同步插入） |
| ES 全文搜索 | 题目搜索 |
| Redis 多级缓存 | 热门数据缓存 |
| API 网关 | Spring Cloud Gateway |

---

## 七、开发规范

### 后端规范
1. **所有代码必须有中文注释** — 每个类/方法说明用途
2. **Controller + DTO 定义先于 Service** — 接口先行
3. **实体类统一用 exam-common** — 不要在 exam-project-service 重复定义
4. **每完成一个接口必须写集成测试** — 连接 Docker MySQL
5. **测试类命名**：`{ControllerName}Test.java`
6. **运行测试**：`mvn test -pl exam-project-service`

### 前端规范
1. **API 调用位置用 `// TODO: 调用 API` 标注**
2. **页面风格统一**：Element Plus + SCSS 变量（`var(--color-primary)` 等）
3. **三栏布局**：答题页/成绩详情页用左侧题号 + 中间内容 + 右侧答题卡

### 数据库规范
1. **表结构不可随意变更** — 变更需同步更新 `01-init.sql`
2. **所有表用雪花 ID** — `@TableId(type = IdType.ASSIGN_ID)`
3. **逻辑删除** — `paper` 表有 `deleted` 字段

---

## 八、启动方式

```bash
# 1. 启动基础设施
cd exam-platform
docker-compose up -d

# 2. 启动后端（端口 8087）
cd exam-project-service
mvn spring-boot:run

# 3. 启动前端（端口 3000）
cd exam-frontend
npm install
npm run dev
```

### Docker 端口映射
| 服务 | 容器端口 | 主机端口 |
|------|---------|---------|
| MySQL | 3306 | 13306 |
| Redis | 6379 | 16379 |
| Kafka | 9092 | 19092 |
| Elasticsearch | 9200 | 19200 |

### 测试账号
| 用户名 | 密码 | 角色 | 学校 |
|--------|------|------|------|
| admin | 123456 | 超级管理员 | — |
| admin_test2 | 123456 | 河南省管理员 | — |
| principal1 | 123456 | 校长 | 郑州一中 |
| teacher1 | 123456 | 老师 | 郑州一中 |
| principal_bj | 123456 | 校长 | 北京四中 |
| teacher_bj1 | 123456 | 老师 | 北京四中 |

---

## 九、测试通过记录

| 日期 | 服务 | 测试类 | 接口数 | 测试数 | 结果 |
|------|------|--------|:---:|:---:|:---:|
| 2026-06-05 | exam-project-service | ConfigControllerTest | 6 | 9 | ✅ 全部通过 |
| 2026-06-08 | exam-project-service | AnswerControllerTest | 1 | 3 | ✅ 全部通过 |

---

## 十、已知问题

1. **DTO 重复** — `exam-project-service` 和 `exam-common` 有同名 DTO/VO 类，应统一
2. **`02-test-data.sql` 会报错** — 引用了 `user` 表已删除的 `county`/`school` 列
3. **前端 mock fallback** — 3 处接口失败时使用 mock 数据，后端完善后应移除
4. **Kafka/ES 未接入** — Docker 已启动但代码未使用
