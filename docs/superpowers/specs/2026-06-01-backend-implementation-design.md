# 后端业务代码实现设计

> 日期：2026-06-01 | 决策：按服务逐个推进，自底向上开发

## 核心决策

- **开发节奏**：按服务逐个推进，每完成一个服务暂停审阅
- **新增模块**：`exam-project-service`（项目管理、工具配置、权限配置）
- **开发顺序**：自底向上（common → project → user → paper → answer → statistics → search → gateway）
- **服务内部顺序**：Entity → DTO/VO → Mapper → Service → Controller（接口先行）

## 模块结构（8 个模块）

```
exam-common            ← 11 实体 + DTO/VO + 统一响应 + 工具类
exam-project-service   ← 新增：项目 CRUD、工具配置、权限管理
exam-user-service      ← 登录、用户查询、老师列表
exam-paper-service     ← 试卷 CRUD、题目/选项管理、发布分发
exam-answer-service    ← 答题、提交、自动批阅、校长批阅
exam-statistics-service ← 统计报表
exam-search-service    ← ES 检索、索引重建
exam-gateway           ← 路由转发、统一鉴权
```

## 预估文件量

| 服务 | 文件数 | 说明 |
|------|:---:|------|
| common | ~19 | 11 实体 + 8 DTO/VO |
| project-service | ~15 | 4 mapper + 4 service + 3 controller + DTO |
| user-service | ~9 | 1 mapper + 2 service + 2 controller + DTO |
| paper-service | ~19 | 3 mapper + 4 service + 6 controller + DTO |
| answer-service | ~18 | 2 mapper + 4 service + 6 controller + DTO |
| statistics-service | ~9 | 1 mapper + 2 service + 3 controller + DTO |
| search-service | ~6 | 1 mapper + 1 service + 2 controller + DTO |

## 数据库表 → 实体映射

| 表 | 服务 | 实体类 |
|------|------|------|
| paper | common | Paper |
| question | common | Question |
| option | common | Option |
| response | common | Response |
| answer | common | Answer |
| user | common | User |
| project | common | Project |
| project_user | common | ProjectUser |
| tool | common | Tool |
| config | common | Config |
| permission | common | Permission |

## API → 服务映射

按 `docs/05-api-design.md` 的 9 个接口组和 P0-P4 优先级：

- P0: 用户登录 + 试卷 CRUD + 题目管理 → user-service + paper-service
- P1: 答题提交 + 自动批阅 + 试卷列表 → answer-service
- P2: 试卷发布 + Kafka 分发 → paper-service + answer-service
- P3: 校长批阅 + 统计 → answer-service(review) + statistics-service
- P4: 检索 + 配置管理 + 项目服务 → search-service + project-service
