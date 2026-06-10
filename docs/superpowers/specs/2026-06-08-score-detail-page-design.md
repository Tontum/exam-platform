# 成绩详情页（ScoreDetail）设计文档

> 日期：2026-06-08
> 范围：学员端查看已批阅试卷的完整答卷回放

## 1. 背景

学员提交试卷并经校长批阅后，需要查看完整的答卷回放：每道题的题干、所有选项、自己的答案、正确答案、是否正确、得分、主观题评语。当前 `ScoreDetail.vue` 仅有骨架 UI 和 mock 数据，后端无对应 API。

## 2. 覆盖状态

- **仅覆盖 response.status = 3（已批阅）**
- status = 2（已提交但未批阅）不展示此页面，PaperList.vue 中该状态仍走「查看详情」按钮

## 3. 页面布局

采用三栏布局，与 ExamPage.vue 风格一致：

```
┌───────────────────────────────────────────────────────────────┐
│ ← 返回              试卷名称                 得分 78/100     │
├──────────┬──────────────────────────────────┬─────────────────┤
│ 左侧     │ 中间：试卷内容区                  │ 右侧：答题卡    │
│ 题号导航  │                                  │                 │
│          │ ┌─ 成绩总览卡片 ──────────────┐  │ 题号格子        │
│  1 ●     │ │ 合格/不合格 | 78分/100分    │  │ 颜色区分：      │
│  2 ✓     │ │ 正确率 60% | 题目数 5       │  │ 绿=正确        │
│  3 ✗     │ │ 批阅时间 2025-02-16 14:00   │  │ 红=错误        │
│  4 ○     │ └────────────────────────────┘  │ 灰=未答        │
│  5 ✓     │                                  │                 │
│          │ ┌─ 第1题 ─────────────────────┐  │ 统计：          │
│          │ │ [单选题] 2分                 │  │ ✓ 答对 3题     │
│          │ │ 题干文本...                  │  │ ✗ 答错 1题     │
│          │ │ A. 选项A                     │  │ ○ 未答 1题     │
│          │ │ B. 选项B  ← 你的答案(绿)     │  │                 │
│          │ │ C. 选项C                     │  │                 │
│          │ │ D. 选项D                     │  │                 │
│          │ │ ─────────────────────────── │  │                 │
│          │ │ 你的答案：B  正确答案：B      │  │                 │
│          │ │ 得分：2/2 ✓                  │  │                 │
│          │ └────────────────────────────┘  │                 │
│          │                                  │                 │
│          │ ┌─ 第4题 ─────────────────────┐  │                 │
│          │ │ [主观题] 15分                │  │                 │
│          │ │ 题干文本...                  │  │                 │
│          │ │ 你的作答：                   │  │                 │
│          │ │ "我认为以学生为中心..."      │  │                 │
│          │ │ 批阅评语：                   │  │                 │
│          │ │ "论述较全面，但缺少..."      │  │                 │
│          │ │ 得分：12/15                  │  │                 │
│          │ └────────────────────────────┘  │                 │
└──────────┴──────────────────────────────────┴─────────────────┘
```

### 3.1 顶部工具栏

- 左侧：返回按钮 + 试卷名称
- 右侧：得分显示（大字体，绿色合格/红色不合格）

### 3.2 成绩总览卡片

| 字段 | 说明 |
|------|------|
| 合格状态 | 大图标 + 文字（合格/不合格） |
| 得分 | `总得分 / 试卷总分`，大字体高亮 |
| 及格线 | 灰色小字 |
| 正确率 | 客观题正确数/客观题总数 |
| 题目数 | 总题目数 |
| 批阅时间 | response.review_time |

### 3.3 逐题展示

#### 客观题（单选/多选/判断）

```
第1题  [单选题] 2分                                        ✓ 正确 / ✗ 错误
───────────────────────────────────────────────────────────────────────
题干文本...

A. 选项内容                                    ← 你的答案时高亮边框
B. 选项内容                                    ← 正确答案绿色背景+勾
C. 选项内容
D. 选项内容

┌─────────────────────────────────────────────┐
│ 你的答案：B        正确答案：B               │
│ 得分：2 / 2                                 │
└─────────────────────────────────────────────┘
```

选项高亮规则：
- 正确选项：绿色背景 + 右侧 ✓ 图标
- 用户选了且错误：红色背景 + 右侧 ✗ 图标
- 用户选了且正确：绿色背景 + 右侧 ✓ 图标
- 未选的错误选项：默认样式

#### 主观题（essay）

```
第4题  [主观题] 15分                                       得分 12/15
───────────────────────────────────────────────────────────────────────
题干文本...

你的作答：
┌─────────────────────────────────────────────┐
│ 作答内容...                                  │
└─────────────────────────────────────────────┘

批阅评语：
┌─────────────────────────────────────────────┐
│ 校长的评语...                                │
└─────────────────────────────────────────────┘

得分：12 / 15
```

### 3.4 右侧答题卡

题号格子，颜色区分：
- 绿色 = 答对（is_correct = 1）
- 红色 = 答错（is_correct = 0 或主观题得分 < 满分）
- 灰色 = 未作答（answer_content 为空）

底部统计：答对 N 题、答错 N 题、未答 N 题

### 3.5 左侧题号导航

与 ExamPage.vue 一致，点击跳转到对应题目区域。颜色同答题卡。

## 4. API 设计

### 4.1 接口

```
GET /api/answer/{paperId}/result
```

### 4.2 请求

- Path: `paperId` — 试卷ID
- Header: `Authorization: Bearer {token}`（JWT 中携带 userId）
- 校验：response.status 必须为 3（已批阅），否则返回 400

### 4.3 响应 VO

```java
public class ExamResultVO {
    private String paperName;
    private BigDecimal totalScore;    // 试卷总分
    private BigDecimal passScore;     // 及格线
    private BigDecimal userScore;     // 用户得分
    private Boolean isPass;           // 是否合格
    private Integer questionCount;    // 题目总数
    private Integer correctCount;     // 客观题正确数
    private Integer objectiveCount;   // 客观题总数
    private String submitTime;        // 提交时间
    private String reviewTime;        // 批阅时间
    private List<ExamResultQuestionVO> questions;
}

public class ExamResultQuestionVO {
    private Long questionId;
    private String stem;              // 题干
    private Integer questionType;     // 1=单选 2=多选 3=判断 4=主观
    private BigDecimal score;         // 该题分值
    private Integer sortOrder;
    private String userAnswer;        // 用户答案（选项标签如 "B" 或 "A,C"，或主观题文本）
    private BigDecimal gotScore;      // 该题得分
    private Boolean isCorrect;        // 客观题是否正确，主观题为 null
    private String reviewComment;     // 批阅评语（主观题）
    private List<ExamResultOptionVO> options;  // 客观题选项列表
}

public class ExamResultOptionVO {
    private Long optionId;
    private String optionLabel;       // A/B/C/D
    private String optionContent;     // 选项内容
    private Boolean isCorrect;        // 是否为正确答案
}
```

## 5. 后端实现要点

### 5.1 Controller

在 `AnswerController` 新增方法：

```java
@GetMapping("/{paperId}/result")
public Result<ExamResultVO> getExamResult(@PathVariable Long paperId,
                                           @RequestAttribute("userId") Long userId)
```

### 5.2 Service 实现逻辑

1. 查 `response` 表：paperId + userId → 必须存在且 status = 3
2. 查 `paper` 表：获取试卷名称、总分、及格线
3. 查 `question` 表：获取所有题目
4. 查 `option` 表：获取所有选项（含 is_correct 标记）
5. 查 `answer` 表：获取用户每题的答案、得分、是否正确、评语
6. 组装 `ExamResultVO` 返回

### 5.3 数据来源

所有数据来自 MySQL（提交时已写入），不依赖 Redis。

## 6. 前端实现要点

### 6.1 改动文件

| 文件 | 改动 |
|------|------|
| `views/teacher/ScoreDetail.vue` | 重写：三栏布局 + 逐题展示 + 答题卡 |
| `api/paper.ts` | 新增 `getExamResult()` 函数 + 类型定义 |

### 6.2 不改动

- 路由不变：`/teacher/score/:paperId`
- PaperList.vue 不变：status=3 的「查看成绩」按钮已指向此页面

### 6.3 样式复用

- 布局复用 ExamPage.vue 的三栏 SCSS 结构
- 配色复用项目 CSS 变量（--color-primary, --color-success, --color-danger 等）
- 选项高亮规则：正确=绿底+✓，错误=红底+✗

## 7. 数据流

```
PaperList.vue (status=3, 点击「查看成绩」)
  → router.push('/teacher/score/{paperId}')
    → ScoreDetail.vue onMounted
      → GET /api/answer/{paperId}/result
        → AnswerController.getExamResult()
          → AnswerServiceImpl.getExamResult()
            → 查 response + paper + question + option + answer
            → 组装 ExamResultVO
      → 渲染三栏布局
```
