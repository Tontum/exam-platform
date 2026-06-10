# CLAUDE.md

> **最后更新：2026-06-10**

## 项目概述

教师培训在线考试系统 — 面向全国 40 万教师的在线培训与考核平台。三端：学员端（老师答题）、管理端（校长发布试卷+批阅）、管理后台（管理员配置项目权限）。Spring Boot + Vue 3。

## Quick Start

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

## 当前开发状态（2026-06-10）

**已完成：**
- 前端 19 个页面（学员端 5 页 + 管理端 7 页 + 管理后台 5 页 + 登录/注册）
- 后端 exam-project-service 完整业务代码（6 个 Controller、5 个 Service、12 个 Mapper）
- 数据库 12 张表建表 SQL + 种子数据（`sql/init/01-init.sql`）
- 层级权限体系（超级管理员/省级管理员/校长/老师）
- 答题链路：项目→试卷→答题→Redis 保存→提交→客观题自动评分→成绩查看
- 集成测试：ConfigControllerTest(9条)、AnswerControllerTest(3条)

**待开发：**
- 批阅功能（校长批阅主观题）— 前端 UI 已有，后端 API 缺失
- 统计分析 — 前端 UI 已有，后端 API 缺失
- Kafka 异步分发（试卷发布时批量创建 response）
- ES 全文搜索（题目搜索）
- API 网关（Spring Cloud Gateway）

## 技术栈

| 层 | 技术 | 端口 |
|------|------|------|
| 前端 | Vue 3 + TypeScript + Element Plus + Pinia + Axios | 3000 |
| 后端 | Spring Boot 3.2 + MyBatis-Plus + JDK 17 | 8087 |
| DB | MySQL 8.0 | 13306 |
| 缓存 | Redis 7.2 | 16379 |
| 消息 | Kafka 3.6 | 19092（未使用） |
| 搜索 | Elasticsearch 7.17 | 19200（未使用） |

## 权限层级

```
超级管理员 (role=1, scope=ALL)       → 看全国
省级管理员 (role=1, scope=PROVINCE)  → 只看本省
校长 (role=2, school_id)            → 管本校
老师 (role=3, school_id)            → 看自己的项目
```

## 项目结构

```
exam-platform/
├── sql/init/                        # 建表 + 种子数据
├── exam-common/                     # 公共实体/DTO/VO/工具类
├── exam-project-service/            # 【唯一有业务代码的服务】
│   ├── controller/                  # Auth/Project/User/Paper/Answer/Config
│   ├── service/impl/                # 业务逻辑
│   ├── mapper/                      # MyBatis-Plus Mapper
│   └── interceptor/                 # JWT + 项目权限拦截器
├── exam-frontend/                   # Vue 3 前端
│   ├── src/api/                     # API 封装
│   ├── src/stores/user.ts           # 用户状态
│   ├── src/router/index.ts          # 路由
│   └── src/views/                   # 三端页面
├── exam-gateway/                    # 空壳
├── exam-user-service/               # 空壳
├── exam-paper-service/              # 空壳
├── exam-answer-service/             # 空壳
├── exam-statistics-service/         # 空壳
└── exam-search-service/             # 空壳
```

## 开发原则

1. **文档先行** — 任何变更先确认文档覆盖
2. **表结构不可随意变更** — 变更同步 `01-init.sql`
3. **自底向上** — common 实体类 → 各微服务 Controller/Service/Mapper
4. **接口先行** — Controller + DTO 定义完成后，再写 Service 和 DAO
5. **所有代码必须写注释** — 中文注释
6. **每完成一个后端接口，必须编写集成测试并确保通过**

## 测试通过记录

| 日期 | 服务 | 测试类 | 接口数 | 测试数 | 结果 |
|------|------|--------|:---:|:---:|:---:|
| 2026-06-05 | exam-project-service | ConfigControllerTest | 6 | 9 | ✅ 全部通过 |
| 2026-06-08 | exam-project-service | AnswerControllerTest | 1 | 3 | ✅ 全部通过 |
