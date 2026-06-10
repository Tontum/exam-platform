# 教师培训在线考试系统

面向全国 40 万教师的在线培训与考核平台。采用微服务架构，支持试卷创建、异步高并发分发、在线答题、自动/手动批阅、数据统计等全流程。

## 项目文档导航

本项目以**文档驱动开发**，所有编码必须参照以下设计文档：

| 文档 | 路径 | 说明 |
|------|------|------|
| 技术选型 | [docs/01-tech-stack.md](docs/01-tech-stack.md) | 技术栈、版本号、兼容性说明 |
| 数据库设计 | [docs/02-database-design.md](docs/02-database-design.md) | 表结构、字段、索引、ER 关系 |
| 系统架构 | [docs/03-architecture.md](docs/03-architecture.md) | 微服务拆分、缓存架构、消息架构 |
| 功能设计 | [docs/04-features.md](docs/04-features.md) | 功能模块、业务流程、边界条件 |
| API 设计 | [docs/05-api-design.md](docs/05-api-design.md) | RESTful 接口规范 |
| 可靠性设计 | [docs/06-reliability-design.md](docs/06-reliability-design.md) | Kafka/Redis/MySQL 全链路故障方案 |
| 缓存设计 | [docs/07-cache-design.md](docs/07-cache-design.md) | 多级缓存架构与问题解决方案 |
| 统计与调优 | [docs/08-statistics-optimization.md](docs/08-statistics-optimization.md) | 数据统计需求 + SQL 优化 |
| ES 搜索 | [docs/09-es-design.md](docs/09-es-design.md) | ES 使用场景、索引设计与优化 |
| 高频问答 | [docs/10-faq.md](docs/10-faq.md) | 30 个常见问题与解答 |
| 开发规范 | [CLAUDE.md](CLAUDE.md) | Claude Code 协作入口 |

## 技术栈概览

- **后端框架**：Spring Boot 3.2 + Spring Cloud 2023.0
- **数据库**：MySQL 8.0 + MyBatis-Plus 3.5
- **缓存**：Redis 7.2 + Caffeine 3.1（多级缓存）
- **消息队列**：Kafka 3.6（异步分发、削峰填谷）
- **搜索引擎**：Elasticsearch 7.17 + IK 分词器 + Canal
- **分布式锁**：Redisson 3.25 + 看门狗机制
- **部署**：Docker + Docker Compose

## 微服务模块

| 模块 | 职责 |
|------|------|
| exam-gateway | API 网关（路由、鉴权、限流） |
| exam-common | 公共模块（工具类、实体、异常） |
| exam-user-service | 用户与权限服务 |
| exam-paper-service | 试卷服务（创建、题目管理、分发） |
| exam-answer-service | 答题服务（提交、自动批阅） |
| exam-statistics-service | 统计服务（分数、报表） |
| exam-search-service | 检索服务（ES 全文检索） |
| exam-project-service | 项目服务（配置、工具管理） |

## 核心业务流程

```
校长创建试卷 → 添加题目选项 → 选择老师发布 → Kafka 异步分发 → 
老师在线答题（Redis 实时存储）→ 提交试卷 → 客观题自动批阅 → 
校长批阅主观题 → 汇总分数 → 统计报表
```

## 快速开始（开发阶段后续完善）

```bash
# 1. 启动中间件
docker-compose up -d

# 2. 初始化数据库（执行 docs/sql/ 下的建表脚本）

# 3. 启动微服务（IDE 或命令行）
cd exam-gateway && mvn spring-boot:run
cd exam-user-service && mvn spring-boot:run
# ... 依此类推
```
