# 08 - 数据统计需求全景与 SQL 调优方案

> 本文档梳理系统中所有需要数据统计的位置，以及每类统计查询的 SQL 优化方案。后半部分给出通用的 SQL 调优原则和本系统特别需要注意的问题。

---

## 第一部分：数据统计需求全景

### 1.1 统计需求总览

系统中有六类统计场景，按实时性要求和计算复杂度分为两级：

| 统计类别 | 实时性 | 触发方式 | 涉及表 |
|----------|--------|----------|--------|
| 试卷维度统计 | 准实时（允许 30min 延迟） | 定时计算 + 缓存 | paper、response、answer |
| 区域维度统计 | 同上 | 定时计算 + 缓存 | response（含层级字段） |
| 老师个人统计 | 半实时（允许 5min 延迟） | 被动查询触发 | response、answer |
| 实时答题监控 | 实时（<5s） | Redis Hash 累加 | Redis paper:stat Hash |
| 考核积分 | 日终汇总 | 凌晨定时任务 | response、config |
| 趋势报表 | 离线（天/周/月） | 凌晨定时任务 | 汇总表 |

---

### 1.2 试卷维度统计

**统计内容**：

| 指标 | 数据来源 | 计算方式 |
|------|----------|----------|
| 应考人数 | paper → 层级 → user 表 | COUNT(符合条件的 teacher) |
| 已提交人数 | response（status=2 或 3） | COUNT WHERE paper_id=? AND status>=2 |
| 已批阅人数 | response（status=3） | COUNT WHERE paper_id=? AND status=3 |
| 平均分 | response（status=3） | AVG(score) WHERE paper_id=? AND status=3 |
| 合格率 | response（status=3） | COUNT(is_pass=1) / COUNT(*) * 100 |
| 各分数段分布 | response（status=3） | CASE WHEN 分段 GROUP BY |
| 每道题正确率 | answer + question | COUNT(is_correct=1) / COUNT(*) per question |

**高频查询示例**：

```sql
-- 试卷整体统计（最常用）
SELECT 
    COUNT(*) AS total_submit,
    COUNT(CASE WHEN is_pass = 1 THEN 1 END) AS pass_count,
    ROUND(AVG(score), 1) AS avg_score,
    MAX(score) AS max_score,
    MIN(score) AS min_score
FROM response
WHERE paper_id = ?
  AND status = 3;
```

**优化要点**：该查询在 `paper_id` 上有索引（`idx_paper_id`），但在 `(paper_id, status)` 上建联合索引更优，避免回表过滤 status。

---

### 1.3 区域维度统计

**统计内容**：同一张试卷，按省/市/县/校分别统计平均分和合格率。

```sql
-- 按市级别统计（以 city 为例）
SELECT 
    city,
    COUNT(*) AS submit_count,
    ROUND(AVG(score), 1) AS avg_score,
    ROUND(COUNT(CASE WHEN is_pass = 1 THEN 1 END) * 100.0 / COUNT(*), 1) AS pass_rate
FROM response
WHERE paper_id = ?
  AND status = 3
  AND city IS NOT NULL
GROUP BY city
ORDER BY avg_score DESC;
```

**优化要点**：
- response 表中冗余存了 province/city/county/school，避免 JOIN user 表
- 索引：`idx_paper_region(paper_id, city)` 或联合索引 `(paper_id, city, status, score)` 做覆盖索引

```sql
-- 推荐索引
ALTER TABLE response ADD INDEX idx_paper_city_status_score 
    (paper_id, city, status, score);
-- 这样上面的 GROUP BY city 查询可以只走索引，不读数据页
```

---

### 1.4 老师个人统计

**统计内容**：某个老师的所有试卷成绩、考核总分。

```sql
-- 老师全部试卷成绩
SELECT 
    r.id AS response_id,
    r.paper_id,
    p.title AS paper_name,
    r.score,
    r.is_pass,
    r.submit_time,
    r.status,
    p.total_score,
    p.pass_score
FROM response r
JOIN paper p ON r.paper_id = p.id
WHERE r.user_id = ?
ORDER BY r.submit_time DESC;
```

**优化要点**：
- response 表的 `idx_user_id(user_id)` 已存在
- paper 表通过主键 `id` 关联，走主键索引
- 如果老师数量多（40 万），每人查一次个人统计，这个查询频繁执行，考虑加 Redis 缓存 120 秒

---

### 1.5 实时答题监控

**统计内容**：某张试卷当前有多少人在答题、已提交多少人。

**实现方式**：不走 MySQL，用 Redis Hash 实时累加（见 docs/06 第二部分）。

```
Redis Hash: paper:stat:{paperId}
  total_started:  1523   ← 已开始答题人数（response.status=1 时 +1）
  submitted:      892    ← 已提交人数（response.status=2 时 +1）
  reviewing:      450    ← 待批阅人数（动态计算）
```

查询时直接 `HGETALL paper:stat:{paperId}`，耗时 <1ms。

---

### 1.6 考核积分统计

**统计内容**：每个老师累计的考核积分（提交试卷加分 + 阅读文章加分 + ...）。

**计算方式**：凌晨定时任务，一天汇总一次。

```sql
-- 某老师某项目的考核积分汇总
SELECT 
    pu.user_id,
    pu.project_id,
    COALESCE(SUM(
        CASE WHEN r.status >= 2 THEN c.score_per_submit ELSE 0 END
    ), 0) AS exam_points,
    -- 其他工具积分（文章、作业等扩展预留）
    COALESCE(SUM(other_tool_points), 0) AS other_points
FROM project_user pu
LEFT JOIN response r ON r.user_id = pu.user_id 
    AND r.created_at >= CURDATE()
LEFT JOIN paper p ON r.paper_id = p.id AND p.project_id = pu.project_id
LEFT JOIN config c ON c.project_id = pu.project_id 
    AND c.tool_id = (SELECT id FROM tool WHERE tool_code = 'paper')
    AND c.role = 3
GROUP BY pu.user_id, pu.project_id;
```

---

### 1.7 趋势报表（预留）

| 报表 | 粒度 | 说明 |
|------|------|------|
| 周报 | 按天 | 本周每天提交数、合格率变化 |
| 月报 | 按天/按周 | 本月考核完成趋势 |
| 同比报表 | 按月 | 与上月/去年同期对比 |

趋势报表数据量大，建议创建**统计汇总表**，凌晨定时任务预计算后写入，查询时直接读汇总表。

```sql
CREATE TABLE stat_daily_summary (
    id           BIGINT PRIMARY KEY,
    stat_date    DATE NOT NULL,
    paper_id     BIGINT,
    project_id   BIGINT,
    total_submit INT DEFAULT 0,
    pass_count   INT DEFAULT 0,
    avg_score    DECIMAL(5,1),
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_date_paper (stat_date, paper_id),
    INDEX idx_project_date (project_id, stat_date)
);
```

---

## 第二部分：SQL 调优方案

### 2.1 本系统高频查询与索引对照

基于 `docs/02-database-design.md` 已有的索引设计，补充统计场景下的优化：

| 查询场景 | 默认使用的索引 | 优化建议 | 预期效果 |
|----------|:---:|------|----------|
| 查某试卷答题列表 | `idx_paper_id` | 改为 `(paper_id, status, score)` 覆盖索引 | 避免回表，省去主键查找 |
| 查某老师未答试卷 | `uk_paper_user` | 当前已最优 | — |
| 查某老师某状态试卷 | `idx_user_status` | 当前已最优 | — |
| 区域统计（按市分组） | `idx_paper_id` → 再按 city 过滤 | 加 `(paper_id, city, status, score)` 覆盖索引 | 只走索引，不读数据页 |
| 成绩汇总（AVG/SUM） | `idx_paper_id` | 同上覆盖索引 | — |
| 按提交时间排序 | 无 | 加 `(paper_id, submit_time)` | 分页排序不再 filesort |
| 按分数排序 | 无 | 加 `(paper_id, score)` | 分数排名不再 filesort |

---

### 2.2 覆盖索引详解（本系统最重要的优化）

覆盖索引是指查询需要的所有字段都在索引中，不需要回表查数据页。

**反面例子**（当前索引设计下的查询）：

```sql
-- 查某试卷所有已批阅的 response
SELECT id, user_id, score, is_pass, submit_time 
FROM response 
WHERE paper_id = 1288291 AND status = 3;

-- 执行过程：
-- 1. 走 idx_paper_id 索引 → 找到符合的 id 列表
-- 2. 回表（聚簇索引）→ 读每行的 user_id、score、is_pass、submit_time
-- 3. 过滤 status=3
-- 消耗：索引扫描 + 大量随机IO回表
```

**优化后**（建覆盖索引）：

```sql
-- 先建覆盖索引
ALTER TABLE response ADD INDEX idx_paper_status_cover 
    (paper_id, status, user_id, score, is_pass, submit_time);

-- 同一个查询，执行过程变为：
-- 1. 走 idx_paper_status_cover 索引
-- 2. 所有需要的字段（user_id, score, is_pass, submit_time）都在索引中
-- 3. 直接返回，不回表
-- 消耗：仅索引顺序扫描，无随机IO
```

**本系统推荐创建的覆盖索引清单**：

```sql
-- 1. 试卷+状态 → 答题列表（高频）
ALTER TABLE response ADD INDEX idx_paper_status_list 
    (paper_id, status, user_id, score, is_pass, submit_time);

-- 2. 试卷+区域 → 区域统计（高频）
ALTER TABLE response ADD INDEX idx_paper_city_stats 
    (paper_id, city, status, score, is_pass);

-- 3. 老师+状态 → 个人试卷列表
ALTER TABLE response ADD INDEX idx_user_status_list 
    (user_id, status, paper_id, score, is_pass, submit_time);

-- 4. 题目查询（老师进入试卷）
ALTER TABLE question ADD INDEX idx_paper_sort 
    (paper_id, sort_order, id, title, question_type, score, is_required);

-- 5. 选项查询（按试卷批量取选项）
ALTER TABLE `option` ADD INDEX idx_paper_question 
    (paper_id, question_id, option_label, option_content, is_correct);
```

> **注意**：覆盖索引不是免费的——每条索引占用磁盘空间，且 INSERT/UPDATE 时需要维护更多索引。上面的清单只覆盖了最高频的查询，不要给每个查询都建覆盖索引。

---

### 2.3 大表 COUNT 优化

**场景**：校长查看"我的试卷列表"时，需要展示每份试卷的已提交人数。

**问题 SQL**：

```sql
SELECT p.*, 
    (SELECT COUNT(*) FROM response r WHERE r.paper_id = p.id) AS submit_count
FROM paper p
WHERE p.publisher_id = ?;
```

40 万 response，每张 paper 都做一次子查询 COUNT，慢且不可控。

**优化方案 A**：不用子查询，用 LEFT JOIN + GROUP BY。

```sql
SELECT p.*, COUNT(r.id) AS submit_count
FROM paper p
LEFT JOIN response r ON r.paper_id = p.id
WHERE p.publisher_id = ?
GROUP BY p.id;
```

**优化方案 B**：在 paper 表上维护冗余字段 `submit_count`，在 response 提交/批阅时异步更新。查询时直接读字段，零计算开销。

```java
// 老师提交试卷后 → 异步更新 paper 的 submit_count
@Async
public void incrementPaperSubmitCount(Long paperId) {
    paperMapper.update(null, new LambdaUpdateWrapper<Paper>()
        .eq(Paper::getId, paperId)
        .setSql("submit_count = submit_count + 1")
    );
}
```

本系统选择方案 B——因为已提交人数是高频展示数据，冗余字段的维护成本远低于每次实时 COUNT。

---

### 2.4 深分页优化

**场景**：校长在批阅列表翻到第 50 页（每页 20 条，offset=1000）。

**问题 SQL**：

```sql
SELECT * FROM response 
WHERE paper_id = ? AND status = 2
ORDER BY submit_time ASC
LIMIT 1000, 20;
```

MySQL 需要扫描前 1020 条再丢弃前 1000 条，offset 越大越慢。

**优化方案——基于游标的分页（Keyset Pagination）**：

```sql
-- 第一页
SELECT * FROM response 
WHERE paper_id = ? AND status = 2
ORDER BY submit_time ASC, id ASC
LIMIT 20;

-- 第二页（传入上一页最后一条的 submit_time 和 id）
SELECT * FROM response 
WHERE paper_id = ? 
  AND status = 2
  AND (submit_time > '2026-05-10 14:30:00' 
       OR (submit_time = '2026-05-10 14:30:00' AND id > 938271))
ORDER BY submit_time ASC, id ASC
LIMIT 20;
```

每次只扫描 20 条，不受页码影响。缺点是前端不能显示总页数和跳页，只能"加载更多"。对于批阅列表这种业务场景，这个限制可接受。

**如果必须用传统分页**：在业务层限制最大翻页深度（比如最多翻到第 100 页），超过则提示使用筛选条件缩小范围。

---

### 2.5 JOIN 优化

**场景**：统计查询中多表关联。

**原则——小表驱动大表**：

```sql
-- 不好：response（大表 40 万）驱动 paper（小表 1000）
SELECT r.*, p.title
FROM response r
JOIN paper p ON r.paper_id = p.id
WHERE p.publisher_id = 100;

-- 更好：先确定 paper 范围，再查 response
SELECT r.*, p.title
FROM paper p
JOIN response r ON r.paper_id = p.id
WHERE p.publisher_id = 100;
```

MySQL 优化器会自动选择驱动表，但在复杂查询中可能选错。可以通过 `STRAIGHT_JOIN` 强制指定驱动顺序，或用子查询先缩小数据范围。

**原则——先过滤再关联**：

```sql
-- 不好：全量关联后再过滤
SELECT ...
FROM response r
JOIN paper p ON r.paper_id = p.id
JOIN user u ON r.user_id = u.id
WHERE r.status = 3 AND p.project_id = 100;

-- 更好：各自先过滤再关联
SELECT ...
FROM (
    SELECT * FROM response WHERE status = 3
) r
JOIN (
    SELECT * FROM paper WHERE project_id = 100
) p ON r.paper_id = p.id
JOIN user u ON r.user_id = u.id;
```

---

### 2.6 统计查询中的临时表优化

**场景**：复杂的多维度统计（按试卷 + 区域 + 时间段）。

**问题**：一次性 JOIN 四张表的 GROUP BY 查询，临时表可能写磁盘。

```sql
-- 可能导致磁盘临时表的查询
SELECT p.title, r.city, COUNT(*) AS cnt, AVG(r.score)
FROM response r
JOIN paper p ON r.paper_id = p.id
WHERE r.status = 3
GROUP BY r.paper_id, r.city
ORDER BY cnt DESC;
```

**优化方案——拆成两次查询**：

```sql
-- 第一步：按试卷和城市聚合（只查 response，不 JOIN）
SELECT paper_id, city, COUNT(*) AS cnt, AVG(score) AS avg_score
FROM response
WHERE status = 3
GROUP BY paper_id, city;

-- 第二步：程序内拿到 paper_id 列表，批量查 paper 表获取 title
SELECT id, title FROM paper WHERE id IN (id1, id2, ...);
```

在应用层拼接结果，数据库只做聚合，不做关联。对于统计查询（不是在线事务），这种拆分通常比一条复杂 SQL 更快。

---

### 2.7 EXPLAIN 检查清单

每一条新增的 SQL 在提交代码前，必须跑 EXPLAIN 检查以下指标：

```sql
EXPLAIN SELECT ...;
```

| 检查项 | 红线（必须修改） | 黄线（建议优化） |
|--------|:---:|:---:|
| type | ALL（全表扫描） | index（全索引扫描） |
| key | NULL（没走索引） | — |
| Extra 含 Using filesort | 在大数据量查询中 | — |
| Extra 含 Using temporary | 在 GROUP BY 大表时 | — |
| rows | >10 万 | >1 万 |
| Extra 含 Using where | — | 配合非覆盖索引时需要回表 |

**本系统重点关注的 SQL 及 EXPLAIN 目标**：

```sql
-- 1. 老师查试卷列表（P0，必须最优）
EXPLAIN SELECT * FROM response WHERE user_id = ? ORDER BY created_at DESC;
-- 目标：type=ref, key=idx_user_id, rows<100, 无 filesort

-- 2. 校长查待批阅列表（P0）
EXPLAIN SELECT * FROM response WHERE paper_id = ? AND status = 2 ORDER BY submit_time ASC LIMIT 20;
-- 目标：type=ref, key=覆盖索引, rows<2000, 无 filesort

-- 3. 区域统计（P2）
EXPLAIN SELECT city, COUNT(*), AVG(score) FROM response WHERE paper_id = ? AND status = 3 GROUP BY city;
-- 目标：type=ref, key=覆盖索引, 无 Using temporary
```

---

### 2.8 慢查询监控

开启 MySQL 慢查询日志，定期分析：

```sql
-- my.cnf 配置
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 0.5           -- 超过 500ms 的查询记入慢日志
log_queries_not_using_indexes = 0  -- 上线初期开 1，稳定后关 0
```

每周用 `pt-query-digest` 分析慢日志，重点看：
- 出现次数最多的慢查询（优先优化频率最高的）
- 执行时间最长的慢查询（看是否有锁等待或大事务）
- 扫描行数最多的慢查询（看是否缺失索引）

---

## 第三部分：特别注意事项

### 3.1 统计不准的问题

**问题**：凌晨统计任务和实时数据之间存在时间差。比如凌晨 3 点统计，但 2:58 分有老师提交了试卷，response.status 刚好在统计窗口边缘。

**解决**：统计截止时间精确到秒，且在统计任务开始时先快照时间：
```java
LocalDateTime cutoffTime = LocalDateTime.now().withNano(0);
// 所有统计 SQL 统一用 cutoffTime，不加 NOW()
```

### 3.2 统计任务影响在线业务

**问题**：凌晨统计任务的 SQL 如果扫全表或大量 JOIN，会占用 MySQL 资源和 IO，可能影响凌晨仍在答题的老师（虽然少但有）。

**解决**：
- 统计任务使用**从库**查询，不压主库
- 统计写入汇总表时使用独立连接池，限制最大连接数
- 如果只有单库，给统计 SQL 加 `/*MAX_EXECUTION_TIME=30000*/` 限制最大执行时间

### 3.3 统计数据的缓存一致性

统计结果缓存在 Redis 中 30 分钟。如果一个老师刚提交了试卷就去查统计，可能看到 30 分钟前的旧数据。这在产品层面是**可接受的**——统计页面明确标注"数据更新时间"，用户有预期。

如果需要即时刷新，提供一个"手动刷新"按钮，按钮点击时删除对应 Redis key，触发重新计算。

### 3.4 索引多了反而是负担

response 表是写入最频繁的表（老师提交、批量分发），每多一个索引，INSERT 就要多维护一个 B+Tree。上文推荐的覆盖索引只建 3 个，不是无限制。原则是：**读频率 > 写频率 × 10 的查询才考虑加覆盖索引。**

### 3.5 SQL 在代码中的规范

- 所有 SQL 写在 Mapper XML 或 MyBatis-Plus 的 LambdaQueryWrapper 中，**禁止字符串拼接 SQL**
- 动态排序字段必须用白名单校验（orderBy 参数只允许 `submit_time`、`score` 等预定义值）
- 分页查询统一用 MyBatis-Plus 的 `Page` 对象，不手写 LIMIT OFFSET
- 批量操作优先用 MyBatis-Plus 的 `saveBatch`（底层是 JDBC batch），不写循环单条 INSERT

---

## 文档变更记录

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-05-11 | v1.0 | 初始版本，六类统计需求梳理 + 覆盖索引/COUNT/分页/JOIN/临时表/EXPLAIN 优化方案 |
