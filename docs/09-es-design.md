# 09 - Elasticsearch 使用场景与优化方案

> 本文档梳理系统中所有可以使用 Elasticsearch 的位置、每个场景的具体实现方式、索引设计与映射、查询优化策略，以及 ES 在生产环境中可能遇到的问题和解决方案。

---

## 1. ES 使用场景全景

### 1.1 场景总览

| 场景 | 优先级 | 数据量级 | 核心能力 |
|------|:---:|------|------|
| 试题检索 | P0（核心） | 100 万+ | 全文检索 + 结构化筛选 |
| 文章检索 | P1 | 10 万+ | 标题/正文全文搜索 |
| 试卷检索 | P2 | 1 万+ | 试卷名称/描述搜索 |
| 用户检索 | P2 | 40 万+ | 姓名/学校模糊搜索（管理后台用） |
| 操作日志检索 | P3（预留） | 千万级 | 审计日志搜索 |

### 1.2 各场景详细需求

**场景一：试题检索（核心）**

老师在组卷或题库浏览时，要能按以下条件搜题：
- 关键词搜题干内容（如搜"一元二次方程"）
- 按题型筛选（单选题/多选题/判断题/主观题）
- 按知识点筛选（数学/英语/语文 等）
- 按难度筛选（简单/中等/困难）
- 按试卷来源筛选（某张已有试卷的题）
- 多重条件组合（如"数学 + 选择题 + 困难 + 含 '三角函数' 关键词"）

相比 MySQL 的 `LIKE '%keyword%'`，ES 的优势在于：
- 中文分词（搜"加法"能匹配到"加法和减法运算"）
- 结构化筛选和全文检索同时进行，一次查询完成
- 100 万数据量下仍保持 <50ms 响应

**场景二：文章检索**

老师在项目文章模块中搜索文章：
- 按标题关键词搜索
- 按正文内容搜索
- 按发布人/发布时间筛选

**场景三：试卷检索**

校长/管理员在试卷列表中搜索：
- 按试卷名称搜索
- 按发布人搜索
- 按创建时间段筛选

**场景四：用户检索（管理后台）**

管理员搜索老师：
- 按姓名模糊搜索
- 按学校精确或模糊搜索
- 按省市县层级筛选

**场景五：操作日志检索（预留扩展）**

运维排查问题时搜索操作日志（如"谁在什么时候删了什么试卷"）。

---

## 2. 索引设计

### 2.1 试题索引（exam_question）

**索引名**：`exam_question`

**Mapping 设计**：

```json
{
  "settings": {
    "number_of_shards": 10,
    "number_of_replicas": 1,
    "refresh_interval": "5s",
    "analysis": {
      "analyzer": {
        "ik_smart_analyzer": {
          "type": "custom",
          "tokenizer": "ik_smart",
          "filter": ["lowercase"]
        },
        "ik_max_word_analyzer": {
          "type": "custom",
          "tokenizer": "ik_max_word",
          "filter": ["lowercase"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "id":              { "type": "long" },
      "paper_id":        { "type": "long" },
      "paper_title":     { "type": "keyword" },
      "title": {
        "type": "text",
        "analyzer": "ik_max_word_analyzer",
        "search_analyzer": "ik_smart_analyzer",
        "fields": {
          "keyword": { "type": "keyword", "ignore_above": 256 }
        }
      },
      "question_type":   { "type": "byte" },
      "question_type_name": { "type": "keyword" },
      "score":           { "type": "float" },
      "difficulty":      { "type": "byte" },
      "difficulty_name": { "type": "keyword" },
      "knowledge_points": {
        "type": "text",
        "analyzer": "ik_smart_analyzer",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "analysis": {
        "type": "text",
        "analyzer": "ik_max_word_analyzer",
        "search_analyzer": "ik_smart_analyzer"
      },
      "options": {
        "type": "nested",
        "properties": {
          "label":   { "type": "keyword" },
          "content": { "type": "text", "analyzer": "ik_smart_analyzer" },
          "is_correct": { "type": "boolean" }
        }
      },
      "project_id":      { "type": "long" },
      "publisher_id":    { "type": "long" },
      "province":        { "type": "keyword" },
      "city":            { "type": "keyword" },
      "created_at":      { "type": "date" },
      "updated_at":      { "type": "date" },
      "is_deleted":      { "type": "boolean" }
    }
  }
}
```

**关键设计决策**：

| 决策 | 理由 |
|------|------|
| `number_of_shards: 10` | 100 万试题 ÷ 10 分片 = 每分片 10 万条，查询并行度好，单分片不过大 |
| `title` 用 `ik_max_word` 索引 + `ik_smart` 搜索 | 索引时尽可能多分词（提高召回率），搜索时用粗粒度分词（提高精度） |
| `knowledge_points` 额外存 `keyword` 子字段 | 精确筛选走 keyword（快），模糊搜索走 text（全） |
| `paper_title` 用 `keyword` | 按试卷名分组/筛选时不需要分词，走 keyword 更快 |
| `options` 用 `nested` 类型 | 选项是数组，需要保持"选项 A 是正确答案"这种关联关系不被扁平化打散 |
| `refresh_interval: 5s` | 试题数据不需要秒级可见，5 秒刷新降低写入开销 |

### 2.2 文章索引（article）

```json
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "refresh_interval": "10s"
  },
  "mappings": {
    "properties": {
      "id":            { "type": "long" },
      "title": {
        "type": "text",
        "analyzer": "ik_max_word_analyzer",
        "search_analyzer": "ik_smart_analyzer",
        "fields": {
          "keyword": { "type": "keyword", "ignore_above": 200 }
        }
      },
      "content": {
        "type": "text",
        "analyzer": "ik_max_word_analyzer",
        "search_analyzer": "ik_smart_analyzer"
      },
      "summary":       { "type": "text", "analyzer": "ik_smart_analyzer" },
      "author_id":     { "type": "long" },
      "author_name":   { "type": "keyword" },
      "project_id":    { "type": "long" },
      "province":      { "type": "keyword" },
      "city":          { "type": "keyword" },
      "tags":          { "type": "keyword" },
      "view_count":    { "type": "integer" },
      "comment_count": { "type": "integer" },
      "created_at":    { "type": "date" },
      "updated_at":    { "type": "date" }
    }
  }
}
```

### 2.3 试卷索引（paper）

```json
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 1,
    "refresh_interval": "10s"
  },
  "mappings": {
    "properties": {
      "id":            { "type": "long" },
      "title": {
        "type": "text",
        "analyzer": "ik_smart_analyzer",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "description":   { "type": "text", "analyzer": "ik_smart_analyzer" },
      "publisher_id":  { "type": "long" },
      "publisher_name":{ "type": "keyword" },
      "project_id":    { "type": "long" },
      "province":      { "type": "keyword" },
      "city":          { "type": "keyword" },
      "status":        { "type": "byte" },
      "total_score":   { "type": "float" },
      "pass_score":    { "type": "float" },
      "question_count":{ "type": "integer" },
      "submit_count":  { "type": "integer" },
      "created_at":    { "type": "date" }
    }
  }
}
```

**为什么只有 1 个分片**：试卷总量只有 1 万级别，数据量小，单分片足够，多分片反而增加协调开销。

### 2.4 用户索引（user）

```json
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "refresh_interval": "30s"
  },
  "mappings": {
    "properties": {
      "id":            { "type": "long" },
      "real_name": {
        "type": "text",
        "analyzer": "ik_smart_analyzer",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "username":      { "type": "keyword" },
      "role":          { "type": "byte" },
      "school": {
        "type": "text",
        "analyzer": "ik_smart_analyzer",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "province":      { "type": "keyword" },
      "city":          { "type": "keyword" },
      "county":        { "type": "keyword" },
      "phone":         { "type": "keyword" },
      "status":        { "type": "byte" },
      "created_at":    { "type": "date" }
    }
  }
}
```

---

## 3. 数据同步方案

### 3.1 同步总览

```
MySQL 变更 → Canal 监听 binlog → 解析变更 → 推送到 ES
                ↑ 实时（<1 秒延迟）
                
MySQL 全量 → DataX / 自定义 Bulk 任务 → 写入 ES
                ↑ 初次建索引时使用
                
定时补偿任务（每小时）→ 对比 MySQL 和 ES 总量 → 差异补全
                ↑ 兜底保障
```

### 3.2 Canal 实时同步（正常路径）

**Canal 配置**：

```properties
# canal.properties
canal.serverMode = tcp
canal.destinations = exam_platform

# instance.properties
canal.instance.master.address = 127.0.0.1:3306
canal.instance.dbUsername = canal
canal.instance.dbPassword = canal123
canal.instance.filter.regex = exam_platform\\.(question|paper|article|user)
canal.instance.filter.black.regex = exam_platform\\.(answer|response|option)
# 注意：answer/response/option 不需要同步到 ES（不需要全文检索）
```

**同步服务实现**：

```java
@Component
public class CanalSyncHandler implements CanalEventListener {
    
    // 处理 INSERT 事件
    @Override
    public void onInsert(CanalEntry.Entry entry) {
        String tableName = entry.getHeader().getTableName();
        List<CanalEntry.RowData> rows = entry.getRowChange().getRowDatasList();
        
        switch (tableName) {
            case "question":
                bulkIndexQuestions(rows, IndexOperation.INDEX);
                break;
            case "paper":
                bulkIndexPapers(rows, IndexOperation.INDEX);
                break;
            case "article":
                bulkIndexArticles(rows, IndexOperation.INDEX);
                break;
            case "user":
                bulkIndexUsers(rows, IndexOperation.INDEX);
                break;
        }
    }
    
    // 处理 UPDATE 事件
    @Override
    public void onUpdate(CanalEntry.Entry entry) {
        // 试题修改 → 更新 ES 中的文档
        // 注意：update 后 Canal 传的是变更后的数据，直接覆盖索引即可
        onInsert(entry);  // 逻辑相同
    }
    
    // 处理 DELETE 事件
    @Override
    public void onDelete(CanalEntry.Entry entry) {
        // 逻辑删除：is_deleted=1 → 更新 ES 文档标记
        // 物理删除：从 ES 删除文档
        String tableName = entry.getHeader().getTableName();
        List<CanalEntry.RowData> rows = entry.getRowChange().getRowDatasList();
        
        for (CanalEntry.RowData row : rows) {
            String id = getColumnValue(row.getBeforeColumnsList(), "id");
            switch (tableName) {
                case "question":
                    deleteFromES("exam_question", id);
                    break;
                // ...
            }
        }
    }
    
    // 批量索引
    private void bulkIndexQuestions(List<CanalEntry.RowData> rows, 
                                     IndexOperation op) {
        BulkRequest bulkRequest = new BulkRequest();
        for (CanalEntry.RowData row : rows) {
            Long questionId = Long.parseLong(
                getColumnValue(row.getAfterColumnsList(), "id")
            );
            // 从 MySQL 查询完整数据（含关联的 paper_title、options 等）
            QuestionESDTO dto = questionEsMapper.buildQuestionDTO(questionId);
            
            bulkRequest.add(new IndexRequest("exam_question")
                .id(questionId.toString())
                .source(JSON.toJSONString(dto), XContentType.JSON)
            );
        }
        // 批量提交
        restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    }
}
```

> **关键点**：Canal 只拿到单表的行变更，但 ES 中一个试题文档包含了 paper_title、options 等跨表数据。所以 Canal 回调中拿到 question.id 后，要从 MySQL 重新查一遍完整的聚合数据再写 ES。不能只写 Canal 给的那几个字段。

### 3.3 全量同步（初次建索引 + 索引重建）

```java
@Component
public class ESFullSyncTask {
    
    /**
     * 全量同步试题到 ES
     * 使用 Scroll API 分批读取 MySQL，Bulk API 分批写入 ES
     */
    public void fullSyncQuestions() {
        int pageSize = 5000;
        Long lastId = 0L;
        int totalSynced = 0;
        
        while (true) {
            // 基于主键游标分页，避免深分页问题
            List<QuestionESDTO> batch = questionMapper
                .selectForESSync(lastId, pageSize);
            
            if (batch.isEmpty()) break;
            
            // 批量写入 ES
            BulkRequest bulkRequest = new BulkRequest();
            for (QuestionESDTO dto : batch) {
                bulkRequest.add(new IndexRequest("exam_question")
                    .id(dto.getId().toString())
                    .source(JSON.toJSONString(dto), XContentType.JSON)
                );
            }
            
            BulkResponse response = restHighLevelClient.bulk(
                bulkRequest, RequestOptions.DEFAULT
            );
            
            if (response.hasFailures()) {
                log.error("全量同步部分失败: {}", response.buildFailureMessage());
            }
            
            totalSynced += batch.size();
            lastId = batch.get(batch.size() - 1).getId();
            
            log.info("试题全量同步进度: {} 条", totalSynced);
        }
        
        log.info("试题全量同步完成，总计 {} 条", totalSynced);
    }
}
```

```sql
-- 对应的 Mapper SQL（游标分页，高效）
SELECT 
    q.id, q.paper_id, p.title AS paper_title, q.title, 
    q.question_type, q.score, q.analysis,
    q.is_required, q.sort_order,
    p.project_id, p.publisher_id, p.province, p.city,
    q.created_at, q.updated_at
FROM question q
LEFT JOIN paper p ON q.paper_id = p.id
WHERE q.id > #{lastId}
  AND q.is_deleted = 0
ORDER BY q.id ASC
LIMIT #{pageSize}
```

### 3.4 定时补偿（对账兜底）

每小时一次的数据量对账 + 差异补偿，逻辑与前文 06 号文档的对账机制保持一致：

```java
@Scheduled(cron = "0 0 * * * ?")  // 每小时
public void reconcileESData() {
    // 对比 MySQL COUNT 和 ES COUNT
    long mysqlCount = questionMapper.selectCount(
        new LambdaQueryWrapper<Question>().eq(Question::getIsDeleted, 0)
    );
    
    CountRequest countRequest = new CountRequest("exam_question");
    countRequest.query(QueryBuilders.boolQuery()
        .mustNot(QueryBuilders.termQuery("is_deleted", true))
    );
    long esCount = restHighLevelClient.count(
        countRequest, RequestOptions.DEFAULT
    ).getCount();
    
    if (mysqlCount != esCount) {
        log.warn("ES 数据量不一致: MySQL={}, ES={}, 差异={}", 
            mysqlCount, esCount, mysqlCount - esCount);
        
        if (Math.abs(mysqlCount - esCount) < 100) {
            // 差异小 → 差异补齐（用 3.5 节的方法）
            reconcileSmallDiff("exam_question");
        } else {
            // 差异大 → 告警，可能 Canal 同步链路出问题
            alertService.send("ES 数据量严重不一致", 
                "MySQL=" + mysqlCount + ", ES=" + esCount);
        }
    }
}
```

### 3.5 差异补齐（增量对账）

当 ES 比 MySQL 少了少量数据时，精确找出缺失的 ID 并补全：

```java
private void reconcileSmallDiff(String indexName) {
    // 拿到 MySQL 中所有问题 ID 的集合
    Set<Long> mysqlIds = questionMapper.selectAllIds();
    
    // 拿到 ES 中所有文档 ID 的集合（用 Scroll API 全量扫）
    Set<Long> esIds = new HashSet<>();
    SearchRequest scrollRequest = new SearchRequest(indexName);
    scrollRequest.scroll(TimeValue.timeValueMinutes(1));
    scrollRequest.source(new SearchSourceBuilder()
        .size(10000)
        .fetchSource(false)  // 只要 _id，不要 _source
    );
    
    SearchResponse scrollResponse = restHighLevelClient
        .search(scrollRequest, RequestOptions.DEFAULT);
    String scrollId = scrollResponse.getScrollId();
    
    do {
        for (SearchHit hit : scrollResponse.getHits().getHits()) {
            esIds.add(Long.parseLong(hit.getId()));
        }
        scrollResponse = restHighLevelClient.scroll(
            new SearchScrollRequest(scrollId).scroll(TimeValue.timeValueMinutes(1)),
            RequestOptions.DEFAULT
        );
    } while (scrollResponse.getHits().getHits().length > 0);
    
    // 差集 = MySQL 有但 ES 没有的
    mysqlIds.removeAll(esIds);
    
    if (!mysqlIds.isEmpty()) {
        log.warn("ES 缺失文档 {} 条，开始补全", mysqlIds.size());
        // 调用全量同步方法，只同步缺失的 ID
        syncSpecificIds(new ArrayList<>(mysqlIds));
    }
}
```

---

## 4. 查询实现

### 4.1 试题检索（核心查询）

```java
public PageResult<QuestionESDTO> searchQuestions(QuestionSearchParam param) {
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    
    // ---------- 全文检索（题干）----------
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
    
    if (StringUtils.hasText(param.getKeyword())) {
        boolQuery.must(QueryBuilders.multiMatchQuery(
            param.getKeyword(),
            "title^3",           // 题干权重最高
            "knowledge_points^2", // 知识点权重次之
            "analysis"           // 解析权重最低
        ).type(MultiMatchQueryBuilder.Type.BEST_FIELDS));
    }
    
    // ---------- 结构化筛选（Filter Context，不影响评分，可缓存）----------
    BoolQueryBuilder filterQuery = QueryBuilders.boolQuery();
    
    filterQuery.mustNot(QueryBuilders.termQuery("is_deleted", true));
    
    if (param.getQuestionType() != null) {
        filterQuery.must(QueryBuilders.termQuery(
            "question_type", param.getQuestionType()
        ));
    }
    if (param.getDifficulty() != null) {
        filterQuery.must(QueryBuilders.termQuery(
            "difficulty", param.getDifficulty()
        ));
    }
    if (StringUtils.hasText(param.getKnowledgePoint())) {
        filterQuery.must(QueryBuilders.termQuery(
            "knowledge_points.keyword", param.getKnowledgePoint()
        ));
    }
    if (param.getPaperId() != null) {
        filterQuery.must(QueryBuilders.termQuery(
            "paper_id", param.getPaperId()
        ));
    }
    if (param.getProjectId() != null) {
        filterQuery.must(QueryBuilders.termQuery(
            "project_id", param.getProjectId()
        ));
    }
    // 层级过滤
    if (StringUtils.hasText(param.getProvince())) {
        filterQuery.must(QueryBuilders.termQuery(
            "province", param.getProvince()
        ));
    }
    if (StringUtils.hasText(param.getCity())) {
        filterQuery.must(QueryBuilders.termQuery(
            "city", param.getCity()
        ));
    }
    
    // 组合：全文检索在 must，结构化筛选在 filter
    boolQuery.filter(filterQuery);
    
    // ---------- 高亮 ----------
    HighlightBuilder highlightBuilder = new HighlightBuilder()
        .field("title", 100, 1)
        .field("knowledge_points", 80, 1)
        .preTags("<em class='highlight'>")
        .postTags("</em>");
    
    // ---------- 排序 ----------
    if (StringUtils.hasText(param.getKeyword())) {
        // 有关键词时按相关性排序
        sourceBuilder.sort(SortBuilders.scoreSort());
    } else {
        // 无关键词时按创建时间倒序
        sourceBuilder.sort("created_at", SortOrder.DESC);
    }
    
    // ---------- 分页 ----------
    sourceBuilder.from((param.getPage() - 1) * param.getSize());
    sourceBuilder.size(param.getSize());
    
    // 构建请求
    sourceBuilder.query(boolQuery);
    sourceBuilder.highlighter(highlightBuilder);
    // 不返回 options 字段（列表不需要），节省带宽
    sourceBuilder.fetchSource(
        new String[]{"id","title","question_type_name","difficulty_name",
            "score","knowledge_points","paper_title","created_at"},
        new String[]{"options","analysis"}
    );
    
    SearchRequest request = new SearchRequest("exam_question");
    request.source(sourceBuilder);
    
    // 执行查询
    SearchResponse response = restHighLevelClient.search(
        request, RequestOptions.DEFAULT
    );
    
    // 结果映射
    List<QuestionESDTO> records = Arrays.stream(response.getHits().getHits())
        .map(hit -> {
            QuestionESDTO dto = JSON.parseObject(
                hit.getSourceAsString(), QuestionESDTO.class
            );
            // 填充高亮
            if (hit.getHighlightFields().containsKey("title")) {
                dto.setHighlightTitle(
                    hit.getHighlightFields().get("title").fragments()[0].string()
                );
            }
            return dto;
        })
        .collect(Collectors.toList());
    
    return new PageResult<>(records, response.getHits().getTotalHits().value);
}
```

### 4.2 Filter Context vs Query Context

```java
// ---------- 正确：结构化筛选用 filter ----------
BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
// 全文检索走 query context（参与评分）
boolQuery.must(QueryBuilders.matchQuery("title", keyword));
// 筛选条件走 filter context（不参与评分，自动缓存）
boolQuery.filter(QueryBuilders.termQuery("question_type", 1));
boolQuery.filter(QueryBuilders.termQuery("is_deleted", false));

// ---------- 错误：把所有条件都放 must ----------
boolQuery.must(QueryBuilders.termQuery("question_type", 1));
boolQuery.must(QueryBuilders.termQuery("is_deleted", false));
// ↑ term 查询在 must 中不仅浪费评分计算，还不能利用 Filter Cache
```

ES 对 filter context 中的查询会自动缓存结果（`_filter_cache`），重复查询时直接读缓存，速度最快。

### 4.3 文章检索

与试题检索类似，但搜索字段不同：

```java
BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
boolQuery.must(QueryBuilders.multiMatchQuery(keyword, 
    "title^3", "content", "summary^2"
));
// 同样用 filter 做结构化筛选
boolQuery.filter(QueryBuilders.termQuery("project_id", projectId));
boolQuery.filter(QueryBuilders.termQuery("province", userProvince));
```

### 4.4 搜索建议（自动补全）

试题搜索框输入时给出搜索建议（如输入"一元"提示"一元二次方程"）：

```java
// Mapping 中需要预先定义 suggest 字段
// "title.suggest": { "type": "completion", "analyzer": "ik_smart" }

public List<String> suggestQuestions(String prefix) {
    CompletionSuggestionBuilder suggestionBuilder = 
        SuggestBuilders.completionSuggestion("title.suggest")
            .prefix(prefix)
            .size(10)
            .skipDuplicates(true);
    
    SearchRequest request = new SearchRequest("exam_question");
    request.source(new SearchSourceBuilder().suggest(
        new SuggestBuilder().addSuggestion("question_suggest", suggestionBuilder)
    ));
    
    SearchResponse response = restHighLevelClient.search(
        request, RequestOptions.DEFAULT
    );
    
    Suggest suggest = response.getSuggest();
    CompletionSuggestion completionSuggestion = 
        suggest.getSuggestion("question_suggest");
    
    return completionSuggestion.getEntries().stream()
        .flatMap(entry -> entry.getOptions().stream())
        .map(CompletionSuggestion.Entry.Option::getText)
        .map(text -> text.string())
        .collect(Collectors.toList());
}
```

---

## 5. 性能优化

### 5.1 分片策略再优化

| 索引 | 分片数 | 单分片大小建议 | 调整策略 |
|------|:---:|------|------|
| exam_question | 10 | 10～30 GB | 数据量超过 300GB 时增加到 15 分片 |
| article | 3 | 5～20 GB | 数据量超过 60GB 时增加到 5 分片 |
| paper | 1 | <10 GB | 固定 1 分片 |
| user | 3 | 5～20 GB | 数据量超过 60GB 时增加到 5 分片 |

**分片过多的代价**：每个分片是一个 Lucene 实例，会消耗文件句柄和内存。10 万条数据建 10 个分片反而是浪费——每个分片只有 1 万条，协调开销大于查询开销。

### 5.2 索引设置优化

```json
{
  "settings": {
    "index": {
      "refresh_interval": "5s",       // 批量导入时设为 -1，导完恢复
      "number_of_replicas": 1,        // 查询压力大时可临时增加到 2
      "translog": {
        "durability": "async",        // 异步刷盘（牺牲少量可靠性换写入速度）
        "sync_interval": "5s"
      },
      "merge": {
        "scheduler": {
          "max_thread_count": 1       // 限制段合并线程数，避免影响查询
        }
      }
    }
  }
}
```

**批量导入时的特殊设置**（全量同步时使用）：

```java
// 导入前调整设置
UpdateSettingsRequest settingsRequest = new UpdateSettingsRequest("exam_question");
settingsRequest.settings(Settings.builder()
    .put("index.refresh_interval", "-1")       // 关闭自动刷新
    .put("index.number_of_replicas", 0)        // 暂时取消副本（导入完恢复）
);
restHighLevelClient.indices().putSettings(settingsRequest, RequestOptions.DEFAULT);

// ... 执行全量导入 ...

// 导入后恢复
settingsRequest.settings(Settings.builder()
    .put("index.refresh_interval", "5s")
    .put("index.number_of_replicas", 1)
);
restHighLevelClient.indices().putSettings(settingsRequest, RequestOptions.DEFAULT);
```

**效果**：关闭 refresh 后，100 万条数据导入时间从 10 分钟降到 2 分钟。

### 5.3 查询优化

**优化 1：字段裁剪（_source filtering）**

```java
// 列表查询只返回必要字段，不返回 options 和 analysis
sourceBuilder.fetchSource(
    new String[]{"id","title","question_type_name","score"},
    new String[]{"options","analysis"}  // 排除大字段
);
// 减少网络传输 60%+
```

**优化 2：避免深度分页**

```java
// ES 深分页限制：from + size <= 10000（默认 max_result_window）
// 如果用户需要翻到更深处，两种方案：

// 方案 A：Search After（游标分页，推荐）
SearchRequest firstRequest = ... ;
SearchResponse firstResponse = client.search(firstRequest);
// 取最后一条的 sort values
Object[] lastSortValues = firstResponse.getHits()
    .getHits()[firstSize - 1].getSortValues();

SearchSourceBuilder nextPage = new SearchSourceBuilder()
    .searchAfter(lastSortValues);  // 基于上一页的最后一条，不跳页

// 方案 B：限制翻页深度
if (param.getPage() > 500) {
    throw new BusinessException("请使用更精确的筛选条件缩小搜索范围");
}
```

**优化 3：多重条件时减少不必要的 must**

```java
// 过多 must 条件（特别是 must 中的 term 查询）会降低性能
// 把所有不参与评分的条件都放 filter
boolQuery.filter(filterQuery);  // filter 自动缓存
```

### 5.4 JVM 与内存优化

```yaml
# jvm.options（ES 7.17 默认已较优，关注以下）
-Xms4g
-Xmx4g  # 堆内存设为物理内存的 50%，但不超过 32GB（压缩指针阈值）

# 给操作系统留足够内存用于文件系统缓存（Lucene 依赖）
# 32GB 物理机 → 堆 16GB，OS cache 16GB
```

---

## 6. 可能的问题与解决方案

### 6.1 数据不一致

**现象**：MySQL 中试题已修改，ES 搜索结果还是旧的。

**原因排查**：
1. Canal 同步延迟（正常情况 <1 秒，高峰可能 >5 秒）
2. Canal 挂了但未告警
3. 同步代码异常被吞掉
4. 批量更新时 Canal 解析失败

**解决**：

```java
// 1. Canal 监控
@Scheduled(fixedDelay = 60000)
public void checkCanalHealth() {
    // 检查 Canal 的位点是否在推进
    long currentPosition = canalService.getCurrentPosition();
    long lastPosition = lastCheckedPosition.get();
    
    if (currentPosition == lastPosition) {
        // 连续 2 次检查位点没变 → Canal 可能卡住
        if (stallCount.incrementAndGet() >= 2) {
            alertService.send("Canal 位点停滞", "可能同步已中断");
        }
    } else {
        stallCount.set(0);
    }
    lastCheckedPosition.set(currentPosition);
}

// 2. 业务层的主动刷新接口（管理员用）
@PostMapping("/api/search/question/{questionId}/refresh")
public Result refreshQuestionInES(@PathVariable Long questionId) {
    QuestionESDTO dto = questionEsMapper.buildQuestionDTO(questionId);
    IndexRequest request = new IndexRequest("exam_question")
        .id(questionId.toString())
        .source(JSON.toJSONString(dto), XContentType.JSON);
    restHighLevelClient.index(request, RequestOptions.DEFAULT);
    return Result.success("已刷新");
}

// 3. 定时对账（见 3.4 节）
```

### 6.2 ES 集群宕机

**预防**：至少 3 个节点，禁止单节点集群。

```yaml
# elasticsearch.yml
discovery.seed_hosts: ["es01", "es02", "es03"]
cluster.initial_master_nodes: ["es01", "es02", "es03"]
```

**故障处理**：
- 1 个节点宕机 → 集群自动将副本提升为主分片，查询不受影响
- 2 个节点宕机 → 剩余节点只读，写操作被拒绝。Canal 同步暂停
- 3 个节点全宕 → 应用层降级：

```java
// 应用层 ES 降级
public PageResult<QuestionESDTO> searchQuestions(QuestionSearchParam param) {
    try {
        return esSearchService.search(param);
    } catch (ElasticsearchException e) {
        log.error("ES 不可用，降级到 MySQL 搜索", e);
        // 降级：走 MySQL LIKE 查询（慢但可用）
        return mysqlSearchService.search(param);
    }
}
```

**恢复流程**：
1. 恢复 ES 节点
2. Canal 积压的消息自动回放
3. 执行对账任务确保数据完整
4. 关闭 MySQL 降级，切回 ES

### 6.3 索引重建（零停机）

**场景**：修改了 Mapping（比如新增字段、修改分词器），需要重建索引。

**问题**：直接删索引重建会导致搜索不可用。

**解决方案——Reindex + 别名切换**：

```java
public void reindexWithZeroDowntime(String indexName) {
    String oldIndex = indexName;              // exam_question
    String newIndex = indexName + "_v2";      // exam_question_v2
    String aliasName = indexName + "_alias";  // exam_question_alias
    
    // 1. 创建新索引（使用新的 Mapping）
    createIndexWithNewMapping(newIndex);
    
    // 2. 将旧索引数据 Reindex 到新索引
    ReindexRequest reindexRequest = new ReindexRequest();
    reindexRequest.setSourceIndices(oldIndex);
    reindexRequest.setDestIndex(newIndex);
    reindexRequest.setDestOpType("create");  // 幂等
    reindexRequest.setConflicts("proceed");   // 冲突跳过
    reindexRequest.setScroll(TimeValue.timeValueMinutes(5));
    
    // 异步执行 Reindex（大索引可能很慢）
    TaskSubmissionResponse task = restHighLevelClient
        .submitReindexTask(reindexRequest, RequestOptions.DEFAULT);
    
    // 轮询等待 Reindex 完成
    while (!isTaskCompleted(task.getTask())) {
        log.info("Reindex 进行中... 已耗时 {} 分钟", elapsed);
        Thread.sleep(30000);
    }
    
    // 3. 原子切换别名：旧索引 → 新索引
    IndicesAliasesRequest aliasRequest = new IndicesAliasesRequest();
    aliasRequest.addAliasAction(
        new AliasActions(AliasActions.Type.ADD)
            .index(newIndex)
            .alias(aliasName)
    );
    // 如果别名之前指向旧索引，先移除
    if (aliasExists(aliasName)) {
        aliasRequest.addAliasAction(
            new AliasActions(AliasActions.Type.REMOVE)
                .index(oldIndex)
                .alias(aliasName)
        );
    }
    restHighLevelClient.indices()
        .updateAliases(aliasRequest, RequestOptions.DEFAULT);
    
    // 4. 应用层永远通过别名访问（不是直接索引名）
    //    切换别名后应用无感知
    //    旧索引暂时保留，确认新索引稳定后再删除
    
    log.info("零停机索引重建完成: {} → {}", oldIndex, newIndex);
}
```

**关键**：应用层访问 ES 时永远用别名（如 `exam_question_alias`）而不是直接索引名（`exam_question`），这样切换别名时应用零感知、零停机。

### 6.4 慢查询

**排查**：开启慢查询日志。

```yaml
# elasticsearch.yml
index.search.slowlog.threshold.query.warn: 1s
index.search.slowlog.threshold.query.info: 500ms
index.search.slowlog.threshold.query.debug: 200ms
index.search.slowlog.threshold.fetch.warn: 500ms
```

**常见慢查询原因与修复**：

| 问题 | 原因 | 修复 |
|------|------|------|
| 深度分页（翻到第 200 页） | from=4000, size=20 需要扫描前 4020 条 | 改用 Search After |
| 高亮大字段（highlight analysis） | 对 `analysis` 字段做高亮，文本很长 | 高亮只对 title 做，analysis 不做 |
| 聚合大量桶 | terms aggregation 返回 10000 个桶 | 限制 aggregation size，或用 composite aggregation |
| 复杂的 nested 查询 | 对 `options` 做 nested 条件查询 | 非必要不查询 nested 字段，或改为 child/parent |
| 通配符/前缀查询 | 前导通配符 `*方程` 无法利用倒排索引 | 用 ngram tokenizer 或 completion suggester 替代 |

### 6.5 内存溢出

**原因**：ES 堆内存不足，常见于：
- 深度分页（每个分页请求要在内存中构建完整的结果集）
- 大聚合（terms aggregation 产生海量桶）
- 大文档批量索引时触发频繁 GC

**解决**：

```java
// 1. 限制搜索结果窗口
PUT exam_question/_settings
{
  "index.max_result_window": 10000   // 默认就是 10000
}

// 2. 聚合查询加 circuit breaker
// 在 elasticsearch.yml 中已默认开启，无需代码改动
// indices.breaker.total.limit: 95%

// 3. 批量索引时控制批次大小
int batchSize = 1000;  // 不是越大越好，>5000 反而因为 GC 变慢
```

### 6.6 ES 与 MySQL 搜索的职责边界

**一个常见的设计误区**：所有查询都丢给 ES。

ES 应该用于**搜索**（全文检索 + 复杂筛选），不应该用于：
- 精确 ID 查找（走 MySQL 主键更快）
- 简单列表分页（走 MySQL 索引更快）
- 需要事务一致性的查询（ES 是近实时的）

**本系统的职责划分**：

| 查询场景 | 走 ES | 走 MySQL | 原因 |
|----------|:---:|:---:|------|
| 关键词搜试题 | ✅ | — | 需要分词 + 相关性排序 |
| 按题型/难度筛选试题 | ✅ | — | 结构化和全文组合查询 |
| 老师查自己的试卷列表 | — | ✅ | 简单索引查询，不需要分词 |
| 校长查批阅列表 | — | ✅ | 精确 paperId + status 查询 |
| 按姓名搜老师 | ✅ | — | 需要分词（"张三"搜到"张三丰"） |
| 按手机号搜老师 | — | ✅ | 精确匹配，不需要分词 |
| 试卷名称搜索 | ✅ | — | 需要分词 |
| 按 ID 查某张试卷 | — | ✅ | 主键查询，ES 无优势 |
| 统计数据 | — | ✅ | 需要精确数值，ES 不擅长 |

---

## 7. ES 相关监控

| 指标 | 含义 | 告警阈值 |
|------|------|------|
| `cluster_status` | 集群状态（green/yellow/red） | red 时紧急告警 |
| `unassigned_shards` | 未分配的分片数 | >0 黄色，>5 红色 |
| `search_query_time_p99` | 99 分位查询耗时 | >500ms |
| `indexing_rate` | 索引写入速率 | 骤降 >50% 告警 |
| `jvm_mem_heap_used_pct` | JVM 堆内存使用率 | >80% 黄色，>90% 红色 |
| `disk_used_pct` | 磁盘使用率 | >85% 黄色 |
| `canal_sync_delay_seconds` | Canal 同步延迟 | >10s 告警 |

---

## 文档变更记录

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-05-11 | v1.0 | 初始版本，五类 ES 使用场景、四项索引 Mapping、Canal 同步、查询实现、性能优化、六类问题与解决方案 |
