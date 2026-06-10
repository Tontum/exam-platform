# 后端业务代码实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement.

**Goal:** 从零开始实现 exam-platform 全部后端业务代码（~100 文件），自底向上逐服务完成。
**Architecture:** Spring Boot 3.2 + Spring Cloud 微服务，8 模块（common + 7 service），MyBatis-Plus + MySQL。
**Tech Stack:** JDK 17, Spring Boot 3.2.5, MyBatis-Plus 3.5.6, MySQL 8.0, Redis 7.2, Kafka 3.6, ES 7.17

---

## Phase 0: 新增 exam-project-service 模块

### Task 0.1: 更新父 POM
**文件:** `G:\在线教育平台\exam-platform\pom.xml`
**操作:** 在 `<modules>` 中添加 `<module>exam-project-service</module>`

### Task 0.2: 创建 exam-project-service/pom.xml
**文件:** `G:\在线教育平台\exam-platform\exam-project-service\pom.xml`
**内容:** 标准 Spring Boot 服务模块，依赖 exam-common、spring-boot-starter-web、mybatis-plus、mysql-connector、druid

### Task 0.3: 创建启动类 + 目录结构
- `exam-project-service/src/main/java/com/exam/project/ExamProjectServiceApplication.java`
- 目录: controller/, service/, service/impl/, mapper/, model/entity/, model/dto/, model/vo/

---

## Phase 1: exam-common 公共模块

### Step 1.1 — 创建包结构
```
exam-common/src/main/java/com/exam/common/
├── entity/       # 11 个数据库实体
├── dto/          # 请求 DTO
├── vo/           # 响应 VO
├── common/       # Result, PageResult, BaseEntity, BusinessException, GlobalExceptionHandler
└── util/         # SnowflakeUtil
```

### Step 1.2 — BaseEntity（公共基类）
**文件:** `exam-common/src/main/java/com/exam/common/common/BaseEntity.java`
**内容:** `createdAt` (LocalDateTime, 自动填充), `updatedAt` (LocalDateTime, 自动填充)，使用 MyBatis-Plus `@TableField(fill = FieldFill.INSERT)` 等注解

### Step 1.3 — Result（统一响应体）
**文件:** `exam-common/src/main/java/com/exam/common/common/Result.java`
**内容:** 泛型类，字段: `code`(Integer), `message`(String), `data`(T), `timestamp`(Long)。静态工厂方法: `ok(T)`, `fail(int, String)`

### Step 1.4 — PageResult（分页响应体）
**文件:** `exam-common/src/main/java/com/exam/common/common/PageResult.java`
**内容:** 泛型类，字段: `records`(List<T>), `total`(Long), `page`(Integer), `size`(Integer)。静态方法: `of(IPage<T>)`

### Step 1.5 — BusinessException（业务异常）
**文件:** `exam-common/src/main/java/com/exam/common/common/BusinessException.java`
**内容:** 继承 RuntimeException，字段: code(Integer), message(String)。常用静态工厂: `notFound(String)`, `unauthorized()`, `forbidden()`

### Step 1.6 — GlobalExceptionHandler（全局异常处理）
**文件:** `exam-common/src/main/java/com/exam/common/common/GlobalExceptionHandler.java`
**内容:** `@RestControllerAdvice`，处理 BusinessException → 400/401/403/404，处理 MethodArgumentNotValidException → 400，处理 Exception → 500

### Step 1.7 — SnowflakeUtil（分布式 ID）
**文件:** `exam-common/src/main/java/com/exam/common/util/SnowflakeUtil.java`
**内容:** 基于 Hutool IdUtil.getSnowflake() 封装，提供 `nextId()` 静态方法

### Step 1.8-1.18 — 11 个实体类

每个实体按数据库文档（02-database-design.md）定义，使用 Lombok @Data、MyBatis-Plus @TableName/@TableId/@TableField 注解。

| Step | 文件 | 表 | 所属服务 |
|------|------|------|------|
| 1.8 | entity/Paper.java | paper | paper-service |
| 1.9 | entity/Question.java | question | paper-service |
| 1.10 | entity/Option.java | option | paper-service |
| 1.11 | entity/Response.java | response | answer-service |
| 1.12 | entity/Answer.java | answer | answer-service |
| 1.13 | entity/User.java | user | user-service |
| 1.14 | entity/Project.java | project | project-service |
| 1.15 | entity/ProjectUser.java | project_user | project-service |
| 1.16 | entity/Tool.java | tool | project-service |
| 1.17 | entity/Config.java | config | project-service |
| 1.18 | entity/Permission.java | permission | project-service |

### Step 1.19-1.24 — DTO 类

| Step | 文件 | 用途 |
|------|------|------|
| 1.19 | dto/LoginDTO.java | username + password，@NotBlank |
| 1.20 | dto/PaperCreateDTO.java | title, description, totalScore, passScore, durationMinutes, projectId, 层级字段 |
| 1.21 | dto/PaperUpdateDTO.java | 同 PaperCreateDTO，字段可选 |
| 1.22 | dto/QuestionCreateDTO.java | title, questionType, score, isRequired, sortOrder, List<OptionDTO> |
| 1.23 | dto/PublishDTO.java | paperId, targetUserIds(可选), 层级筛选(可选) |
| 1.24 | dto/AnswerSubmitDTO.java | answerContent, durationSeconds |

### Step 1.25-1.30 — VO 类

| Step | 文件 | 用途 |
|------|------|------|
| 1.25 | vo/PaperDetailVO.java | 试卷详情含题目列表 |
| 1.26 | vo/PaperAnswerVO.java | 答题页面（题目+选项，不含正确答案） |
| 1.27 | vo/MyPaperVO.java | 学员端试卷列表项 |
| 1.28 | vo/LoginVO.java | token, userId, role, realName |
| 1.29 | vo/UserVO.java | 用户信息（不含密码） |
| 1.30 | vo/ReviewDetailVO.java | 批阅详情（答题内容+正确答案+得分） |

---

## Phase 2: exam-project-service（项目服务）

### 2.1-2.4 Mapper
- `mapper/ProjectMapper.java` — 继承 BaseMapper<Project>
- `mapper/ProjectUserMapper.java` — 继承 BaseMapper<ProjectUser>
- `mapper/ToolMapper.java` — 继承 BaseMapper<Tool>
- `mapper/ConfigMapper.java` — 继承 BaseMapper<Config>

### 2.5-2.7 DTO
- `model/dto/ProjectCreateDTO.java`
- `model/dto/ConfigUpdateDTO.java`
- `model/dto/ProjectQueryDTO.java`

### 2.8-2.11 VO
- `model/vo/ProjectVO.java`
- `model/vo/ConfigVO.java`

### 2.12-2.15 Service
- `service/ProjectService.java` — CRUD + 加入/退出
- `service/impl/ProjectServiceImpl.java`
- `service/ConfigService.java` — 配置管理
- `service/impl/ConfigServiceImpl.java`

### 2.16-2.18 Controller
- `controller/ProjectController.java` — `/api/project`
  - GET list, GET {id}, POST, PUT {id}
  - POST {id}/join, DELETE {id}/leave
- `controller/ConfigController.java` — `/api/project/{id}/config`
  - GET, PUT

### 2.19 配置文件
- `src/main/resources/application.yml` — 端口 8087, MySQL/Redis 连接

---

## Phase 3-8: 后续服务

> 详细任务将在完成 Phase 1-2 后拆分，遵循相同模式。

| Phase | 服务 | 核心内容 |
|-------|------|----------|
| 3 | exam-user-service | UserMapper, UserService(login/current/teachers), UserController, Feign 客户端 |
| 4 | exam-paper-service | PaperMapper, QuestionMapper, OptionMapper, PaperService(CRUD/publish), QuestionService, PaperController, QuestionController |
| 5 | exam-answer-service | ResponseMapper, AnswerMapper, AnswerService(submit/auto-grade), ReviewService, AnswerController, ReviewController |
| 6 | exam-statistics-service | StatisticsService(paper/region/teacher stats), StatisticsController |
| 7 | exam-search-service | SearchService(ES query/rebuild), SearchController |
| 8 | exam-gateway | 路由配置, JWT 鉴权过滤器, 跨域配置 |

---

## 验证标准

每个 Phase 完成后：
1. `mvn compile -pl exam-common` 编译通过
2. 对应微服务 `mvn compile` 编译通过
3. 代码审查：所有文件有中文注释、类级别和函数级别注释完整

## 分支策略

在 feature/backend-implementation 分支上开发，每 Phase 完成一次 commit。
