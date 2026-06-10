# CLAUDE.md

This file provides guidance to Claude when working with code in this repository.

## Project Overview

教师培训在线考试系统 — 面向全国 40 万教师的在线培训与考核平台。三端：学员端（老师答题）、管理端（校长发布试卷+批阅）、管理后台（管理员配置项目权限）。微服务架构，Spring Boot + Vue 3。

## Quick Start（当前状态启动方式）

```bash
# 1. 启动基础设施（端口已映射避免冲突：MySQL→13306, Redis→16379, Kafka→19092, ES→19200）
cd exam-platform
docker-compose up -d

# 2. 启动前端
cd exam-frontend
npm install
npm run dev        # → http://localhost:3000
```

## 当前开发状态（2026-05-18）

**已完成：**
- 前端完整页面（13 个页面 + 路由 + 布局），Vue 3 + TypeScript + Element Plus，mock 数据可预览
- 后端 Maven 多模块骨架（父 POM + 7 子模块 pom.xml，依赖已配齐）
- 数据库 11 张表建表 SQL + 种子数据（`sql/init/01-init.sql`，Docker 首次启动时自动执行）
- docker-compose.yml（MySQL 8.0 + Redis 7.2 + Kafka + ES 7.17 + Canal）

**待开发：**
- 后端业务代码（公共实体 → 各微服务 Controller/Service/Mapper）
- 后端 application.yml 中端口需改为映射端口（13306/16379/19092/19200）

## 文档驱动开发

所有开发必须参照 `docs/` 目录下的设计文档：
- `01-tech-stack.md` — 技术选型与版本规范
- `02-database-design.md` — 数据库设计（11 张表的 ER 图、字段、索引策略）
- `03-architecture.md` — 系统架构（微服务拆分、分层设计、部署架构）
- `04-features.md` — 功能模块详细设计（核心业务流程）
- `05-api-design.md` — RESTful API 接口规范
- `06-09` — 可靠性/缓存/统计/ES 设计
- `10-faq.md` — 高频问题与回答

## 技术栈

| 层 | 技术 | 版本 |
|------|------|------|
| 前端 | Vue 3 + TypeScript + Element Plus + Pinia + Axios | 见 package.json |
| 后端 | Spring Boot 3.2.x + Spring Cloud 2023.0.x + JDK 17 | 见父 pom.xml |
| ORM | MyBatis-Plus 3.5.x | |
| DB | MySQL 8.0（13306 端口映射） | Docker |
| 缓存 | Redis 7.2 + Caffeine + Redisson | Docker（16379） |
| 消息 | Kafka 3.6 | Docker（19092） |
| 搜索 | Elasticsearch 7.17 + IK 分词 + Canal 同步 | Docker（19200） |

## 项目结构

```
exam-platform/
├── docs/                       # 10 份设计文档（事实来源）
├── exam-frontend/              # Vue 3 前端（端口 3000）
│   └── src/
│       ├── router/index.ts     # 三端路由：/teacher /principal /admin
│       ├── views/teacher/      # 学员端 4 页：项目列表→试卷→答题→成绩
│       ├── views/principal/    # 管理端 6 页：试卷管理→创建→题目→批阅列表→批阅详情→统计
│       ├── views/admin/        # 后台 3 页：项目管理→角色配置→工具配置
│       ├── components/         # AppLayout + SideMenu
│       ├── api/index.ts        # Axios 封装，所有 TODO 标注了接口位置
│       └── stores/user.ts      # 用户状态（mock 切换角色）
├── exam-common/                # 公共模块（pom.xml 已配依赖，业务代码待写）
├── exam-gateway/               # API 网关
├── exam-user-service/          # 用户服务
├── exam-paper-service/         # 试卷服务
├── exam-answer-service/        # 答题服务
├── exam-statistics-service/    # 统计服务
├── exam-search-service/        # 搜索服务
├── sql/init/01-init.sql        # 建表 + 种子数据
├── docker-compose.yml          # 基础设施
└── pom.xml                     # 父 POM
```

## 开发原则

1. **文档先行** — 任何变更先确认文档覆盖
2. **表结构不可随意变更** — 以 `02-database-design.md` 为准，变更同步文档
3. **自底向上** — common 实体类 → 各微服务 Controller/Service/Mapper
4. **接口先行** — Controller + DTO 定义完成后，再写 Service 和 DAO
5. **所有代码必须写注释** — 中文注释，每个文件/函数说明用途
6. **前端 API 调用位置用 `// TODO: 调用 API` 标注** — 参考对应页面找到接口位置
7. **【HARD RULE】每完成一个后端接口，必须编写集成测试并确保通过，测试结果写入本文件末尾** — 详见下方「测试强制规则」

---

## 测试强制规则（HARD RULE — 不可跳过）

> 因为 Claude 无法读取 IDEA 控制台输出，测试是唯一可验证代码正确性的手段。

### 规则

1. **每个 Controller 的每个接口必须至少有一条集成测试** — 覆盖正常请求和关键异常路径
2. **测试必须连接真实 Docker MySQL** — 端口 13306，数据库 exam_platform，不 mock 数据库层
3. **测试类命名**：`{ControllerName}Test.java`，放在 `src/test/java/com/exam/{module}/controller/` 下
4. **测试方法命名**：`test{接口描述}_{预期结果}`，如 `testListProjects_ShouldReturnPageResult`
5. **每写完一个服务的所有测试，必须运行 `mvn test -pl exam-{service}-service`**，全部通过后才能在下方登记
6. **测试未通过时不得标记服务为"已完成"，不得进入下一个开发任务**

### 工作流程

```
写完接口 → 写测试 → mvn test → 全部通过 ✓ → 登记到 CLAUDE.md → 继续下一个
                                 → 失败 ✗ → 修复 → 重新运行 → 通过后再登记
```

### 测试结果登记格式

在本文末 `## 测试通过记录` 下追加：

```
| 日期 | 服务 | 测试类 | 接口数 | 测试数 | 结果 |
|------|------|--------|:---:|:---:|:---:|
| YYYY-MM-DD | exam-xxx-service | XxxControllerTest | N | M | ✅ 全部通过 |
```

---

## 测试通过记录

| 日期 | 服务 | 测试类 | 接口数 | 测试数 | 结果 |
|------|------|--------|:---:|:---:|:---:|
| 2026-06-05 | exam-project-service | ConfigControllerTest | 6 | 9 | ✅ 全部通过 |
| 2026-06-08 | exam-project-service | AnswerControllerTest | 1 | 3 | ✅ 全部通过 |
