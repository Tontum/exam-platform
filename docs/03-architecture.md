# 03 - 系统架构设计

> 本文档定义教师培训在线考试系统的系统架构、微服务拆分、分层设计、部署架构和关键技术决策。

---

## 1. 总体架构：微服务 + 异步消息 + 多级缓存

```
                          ┌──────────────────┐
                          │   前端 (Vue/React) │
                          └────────┬─────────┘
                                   │ HTTPS
                                   ▼
                          ┌──────────────────┐
                          │  API Gateway      │
                          │  (Spring Cloud    │
                          │   Gateway)        │
                          └────────┬─────────┘
                                   │ 服务路由 + 限流(Sentinel)
          ┌──────────┬──────────┬──┴──────┬──────────┬───────────┐
          ▼          ▼          ▼         ▼          ▼           ▼
    ┌──────────┐┌──────────┐┌──────────┐┌──────────┐┌──────────┐┌──────────┐
    │ 用户服务  ││ 试卷服务  ││ 答题服务  ││ 统计服务  ││ 检索服务  ││ 项目服务  │
    │ user-    ││ paper-   ││ answer-  ││statistics││ search-  ││ project- │
    │ service  ││ service  ││ service  ││ service  ││ service  ││ service  │
    └────┬─────┘└────┬─────┘└────┬─────┘└────┬─────┘└────┬─────┘└────┬─────┘
         │           │          │          │          │          │
         │           │          │          │          │          │
    ┌────┴───────────┴──────────┴──────────┴──────────┴──────────┴────┐
    │                        中间件层                                  │
    │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
    │  │  MySQL   │  │  Redis   │  │  Kafka   │  │  ES      │        │
    │  │  (主库)   │  │  (缓存)   │  │  (消息)   │  │  (搜索)   │        │
    │  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
    └─────────────────────────────────────────────────────────────────┘
```

## 2. 微服务拆分与职责

### 2.1 服务清单

| 服务模块 | 职责 | 核心功能 |
|---------|------|----------|
| exam-gateway | API 网关 | 路由转发、统一鉴权、限流（Sentinel） |
| exam-common | 公共模块 | 工具类、通用实体、异常处理、统一响应 |
| exam-user-service | 用户与权限服务 | 登录注册、角色管理、层级管理、项目关联 |
| exam-paper-service | 试卷服务 | 试卷 CRUD、题目管理、选项管理、试卷分发 |
| exam-answer-service | 答题服务 | 提交答案（含 Redis 暂存）、提交试卷、自动批阅 |
| exam-statistics-service | 统计服务 | 分数统计、合格率分析、报表生成、分布式锁调度 |
| exam-search-service | 检索服务 | Elasticsearch 试题检索、Canal 数据同步 |
| exam-project-service | 项目服务 | 项目管理、工具配置、权限配置 |

### 2.2 服务职责边界

**exam-paper-service（试卷服务）**：
- 试卷的增删改查（仅校长和管理员有权限）
- 题目和选项的增删改查
- 调用 user-service 查询老师列表（按层级筛选）
- 试卷发布时向 Kafka 发送分发消息，不直接操作 response 表

**exam-answer-service（答题服务）**：
- 接收 Kafka 分发消息，批量写入 response 表
- 老师提交答案时先写入 Redis，再定时落库 MySQL
- 提交试卷时更新 response 状态
- 自动批阅客观题（比对 answer 与 option.is_correct）

**exam-statistics-service（统计服务）**：
- 定时统计任务（凌晨执行，使用分布式锁确保单实例执行）
- 考核积分统计（每提交/阅读加分配置）
- 合格率、平均分等报表
- 数据对账（Redis vs MySQL）

**exam-search-service（检索服务）**：
- 监听 Canal 推送的 MySQL binlog 变更，实时同步到 ES
- 全文检索 + 结构化筛选（题型、难度、知识点）
- ES 数据补偿与一致性校验

## 3. 分层架构（单服务内部）

每个微服务采用标准分层结构：

```
└── exam-{xxx}-service/
    ├── controller/     # 控制器层：接收 HTTP 请求，参数校验
    ├── service/        # 业务逻辑层：核心业务 + 事务管理
    │   └── impl/
    ├── mapper/         # 数据访问层：MyBatis-Plus Mapper 接口
    ├── model/
    │   ├── entity/     # 数据库实体类
    │   ├── dto/        # 数据传输对象（请求/响应）
    │   └── vo/         # 视图对象（前端展示专用）
    ├── feign/          # OpenFeign 远程调用客户端
    ├── config/         # 配置类
    └── util/           # 工具类
```

**调用链：** Controller → Service → Mapper → Database
**跨服务调用链：** Service → Feign Client → 远程服务 Controller → Service

## 4. 多级缓存架构

```
请求 → Caffeine 本地缓存（一级，1ms）
           ↓ 未命中
        Redis 缓存（二级，<5ms）
           ↓ 未命中
        MySQL 数据库（三级，>20ms）
           ↓ 回填
        写入 Caffeine + Redis → 返回结果
```

**缓存策略矩阵：**

| 数据类别 | Caffeine（一级） | Redis（二级） | 失效策略 |
|----------|:---:|:---:|----------|
| 试卷基本信息 | ✅ 1min | ✅ 5min | 试卷修改 → 删除缓存 |
| 题目列表 | ✅ 1min | ✅ 5min | 试卷修改 → 删除缓存 |
| 用户权限/配置 | — | ✅ 10min | 配置变更 → 删除缓存 |
| 实时答题数据 | — | ✅ 持久（5s落库） | 落库后清 Redis |
| 统计数据 | — | ✅ 30min | 定时刷新 |
| 热点试卷 | ✅ 自动（Caffeine LRU） | ✅ 手动预热 | 凌晨预热 |

### 防缓存穿透/击穿

- **穿透**：布隆过滤器（Bloom Filter）拦截不存在的数据
- **击穿**：Redis 分布式锁，仅一个线程查库，其余等待
- **雪崩**：Redis 主从 + 过期时间随机化（±20%）

## 5. 异步消息架构（Kafka）

### 5.1 Topic 定义

| Topic | 生产者 | 消费者 | 说明 |
|-------|--------|--------|------|
| `paper_distribute` | paper-service | answer-service | 试卷分发消息 |
| `answer_sync` | answer-service | statistics-service | 答题数据同步 |
| `paper_expired` | paper-service（定时） | answer-service | 试卷过期通知 |

### 5.2 高并发分发流程

```
校长点击"发布试卷"
  │
  ├─ 1. paper-service 基础校验（权限、试卷状态）
  ├─ 2. 查询 user-service 获取目标老师列表
  ├─ 3. 构建分发消息 → 发送到 Kafka (200ms 内返回"分发中")
  │
  ▼
Kafka Topic: paper_distribute
  │  (分区消费，按 paper_id 哈希)
  ▼
answer-service 消费者（3实例）
  ├─ 拉取消息 → 写入本地缓冲队列
  ├─ 批量写入 (每500条) → INSERT INTO response
  ├─ 失败 → 死信队列 → 人工排查后重试
  └─ 成功 → 手动提交 Offset
```

**Kafka 可靠性保障：**
- 生产者 `acks=all`，3 副本同步确认
- 消费者手动提交 offset（写入 DB 后提交）
- 死信队列兜底失败消息

## 6. Redis 实时存储与分布式锁

### 6.1 答题实时存储流程

```
老师答题（每道题提交）
  │
  ├─ 写入 Redis String: answer:{response_id}:{question_id}
  │   → 存储 JSON: {questionId, answerContent, timestamp}
  │
  ├─ 更新 Redis Hash: paper_stat:{paper_id}
  │   → 实时累加: {totalSubmit, correctCount, avgTime}
  │
  ├─ 定时任务（每5秒）：
  │   ├─ Lua 脚本校验数据 → 合法数据批量 INSERT MySQL
  │   ├─ 清空已处理 Redis 数据
  │   └─ 失败数据标记"待处理"，1分钟后重试
  │
  └─ 自动保存进度（每3秒）：
      → 老师断网重连后可恢复答题进度
```

### 6.2 分布式锁（统计任务）

```
定时统计任务触发
  │
  ├─ SET lock:statistics:{paper_id} {UUID} NX EX 30
  │    ├─ 获取成功 → 执行统计 → Lua脚本释放锁（校验UUID）
  │    └─ 获取失败 → 其他实例已在执行，跳过
  │
  ├─ Redisson 看门狗：每10秒续期30秒（防止锁超时）
  │
  └─ 凌晨错峰统计 + SETNX 分布式锁
```

## 7. Elasticsearch 检索架构

```
MySQL binlog 变更
  │
  ▼
Canal 监听 → 实时推送到
  │
  ▼
Elasticsearch (100万+试题)
  ├─ 10 分片，每分片 10 万条
  ├─ IK 分词器（中文分词）
  ├─ 全文检索（题干、解析、知识点字段）
  └─ 结构化筛选（题型、难度、知识点）
      → 查询耗时: 50ms 以内
```

**数据一致性保障：**
- Canal 实时同步（正常路径）
- 每小时补偿任务对比 ES 与 MySQL 总量
- ES 宕机 → 本地消息队列缓存 → 恢复后回补

## 8. Docker 本地部署架构

```yaml
services:
  mysql:        # MySQL 8.0，端口 3306
  redis:        # Redis 7.2（单机，开发环境），端口 6379
  kafka:        # Kafka 3.6 + Zookeeper，端口 9092
  elasticsearch: # ES 7.17，端口 9200
  canal:        # Canal 1.1，端口 11111（可选）
  # 各微服务通过 docker-compose 启动或 IDE 直接运行
```

微服务通过 IDE（IDEA）直接启动，连接 Docker 中的中间件，方便调试。

## 9. 关键技术决策记录

| 决策 | 方案 | 理由 |
|------|------|------|
| 试卷分发 | Kafka 异步批量，非同步循环 INSERT | 同步方式 1 万次 INSERT 耗时过长，影响用户体验 |
| 答题暂存 | Redis String + 5s 定时落库 | 高频写入场景，Redis 比 MySQL 快 100 倍 |
| 分布式 ID | 雪花算法 | 分布式环境下避免主键冲突，趋势递增利于索引 |
| 多级缓存 | Caffeine → Redis → MySQL | 99% 请求走缓存，QPS 从 500 提升到 5000+ |
| 缓存更新 | 先更新 DB → 删除缓存 | 避免更新缓存成本高、并发出错的经典问题 |
| 数据补偿 | 查询时按需补偿 + 异步批量补偿 | 新增老师/试卷变动后自动补齐 response 记录 |
| 统计任务 | 分布式锁 + 凌晨错峰 | 避免多实例重复统计，错峰减轻 DB 压力 |
| ES 同步 | Canal binlog 监听 + 定时补偿 | 实时性高 + 补偿兜底，保证最终一致性 |
