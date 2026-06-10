# 07 - 多级缓存设计与可靠性方案

> 本文档定义教师培训在线考试系统的多级缓存架构，包括每一级缓存存了什么、Key 设计规则、失效策略、扩展预留，以及缓存常见问题的解决方案。

---

## 1. 三级缓存总览

```
请求到达
  │
  ├─ 第①步：查 Caffeine（本地 JVM 内存）
  │   命中 → 返回（耗时 <1ms）
  │   未命中 ↓
  │
  ├─ 第②步：查 Redis（分布式缓存）
  │   命中 → 回填 Caffeine → 返回（耗时 <5ms）
  │   未命中 ↓
  │
  ├─ 第③步：查 MySQL（数据库）
  │   命中 → 回填 Redis → 回填 Caffeine → 返回（耗时 >20ms）
  │   未命中 →
  │     ├─ 如果是合法但不存在的数据 → 缓存空值标记（防穿透）
  │     └─ 如果是非法不存在的请求 → 布隆过滤器直接拦截
```

**各级缓存的定位**：

| 层级 | 技术 | 定位 | 数据量级 | 生效范围 |
|------|------|------|----------|----------|
| L1 | Caffeine | 热点数据的极速通道 | 几百～几千条，LRU 淘汰 | 单 JVM 实例内 |
| L2 | Redis | 共享数据的分布式缓存 | 几十万条 | 所有服务实例共享 |
| L3 | MySQL | 数据的最终事实来源 | 全量 | 所有服务实例共享 |

---

## 2. 每一级缓存存了什么

### 2.1 试卷信息缓存

**缓存原因**：老师进入试卷、查看试卷列表时高频访问。10 万老师同时查同一张试卷，不去 Redis 查 10 万次就应该从 Caffeine 本地返回。

**Caffeine（L1）**：

| Key | Value | TTL | 最大容量 |
|-----|-------|-----|----------|
| `paper:info:{paperId}` | `PaperCacheDTO`（JSON 字符串） | 60 秒 | 500 条 |

PaperCacheDTO 包含：试卷名称、总分、及格分、答题时长、状态、发布人姓名、题目数量。**不含**题目详情（题目列表单独缓存）。

**Redis（L2）**：

| Key | Value | TTL |
|-----|-------|-----|
| `paper:info:{paperId}` | `PaperCacheDTO`（JSON 字符串） | 300 秒 |

**失效时机**：试卷被编辑 → 先改 MySQL → 删 Redis key → Caffeine 自然过期（最多 60 秒后读到新数据）。

---

### 2.2 题目与选项缓存

**缓存原因**：老师进入试卷答题页时，需要一次性加载所有题目和选项。这是一次查询量最大、并发最集中的场景。

**Caffeine（L1）**：

| Key | Value | TTL | 最大容量 |
|-----|-------|-----|----------|
| `paper:questions:{paperId}` | `List<QuestionWithOptionsDTO>`（JSON） | 60 秒 | 300 条 |

QuestionWithOptionsDTO 包含每道题的题干、类型、分值、是否必答、选项列表（含正确选项标记）。

**Redis（L2）**：

| Key | Value | TTL |
|-----|-------|-----|
| `paper:questions:{paperId}` | `List<QuestionWithOptionsDTO>`（JSON） | 300 秒 |

**敏感信息控制**：
- 答题时：前端需要选项但**不需要**正确答案 → 缓存中包含 `isCorrect` 字段，但序列化到答题接口响应时做过滤
- 批阅时：校长需要看到正确答案 → 不过滤

**失效时机**：试卷的题目被增删改 → 删 Redis key + 对应的 paper:info key。

---

### 2.3 老师试卷列表缓存

**缓存原因**：老师点击"试题"菜单时要展示自己的试卷列表（含状态标签），每人每次登录都要查。

| 缓存层级 | Key | Value | TTL |
|----------|-----|-------|-----|
| Redis（L2） | `teacher:papers:{userId}` | `List<MyPaperVO>`（JSON） | 120 秒 |

MyPaperVO 包含：paperId、试卷名、responseId、状态、得分、提交时间、是否合格。

**注意**：不放 Caffeine。原因——这个列表因人而异，Caffeine 的 LRU 池会被大量不同 userId 的 key 撑满，命中率很低，反而浪费内存。只在 Redis 中缓存。

**失效时机**：
- 老师提交试卷 → 删该 key
- 新试卷分发到该老师 → 删该 key（在 Kafka 消费端处理完 response 批量插入后）

---

### 2.4 权限与配置缓存

**缓存原因**：老师每进入一个项目、每打开一个菜单，都要查 config 表判断当前角色有没有权限。这个查询极其频繁，但数据极少变动。

| 缓存层级 | Key | Value | TTL |
|----------|-----|-------|-----|
| Redis（L2） | `config:project:{projectId}` | 该项目下所有工具 × 所有角色的权限 Map | 600 秒 |

数据结构示例：
```json
{
  "projectId": 100,
  "configs": {
    "paper": {
      "role_2": { "isEnabled": true, "allowPublish": true, "allowReview": true },
      "role_3": { "isEnabled": true, "allowPublish": false, "allowReview": false }
    },
    "article": {
      "role_2": { "isEnabled": true, "allowPublish": true },
      "role_3": { "isEnabled": true, "allowPublish": false }
    }
  }
}
```

**不放 Caffeine**：配置可能较大，且访问模式相对分散。

**失效时机**：管理员修改了项目配置 → 删该 key。

---

### 2.5 统计数据缓存

**缓存原因**：统计查询计算量大，且不需要实时精度（允许 30 分钟延迟）。

| 缓存层级 | Key | Value | TTL |
|----------|-----|-------|-----|
| Redis（L2） | `stat:paper:{paperId}` | 试卷统计结果（平均分、合格率、各分数段） | 1800 秒 |

| 缓存层级 | Key | Value | TTL |
|----------|-----|-------|-----|
| Redis（L2） | `stat:region:{paperId}:{level}` | 某试卷在某层级下的统计结果 | 1800 秒 |

**失效时机**：定时刷新（每 30 分钟重新计算一次），不根据单条数据变更触发（因为统计是聚合结果，逐条触发刷新没有意义且开销过大）。

---

### 2.6 答题实时数据（特殊用途的 Redis 缓存）

这部分已在 `docs/06-reliability-design.md` 详细设计，此处仅做索引：

| Key 模式 | 用途 | 生命周期 |
|----------|------|----------|
| `answer:single:{responseId}:{questionId}` | 老师提交的单题答案 | 落库 MySQL 后删除 |
| `answer:flush:queue` | 待落库队列 | 定时消费后清空 |
| `answer:progress:{responseId}` | 答题进度（断网恢复用） | 提交试卷后删除 |
| `paper:stat:{paperId}` (Hash) | 实时统计累加 | 不删除，持续累加 |

---

### 2.7 缓存 Key 设计规范

为了保证扩展性（后续新增缓存模块时不混乱），所有 Key 遵循统一规范：

```
{domain}:{subdomain}:{identifier}

示例：
  paper:info:1288291          ← 试卷信息
  paper:questions:1288291     ← 试卷题目
  teacher:papers:88372        ← 老师试卷列表
  config:project:100          ← 项目配置
  stat:paper:1288291          ← 试卷统计
  stat:region:1288291:city    ← 区域统计
  answer:single:1001:25       ← 答题单题
  answer:progress:1001        ← 答题进度
  lock:statistics:1288291     ← 分布式锁
  kafka:processed:uuid-xxx    ← 消息去重标记
```

新增缓存模块时，选择一个未占用的 `domain` 前缀即可，不会与现有 Key 冲突。

---

## 3. 缓存常见问题与解决方案

### 3.1 缓存穿透

**现象**：大量请求查询一个数据库中根本不存在的 key（比如伪造的 paperId=-999），缓存中没有，请求直接穿透到 MySQL。

**解决方案——布隆过滤器**：

启动时将所有有效 paperId 加载到布隆过滤器：
```java
@Component
public class PaperBloomFilter {
    
    private BloomFilter<Long> filter;
    private final long expectedInsertions = 1_000_000L; // 预期 100 万试卷
    private final double fpp = 0.01;                    // 1% 误判率
    
    @PostConstruct
    public void init() {
        filter = BloomFilter.create(
            Funnels.longFunnel(), expectedInsertions, fpp
        );
        // 从 DB 加载所有有效 paperId
        List<Long> paperIds = paperMapper.selectAllIds();
        paperIds.forEach(filter::put);
    }
    
    public boolean mightContain(Long paperId) {
        return filter.mightContain(paperId);
    }
    
    // 新试卷发布时增量添加
    public void add(Long paperId) {
        filter.put(paperId);
    }
}

// 在缓存查询入口处拦截
public PaperCacheDTO getPaper(Long paperId) {
    if (!bloomFilter.mightContain(paperId)) {
        return null;  // 布隆过滤器说不存在 → 直接返回，不查 DB
    }
    // 正常走三级缓存...
}
```

**补充措施**：对于布隆过滤器判断"可能存在"但 DB 中确实不存在的情况（1% 误判），缓存一个短 TTL 的空值标记：
```java
redisTemplate.opsForValue()
    .set("paper:info:" + paperId, "NULL", Duration.ofSeconds(30));
```

### 3.2 缓存击穿

**现象**：某个热点的缓存 key 刚好在过期那一刻，同时有大量请求涌入，全部穿透到 MySQL。

**热点举例**：某张正在进行中的热门试卷，1 万老师同时答题，`paper:questions:{paperId}` 的 Caffeine 和 Redis 缓存同时过期。

**解决方案——分布式锁控制单线程回源**：

```java
public List<QuestionWithOptionsDTO> getPaperQuestions(Long paperId) {
    // 1. 先查 Caffeine
    String caffeineKey = "paper:questions:" + paperId;
    List<QuestionWithOptionsDTO> cached = caffeineCache.getIfPresent(caffeineKey);
    if (cached != null) return cached;
    
    // 2. 再查 Redis
    String redisKey = "paper:questions:" + paperId;
    String redisValue = redisTemplate.opsForValue().get(redisKey);
    if (redisValue != null) {
        List<QuestionWithOptionsDTO> result = JSON.parseArray(redisValue, ...);
        caffeineCache.put(caffeineKey, result);  // 回填 Caffeine
        return result;
    }
    
    // 3. 缓存都未命中 → 分布式锁控制回源
    String lockKey = "lock:cache:paper:questions:" + paperId;
    RLock lock = redissonClient.getLock(lockKey);
    
    try {
        if (lock.tryLock(2, 10, TimeUnit.SECONDS)) {
            // Double Check：获取锁后再次查 Redis
            redisValue = redisTemplate.opsForValue().get(redisKey);
            if (redisValue != null) {
                // 前面的线程已经回源过了，直接用
                List<QuestionWithOptionsDTO> result = JSON.parseArray(redisValue, ...);
                caffeineCache.put(caffeineKey, result);
                return result;
            }
            
            // 真正的回源：查 DB
            List<QuestionWithOptionsDTO> fromDB = questionMapper
                .selectQuestionsWithOptionsByPaperId(paperId);
            
            // 回填 Redis
            redisTemplate.opsForValue().set(redisKey, 
                JSON.toJSONString(fromDB), Duration.ofSeconds(300));
            // 回填 Caffeine
            caffeineCache.put(caffeineKey, fromDB);
            
            return fromDB;
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
    
    // 没拿到锁 → 短暂等待后重试（此时锁持有者已回填了缓存）
    try { Thread.sleep(50); } catch (InterruptedException e) { }
    return getPaperQuestions(paperId);  // 递归重试，最多 3 次
}
```

**关键点**：Double Check 防止多个等锁的线程排队去查 DB；没拿到锁的线程 sleep 后重试而非自旋，避免浪费 CPU。

### 3.3 缓存雪崩

**现象**：大量缓存 key 在同一时刻集中过期，大量请求同时穿透到 DB。

**解决方案**：设置 Redis TTL 时加随机偏移。

```java
public void cacheWithJitter(String key, Object value, int baseTTLSeconds) {
    // 在基准 TTL 上 ±20% 的随机偏移
    int jitter = ThreadLocalRandom.current()
        .nextInt((int)(baseTTLSeconds * 0.2));
    int actualTTL = baseTTLSeconds + (ThreadLocalRandom.current().nextBoolean() 
        ? jitter : -jitter);
    
    redisTemplate.opsForValue().set(key, JSON.toJSONString(value), 
        Duration.ofSeconds(actualTTL));
}
```

示例效果：300 秒基准 TTL 的缓存，实际过期时间分布在 240～360 秒之间，自然错开。

### 3.4 缓存与数据库一致性

**原则**：先更新 DB，后删除缓存（不是更新缓存）。

```
正确做法：  UPDATE paper SET title='新标题' WHERE id=?  →  DEL paper:info:{id}
错误做法：  DEL paper:info:{id}  →  UPDATE paper SET title='新标题' WHERE id=?
                     ↑ 删除后、更新前，有请求进来读了旧数据并回填了缓存，缓存中永久是旧数据
```

```java
@Transactional
public void updatePaper(Long paperId, PaperUpdateDTO dto) {
    // 1. 更新 DB
    paperMapper.updateById(paperEntity);
    
    // 2. 删除 Redis 缓存（多条关联的 key 都要删）
    redisTemplate.delete("paper:info:" + paperId);
    redisTemplate.delete("paper:questions:" + paperId);
    
    // 3. Caffeine 不主动删：等它自然过期（最多 60 秒）
    //    如果业务要求强一致，可以发 MQ 广播，让各实例删 Caffeine
    //    对于本系统：试卷修改后 60 秒内有概率看到旧数据是可接受的
}
```

**关于 Caffeine 本地缓存一致性的取舍**：

多实例部署时，A 实例更新了 DB + Redis，但 B 实例的 Caffeine 中还缓存着旧数据。60 秒 TTL 意味着最多 60 秒的不一致窗口。对于本系统而言（试卷修改后老师看到旧数据 60 秒不影响答题正确性），这个代价可接受。如果未来需要强一致，可以在 Redis Pub/Sub 通知各实例清 Caffeine：

```java
// 扩展预留：分布式 Caffeine 失效通知
redisTemplate.convertAndSend("cache:invalidate:caffeine", "paper:questions:" + paperId);

// 各实例监听
@RedisMessageListener(topic = "cache:invalidate:caffeine")
public void onCacheInvalidate(String key) {
    caffeineCache.invalidate(key);
}
```

### 3.5 缓存预热

**现象**：系统刚启动或凌晨缓存全部过期，第一波请求全部穿透到 DB。

**解决方案——凌晨预热任务**：

```java
@Component
public class CacheWarmUpTask {
    
    // 每天凌晨 5 点（在每日对账之后）
    @Scheduled(cron = "0 0 5 * * ?")
    public void warmUpHotData() {
        RLock lock = redissonClient.getLock("lock:cache:warmup");
        try {
            if (lock.tryLock(0, 120, TimeUnit.SECONDS)) {
                // 1. 预热当天正在进行的试卷（status=1 的已发布试卷）
                List<Paper> activePapers = paperMapper.selectList(
                    new LambdaQueryWrapper<Paper>()
                        .eq(Paper::getStatus, 1)
                );
                for (Paper paper : activePapers) {
                    // 预热试卷信息
                    cachePaperInfo(paper);
                    // 预热题目
                    cachePaperQuestions(paper.getId());
                }
                
                // 2. 预热近 7 天热门试卷（按 response 记录数排序）
                List<Long> hotPaperIds = responseMapper.selectHotPaperIds(7, 100);
                for (Long paperId : hotPaperIds) {
                    cachePaperInfo(paperMapper.selectById(paperId));
                    cachePaperQuestions(paperId);
                }
                
                // 3. 预热所有项目配置
                List<Long> projectIds = projectMapper.selectAllIds();
                for (Long projectId : projectIds) {
                    cacheProjectConfig(projectId);
                }
                
                log.info("缓存预热完成，预热试卷数={}, 项目配置数={}", 
                    activePapers.size() + hotPaperIds.size(), projectIds.size());
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

---

## 4. 缓存使用决策矩阵（供后续扩展参考）

当有新数据需要缓存时，按以下流程判断：

```
新数据需要缓存？
  │
  ├─ 访问频率高吗？（每分钟 >100 次）
  │   否 → 不需要缓存
  │   是 ↓
  │
  ├─ 数据量大吗？（单条 >10KB 或总量 >1000 条）
  │   量小 → 只放 Caffeine（L1）
  │   量大 ↓
  │
  ├─ 多实例需要共享吗？
  │   不需要 → 只放 Caffeine（L1）
  │   需要 ↓
  │
  ├─ 实时性要求高吗？（≤5 秒内必须看到最新数据）
  │   高 → 只放 Redis（L2），不建本地缓存
  │   不高 ↓
  │
  └─ 适合三级缓存（Caffeine L1 + Redis L2 + MySQL L3）
```

---

## 5. 缓存监控指标

| 指标 | 说明 | 告警阈值 |
|------|------|----------|
| Caffeine 命中率 | `hitCount / (hitCount + missCount)` | <70% 需调整容量或策略 |
| Redis 命中率 | `keyspace_hits / (keyspace_hits + keyspace_misses)` | <80% 需排查 |
| Redis 内存使用率 | `used_memory / maxmemory` | >80% 黄色，>90% 红色 |
| 缓存回源耗时 | 从 DB 加载并回填缓存的耗时 | >500ms 告警 |
| 布隆过滤器误判率 | 穿透到 DB 发现数据不存在的比例 | >2% 需增大布隆过滤器 |

> 注意：Caffeine 命中率低不一定有问题。比如 `teacher:papers:{userId}` 就不适合放 Caffeine（原因见 2.3 节），这是合理的设计选择。

---

## 文档变更记录

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-05-11 | v1.0 | 初始版本，三级缓存设计、数据存储清单、Key 规范、问题方案、扩展决策矩阵 |
