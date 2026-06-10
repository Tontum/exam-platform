# 成绩详情页（ScoreDetail）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现学员端已批阅试卷的完整答卷回放页面，包含后端 API 和前端三栏布局。

**Architecture:** 后端在 AnswerController 新增 `GET /api/answer/{paperId}/result` 接口，从 MySQL 查询 response + paper + question + option + answer 五表数据组装返回。前端重写 ScoreDetail.vue 为三栏布局（题号导航 + 试卷内容 + 答题卡），复用 ExamPage.vue 的布局风格。

**Tech Stack:** Spring Boot 3.2 + MyBatis-Plus + Vue 3 + TypeScript + Element Plus

---

## 文件清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 创建 | `exam-project-service/.../model/vo/ExamResultVO.java` | 成绩详情总 VO |
| 创建 | `exam-project-service/.../model/vo/ExamResultQuestionVO.java` | 逐题结果 VO |
| 创建 | `exam-project-service/.../model/vo/ExamResultOptionVO.java` | 选项结果 VO |
| 修改 | `exam-project-service/.../service/AnswerService.java:36` | 新增接口方法 |
| 修改 | `exam-project-service/.../service/impl/AnswerServiceImpl.java:222` | 新增实现方法 |
| 修改 | `exam-project-service/.../controller/AnswerController.java:49` | 新增接口 |
| 创建 | `exam-project-service/.../controller/AnswerControllerTest.java` | 集成测试 |
| 修改 | `exam-frontend/src/api/paper.ts:243` | 新增 API 函数 + 类型 |
| 重写 | `exam-frontend/src/views/teacher/ScoreDetail.vue` | 三栏布局完整重写 |

---

## Task 1: 创建 VO 类

**Files:**
- Create: `exam-platform/exam-project-service/src/main/java/com/exam/project/model/vo/ExamResultVO.java`
- Create: `exam-platform/exam-project-service/src/main/java/com/exam/project/model/vo/ExamResultQuestionVO.java`
- Create: `exam-platform/exam-project-service/src/main/java/com/exam/project/model/vo/ExamResultOptionVO.java`

- [ ] **Step 1: 创建 ExamResultOptionVO**

```java
package com.exam.project.model.vo;

import lombok.Data;

/**
 * 成绩详情 — 选项 VO（含正确答案标记）
 */
@Data
public class ExamResultOptionVO {

    /** 选项 ID */
    private Long optionId;

    /** 选项标签（A/B/C/D/对/错） */
    private String optionLabel;

    /** 选项内容 */
    private String optionContent;

    /** 是否为正确答案 */
    private Boolean isCorrect;
}
```

- [ ] **Step 2: 创建 ExamResultQuestionVO**

```java
package com.exam.project.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 成绩详情 — 逐题结果 VO（含用户答案和批改结果）
 */
@Data
public class ExamResultQuestionVO {

    /** 题目 ID */
    private Long questionId;

    /** 题干 */
    private String stem;

    /** 题目类型：1=单选 2=多选 3=判断 4=主观 */
    private Integer questionType;

    /** 该题分值 */
    private BigDecimal score;

    /** 排序号 */
    private Integer sortOrder;

    /** 用户答案（选项标签如 "B" 或 "A,C"，或主观题文本） */
    private String userAnswer;

    /** 该题得分 */
    private BigDecimal gotScore;

    /** 客观题是否正确，主观题为 null */
    private Boolean isCorrect;

    /** 批阅评语（主观题） */
    private String reviewComment;

    /** 选项列表（客观题有，主观题为空） */
    private List<ExamResultOptionVO> options;
}
```

- [ ] **Step 3: 创建 ExamResultVO**

```java
package com.exam.project.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 成绩详情 — 试卷级 VO
 */
@Data
public class ExamResultVO {

    /** 试卷名称 */
    private String paperName;

    /** 试卷总分 */
    private BigDecimal totalScore;

    /** 及格线 */
    private BigDecimal passScore;

    /** 用户得分 */
    private BigDecimal userScore;

    /** 是否合格 */
    private Boolean isPass;

    /** 题目总数 */
    private Integer questionCount;

    /** 客观题正确数 */
    private Integer correctCount;

    /** 客观题总数 */
    private Integer objectiveCount;

    /** 提交时间 */
    private String submitTime;

    /** 批阅时间 */
    private String reviewTime;

    /** 题目列表 */
    private List<ExamResultQuestionVO> questions;
}
```

## Task 2: 后端 — 接口定义 + 实现

**Files:**
- Modify: `exam-platform/exam-project-service/src/main/java/com/exam/project/service/AnswerService.java`
- Modify: `exam-platform/exam-project-service/src/main/java/com/exam/project/service/impl/AnswerServiceImpl.java`
- Modify: `exam-platform/exam-project-service/src/main/java/com/exam/project/controller/AnswerController.java`

- [ ] **Step 1: 在 AnswerService 接口新增方法**

在 `AnswerService.java` 末尾 `}` 前添加：

```java
    /**
     * 查询已批阅试卷的完整答卷结果
     *
     * @param paperId 试卷 ID
     * @param userId  学员 ID
     * @return 完整答卷结果
     */
    ExamResultVO getExamResult(Long paperId, Long userId);
```

同时在文件头部 import 区添加：
```java
import com.exam.project.model.vo.ExamResultVO;
```

- [ ] **Step 2: 在 AnswerServiceImpl 实现 getExamResult**

在 `AnswerServiceImpl.java` 末尾 `}` 前添加以下方法：

```java
    @Override
    public ExamResultVO getExamResult(Long paperId, Long userId) {
        // 1. 查询答题记录，必须已批阅
        Response response = findResponse(paperId, userId);
        if (response.getStatus() != 3) {
            throw BusinessException.badRequest("试卷尚未批阅完成，无法查看成绩");
        }

        // 2. 查询试卷信息
        Paper paper = paperMapper.selectById(paperId);

        // 3. 查询所有题目
        LambdaQueryWrapper<Question> qw = new LambdaQueryWrapper<>();
        qw.eq(Question::getPaperId, paperId).orderByAsc(Question::getSortOrder);
        List<Question> questions = questionMapper.selectList(qw);

        // 4. 查询所有选项
        LambdaQueryWrapper<Option> ow = new LambdaQueryWrapper<>();
        ow.eq(Option::getPaperId, paperId).orderByAsc(Option::getSortOrder);
        List<Option> allOptions = optionMapper.selectList(ow);
        Map<Long, List<Option>> optionsByQuestion = allOptions.stream()
                .collect(Collectors.groupingBy(Option::getQuestionId));

        // 5. 查询用户所有答案
        LambdaQueryWrapper<Answer> aw = new LambdaQueryWrapper<>();
        aw.eq(Answer::getResponseId, response.getId());
        List<Answer> answers = answerMapper.selectList(aw);
        Map<Long, Answer> answerByQuestion = answers.stream()
                .collect(Collectors.toMap(Answer::getQuestionId, a -> a, (a, b) -> a));

        // 6. 组装逐题结果
        List<ExamResultQuestionVO> questionVOs = new ArrayList<>();
        int correctCount = 0;
        int objectiveCount = 0;

        for (Question q : questions) {
            ExamResultQuestionVO qvo = new ExamResultQuestionVO();
            qvo.setQuestionId(q.getId());
            qvo.setStem(q.getTitle());
            qvo.setQuestionType(q.getQuestionType());
            qvo.setScore(q.getScore());
            qvo.setSortOrder(q.getSortOrder());

            Answer answer = answerByQuestion.get(q.getId());
            if (answer != null) {
                qvo.setUserAnswer(answer.getAnswerContent());
                qvo.setGotScore(answer.getScore());
                qvo.setIsCorrect(answer.getIsCorrect() != null ? answer.getIsCorrect() == 1 : null);
                qvo.setReviewComment(answer.getReviewComment());
            }

            // 客观题统计
            if (q.getQuestionType() != null && q.getQuestionType() != 4) {
                objectiveCount++;
                if (answer != null && answer.getIsCorrect() != null && answer.getIsCorrect() == 1) {
                    correctCount++;
                }
            }

            // 组装选项（客观题）
            List<Option> opts = optionsByQuestion.getOrDefault(q.getId(), List.of());
            List<ExamResultOptionVO> optionVOs = opts.stream().map(o -> {
                ExamResultOptionVO ovo = new ExamResultOptionVO();
                ovo.setOptionId(o.getId());
                ovo.setOptionLabel(o.getOptionLabel());
                ovo.setOptionContent(o.getOptionContent());
                ovo.setIsCorrect(o.getIsCorrect() == 1);
                return ovo;
            }).toList();
            qvo.setOptions(optionVOs);

            questionVOs.add(qvo);
        }

        // 7. 组装总结果
        ExamResultVO result = new ExamResultVO();
        result.setPaperName(paper.getTitle());
        result.setTotalScore(paper.getTotalScore());
        result.setPassScore(paper.getPassScore());
        result.setUserScore(response.getScore());
        result.setIsPass(response.getIsPass() != null && response.getIsPass() == 1);
        result.setQuestionCount(questions.size());
        result.setCorrectCount(correctCount);
        result.setObjectiveCount(objectiveCount);
        result.setSubmitTime(response.getSubmitTime() != null ? response.getSubmitTime().toString() : null);
        result.setReviewTime(response.getReviewTime() != null ? response.getReviewTime().toString() : null);
        result.setQuestions(questionVOs);

        return result;
    }
```

同时在文件头部 import 区添加：
```java
import com.exam.project.model.vo.ExamResultVO;
import com.exam.project.model.vo.ExamResultQuestionVO;
import com.exam.project.model.vo.ExamResultOptionVO;
```

- [ ] **Step 3: 在 AnswerController 新增接口**

在 `AnswerController.java` 末尾 `}` 前添加：

```java
    /**
     * 查询已批阅试卷的完整答卷结果
     */
    @GetMapping("/{paperId}/result")
    public Result<ExamResultVO> getExamResult(@PathVariable Long paperId,
                                               @RequestAttribute("userId") Long userId) {
        return Result.ok(answerService.getExamResult(paperId, userId));
    }
```

同时在文件头部 import 区添加：
```java
import com.exam.project.model.vo.ExamResultVO;
```

- [ ] **Step 4: 编译验证**

Run: `mvn compile -pl exam-project-service -am`
Expected: BUILD SUCCESS

## Task 3: 集成测试

**Files:**
- Create: `exam-platform/exam-project-service/src/test/java/com/exam/project/controller/AnswerControllerTest.java`

- [ ] **Step 1: 创建测试类**

```java
package com.exam.project.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AnswerController 集成测试 — 连接 Docker MySQL 真实数据库
 * 测试查看已批阅试卷成绩接口
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AnswerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== 1. 查看已批阅试卷成绩 ====================

    @Test
    @Order(1)
    @DisplayName("查看已批阅试卷成绩 — 应返回完整答卷数据")
    void testGetExamResult_ReviewedPaper_ShouldReturnFullResult() throws Exception {
        // 种子数据中 response(id=4, paperId=4, userId=3, status=3, score=78)
        MvcResult result = mockMvc.perform(get("/api/answer/4/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");

        // 验证试卷级字段
        assertNotNull(data.get("paperName"), "应有试卷名称");
        assertNotNull(data.get("totalScore"), "应有试卷总分");
        assertNotNull(data.get("passScore"), "应有及格线");
        assertNotNull(data.get("userScore"), "应有用户得分");
        assertNotNull(data.get("isPass"), "应有是否合格");
        assertNotNull(data.get("questions"), "应有题目列表");
        assertTrue(data.get("questions").isArray(), "题目列表应为数组");

        // 验证逐题数据
        JsonNode questions = data.get("questions");
        assertTrue(questions.size() > 0, "应至少有1道题");
        for (JsonNode q : questions) {
            assertNotNull(q.get("questionId"), "每题应有 questionId");
            assertNotNull(q.get("stem"), "每题应有题干");
            assertNotNull(q.get("questionType"), "每题应有类型");
            assertNotNull(q.get("score"), "每题应有分值");
        }
    }

    @Test
    @Order(2)
    @DisplayName("查看未批阅试卷成绩 — 应返回 400 错误")
    void testGetExamResult_UnreviewedPaper_ShouldReturn400() throws Exception {
        // 种子数据中 response(id=1, paperId=1, userId=3, status=0) — 未答题
        mockMvc.perform(get("/api/answer/1/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("试卷尚未批阅完成，无法查看成绩"));
    }

    @Test
    @Order(3)
    @DisplayName("查看不存在的试卷成绩 — 应返回 404 错误")
    void testGetExamResult_NonExistent_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/answer/99999/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }
}
```

- [ ] **Step 2: 运行测试**

Run: `mvn test -pl exam-project-service -Dtest=AnswerControllerTest`
Expected: 全部 PASS（3 个测试）

## Task 4: 前端 API 层

**Files:**
- Modify: `exam-platform/exam-frontend/src/api/paper.ts`

- [ ] **Step 1: 新增类型定义和 API 函数**

在 `paper.ts` 文件末尾（`submitExam` 函数之后）追加：

```typescript
// ==================== 成绩查询 API ====================

/** 成绩详情 — 选项 VO */
export interface ExamResultOptionVO {
  optionId: string
  optionLabel: string
  optionContent: string
  isCorrect: boolean
}

/** 成绩详情 — 逐题结果 VO */
export interface ExamResultQuestionVO {
  questionId: string
  stem: string
  questionType: number
  score: number
  sortOrder: number
  userAnswer: string
  gotScore: number
  isCorrect: boolean | null
  reviewComment: string | null
  options: ExamResultOptionVO[]
}

/** 成绩详情 — 试卷级 VO */
export interface ExamResultVO {
  paperName: string
  totalScore: number
  passScore: number
  userScore: number
  isPass: boolean
  questionCount: number
  correctCount: number
  objectiveCount: number
  submitTime: string
  reviewTime: string
  questions: ExamResultQuestionVO[]
}

/** 查询已批阅试卷的完整答卷结果 */
export function getExamResult(paperId: string) {
  return get<ExamResultVO>(`/answer/${paperId}/result`)
}
```

## Task 5: 重写 ScoreDetail.vue

**Files:**
- Rewrite: `exam-platform/exam-frontend/src/views/teacher/ScoreDetail.vue`

- [ ] **Step 1: 重写 ScoreDetail.vue 完整内容**

```vue
<!--
  ScoreDetail.vue — 学员端：成绩详情页
  三栏布局：左侧题号导航 + 中间试卷内容（含批改标记）+ 右侧答题卡
  仅在 response.status=3（已批阅）时展示
  设计风格：复用 ExamPage.vue 的布局结构 + 清新蓝白配
-->
<template>
  <div class="score-page">
    <!-- 顶部工具栏 -->
    <div class="score-toolbar">
      <div class="toolbar-left">
        <div class="back-btn" @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
        </div>
        <div class="paper-title">{{ resultData.paperName }}</div>
      </div>
      <div class="toolbar-right">
        <div class="score-badge" :class="{ passed: resultData.isPass }">
          <span class="badge-label">{{ resultData.isPass ? '合格' : '不合格' }}</span>
          <span class="badge-score">{{ resultData.userScore }}/{{ resultData.totalScore }}</span>
        </div>
      </div>
    </div>

    <div class="score-body">
      <!-- 左侧：题号导航 -->
      <div class="question-nav">
        <div class="nav-header">题号</div>
        <div class="nav-grid">
          <div
            v-for="(q, index) in resultData.questions"
            :key="q.questionId"
            class="nav-item"
            :class="{
              active: currentIndex === index,
              correct: q.isCorrect === true,
              wrong: q.isCorrect === false,
              subjective: q.isCorrect === null,
            }"
            @click="currentIndex = index"
          >
            {{ index + 1 }}
          </div>
        </div>
        <div class="nav-summary">
          <div class="summary-item">
            <span class="dot correct"></span>
            <span>答对 {{ correctCount }} 题</span>
          </div>
          <div class="summary-item">
            <span class="dot wrong"></span>
            <span>答错 {{ wrongCount }} 题</span>
          </div>
          <div class="summary-item" v-if="subjectiveCount > 0">
            <span class="dot subjective"></span>
            <span>主观题 {{ subjectiveCount }} 题</span>
          </div>
        </div>
      </div>

      <!-- 中间：试卷内容区 -->
      <div class="question-area">
        <!-- 成绩总览卡片 -->
        <div class="result-summary">
          <div class="summary-icon" :class="{ passed: resultData.isPass }">
            <el-icon :size="40">
              <CircleCheck v-if="resultData.isPass" />
              <CircleClose v-else />
            </el-icon>
          </div>
          <div class="summary-info">
            <div class="summary-title">{{ resultData.isPass ? '恭喜通过' : '未通过' }}</div>
            <div class="summary-detail">
              得分 <span class="user-score">{{ resultData.userScore }}</span> / {{ resultData.totalScore }}
              <span class="pass-line">（及格线 {{ resultData.passScore }} 分）</span>
            </div>
          </div>
          <div class="summary-stats">
            <div class="stat">
              <span class="stat-value">{{ correctRate }}%</span>
              <span class="stat-label">正确率</span>
            </div>
            <div class="stat">
              <span class="stat-value">{{ resultData.questionCount }}</span>
              <span class="stat-label">总题数</span>
            </div>
            <div class="stat" v-if="resultData.reviewTime">
              <span class="stat-value">{{ formatTime(resultData.reviewTime) }}</span>
              <span class="stat-label">批阅时间</span>
            </div>
          </div>
        </div>

        <!-- 逐题展示 -->
        <div
          v-for="(q, index) in resultData.questions"
          :key="q.questionId"
          :id="'question-' + index"
          class="question-block"
        >
          <!-- 题目头部 -->
          <div class="block-header">
            <div class="header-left">
              <span class="q-index">第 {{ index + 1 }} 题</span>
              <el-tag :type="questionTypeTag(q.questionType)" size="small" effect="light">
                {{ questionTypeLabel(q.questionType) }}
              </el-tag>
              <span class="q-score">{{ q.score }} 分</span>
            </div>
            <div class="header-right">
              <el-tag v-if="q.isCorrect === true" type="success" size="small" effect="dark">
                <el-icon><CircleCheck /></el-icon> 正确
              </el-tag>
              <el-tag v-else-if="q.isCorrect === false" type="danger" size="small" effect="dark">
                <el-icon><CircleClose /></el-icon> 错误
              </el-tag>
              <span class="got-score" :class="{ full: q.gotScore === q.score, zero: q.gotScore === 0 }">
                {{ q.gotScore }}/{{ q.score }}
              </span>
            </div>
          </div>

          <!-- 题干 -->
          <div class="block-stem">{{ q.stem }}</div>

          <!-- 客观题选项 -->
          <div class="block-options" v-if="q.questionType !== 4">
            <div
              v-for="opt in q.options"
              :key="opt.optionId"
              class="option-row"
              :class="{
                'is-correct': opt.isCorrect,
                'is-user-answer': isUserSelected(q.userAnswer, opt.optionLabel),
                'is-wrong-selection': isUserSelected(q.userAnswer, opt.optionLabel) && !opt.isCorrect,
              }"
            >
              <span class="option-label">{{ opt.optionLabel }}.</span>
              <span class="option-content">{{ opt.optionContent }}</span>
              <span class="option-mark">
                <el-icon v-if="opt.isCorrect" class="mark-correct"><CircleCheck /></el-icon>
                <el-icon v-if="isUserSelected(q.userAnswer, opt.optionLabel) && !opt.isCorrect" class="mark-wrong"><CircleClose /></el-icon>
                <el-icon v-if="isUserSelected(q.userAnswer, opt.optionLabel) && opt.isCorrect" class="mark-correct"><CircleCheck /></el-icon>
              </span>
            </div>
          </div>

          <!-- 客观题答案汇总 -->
          <div class="block-answer" v-if="q.questionType !== 4">
            <div class="answer-row">
              <span class="answer-label">你的答案：</span>
              <span :class="{ 'answer-wrong': q.isCorrect === false, 'answer-correct': q.isCorrect === true }">
                {{ q.userAnswer || '未作答' }}
              </span>
            </div>
            <div class="answer-row">
              <span class="answer-label">正确答案：</span>
              <span class="answer-correct">{{ getCorrectLabels(q.options) }}</span>
            </div>
          </div>

          <!-- 主观题作答 -->
          <div class="block-essay" v-if="q.questionType === 4">
            <div class="essay-section">
              <div class="essay-label">你的作答：</div>
              <div class="essay-content">{{ q.userAnswer || '未作答' }}</div>
            </div>
            <div class="essay-section" v-if="q.reviewComment">
              <div class="essay-label">批阅评语：</div>
              <div class="essay-comment">{{ q.reviewComment }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：答题卡 -->
      <div class="answer-card">
        <div class="card-header">
          <h4>答题卡</h4>
        </div>
        <div class="card-grid">
          <div
            v-for="(q, index) in resultData.questions"
            :key="q.questionId"
            class="card-cell"
            :class="{
              correct: q.isCorrect === true,
              wrong: q.isCorrect === false,
              subjective: q.isCorrect === null,
              current: currentIndex === index,
            }"
            @click="scrollToQuestion(index)"
          >
            {{ index + 1 }}
          </div>
        </div>
        <div class="card-summary">
          <div class="summary-item">
            <span class="summary-dot correct"></span>
            <span>答对：{{ correctCount }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-dot wrong"></span>
            <span>答错：{{ wrongCount }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-dot unanswered"></span>
            <span>未答：{{ unansweredCount }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="empty-state" v-if="!loading && resultData.questions.length === 0">
      <el-empty description="暂无成绩数据" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getExamResult, type ExamResultVO } from '@/api/paper'

const route = useRoute()
const paperId = route.params.paperId as string
const loading = ref(true)
const currentIndex = ref(0)

const resultData = ref<ExamResultVO>({
  paperName: '',
  totalScore: 0,
  passScore: 0,
  userScore: 0,
  isPass: false,
  questionCount: 0,
  correctCount: 0,
  objectiveCount: 0,
  submitTime: '',
  reviewTime: '',
  questions: [],
})

// 统计
const correctCount = computed(() => resultData.value.questions.filter(q => q.isCorrect === true).length)
const wrongCount = computed(() => resultData.value.questions.filter(q => q.isCorrect === false).length)
const subjectiveCount = computed(() => resultData.value.questions.filter(q => q.isCorrect === null).length)
const unansweredCount = computed(() => resultData.value.questions.filter(q => !q.userAnswer || q.userAnswer.trim() === '').length)
const correctRate = computed(() => {
  if (resultData.value.objectiveCount === 0) return 0
  return Math.round((resultData.value.correctCount / resultData.value.objectiveCount) * 100)
})

// 题目类型
function questionTypeTag(type: number) {
  const map: Record<number, string> = { 1: '', 2: 'success', 3: 'warning', 4: 'info' }
  return map[type] || 'info'
}
function questionTypeLabel(type: number) {
  const map: Record<number, string> = { 1: '单选题', 2: '多选题', 3: '判断题', 4: '主观题' }
  return map[type] || '未知'
}

// 判断用户是否选了某选项
function isUserSelected(userAnswer: string | null, label: string): boolean {
  if (!userAnswer) return false
  return userAnswer.split(',').map(s => s.trim()).includes(label)
}

// 获取正确答案标签
function getCorrectLabels(options: { optionLabel: string; isCorrect: boolean }[]): string {
  return options.filter(o => o.isCorrect).map(o => o.optionLabel).join(', ') || '无'
}

// 格式化时间
function formatTime(time: string): string {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// 滚动到指定题目
function scrollToQuestion(index: number) {
  currentIndex.value = index
  const el = document.getElementById('question-' + index)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

onMounted(async () => {
  try {
    resultData.value = await getExamResult(paperId)
  } catch (e: any) {
    ElMessage.error(e.message || '加载成绩失败')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.score-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-color-page);
}

/* 顶部工具栏 */
.score-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-color-card);
  padding: 0 24px;
  height: 64px;
  border-bottom: 1px solid var(--border-color-lighter);
  box-shadow: var(--shadow-sm);
  z-index: 100;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--text-secondary);
  transition: all var(--transition-fast);

  &:hover {
    background: var(--bg-color);
    color: var(--color-primary);
  }
}

.paper-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.score-badge {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 20px;
  border-radius: var(--radius-lg);
  background: var(--color-danger-light);
  color: var(--color-danger);
  font-weight: 600;

  &.passed {
    background: var(--color-success-light);
    color: var(--color-success);
  }

  .badge-label {
    font-size: 14px;
  }

  .badge-score {
    font-size: 20px;
    font-weight: 700;
  }
}

/* 主体三栏布局 */
.score-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding: 20px;
  gap: 20px;
}

/* 左侧题号导航 */
.question-nav {
  width: 80px;
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  padding: 16px 12px;
  overflow-y: auto;
  box-shadow: var(--shadow-sm);
}

.nav-header {
  font-size: 12px;
  color: var(--text-secondary);
  text-align: center;
  margin-bottom: 12px;
  font-weight: 500;
}

.nav-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-bottom: 16px;
}

.nav-item {
  width: 32px;
  height: 32px;
  line-height: 32px;
  text-align: center;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  background: var(--bg-color);
  color: var(--text-secondary);
  transition: all var(--transition-fast);
  margin: 0 auto;

  &:hover {
    background: var(--color-primary-light);
    color: var(--color-primary);
  }

  &.active {
    background: var(--color-primary);
    color: white;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
  }

  &.correct {
    background: var(--color-success-light);
    color: var(--color-success);
    border: 1px solid var(--color-success);

    &.active {
      background: var(--color-success);
      color: white;
      border-color: var(--color-success);
    }
  }

  &.wrong {
    background: var(--color-danger-light);
    color: var(--color-danger);
    border: 1px solid var(--color-danger);

    &.active {
      background: var(--color-danger);
      color: white;
      border-color: var(--color-danger);
    }
  }

  &.subjective {
    background: var(--color-warning-light);
    color: var(--color-warning);
    border: 1px solid var(--color-warning);

    &.active {
      background: var(--color-warning);
      color: white;
      border-color: var(--color-warning);
    }
  }
}

.nav-summary {
  padding-top: 12px;
  border-top: 1px solid var(--border-color-lighter);
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 6px;

  &:last-child {
    margin-bottom: 0;
  }
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;

  &.correct {
    background: var(--color-success);
  }

  &.wrong {
    background: var(--color-danger);
  }

  &.subjective {
    background: var(--color-warning);
  }
}

/* 中间试卷内容区 */
.question-area {
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
}

/* 成绩总览卡片 */
.result-summary {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 28px 32px;
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  margin-bottom: 20px;
}

.summary-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-danger-light);
  color: var(--color-danger);
  flex-shrink: 0;

  &.passed {
    background: var(--color-success-light);
    color: var(--color-success);
  }
}

.summary-info {
  flex: 1;
}

.summary-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.summary-detail {
  font-size: 15px;
  color: var(--text-secondary);
}

.user-score {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-primary);
}

.pass-line {
  font-size: 13px;
  color: var(--text-secondary);
}

.summary-stats {
  display: flex;
  gap: 28px;
}

.stat {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

/* 逐题展示 */
.question-block {
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  margin-bottom: 16px;
  overflow: hidden;
}

.block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: var(--bg-color);
  border-bottom: 1px solid var(--border-color-lighter);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.q-index {
  font-weight: 600;
  color: var(--text-primary);
}

.q-score {
  font-size: 13px;
  color: var(--text-secondary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.got-score {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-secondary);

  &.full {
    color: var(--color-success);
  }

  &.zero {
    color: var(--color-danger);
  }
}

.block-stem {
  padding: 20px 24px;
  font-size: 16px;
  line-height: 1.8;
  color: var(--text-primary);
}

/* 选项 */
.block-options {
  padding: 0 24px 20px;
}

.option-row {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 8px;
  background: var(--bg-color);
  border-radius: var(--radius-md);
  border: 2px solid transparent;
  transition: all var(--transition-fast);

  &.is-correct {
    background: var(--color-success-light);
    border-color: var(--color-success);
  }

  &.is-wrong-selection {
    background: var(--color-danger-light);
    border-color: var(--color-danger);
  }

  &.is-user-answer:not(.is-correct) {
    background: var(--color-danger-light);
    border-color: var(--color-danger);
  }
}

.option-label {
  font-weight: 600;
  color: var(--color-primary);
  margin-right: 10px;
  flex-shrink: 0;
}

.option-content {
  flex: 1;
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.6;
}

.option-mark {
  margin-left: 12px;
  flex-shrink: 0;
  font-size: 18px;
}

.mark-correct {
  color: var(--color-success);
}

.mark-wrong {
  color: var(--color-danger);
}

/* 客观题答案汇总 */
.block-answer {
  padding: 0 24px 20px;
  margin: 0 24px 20px;
  background: var(--bg-color);
  border-radius: var(--radius-md);
  padding: 16px;
}

.answer-row {
  display: flex;
  align-items: baseline;
  margin-bottom: 8px;
  font-size: 14px;

  &:last-child {
    margin-bottom: 0;
  }
}

.answer-label {
  color: var(--text-secondary);
  margin-right: 12px;
  white-space: nowrap;
}

.answer-correct {
  color: var(--color-success);
  font-weight: 600;
}

.answer-wrong {
  color: var(--color-danger);
  font-weight: 600;
}

/* 主观题 */
.block-essay {
  padding: 0 24px 24px;
}

.essay-section {
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }
}

.essay-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
  font-weight: 500;
}

.essay-content {
  padding: 16px;
  background: var(--bg-color);
  border-radius: var(--radius-md);
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-primary);
  white-space: pre-wrap;
}

.essay-comment {
  padding: 16px;
  background: var(--color-primary-light);
  border-radius: var(--radius-md);
  font-size: 14px;
  line-height: 1.8;
  color: var(--color-primary-dark);
  font-style: italic;
}

/* 右侧答题卡 */
.answer-card {
  width: 200px;
  background: var(--bg-color-card);
  padding: 20px;
  overflow-y: auto;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.card-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color-lighter);

  h4 {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary);
    text-align: center;
  }
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  margin-bottom: 20px;
}

.card-cell {
  width: 32px;
  height: 32px;
  line-height: 32px;
  text-align: center;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  background: var(--bg-color);
  color: var(--text-secondary);
  transition: all var(--transition-fast);
  margin: 0 auto;

  &:hover {
    transform: scale(1.1);
  }

  &.correct {
    background: var(--color-success-light);
    color: var(--color-success);
    border: 1px solid var(--color-success);
  }

  &.wrong {
    background: var(--color-danger-light);
    color: var(--color-danger);
    border: 1px solid var(--color-danger);
  }

  &.subjective {
    background: var(--color-warning-light);
    color: var(--color-warning);
    border: 1px solid var(--color-warning);
  }

  &.current {
    box-shadow: 0 0 0 2px var(--color-primary);
    transform: scale(1.1);
  }
}

.card-summary {
  padding-top: 16px;
  border-top: 1px solid var(--border-color-lighter);
}

.summary-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  display: inline-block;

  &.correct {
    background: var(--color-success);
  }

  &.wrong {
    background: var(--color-danger);
  }

  &.unanswered {
    background: var(--bg-color);
    border: 2px solid var(--border-color);
  }
}

/* 空状态 */
.empty-state {
  padding: 80px 0;
  text-align: center;
}

/* 响应式调整 */
@media (max-width: 1024px) {
  .score-body {
    flex-direction: column;
  }

  .question-nav {
    width: 100%;
    max-height: 80px;
  }

  .nav-grid {
    grid-template-columns: repeat(auto-fill, minmax(32px, 1fr));
  }

  .nav-summary {
    display: flex;
    gap: 16px;
    padding-top: 8px;
  }

  .answer-card {
    width: 100%;
    max-height: 200px;
  }

  .card-grid {
    grid-template-columns: repeat(10, 1fr);
  }

  .result-summary {
    flex-direction: column;
    text-align: center;
  }

  .summary-stats {
    width: 100%;
    justify-content: space-around;
  }
}
</style>
```

## Task 6: 验证

- [ ] **Step 1: 编译后端**

Run: `mvn compile -pl exam-project-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 2: 运行后端测试**

Run: `mvn test -pl exam-project-service -Dtest=AnswerControllerTest`
Expected: 3 个测试全部 PASS

- [ ] **Step 3: 启动前端开发服务器**

Run: `npm run dev` (在 exam-frontend 目录)
Expected: 无编译错误

- [ ] **Step 4: 手动验证页面**

访问 `http://localhost:3000/teacher/score/4`（种子数据中 paperId=4, userId=3, status=3 已批阅），确认：
1. 顶部显示试卷名称和得分
2. 成绩总览卡片显示合格/不合格、正确率、批阅时间
3. 逐题展示：选项高亮正确（绿）/错误（红）、正确答案标记、得分
4. 右侧答题卡颜色正确
5. 点击答题卡题号可滚动到对应题目
