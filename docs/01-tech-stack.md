# 01 - 技术选型与版本规范

> 本文档定义教师培训在线考试系统的完整技术栈与版本号，所有模块开发必须严格遵循此规范。

---

## 1. 后端核心技术

| 技术 | 版本 | 用途 | 备注 |
|------|------|------|------|
| JDK | 17 LTS | Java 运行环境 | 长期支持版本 |
| Spring Boot | 3.2.x | 微服务基础框架 | 与 Spring Cloud 2023.0.x 配套 |
| Spring Cloud | 2023.0.x | 微服务治理（Gateway、OpenFeign、LoadBalancer） | 对应 Boot 3.2.x |
| Spring Cloud Gateway | 4.1.x | API 网关 | 替代 Zuul，基于 WebFlux |
| Spring Cloud OpenFeign | 4.1.x | 服务间 RPC 调用 | 声明式 HTTP 客户端 |
| MyBatis-Plus | 3.5.x | ORM 框架 | 增强 MyBatis，简化单表 CRUD |
| MySQL | 8.0.x | 关系型数据库（主库） | InnoDB 引擎，UTF-8 编码 |
| Druid | 1.2.x | 数据库连接池 | 阿里巴巴开源，自带监控 |

## 2. 缓存与 NoSQL

| 技术 | 版本 | 用途 | 备注 |
|------|------|------|------|
| Redis | 7.2.x | 分布式缓存、实时存储、分布式锁 | 主从 + 哨兵高可用 |
| Caffeine | 3.1.x | JVM 本地缓存（一级缓存） | 高性能进程内缓存 |
| Redisson | 3.25.x | Redis 分布式锁、看门狗自动续期 | 基于 Netty 的 Redis 客户端 |
| Spring Cache | — | 缓存抽象层 | 注解驱动，统一缓存 API |

## 3. 消息队列与异步

| 技术 | 版本 | 用途 | 备注 |
|------|------|------|------|
| Apache Kafka | 3.6.x | 异步消息、试卷分发削峰 | 高吞吐、持久化、分区消费 |
| Spring Kafka | 3.1.x | Kafka 集成 | 与 Spring Boot 3.2.x 配套 |

## 4. 搜索引擎

| 技术 | 版本 | 用途 | 备注 |
|------|------|------|------|
| Elasticsearch | 7.17.x | 试题全文检索 | 成熟稳定版本 |
| IK Analyzer | 7.17.x | 中文分词插件 | 与 ES 版本严格对应 |
| Canal | 1.1.x | MySQL binlog 实时同步到 ES | 阿里巴巴开源 |

## 5. 工具与基础设施

| 技术 | 版本 | 用途 | 备注 |
|------|------|------|------|
| Docker | 24.x | 容器化运行时 | 本地开发与生产部署 |
| Docker Compose | v2 | 本地多容器编排 | 一键启动 MySQL、Redis、Kafka、ES |
| Maven | 3.9.x | 项目构建管理 | wrapper 锁定版本 |
| Lombok | 1.18.x | 简化 Java Bean | 编译期注解 |
| Hutool | 5.8.x | Java 工具类库 | 国产常用工具集 |
| Fastjson2 | 2.0.x | JSON 序列化 | 高性能 JSON 处理 |
| Snowflake (Hutool) | — | 分布式 ID 生成 | Hutool IdUtil 内置 |
| Sentinel | 1.8.x | 流量控制、熔断降级 | 阿里巴巴开源 |
| Spring Validation | — | 参数校验 | JSR-303 标准 |

## 6. 测试与监控

| 技术 | 版本 | 用途 |
|------|------|------|
| JUnit 5 | 5.10.x | 单元测试 |
| Mockito | 5.x | Mock 测试 |
| Spring Boot Test | — | 集成测试支持 |

## 7. 版本兼容性说明

- **Spring Boot 3.2.x** 要求 **JDK 17+**，放弃 Java 8/11
- **Spring Cloud 2023.0.x** 是 Boot 3.2.x 对应的微服务版本，内部组件版本自动对齐
- **MyBatis-Plus 3.5.x** 完全兼容 Spring Boot 3.x
- **Elasticsearch 7.17.x** 是目前生产环境使用最广泛的版本，社区支持成熟
- **Kafka 3.6.x** 适配 Spring Kafka 3.1.x，两者兼容
- 本地开发所有中间件通过 **Docker Compose** 统一管理，不要求开发者本机安装

## 8. 本地开发环境搭建

```bash
# 第一步：启动所有中间件
docker-compose up -d

# 启动的容器包括：
# - MySQL 8.0        → 3306
# - Redis 7.2        → 6379
# - Kafka 3.6        → 9092
# - Elasticsearch 7.17 → 9200
# - Canal 1.1        → 11111（如果配置了）
```

详细配置见项目根目录 `docker-compose.yml`（后续开发时创建）。
