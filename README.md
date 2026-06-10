# 教师培训在线考试系统

面向全国 40 万教师的在线培训与考核平台。

## 三端架构

| 端 | 路由 | 角色 | 功能 |
|---|------|------|------|
| 学员端 | `/teacher` | 老师 | 答题、查成绩 |
| 管理端 | `/principal` | 校长 | 发布试卷、批阅 |
| 管理后台 | `/admin` | 管理员 | 配置项目、管理用户 |

## 技术栈

- **前端**：Vue 3 + TypeScript + Element Plus + Pinia
- **后端**：Spring Boot 3.2 + MyBatis-Plus + JDK 17
- **数据库**：MySQL 8.0 + Redis 7.2
- **基础设施**：Docker Compose（MySQL/Redis/Kafka/ES）

## 快速启动

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

## 项目结构

```
exam-platform/
├── sql/init/                  # 建表 + 种子数据
├── exam-common/               # 公共实体/DTO/VO
├── exam-project-service/      # 后端业务服务（唯一有代码的服务）
├── exam-frontend/             # Vue 3 前端
├── exam-gateway/              # API 网关（待开发）
├── exam-user-service/         # 用户服务（待开发）
├── exam-paper-service/        # 试卷服务（待开发）
├── exam-answer-service/       # 答题服务（待开发）
├── exam-statistics-service/   # 统计服务（待开发）
└── exam-search-service/       # 搜索服务（待开发）
```

## 数据库

12 张表：user, school, project, project_user, project_school, tool, config, permission, paper, question, option, response, answer

建表 SQL：`exam-platform/sql/init/01-init.sql`

## 文档

- [AI 开发接手指南](docs/AI-HANDOFF.md) — **新 AI 必读**
- [项目规范](docs/00-conventions.md)
- [技术栈](docs/01-tech-stack.md)
- [数据库设计](docs/02-database-design.md)
- [架构设计](docs/03-architecture.md)
- [功能清单](docs/04-features.md)
- [API 设计](docs/05-api-design.md)
- [测试指南](docs/12-testing-guide.md)

## 当前状态

- ✅ 前端 19 个页面
- ✅ 后端 6 个 Controller + 5 个 Service
- ✅ 数据库 12 张表 + 种子数据
- ✅ 层级权限体系
- ✅ 答题链路（项目→试卷→答题→提交→评分→成绩）
- ⬜ 批阅功能（校长批阅主观题）
- ⬜ 统计分析
- ⬜ Kafka/ES 集成

## 测试

| 日期 | 测试类 | 测试数 | 结果 |
|------|--------|:---:|:---:|
| 2026-06-05 | ConfigControllerTest | 9 | ✅ |
| 2026-06-08 | AnswerControllerTest | 3 | ✅ |
