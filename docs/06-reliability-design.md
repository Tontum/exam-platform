# 06 - 企业级可靠性设计方案

> 本文档专门针对系统中所有可能出现的数据风险场景，给出经过生产验证的企业级解决方案。涵盖 Kafka 消息可靠性、Redis 数据中转安全、答题数据全链路保障三大主题。

---

## 第一部分：Kafka 消息可靠性全方案

### 1.1 消息不丢失 —— 三阶段保障

#### 生产端（Producer）

```
配置层面：
  acks = all（或 -1）    ← 必须等所有 ISR 副本同步完成才返回确认
  retries = 10            ← 发送失败自动重试，上限 10 次
  retry.backoff.ms = 500  ← 重试间隔 500ms，避免频繁重试打崩 Broker
  enable.idempotence = true ← 开启幂等生产者，避免重试导致消息重复
  max.in.flight.requests.per.connection = 5 ← 与幂等一起用时自动降为 5

代码层面：
  producer.send(record, (metadata, exception) -> {
      if (exception != null) {
          // 发送失败：写入本地兜底文件 + 告警
          fallbackToLocalFile(record);
          alertService.send("Kafka 发送失败", exception.getMessage());
      }
  });
```

**兜底机制**：如果 Kafka 集群整体不可用，消息写入服务器本地文件（`/data/kafka-fallback/`），等 Kafka 恢复后由补偿任务扫描并重新发送。

#### Broker 端

```
配置层面：
  replication.factor = 3              ← 每个 Topic 至少 3 个副本
  min.insync.replicas = 2             ← 至少 2 个 ISR 副本确认才算写入成功
  unclean.leader.election.enable = false ← 禁止"脏"副本当选 Leader（会丢数据）
  log.flush.interval.messages = 10000 ← 每 1 万条消息刷盘一次
  log.flush.interval.ms = 1000        ← 或每 1 秒刷盘一次

Topic 创建：
  paper_distribute:   partitions=6, replication-factor=3, min.insync.replicas=2
  answer_sync:        partitions=6, replication-factor=3, min.insync.replicas=2
  paper_expired:      partitions=3, replication-factor=3, min.insync.replicas=2
```

**数学验证**：`replication-factor=3, min.insync.replicas=2` 意味着：容忍 1 个副本故障，ack=all 时至少 2 个副本写入成功。只有 2 个以上副本同时故障才会丢消息（概率极低）。

#### 消费端（Consumer）

```
配置层面：
  enable.auto.commit = false  ← 关闭自动提交，改为手动控制

代码层面（关键）：
  @KafkaListener(topics = "paper_distribute")
  public void consume(ConsumerRecord<String, String> record,
                      Acknowledgment ack) {
      try {
          // 1. 解析消息
          DistributeMessage msg = parseMessage(record.value());
          
          // 2. 执行数据库写入（带唯一键防重）
          int inserted = responseMapper.batchInsertIfAbsent(msg.getResponseList());
          
          // 3. 写入成功后才手动提交 offset
          ack.acknowledge();
          
      } catch (Exception e) {
          // 4. 写入失败：不提交 offset，消息会被重新消费
          log.error("消费失败，offset={}", record.offset(), e);
          // 不调用 ack.acknowledge()，Kafka 会重新投递
      }
  }
```

**核心原则**：只有业务处理成功（数据写入 DB）后才提交 Offset。任何异常都不提交，消息会被重新消费。

---

### 1.2 消息不重复 —— 幂等性设计

消息重复的原因：生产者重试、消费者 rebalance、手动提交 offset 的"至少一次"语义。从三个层面消除重复影响。

#### 第一层：生产者幂等

```yaml
spring.kafka.producer.properties.enable.idempotence: true
```

Kafka 内部机制：给每条消息分配 Producer ID + Sequence Number，Broker 自动去重。但仅对单分区会话内有效，跨会话/跨分区无效。

#### 第二层：消费端数据库唯一约束（核心防线）

```sql
-- response 表：同一老师 + 同一试卷仅一条记录
UNIQUE KEY `uk_paper_user` (`paper_id`, `user_id`)

-- 插入时自动跳过重复
INSERT INTO response (paper_id, user_id, status, ...)
VALUES (?, ?, 0, ...)
ON DUPLICATE KEY UPDATE id = id;  -- 无操作，但返回 affected_rows=0
```

```sql
-- answer 表：同一答题记录 + 同一题目仅一条
UNIQUE KEY `uk_response_question` (`response_id`, `question_id`)

INSERT INTO answer (response_id, question_id, user_id, answer_content, ...)
VALUES (?, ?, ?, ?, ...)
ON DUPLICATE KEY UPDATE
    answer_content = VALUES(answer_content),
    updated_at = NOW();  -- 更新为最新答案
```

#### 第三层：消息级别唯一 ID

每条 Kafka 消息携带全局唯一 ID：

```java
public class DistributeMessage {
    private String messageId;     // UUID，全局唯一
    private Long paperId;
    private List<Long> userIds;
    private Long timestamp;
}
```

消费端在处理前，先检查 `messageId` 是否已处理：

```java
// Redis 记录已处理的消息 ID，TTL = 24 小时
String key = "kafka:processed:" + messageId;
Boolean isProcessed = redisTemplate.opsForValue()
    .setIfAbsent(key, "1", Duration.ofHours(24));

if (Boolean.FALSE.equals(isProcessed)) {
    log.warn("重复消息，跳过. messageId={}", messageId);
    ack.acknowledge();  // 仍然提交，不阻塞后续消息
    return;
}
```

#### 第四层：业务层乐观锁（防止并发覆盖）

```java
// 批阅操作：防止两个校长同时批阅同一份试卷
UPDATE response 
SET status = 3, score = ?, is_pass = ?, reviewer_id = ?, review_time = NOW()
WHERE id = ? 
  AND status = 2        -- 乐观锁条件：只有已提交状态才能批阅
  AND reviewer_id IS NULL;  -- 只有未被批阅的才能批阅

// 若 affected_rows = 0，说明已被他人批阅，返回友好提示
```

---

### 1.3 消息顺序性

#### 需求分析

本项目对消息顺序性有要求：
- 同一份试卷的分发消息应被同一个消费者顺序处理
- 同一老师的答题数据落库应保持顺序（先答的题先落库）

#### 方案：分区键路由

```
生产端：
  按 paper_id 哈希 → 同一试卷的所有消息进入同一分区
  Kafka 保证：同一分区内消息严格有序

消费端：
  每个分区只有一个消费者线程处理（默认行为）
  消费者内部单线程处理，不开启并发
```

```java
// 生产者指定分区键
ProducerRecord<String, String> record = new ProducerRecord<>(
    "paper_distribute",
    null,                         // 不指定分区
    paperId.toString(),           // Key = paperId，Kafka 按 Key 哈希选分区
    messageJson
);
```

```yaml
# 消费者端：关闭并发（默认行为，显式声明）
spring.kafka.consumer.concurrency: 1
spring.kafka.listener.concurrency: 1
```

#### 重要权衡

| 保证力度 | 方案 | 吞吐量影响 |
|---------|------|-----------|
| 同一试卷内有序 | paper_id 哈希分区 + 单线程消费 | 小（分区内自然有序） |
| 全局有序 | 单分区 + 单消费者 | 极大（几乎不可用） |

本项目选择**同一试卷内有序**，满足业务需求的同时不严重牺牲吞吐。

---

### 1.4 Kafka 集群宕机

#### 预防：集群高可用

```
至少 3 个 Broker 节点，分布在不同的物理机/机架上
├── broker-1: 192.168.1.101:9092
├── broker-2: 192.168.1.102:9092
└── broker-3: 192.168.1.103:9092
```

#### 监控告警

```
Kafka 关键指标监控：
├── UnderReplicatedPartitions > 0  → 严重告警（副本不足）
├── ActiveControllerCount ≠ 1    → 严重告警（脑裂）
├── OfflinePartitionsCount > 0   → 紧急告警（分区不可用）
├── BytesInPerSec 骤降           → 告警（生产端异常）
└── ConsumerLag > 10000          → 告警（消费堆积）
```

#### 故障恢复流程

```
1 个 Broker 宕机：
  → Kafka 自动将 Leader 切换到 ISR 中的另一个副本
  → 生产者/消费者自动重连新 Leader
  → 无需人工介入，业务无感知（连接会有短暂抖动 < 30s）

2 个 Broker 同时宕机（罕见）：
  → min.insync.replicas=2 不再满足
  → 生产者写入被拒绝，触发本地文件兜底
  → 运维紧急恢复宕机节点
  → 恢复后消费者自动从上次 offset 继续

3 个 Broker 全部宕机（极罕见，机房级故障）：
  → 生产者写入本地文件兜底
  → 消费者等待集群恢复
  → 恢复后全量补偿
```

---

### 1.5 Kafka 消息堆积

#### 监控指标

```
消费者 Lag（未消费消息数）：
  Lag < 1000   → 正常
  Lag 1000-5000 → 黄色预警（关注）
  Lag > 5000   → 红色告警（需要扩容）
```

#### 应急处理方案（由轻到重）

**方案 A：增加消费者实例（首选）**
```
消费者组内实例数 3 → 6
前提：Topic 分区数 >= 6（否则再加也无效）
每个分区只能被一个消费者实例消费
```

**方案 B：临时增大批量处理能力**
```yaml
# 临时调整（堆积期间）
spring.kafka.consumer.max-poll-records: 1000   # 一次拉取 1000 条（默认 500）
spring.kafka.consumer.fetch-min-size: 1048576  # 至少拉 1MB 才返回
```

**方案 C：暂停非核心消费者，资源倾斜**
```
暂停 answer_sync、paper_expired 等非核心 Topic 的消费
把 CPU / 网络 / DB 连接池全部用于 paper_distribute 消费
堆积消解后恢复
```

**方案 D：限流保护（防止雪崩）**
```java
// Sentinel 对生产者限流
@SentinelResource(value = "paperPublish", 
    blockHandler = "publishBlockHandler")
public Result publishPaper(PublishDTO dto) {
    // ... 发送 Kafka
}
// 当消费端堆积严重时，限制发布端速率，避免消息越积越多
```

**方案 E：死信队列兜底**
```
失败 N 次的消息 → 移入死信 Topic: paper_distribute.DLQ
├── 避免反复重试阻塞主队列
├── 人工排查死信消息原因
└── 修复后重新投递到原 Topic
```

> **注意：Kafka 死信队列 ≠ Redis 失败重试**
>
> Kafka 死信队列是一个真正的 Kafka Topic（`paper_distribute.DLQ`），解决的是**消费阻塞**问题：某条消息反复处理失败会堵住整个分区的后续消费，需要把它移走，主队列继续前进。死信里的消息需要人工排查根因，修好后重新投递。
>
> Redis 侧的失败重试（见第三部分 3.4 节）是另一个概念：5 秒定时任务从 Redis 取答题数据写 MySQL，单条 INSERT 失败时，不删除 Redis 中的那条数据，而是打上 `failed` 标记 + 重试计数，交给每分钟一次的补偿任务重新写入。重试 5 次仍失败才记入数据库死信表，发告警让人介入。
>
> 两者各管各的链路，互不相干：Kafka 死信管的是"消息分发"链路，Redis 失败重试管的是"数据落库"链路。

---

## 第二部分：Redis 答题数据中转 —— 详细设计方案

### 2.1 为什么用 Redis 而非直接写 MySQL

| 对比维度 | 直接写 MySQL | Redis 中转 |
|---------|-------------|-----------|
| 单次写入延迟 | 5-20ms | <1ms |
| 10 万老师同时答题 QPS | ~2000 | ~100,000 |
| 答题体验 | 点提交后有延迟感 | 几乎无感知 |
| 数据丢失风险 | 低（写入即持久化） | 需要额外保障（本章重点） |

### 2.2 Redis 数据结构设计

```
# 单题答案（String 类型）
Key: answer:single:{responseId}:{questionId}
Value: {
  "responseId": 1001,
  "questionId": 25,
  "userId": 88,
  "answerContent": "A",
  "durationSeconds": 15,
  "timestamp": 1715400000000
}
TTL: 600 秒（10 分钟，足够覆盖 5s 落库周期 + 重试）

# 试卷维度统计（Hash 类型）
Key: paper:stat:{paperId}
Field-Value:
  totalSubmit: 1523       # 已提交人数
  totalAnswer: 48620      # 总答题数
  correctCount: 38900     # 正确数
  avgScore: 78.5          # 平均分
TTL: 3600 秒（1 小时）

# 答题进度标记（String 类型，用于断网恢复）
Key: answer:progress:{responseId}
Value: {
  "responseId": 1001,
  "lastQuestionId": 18,
  "answeredCount": 18,
  "totalQuestions": 30,
  "remainingSeconds": 1200,
  "lastSaveTime": 1715400000000
}
TTL: 7200 秒（2 小时）

# 待落库队列（List 类型）
Key: answer:flush:queue
Value: [{responseId:1001, questionId:25}, {responseId:1002, questionId:3}, ...]
# 每 5 秒定时任务从此队列批量消费并写入 MySQL
```

### 2.3 正常流程

```
老师答题 →
  ① 写入 answer:single:{responseId}:{questionId}（Redis String）
  ② LPUSH answer:flush:queue（加入待落库队列）
  ③ HINCRBY paper:stat:{paperId}（更新统计）
  ← 3 步全在 Redis 完成，耗时 <5ms

定时任务（每 5 秒）→
  ① RPOP answer:flush:queue（批量取 500 条）
  ② Lua 脚本校验数据合法性
  ③ 批量 INSERT INTO answer (ON DUPLICATE KEY UPDATE)
  ④ DELETE answer:single:{responseId}:{questionId}（清理已落库数据）
  ⑤ 更新 MySQL 统计表
```

### 2.4 前/后端自动保存进度（每 3 秒）

```
前端：
  setInterval(() => {
      if (isAnswering && !hasSubmitted) {
          // 调用后端保存接口
          fetch('/api/answer/' + responseId + '/save-progress', {
              method: 'POST',
              body: JSON.stringify({
                  currentQuestionId: currentQId,
                  remainingSeconds: timerValue
              })
          });
      }
  }, 3000);

后端：
  @PostMapping("/{responseId}/save-progress")
  public Result saveProgress(@PathVariable Long responseId, 
                             @RequestBody ProgressDTO dto) {
      // 写入 Redis 进度标记
      String key = "answer:progress:" + responseId;
      redisTemplate.opsForValue().set(key, dto, Duration.ofHours(2));
      
      // 同时把前端暂存的所有未提交单题答案一并保存
      // （见 3.2 节前端本地缓存的恢复逻辑）
      
      return Result.success();
  }
```

---

## 第三部分：答题数据全链路故障场景与解决方案

### 3.1 故障全景图

```
老师答题 → [前端] → [网络] → [后端接口] → [Redis] → [定时任务] → [MySQL]
             ①         ②         ③           ④          ⑤           ⑥
```

每个环节都可能出问题，下面逐一枚举。

---

### 3.2 场景 ①：用户在答题页，但网络断开（前端断网）

**发生时机**：老师正在答题，但 WiFi 断开或信号不佳。

**风险**：每道题的提交请求失败，已答过的题丢失，老师需要重做。

**解决方案 —— 前端本地缓存 + 网络恢复重放：**

**核心机制**：localStorage 中每条答案带一个 `synced` 布尔标记。新增答案时 `synced: false`，后端返回 200 后改为 `synced: true`。系统以此判断哪些答案已经安全到达服务端、哪些还在本地等待重发。网络恢复时的重放只挑 `synced === false` 的记录去重发，已同步的不再发送。

```javascript
// === 答题页面核心逻辑 ===

// 1. 维护本地答案队列（localStorage），每条记录包含 synced 标记
const ANSWER_QUEUE_KEY = 'answer_queue_' + responseId;

function saveAnswerLocally(questionId, answerContent) {
    let queue = JSON.parse(localStorage.getItem(ANSWER_QUEUE_KEY) || '[]');
    queue.push({
        questionId: questionId,
        answerContent: answerContent,
        timestamp: Date.now(),
        synced: false   // 标记未同步
    });
    localStorage.setItem(ANSWER_QUEUE_KEY, JSON.stringify(queue));
}

// 2. 提交答案（带重试 + 本地兜底）
async function submitAnswer(questionId, answerContent) {
    // 先存本地（无论网络是否正常都存）
    saveAnswerLocally(questionId, answerContent);
    
    try {
        const resp = await fetch('/api/answer/' + responseId 
            + '/question/' + questionId, {
            method: 'POST',
            body: JSON.stringify({ answerContent: answerContent }),
            signal: AbortSignal.timeout(3000)  // 3 秒超时
        });
        if (resp.ok) {
            // 标记已同步
            markAnswerSyncedLocally(questionId);
        }
    } catch (e) {
        // 网络失败：不报错，本地已保存
        console.warn('提交失败，已本地缓存，网络恢复后自动同步:', e);
        showToast('网络不稳定，答案已本地保存');
    }
}

// 3. 网络恢复后自动重放
window.addEventListener('online', async () => {
    const queue = JSON.parse(
        localStorage.getItem(ANSWER_QUEUE_KEY) || '[]'
    );
    const unsynced = queue.filter(item => !item.synced);
    
    for (const item of unsynced) {
        try {
            await fetch('/api/answer/' + responseId 
                + '/question/' + item.questionId, {
                method: 'POST',
                body: JSON.stringify({ answerContent: item.answerContent })
            });
            markAnswerSyncedLocally(item.questionId);
        } catch (e) {
            break;  // 仍失败就停下，等下次 online 事件
        }
    }
});

// 4. 心跳检测
setInterval(async () => {
    try {
        await fetch('/api/health/ping', { signal: AbortSignal.timeout(2000) });
        // 网络正常
    } catch (e) {
        showBanner('网络连接异常，答案已自动保存到本地');
    }
}, 5000);
```

**兜底层级**：
1. 网络正常 → 直接 POST 到后端 → 写入 Redis
2. 网络抖动（3s 内恢复）→ 前端 fetch 重试机制（内置 3 次重试，间隔 1s/2s/4s）
3. 网络长时间断开 → 存入 localStorage → 恢复后自动重放
4. 浏览器崩溃/关机 → localStorage 持久化，下次打开页面检测未同步数据

---

### 3.3 场景 ②：前端提交成功，但 Redis 写入失败

**发生时机**：后端收到 POST 请求，但 Redis 连接断开或 OOM。

**风险**：接口返回 500，前端认为提交失败，但题目答案实际丢失。

**解决方案 —— 服务端三层兜底：**

```java
@Service
public class AnswerSaveService {

    // 第一层：Redis 正常写入
    public boolean saveToRedis(AnswerDTO dto) {
        try {
            String key = "answer:single:" + dto.getResponseId() 
                + ":" + dto.getQuestionId();
            redisTemplate.opsForValue()
                .set(key, JSON.toJSONString(dto), Duration.ofMinutes(10));
            redisTemplate.opsForList()
                .leftPush("answer:flush:queue", key);
            return true;
        } catch (RedisConnectionFailureException e) {
            // Redis 不可用 → 进入第二层
            log.error("Redis 写入失败，降级到本地存储", e);
            return false;
        }
    }
    
    // 第二层：Redis 不可用时，写入本地文件（服务器磁盘）
    public void fallbackToLocalFile(AnswerDTO dto) {
        String line = JSON.toJSONString(dto) + "\n";
        // 追加写入本地文件
        String filePath = "/data/answer-fallback/" 
            + LocalDate.now() + "-" + dto.getResponseId() + ".log";
        Files.write(Paths.get(filePath), line.getBytes(), 
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        
        // 告警
        alertService.send("Redis 不可用，答题数据降级到本地文件");
    }
    
    // 第三层：Redis 恢复后的补偿任务
    @Scheduled(fixedDelay = 60000)  // 每分钟扫描一次
    public void compensateFromLocalFile() {
        File dir = new File("/data/answer-fallback/");
        for (File file : dir.listFiles()) {
            if (isRedisHealthy()) {  // Redis 恢复了
                // 逐行读取本地文件 → 写入 Redis
                Files.lines(file.toPath()).forEach(line -> {
                    AnswerDTO dto = JSON.parseObject(line, AnswerDTO.class);
                    saveToRedis(dto);  // 回补到 Redis
                });
                // 成功回补后删除文件
                file.delete();
            }
        }
    }
}
```

**接口返回值优化**：

```java
@PostMapping("/{responseId}/question/{questionId}")
public Result submitAnswer(...) {
    try {
        boolean saved = answerSaveService.saveToRedis(dto);
        if (saved) {
            return Result.success("答案已保存");
        } else {
            // 降级到本地文件后，返回成功但带提示
            answerSaveService.fallbackToLocalFile(dto);
            return Result.success("答案已保存（降级模式）");
        }
    } catch (Exception e) {
        // 所有兜底都失败
        return Result.error("保存失败，请重试");
    }
}
```

**关键设计**：即使 Redis 挂了，只要服务器磁盘没坏，答题数据不丢。服务器磁盘是最后一道物理防线。

---

### 3.4 场景 ③④：5 秒定时任务执行时 MySQL 写入失败

**发生时机**：定时任务从 Redis 读取 500 条数据，执行批量 INSERT 时 MySQL 连接断开、死锁、或磁盘满。

**风险**：Redis 数据被消费但未写入 MySQL → 数据永久丢失；或部分成功部分失败 → 数据不一致。

**解决方案 —— 事务 + 状态机 + 死信重试：**

```
Redis 中每条待落库数据有三种状态：
  pending   → 等待处理（刚写入 Redis 时）
  processing → 正在处理（定时任务已标记，正在落库）
  failed    → 处理失败（等待重试）
```

**队列消费方式——为什么用 Range 而非 RPOP**：

```
写入端（每次答题）：
  LPUSH answer:flush:queue "answer:single:1001:18"  ← 左边进

消费端（5 秒定时任务）：
  Range 取前 500 条（只读，不删除）
  → 标记每条为 processing
  → 读数据 → 批量写 MySQL
  → 全部成功后：逐条从队列 LREM 删除 + 删除 answer:single key
  → 失败：processing 回退为 pending，等下一轮重试
```

**关键设计**：不用 RPOP（破坏性读取）。RPOP 一旦弹出，数据就从 Redis 消失，如果此后定时任务崩溃，这 500 条数据永久丢失。改用 Range 读到内存 + 成功后删除，确保数据在 MySQL 确认写入之前始终可恢复。

```java
@Component
public class AnswerFlushService {

    private static final int BATCH_SIZE = 500;

    @Scheduled(fixedDelay = 5000)
    public void flushAnswers() {
        // Step 1: Range 读取队列前 500 条（不删除，只读）
        List<String> keys = redisTemplate.opsForList()
            .range("answer:flush:queue", 0, BATCH_SIZE - 1);
        if (keys == null || keys.isEmpty()) return;
        
        // Step 2: 标记为 processing（先占位，其他定时任务实例看到的仍是 pending）
        List<String> processingKeys = new ArrayList<>();
        for (String key : keys) {
            Boolean ok = redisTemplate.opsForValue()
                .setIfAbsent(key + ":status", "processing", Duration.ofMinutes(5));
            if (Boolean.TRUE.equals(ok)) {
                processingKeys.add(key);
            }
            // 已经是 processing 的（上一轮失败的残留）→ 跳过
        }
        if (processingKeys.isEmpty()) return;
        
        // Step 3: 读取实际数据
        List<AnswerDTO> answers = new ArrayList<>();
        List<String> dataKeys = new ArrayList<>();
        for (String key : processingKeys) {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                answers.add(JSON.parseObject(json, AnswerDTO.class));
                dataKeys.add(key);
            }
        }
        
        // Step 4: 批量写入 MySQL（事务保护）
        try {
            answerMapper.batchInsertOrUpdate(answers);
            
            // Step 5: 全部成功 → 从队列删除 + 删除数据 key + 删除状态 key
            for (String key : processingKeys) {
                redisTemplate.opsForList()
                    .remove("answer:flush:queue", 1, key);  // 从队列删除
                redisTemplate.delete(key);                   // 删除 answer:single
                redisTemplate.delete(key + ":status");       // 删除状态标记
            }
            log.info("落库成功，条数={}", answers.size());
            
        } catch (Exception e) {
            log.error("落库失败，条数={}, 错误={}", answers.size(), e.getMessage());
            
            // Step 6: 失败 → 逐条降级重试
            int successCount = 0, failCount = 0;
            for (int i = 0; i < answers.size(); i++) {
                try {
                    answerMapper.insertOrUpdate(answers.get(i));
                    // 单条成功 → 清理
                    redisTemplate.opsForList()
                        .remove("answer:flush:queue", 1, dataKeys.get(i));
                    redisTemplate.delete(dataKeys.get(i));
                    redisTemplate.delete(dataKeys.get(i) + ":status");
                    successCount++;
                } catch (Exception ex) {
                    // 单条也失败 → 标记 failed
                    redisTemplate.opsForValue()
                        .set(dataKeys.get(i) + ":status", "failed", Duration.ofHours(1));
                    redisTemplate.opsForValue()
                        .set(dataKeys.get(i) + ":retryCount", "1", Duration.ofHours(1));
                    redisTemplate.opsForValue()
                        .set(dataKeys.get(i) + ":error", ex.getMessage(), Duration.ofHours(1));
                    failCount++;
                }
            }
            alertService.send("落库部分失败", 
                "成功=" + successCount + ", 失败=" + failCount);
        }
    }
    
    // Step 7: 补偿任务（每分钟检查 failed 数据，重试）
    @Scheduled(fixedDelay = 60000)
    public void retryFailedAnswers() {
        Set<String> failedKeys = redisTemplate.keys("answer:single:*:status");
        for (String statusKey : failedKeys) {
            String status = redisTemplate.opsForValue().get(statusKey);
            if (!"failed".equals(status)) continue;
            
            String dataKey = statusKey.replace(":status", "");
            int retryCount = Integer.parseInt(
                redisTemplate.opsForValue().get(dataKey + ":retryCount") == null 
                ? "0" : redisTemplate.opsForValue().get(dataKey + ":retryCount")
            );
            
            if (retryCount >= 5) {
                // 重试 5 次仍失败 → 记入数据库死信表（注意：这是 Redis 落库失败的死信，
                // 与 Kafka 的 DLQ Topic 是两套独立机制，详见 1.5 节末尾的区别说明）
                deadLetterService.save("answer_flush", dataKey);
                redisTemplate.delete(dataKey);
                redisTemplate.delete(statusKey);
                alertService.send("答题数据落库失败超过5次", "key=" + dataKey);
                continue;
            }
            
            // 重试
            try {
                String json = redisTemplate.opsForValue().get(dataKey);
                AnswerDTO dto = JSON.parseObject(json, AnswerDTO.class);
                answerMapper.insertOrUpdate(dto);
                
                redisTemplate.delete(dataKey);
                redisTemplate.delete(statusKey);
                log.info("补偿重试成功: {}", dataKey);
            } catch (Exception e) {
                redisTemplate.opsForValue()
                    .increment(dataKey + ":retryCount");
                log.error("补偿重试失败: {}, 已重试{}次", dataKey, retryCount + 1);
            }
        }
    }
}
```

**关于重试策略——为什么不用指数退避**

网络请求（如 HTTP 重试）用指数退避（1s → 2s → 4s → 8s）是为了避免打崩对方服务器。但本系统的重试两端都在自己机房内（Redis 和 MySQL 都是本地），不存在"把对方打崩"的风险，固定间隔反而能让数据最快恢复。

具体来说：
- **5 秒定时任务**：一批失败后，5 秒后的下一轮自然重试，间隔固定
- **60 秒补偿任务**：每分钟扫描一次 `failed` 数据并重试，间隔固定

唯一的例外是——如果 MySQL 连接池被打满导致落库失败，高频率重试会加剧压力。但本系统已给落库任务配置了独立的数据库连接池（与在线答题业务隔离），所以这个风险可控。

```
重试时间线：
  0s     5s     10s    15s    60s    120s   180s   240s   300s
  │      │      │      │      │      │      │      │      │
  ├─Timed├─Timed├─Timed├─Comp ├─Comp ├─Comp ├─Comp ├─Dead │
  │ 失败  │ 失败  │ 成功  │      │      │      │      │ 5次→死信
  └────────────────定时任务重试───────补偿任务重试───────────┘
```

---

### 3.5 场景 ⑤：Redis 自身故障

**发生时机**：Redis 进程崩溃、内存满、主从切换。

**Redis 高可用方案：**

```
生产环境 Redis 哨兵架构：
       ┌──────────┐
       │  Sentinel 1 │
       └─────┬──────┘
             │ 监控
    ┌────────┼────────┐
    ▼        ▼        ▼
┌───────┐┌───────┐┌───────┐
│Redis  ││Redis  ││Redis  │
│Master ││Slave 1││Slave 2│
│(写)    ││(读)    ││(读)    │
└───┬───┘└───────┘└───────┘
    │ 异步复制
    └──────────────▶ Slave1, Slave2
```

**故障处理流程：**

| 故障 | 自动处理 | 数据影响 |
|------|----------|----------|
| Slave 宕机 | Master 继续服务，新 Slave 自动加入 | 无 |
| Master 宕机 | Sentinel 自动选举新 Master（<10s） | 可能丢失最后几毫秒未同步的数据 |
| Master + 1 Slave 同时宕机 | 剩余 1 Slave 可提升为 Master | 同上 |
| 全部宕机 | 应用降级到本地文件 | 见场景②的本地文件兜底方案 |

**应用层连接保活：**

```yaml
spring.data.redis:
  lettuce:
    pool:
      max-active: 20
      max-idle: 10
      min-idle: 5
    cluster:
      refresh:
        adaptive: true    # 自适应拓扑刷新
        period: 30s       # 每 30 秒刷新集群拓扑
  timeout: 3000ms         # 命令超时
  connect-timeout: 5000ms # 连接超时
```

---

### 3.6 场景 ⑥：用户点击"提交试卷"时 Redis 中还有未落库数据

**发生时机**：用户答完最后一道题立刻点提交，5 秒定时任务还没执行。

**解决方案 —— 提交时主动触发落库：**

```java
@PostMapping("/{responseId}/submit")
@Transactional
public Result submitPaper(@PathVariable Long responseId) {
    
    // Step 1: 主动将 Redis 中该 responseId 的所有答案强制落库
    forceFlushForResponse(responseId);
    // 此时保证该老师的全部答案已写入 answer 表
    
    // Step 2: 客观题自动批阅
    autoReviewObjectiveQuestions(responseId);
    
    // Step 3: 计算总分
    BigDecimal totalScore = answerMapper.sumScoreByResponse(responseId);
    
    // Step 4: 更新 response 表
    responseMapper.updateById(new Response()
        .setId(responseId)
        .setStatus(2)  // 已提交
        .setScore(totalScore)
        .setSubmitTime(LocalDateTime.now())
    );
    
    // Step 5: 清理该试卷的 Redis 进度缓存
    redisTemplate.delete("answer:progress:" + responseId);
    
    return Result.success();
}

private void forceFlushForResponse(Long responseId) {
    // 查找 Redis 中该 responseId 的所有待落库答案
    Set<String> keys = redisTemplate.keys("answer:single:" + responseId + ":*");
    List<AnswerDTO> answers = new ArrayList<>();
    
    for (String key : keys) {
        if (key.endsWith(":status")) continue; // 跳过状态标记
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            answers.add(JSON.parseObject(json, AnswerDTO.class));
        }
    }
    
    // 批量写入
    if (!answers.isEmpty()) {
        answerMapper.batchInsertOrUpdate(answers);
        // 清理 Redis
        for (String key : keys) {
            if (!key.endsWith(":status")) {
                redisTemplate.delete(key);
            }
        }
    }
}
```

---

### 3.6b `answer:progress:{responseId}` —— 三个使用时机

这个 Redis key 并非只在"断网恢复"时使用，它有三个独立的使用场景：

**时机一：断网 / 浏览器崩溃后恢复答题**

老师 WiFi 断了、浏览器关了、电脑没电了，重新打开试卷时调用恢复接口：

```java
@GetMapping("/{responseId}/recover")
public Result recoverProgress(@PathVariable Long responseId) {
    // 1. 读 Redis 进度
    String progressStr = redisTemplate.opsForValue()
        .get("answer:progress:" + responseId);
    
    if (progressStr != null) {
        ProgressDTO progress = JSON.parseObject(progressStr, ProgressDTO.class);
        
        // 2. 同时查出 Redis 中已保存的答案（自动填充，不用重做）
        Set<String> answerKeys = redisTemplate.keys(
            "answer:single:" + responseId + ":*"
        );
        List<AnswerDTO> savedAnswers = new ArrayList<>();
        for (String k : answerKeys) {
            if (!k.endsWith(":status")) {
                String json = redisTemplate.opsForValue().get(k);
                if (json != null) {
                    savedAnswers.add(JSON.parseObject(json, AnswerDTO.class));
                }
            }
        }
        
        RecoverVO vo = new RecoverVO();
        vo.setHasProgress(true);
        vo.setCurrentQuestionId(progress.getLastQuestionId());  // 定位到哪一题
        vo.setAnsweredCount(progress.getAnsweredCount());       // 已答几题
        vo.setRemainingSeconds(progress.getRemainingSeconds()); // 剩余时间
        vo.setSavedAnswers(savedAnswers); // 已保存的答案自动填入
        return Result.success(vo);
    }
    
    return Result.success(new RecoverVO(false));
}
```

前端收到后的行为：
1. 定位到 `currentQuestionId` 那道题（不是从头开始）
2. 已保存的答案自动填充到对应题目（不用重做已答的题）
3. 倒计时恢复到 `remainingSeconds`（不是重新计时）
4. localStorage 中 `synced: false` 的记录重新发起重发

**时机二：提交试卷时验证完整性**

```java
// 提交试卷接口中
String progressStr = redisTemplate.opsForValue()
    .get("answer:progress:" + responseId);
ProgressDTO progress = JSON.parseObject(progressStr, ProgressDTO.class);

if (progress.getAnsweredCount() < progress.getTotalQuestions()) {
    // 学生答了 18/30 题就想提交 → 前端弹确认框
    // 这个校验不需要查 MySQL，直接读 Redis 进度即可
}
```

**时机三：阅卷时判断异常**

校长批阅时发现某老师只交了 18 道题的答案但试卷有 30 道题——查 `answer:progress:{responseId}` 可以确认：是老师没答完就提交了，还是系统数据丢了（进度显示 answeredCount=30 但 answer 表只有 18 条 → 数据丢失）。

---

### 3.7 场景 ⑦：前端并发提交与 3 秒定时保存的冲突

**发生时机**：用户刚把 A 选项改成 B，点击"下一题"触发提交（新数据 B），同时 3 秒定时保存也在发请求（可能还是旧数据 A）。由于网络时序不确定，旧请求可能后到达，覆盖新数据。

**为什么不用分布式锁**：两个请求写的是同一个 Redis key（`answer:single:{responseId}:{questionId}`），Redis 本身是单线程的，不存在写冲突。真正的问题是**哪个请求先到、哪个后到**——分布式锁解决不了这个时序问题。

**解决方案 —— 客户端时间戳**：

```javascript
// 前端：每条请求带客户端时间戳
async function submitAnswer(questionId, answerContent) {
    const clientTimestamp = Date.now();
    saveAnswerLocally(questionId, answerContent, clientTimestamp);
    
    try {
        await fetch('/api/answer/' + responseId + '/question/' + questionId, {
            method: 'POST',
            body: JSON.stringify({ 
                answerContent: answerContent,
                clientTimestamp: clientTimestamp
            })
        });
        markAnswerSyncedLocally(questionId);
    } catch (e) {
        // 网络失败，synced 保持 false，等待重发
    }
}
```

```java
// 后端：只有时间戳更新的请求才覆盖
public void saveToRedis(AnswerDTO dto) {
    String key = "answer:single:" + dto.getResponseId() 
        + ":" + dto.getQuestionId();
    
    String existing = redisTemplate.opsForValue().get(key);
    if (existing != null) {
        AnswerDTO existingDTO = JSON.parseObject(existing, AnswerDTO.class);
        if (dto.getClientTimestamp() <= existingDTO.getClientTimestamp()) {
            // 已存在更新或同时到达的数据，丢弃旧请求
            log.debug("忽略过期答案: key={}, clientTs={}, existingTs={}", 
                key, dto.getClientTimestamp(), existingDTO.getClientTimestamp());
            return;
        }
    }
    
    // 写入新数据
    redisTemplate.opsForValue()
        .set(key, JSON.toJSONString(dto), Duration.ofMinutes(10));
    redisTemplate.opsForList()
        .leftPush("answer:flush:queue", key);
    redisTemplate.opsForHash()
        .increment("paper:stat:" + dto.getPaperId(), "totalAnswer", 1);
}
```

**但有一个场景仍然需要分布式锁**：5 秒定时落库任务和用户点击"提交试卷"触发的 `forceFlushForResponse` 同时操作同一 response 的 MySQL 写入——此时两边都在做批量 INSERT + Redis 清理，需要锁（详见 3.6 节的 `forceFlushForResponse` 实现，那里保留了 `lock:answer:flush:{responseId}` 细粒度锁）。

**总结**：Redis 写入层用时间戳（无锁，轻量），MySQL 落库层用分布式锁（有锁，必须）。

---

### 3.8 场景 ⑧：MySQL 死锁

**发生时机**：批量 INSERT 时与其他事务冲突导致死锁。

**解决方案 —— Spring 重试 + 死锁检测：**

```java
@Retryable(
    value = DeadlockLoserDataAccessException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 500, multiplier = 2)
)
@Transactional(rollbackFor = Exception.class)
public void batchInsertOrUpdate(List<AnswerDTO> answers) {
    // MyBatis-Plus 的 saveOrUpdateBatch
    // 以 (response_id, question_id) 为唯一键
    baseMapper.insertOrUpdate(answers);
}
```

---

## 第四部分：全链路故障影响矩阵

| 故障点 | 用户感知 | 数据安全 | 自动恢复 | 人工介入 |
|--------|----------|----------|----------|----------|
| ① 前端断网 | "网络不稳定"提示 | localStorage 兜底 | 网络恢复后自动重放 | 不需要 |
| ② Redis 写入失败 | 无感知（降级后仍返回成功） | 本地文件兜底 | Redis 恢复后自动补偿 | 告警通知 |
| ③ 定时任务落库失败 | 无感知 | Redis 数据未删 + 状态标记 | 1 分钟后自动重试 | 重试 5 次后需介入 |
| ④ 批量落库部分失败 | 无感知 | 逐条重试 + 死信队列 | 补偿任务自动重试 | 死信数据需人工排查 |
| ⑤ Redis 宕机 | 无感知（降级） | 本地文件兜底 | Sentinel 自动切换 | 需恢复 Redis |
| ⑥ 提交时未落库数据 | 可能出现延迟 | 提交接口主动触发落库 | 自动 | 不需要 |
| ⑦ 并发冲突 | 无感知 | 分布式锁保护 | 锁等待后自动执行 | 不需要 |
| ⑧ MySQL 死锁 | 无感知 | 事务回滚 + 重试 | Spring Retry 自动重试 | 3 次重试后告警 |

---

## 第五部分：每日数据对账机制

无论多少层兜底，最终要有机制验证数据完整性。每天凌晨 3 点，由分布式锁控制的单实例执行全量对账。

### 5.0 对账总览：三道递进校验

对账不是漫无目的地"到处查"，而是三条精准的校验线，从粗到细递进：

```
第一道：paper  ↔ response   → 查"试卷有没有发到人"
第二道：response ↔ answer    → 查"人答的题有没有存下来"
第三道：response.score ↔ SUM(answer.score) → 查"存下来的数据算没算对"
```

### 5.1 第一道对账：试卷分发完整性（paper ↔ response）

**对账双方**：paper 表的已发布试卷 vs response 表的记录

**校验规则**：一份已发布试卷期望覆盖的老师数量 = response 表中该 paper_id 的实际记录数

**期望值来源**：根据 paper 表中存储的层级字段（province / city / county / school）和 project_id，查询 user 表中符合条件的 teacher 数量

```java
private void reconcilePaperDistribute() {
    List<Paper> activePapers = paperMapper.selectList(
        new LambdaQueryWrapper<Paper>()
            .eq(Paper::getStatus, 1)  // 已发布（含已截止但仍可能有老师在答的）
            .ge(Paper::getCreatedAt, LocalDate.now().minusDays(30)) // 只对账最近30天
    );
    
    for (Paper paper : activePapers) {
        // 期望：符合层级条件的老师数
        long expectedCount = userMapper.countTeachersByRegion(
            paper.getProvince(), paper.getCity(), 
            paper.getCounty(), paper.getSchool()
        );
        
        // 实际：response 表中该 paper_id 的记录数
        long actualCount = responseMapper.selectCount(
            new LambdaQueryWrapper<Response>()
                .eq(Response::getPaperId, paper.getId())
        );
        
        if (expectedCount != actualCount) {
            // 记录差异日志
            ReconciliationLog log = ReconciliationLog.builder()
                .checkType("PAPER_DISTRIBUTE")
                .refId(paper.getId())
                .expectedCount(expectedCount)
                .actualCount(actualCount)
                .diffCount(expectedCount - actualCount)
                .build();
            reconciliationLogMapper.insert(log);
            
            // 分级处理（详见第六部分）
            handlePaperDistributeDiff(paper, expectedCount, actualCount);
        }
    }
}
```

### 5.2 第二道对账：答题数据完整性（response ↔ answer）

**对账双方**：已提交的 response 记录 vs answer 表的答题明细

**校验规则**：状态为"已提交"(2)或"已批阅"(3)的 response，其对应的 answer 记录数应等于试卷题目数

**恢复路径**（从快到慢，逐级查找）：
1. Redis → 2. 服务器本地降级文件 → 3. 前端 localStorage（通过接口反向查询）→ 4. 确认丢失

```java
private void reconcileAnswerData() {
    // 只查最近 7 天提交的，控制对账量
    List<Response> responses = responseMapper.selectList(
        new LambdaQueryWrapper<Response>()
            .in(Response::getStatus, 2, 3)  // 已提交或已批阅
            .ge(Response::getSubmitTime, LocalDate.now().minusDays(7))
    );
    
    for (Response resp : responses) {
        Paper paper = paperMapper.selectById(resp.getPaperId());
        int expectedCount = paper.getQuestionCount();  // 该试卷应有多少题
        
        int actualCount = answerMapper.selectCount(
            new LambdaQueryWrapper<Answer>()
                .eq(Answer::getResponseId, resp.getId())
        );
        
        if (expectedCount != actualCount) {
            // 先尝试从 Redis 恢复
            Set<String> redisKeys = redisTemplate.keys(
                "answer:single:" + resp.getId() + ":*"
            );
            Set<String> dataKeys = redisKeys.stream()
                .filter(k -> !k.endsWith(":status"))
                .collect(Collectors.toSet());
            
            if (!dataKeys.isEmpty()) {
                // Redis 中还有 → 自动修复：强制落库
                answerFlushService.forceFlushForResponse(resp.getId());
                log.warn("对账修复：从 Redis 恢复缺失答案, responseId={}, "
                    + "Redis中有{}条, 缺失{}条", 
                    resp.getId(), dataKeys.size(), 
                    expectedCount - actualCount);
            } else {
                // Redis 中没有 → 查本地降级文件
                List<AnswerDTO> fallbackAnswers = 
                    fallbackFileService.readAnswerFallback(resp.getId());
                
                if (!fallbackAnswers.isEmpty()) {
                    // 降级文件中有 → 自动修复
                    answerMapper.batchInsertOrUpdate(fallbackAnswers);
                    fallbackFileService.clearFallback(resp.getId());
                    log.warn("对账修复：从降级文件恢复缺失答案, "
                        + "responseId={}, 恢复{}条", 
                        resp.getId(), fallbackAnswers.size());
                } else {
                    // 确认丢失 → 记录并告警
                    ReconciliationLog log = ReconciliationLog.builder()
                        .checkType("ANSWER_MISSING")
                        .refId(resp.getId())
                        .expectedCount((long) expectedCount)
                        .actualCount((long) actualCount)
                        .diffCount((long) (expectedCount - actualCount))
                        .recoverySource("NONE")
                        .build();
                    reconciliationLogMapper.insert(log);
                    
                    // 分级告警（见第六部分）
                    handleAnswerMissing(resp, expectedCount, actualCount);
                }
            }
        }
    }
}
```

### 5.3 第三道对账：分数一致性（汇总 ↔ 明细）

**对账双方**：response.score vs SUM(answer.score)

**校验规则**：对于已批阅（status=3）的 response，其 score 字段必须等于 answer 表中该 response_id 下所有记录的 score 之和

```java
private void reconcileStatistics() {
    // 查出最近 7 天批阅的
    List<Response> reviewed = responseMapper.selectList(
        new LambdaQueryWrapper<Response>()
            .eq(Response::getStatus, 3)  // 已批阅
            .ge(Response::getReviewTime, LocalDate.now().minusDays(7))
    );
    
    for (Response resp : reviewed) {
        // 从 answer 表汇总各题分数
        BigDecimal sumScore = answerMapper.sumScoreByResponseId(resp.getId());
        // sumScore 可能为 null（answer 全丢的情况）
        BigDecimal actualSum = sumScore != null ? sumScore : BigDecimal.ZERO;
        BigDecimal recordScore = resp.getScore() != null 
            ? resp.getScore() : BigDecimal.ZERO;
        
        if (recordScore.compareTo(actualSum) != 0) {
            // 以明细为准，自动修正汇总
            BigDecimal oldScore = resp.getScore();
            boolean oldPass = resp.getIsPass() == 1;
            
            // 重新计算合格判定
            Paper paper = paperMapper.selectById(resp.getPaperId());
            boolean newPass = actualSum.compareTo(paper.getPassScore()) >= 0;
            
            // 写入修正后的数据
            responseMapper.update(null, new LambdaUpdateWrapper<Response>()
                .eq(Response::getId, resp.getId())
                .set(Response::getScore, actualSum)
                .set(Response::getIsPass, newPass ? 1 : 0)
            );
            
            // 记录修正日志（修之前 vs 修之后，追溯用）
            ReconciliationLog log = ReconciliationLog.builder()
                .checkType("SCORE_MISMATCH")
                .refId(resp.getId())
                .oldValue(oldScore.toPlainString())
                .newValue(actualSum.toPlainString())
                .oldPass(oldPass)
                .newPass(newPass)
                .autoFixed(true)
                .build();
            reconciliationLogMapper.insert(log);
            
            log.warn("对账修正：responseId={}, 分数 {}→{}, 合格 {}→{}",
                resp.getId(), oldScore, actualSum, oldPass, newPass);
        }
    }
}
```

### 5.4 对账日志表（reconciliation_log）

所有差异必须入库，用于趋势分析和问题追溯：

```sql
CREATE TABLE reconciliation_log (
    id           BIGINT PRIMARY KEY,
    check_type   VARCHAR(50)  NOT NULL COMMENT 'PAPER_DISTRIBUTE|ANSWER_MISSING|SCORE_MISMATCH',
    ref_id       BIGINT       NOT NULL COMMENT '关联ID（paper_id或response_id）',
    expected_count BIGINT     COMMENT '期望值',
    actual_count   BIGINT     COMMENT '实际值',
    diff_count     BIGINT     COMMENT '差异值',
    old_value      VARCHAR(50) COMMENT '修正前值（分数对账用）',
    new_value      VARCHAR(50) COMMENT '修正后值（分数对账用）',
    old_pass       TINYINT    COMMENT '修正前合格状态',
    new_pass       TINYINT    COMMENT '修正后合格状态',
    recovery_source VARCHAR(50) COMMENT '恢复来源：REDIS|LOCAL_FILE|NONE',
    auto_fixed     TINYINT DEFAULT 0 COMMENT '是否自动修复：0=否、1=是',
    severity       TINYINT DEFAULT 0 COMMENT '严重级别：0=微小、1=一般、2=严重',
    handled        TINYINT DEFAULT 0 COMMENT '是否已处理',
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_check_type (check_type),
    INDEX idx_created_at (created_at),
    INDEX idx_severity (severity)
);
```

---

## 第六部分：对账差异发现后的处理策略

对账不是为对而对——查出差异后怎么处理，才真正决定数据安全。

### 6.0 处理总则

三条原则：
1. **能自动修复的不惊动人**，修完记日志。
2. **修不了的按差异量级分级告警**，由人判断。
3. **不掩盖**——无论是否自动修复，差异日志一律写入 reconciliation_log 表。

### 6.1 第一道差异处理：response 少了人

**这是什么问题**：某份试卷应该发给 1000 个老师，但 response 表里只有 950 条。

**差异量 < 50 人，且 paper 状态仍为"已发布"** → 自动修复：
```
找出缺失的 50 个 teacher_id → 批量 INSERT INTO response → 
记录 reconciliation_log（auto_fixed=1, severity=0）
```

**差异量在 50～500 人，且 paper 状态仍为"已发布"** → 自动修复 + 告警：
```
同上自动补 response 记录
同时发黄色告警：说明可能 Kafka 消费端出现过短暂异常
```

**差异量 > 500 人，或 paper 已截止** → 不自动修复，发红色告警：
```
差异太大说明可能是系统级故障（Kafka 消息批量丢失、消费端长时间挂掉）
paper 已截止说明即使补了老师也来不及作答
需要人判断：重新开放试卷 → 补发 → 还是直接作废这批老师的数据
```

### 6.2 第二道差异处理：answer 少了题

**这是什么问题**：老师提交了试卷（response.status=2），但 answer 表里少了几道题的记录。

**按恢复来源分级处理**：

| 恢复来源 | 自动动作 | 告警级别 | 说明 |
|----------|----------|----------|------|
| Redis 中找到 | 自动 forceFlush → 修复 | 不告警（仅记日志） | 定时任务慢了，数据没丢 |
| 本地降级文件中找到 | 自动 INSERT → 修复 | 黄色预警 | Redis 曾不可用，兜底生效 |
| 前端 localStorage 找到 | 调接口拉取 → 修复 | 黄色预警 | 网络曾断开，前端兜底生效 |
| 全部找不到 | 无法修复 | 红色告警 | 兜底机制全部失效，数据丢失 |

**丢题量级对结果的影响**：

```
丢失 1～2 题 → 标记为"数据缺失"，response 仍可正常批阅
               缺失题记 0 分，在批阅结果中标注"数据异常"
               
丢失 3 题以上但仍可汇总 → response 正常批阅，但全体告警

整张试卷 answer 全丢 → response 状态回退，联系老师重新作答
```

### 6.3 第三道差异处理：分数对不上

**这是什么问题**：response.score=85，但 SUM(answer.score)=80。

**处理方式**：以明细（answer 表）为准，自动覆盖汇总（response 表），因为明细是最小颗粒的事实。

```
自动修正步骤：
1. SUM(answer.score) → 新总分
2. 新总分 >= paper.pass_score → 重新判定 is_pass
3. 写入 response.score 和 response.is_pass
4. 记录 reconciliation_log（old_value、new_value），方便追溯
5. 发黄色告警（因为说明有操作绕过了正常的批阅完成流程）
```

> **为什么能自动修**：分数不一致通常不是因为数据丢了，而是批阅完成后有人手动改了 answer 表的单题分数但忘记重新汇总。数据本身是完整的，只是汇总字段陈旧了，重新 SUM 一把即修复。

### 6.4 共性规则

不管哪道对账的差异，都遵守以下规则：

**分级告警阈值**：

| 差异量 | 动作 | 级别 |
|--------|------|------|
| 0 条 | 无 | 正常 |
| 1～10 条 | 仅记录 reconciliation_log | 微小（可能是并发时序问题） |
| 10～100 条 | 日志 + 黄色预警 | 一般（需关注，但不紧急） |
| 100 条以上 | 日志 + 红色紧急告警 | 严重（需立即排查） |

**趋势分析**：连续 3 天对账都有同一类差异（同一 check_type），说明对应的兜底机制存在系统性问题。此时自动触发深度对账——全量扫描该 check_type 的所有历史数据，生成趋势报告供人工排查根因。

**reconciliation_log 表定期清理**：已自动修复且超过 30 天的日志归档到历史表，保持主表轻量。

---

## 第七部分：监控与告警总览

| 监控项 | 指标 | 告警阈值 | 级别 |
|--------|------|----------|------|
| Kafka Lag | `kafka_consumer_lag` | >5000 | 红色 |
| Kafka 副本不足 | `kafka_under_replicated` | >0 | 黄色 |
| Redis 连接状态 | `redis_connected` | =0 | 红色 |
| Redis 内存使用率 | `redis_memory_used_pct` | >80% | 黄色 |
| 落库失败率 | `answer_flush_fail_rate` | >1% | 红色 |
| 落库重试次数 | `answer_retry_count` | >100/hour | 黄色 |
| MySQL 死锁次数 | `mysql_deadlock_count` | >10/hour | 黄色 |
| 本地降级文件大小 | `fallback_file_size` | >10MB | 黄色 |
| 对账差异数量 | `reconciliation_diff` | >0 | 黄色 |
| 对账差异超过100条 | `reconciliation_diff` | >100 | 红色 |

---

## 文档变更记录

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-05-11 | v1.0 | 初始版本，覆盖 Kafka/Redis/MySQL 全链路故障场景 |
| 2026-05-11 | v1.1 | 新增 Kafka DLQ 与 Redis 失败重试的区别说明；重写对账机制为三道递进校验 + 详细差异处理策略；新增 reconciliation_log 表设计 |
| 2026-05-11 | v1.2 | 修正：待落库队列从 RPOP 改为 Range + 成功后删除（解决定时任务崩溃丢数据风险）；并发控制从分布式锁改为客户端时间戳（解决覆盖问题）；补充 localStorage synced 标记机制说明；补充重试策略不用指数退避的原因分析；新增 answer:progress 三个使用时机详解 |
