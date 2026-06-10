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
import { ref, computed, onMounted } from 'vue'
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

const correctCount = computed(() => resultData.value.questions.filter(q => q.isCorrect === true).length)
const wrongCount = computed(() => resultData.value.questions.filter(q => q.isCorrect === false).length)
const subjectiveCount = computed(() => resultData.value.questions.filter(q => q.isCorrect === null).length)
const unansweredCount = computed(() => resultData.value.questions.filter(q => !q.userAnswer || q.userAnswer.trim() === '').length)
const correctRate = computed(() => {
  if (resultData.value.objectiveCount === 0) return 0
  return Math.round((resultData.value.correctCount / resultData.value.objectiveCount) * 100)
})

function questionTypeTag(type: number) {
  const map: Record<number, string> = { 1: '', 2: 'success', 3: 'warning', 4: 'info' }
  return map[type] || 'info'
}
function questionTypeLabel(type: number) {
  const map: Record<number, string> = { 1: '单选题', 2: '多选题', 3: '判断题', 4: '主观题' }
  return map[type] || '未知'
}

function isUserSelected(userAnswer: string | null, label: string): boolean {
  if (!userAnswer) return false
  return userAnswer.split(',').map(s => s.trim()).includes(label)
}

function getCorrectLabels(options: { optionLabel: string; isCorrect: boolean }[]): string {
  return options.filter(o => o.isCorrect).map(o => o.optionLabel).join(', ') || '无'
}

function formatTime(time: string): string {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

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

.score-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding: 20px;
  gap: 20px;
}

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

.question-area {
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
}

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

.empty-state {
  padding: 80px 0;
  text-align: center;
}

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
