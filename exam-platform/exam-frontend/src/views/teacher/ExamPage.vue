<!--
  ExamPage.vue — 学员端：答题页面
  左侧题号导航 + 中间题目区域 + 右侧答题卡
  支持每 3 秒自动保存到 Redis、倒计时、提交试卷
  设计风格：现代简约 + 清新蓝白配
-->
<template>
  <div class="exam-page">
    <!-- 顶部工具栏：倒计时 + 提交按钮 -->
    <div class="exam-toolbar">
      <div class="toolbar-left">
        <div class="back-btn" @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>
        </div>
        <div class="exam-title">{{ paperName }}</div>
      </div>
      <div class="toolbar-center">
        <div class="countdown-wrapper" :class="{ warning: countdownMinutes <= 5 }">
          <el-icon><Clock /></el-icon>
          <span class="countdown-text">剩余时间</span>
          <span class="countdown-value">{{ formatTime(remainingSeconds) }}</span>
        </div>
      </div>
      <div class="toolbar-right">
        <div class="progress-info">
          <span class="progress-text">已答</span>
          <span class="progress-count">{{ answeredCount }}/{{ questions.length }}</span>
        </div>
        <el-button type="danger" @click="submitExam" :loading="submitting" class="submit-btn">
          <el-icon><Upload /></el-icon>
          提交试卷
        </el-button>
      </div>
    </div>

    <div class="exam-body">
      <!-- 左侧：题号导航 -->
      <div class="question-nav">
        <div class="nav-header">题号</div>
        <div class="nav-grid">
          <div
            v-for="(q, index) in questions"
            :key="q.questionId"
            class="nav-item"
            :class="{
              active: currentIndex === index,
              answered: answers[q.questionId] !== undefined,
            }"
            @click="currentIndex = index"
          >
            {{ index + 1 }}
          </div>
        </div>
      </div>

      <!-- 中间：题目内容区 -->
      <div class="question-area" v-if="currentQuestion">
        <!-- 题目类型标识 -->
        <div class="question-header">
          <el-tag :type="questionTypeTag(currentQuestion.type)" size="small" effect="light">
            {{ questionTypeLabel(currentQuestion.type) }}
          </el-tag>
          <span class="question-score">{{ currentQuestion.score }} 分</span>
        </div>

        <!-- 题干 -->
        <div class="question-stem">
          <span class="question-number">{{ currentIndex + 1 }}.</span>
          {{ currentQuestion.stem }}
        </div>

        <!-- 选项列表（选择题/判断题） -->
        <div class="question-options" v-if="['single', 'multiple', 'judge'].includes(currentQuestion.type)">
          <el-radio-group
            v-if="currentQuestion.type === 'single' || currentQuestion.type === 'judge'"
            v-model="answers[currentQuestion.questionId]"
            class="option-group"
          >
            <el-radio
              v-for="opt in currentQuestion.options"
              :key="opt.optionId"
              :value="opt.optionKey"
              class="option-item"
              size="large"
            >
              <span class="option-key">{{ opt.optionKey }}.</span>
              {{ opt.content }}
            </el-radio>
          </el-radio-group>

          <el-checkbox-group
            v-if="currentQuestion.type === 'multiple'"
            v-model="multiAnswers[currentQuestion.questionId]"
            class="option-group"
          >
            <el-checkbox
              v-for="opt in currentQuestion.options"
              :key="opt.optionId"
              :label="opt.optionKey"
              class="option-item"
              size="large"
            >
              <span class="option-key">{{ opt.optionKey }}.</span>
              {{ opt.content }}
            </el-checkbox>
          </el-checkbox-group>
        </div>

        <!-- 主观题输入区 -->
        <div class="question-essay" v-if="currentQuestion.type === 'essay'">
          <el-input
            v-model="answers[currentQuestion.questionId]"
            type="textarea"
            :rows="8"
            placeholder="请输入你的答案..."
            class="essay-input"
          />
        </div>

        <!-- 导航按钮 -->
        <div class="question-nav-buttons">
          <el-button @click="prevQuestion" :disabled="currentIndex === 0" class="nav-btn">
            <el-icon><ArrowLeft /></el-icon>
            上一题
          </el-button>
          <span class="nav-indicator">{{ currentIndex + 1 }} / {{ questions.length }}</span>
          <el-button type="primary" @click="nextQuestion" :disabled="currentIndex === questions.length - 1" class="nav-btn">
            下一题
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 右侧：答题卡 -->
      <div class="answer-card">
        <div class="card-header">
          <h4>答题卡</h4>
        </div>
        <div class="card-grid">
          <div
            v-for="(q, index) in questions"
            :key="q.questionId"
            class="card-cell"
            :class="{
              answered: isAnswered(q.questionId),
              current: currentIndex === index,
            }"
            @click="currentIndex = index"
          >
            {{ index + 1 }}
          </div>
        </div>
        <div class="card-summary">
          <div class="summary-item">
            <span class="summary-dot answered"></span>
            <span>已答：{{ answeredCount }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-dot unanswered"></span>
            <span>未答：{{ questions.length - answeredCount }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { getExamPaper, startExam, saveProgress, submitExam as submitExamApi, type ExamQuestionVO, type SubmitResultVO } from '@/api/paper'

const route = useRoute()
const router = useRouter()

const paperName = ref('')
const paperId = route.params.paperId as string
const questions = ref<ExamQuestionVO[]>([])
const currentIndex = ref(0)
const remainingSeconds = ref(3600)
const submitting = ref(false)

// 单选/判断题答案：questionId → optionKey
const answers = ref<Record<string, string>>({})
// 多选题答案：questionId → optionKey[]
const multiAnswers = ref<Record<string, string[]>>({})

// 定时器引用
let countdownTimer: number | null = null
let autoSaveTimer: number | null = null

// 当前题目
const currentQuestion = computed(() => questions.value[currentIndex.value] || null)

// 已答题数
const answeredCount = computed(() => {
  let count = 0
  questions.value.forEach(q => {
    if (isAnswered(q.questionId)) count++
  })
  return count
})

// 倒计时分钟数
const countdownMinutes = computed(() => Math.ceil(remainingSeconds.value / 60))

// 判断某题是否已答
function isAnswered(qid: string): boolean {
  if (answers.value[qid] !== undefined && answers.value[qid] !== '') return true
  const multi = multiAnswers.value[qid]
  if (multi && multi.length > 0) return true
  return false
}

// 题目类型标签
function questionTypeTag(type: string) {
  const map: Record<string, string> = { single: 'primary', multiple: 'success', judge: 'warning', essay: 'info' }
  return map[type] || 'info'
}
function questionTypeLabel(type: string) {
  const map: Record<string, string> = { single: '单选题', multiple: '多选题', judge: '判断题', essay: '主观题' }
  return map[type] || type
}

// 格式化倒计时 mm:ss
function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

// 题目导航
function prevQuestion() { if (currentIndex.value > 0) currentIndex.value-- }
function nextQuestion() { if (currentIndex.value < questions.value.length - 1) currentIndex.value++ }

// 监听多选题答案变化 → 合并到 answers 用于保存
watch(multiAnswers, (val) => {
  Object.entries(val).forEach(([qid, arr]) => {
    answers.value[qid] = arr.join(',')
  })
}, { deep: true })

// 每 3 秒自动保存到 Redis
function autoSave() {
  saveProgress(paperId, {
    answers: answers.value,
    remainingSeconds: remainingSeconds.value
  }).catch(() => {
    // 静默失败，不打扰学员答题
  })
}

// 提交试卷
async function submitExam() {
  try {
    await ElMessageBox.confirm(
      `已答 ${answeredCount.value} / ${questions.value.length} 题，确认提交？提交后不可修改。`,
      '确认提交',
      { confirmButtonText: '确认提交', cancelButtonText: '继续答题', type: 'warning' }
    )
    submitting.value = true
    await submitExamApi(paperId)
    allowLeave = true

    if (countdownTimer) clearInterval(countdownTimer)
    if (autoSaveTimer) clearInterval(autoSaveTimer)

    ElMessage.success('答题结束，试卷已提交')
    // 延迟跳转，让用户看到提示
    setTimeout(() => router.push('/teacher/projects'), 1500)
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '提交失败')
    }
  } finally {
    submitting.value = false
  }
}

// 路由守卫：防止意外退出答题页
let allowLeave = false

async function handleBack() {
  try {
    await ElMessageBox.confirm(
      '答题中的答案不会自动保存，确定要退出吗？',
      '确认退出',
      { confirmButtonText: '继续答题', cancelButtonText: '退出答题', type: 'warning' }
    )
  } catch {
    allowLeave = true
    router.back()
  }
}

onBeforeRouteLeave(async (to, from, next) => {
  if (allowLeave) {
    next()
    return
  }
  try {
    await ElMessageBox.confirm(
      '答题中的答案不会自动保存，确定要离开吗？',
      '确认离开',
      { confirmButtonText: '继续答题', cancelButtonText: '离开页面', type: 'warning' }
    )
    next(false)
  } catch {
    next()
  }
})

onMounted(async () => {
  try {
    const examPaper = await getExamPaper(paperId)
    paperName.value = examPaper.paperName
    remainingSeconds.value = examPaper.durationMinutes * 60
    questions.value = examPaper.questions

    // 标记开始答题
    await startExam(paperId)
  } catch (e: any) {
    ElMessage.error(e.message || '加载试卷失败')
  }

  countdownTimer = window.setInterval(() => {
    if (remainingSeconds.value > 0) {
      remainingSeconds.value--
    } else {
      submitExam()
    }
  }, 1000)

  autoSaveTimer = window.setInterval(autoSave, 3000)
})

onBeforeUnmount(() => {
  if (countdownTimer) clearInterval(countdownTimer)
  if (autoSaveTimer) clearInterval(autoSaveTimer)
})
</script>

<style scoped lang="scss">
.exam-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-color-page);
}

/* 顶部工具栏 */
.exam-toolbar {
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

.exam-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.toolbar-center {
  display: flex;
  align-items: center;
}

.countdown-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  background: var(--color-primary-light);
  border-radius: var(--radius-lg);
  color: var(--color-primary);
  font-weight: 500;
  
  &.warning {
    background: var(--color-danger-light);
    color: var(--color-danger);
    animation: pulse 1s infinite;
  }
  
  .el-icon {
    font-size: 18px;
  }
  
  .countdown-text {
    font-size: 14px;
  }
  
  .countdown-value {
    font-size: 20px;
    font-weight: 700;
    font-family: 'Courier New', monospace;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.progress-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-text {
  font-size: 14px;
  color: var(--text-secondary);
}

.progress-count {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-primary);
}

.submit-btn {
  min-width: 120px;
  
  .el-icon {
    margin-right: 6px;
  }
}

/* 主体三栏布局 */
.exam-body {
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
  
  &.answered {
    background: var(--color-primary-light);
    color: var(--color-primary);
    border: 1px solid var(--color-primary);
    
    &.active {
      background: var(--color-primary);
      color: white;
      border-color: var(--color-primary);
    }
  }
}

/* 中间题目区 */
.question-area {
  flex: 1;
  background: var(--bg-color-card);
  padding: 32px;
  overflow-y: auto;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.question-score {
  margin-left: auto;
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.question-stem {
  font-size: 17px;
  line-height: 1.8;
  margin-bottom: 32px;
  color: var(--text-primary);
  
  .question-number {
    font-weight: 700;
    margin-right: 8px;
    color: var(--color-primary);
  }
}

.question-options {
  margin-bottom: 32px;
}

.option-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: stretch !important;
  
  :deep(.el-radio),
  :deep(.el-checkbox) {
    display: flex !important;
    align-items: flex-start !important;
    height: auto !important;
    min-height: 44px;
    margin-right: 0 !important;
    white-space: normal !important;
  }
  
  :deep(.el-radio__input),
  :deep(.el-checkbox__input) {
    flex-shrink: 0;
    margin-top: 3px;
  }
  
  :deep(.el-radio__label),
  :deep(.el-checkbox__label) {
    flex: 1;
    text-align: left !important;
    white-space: normal !important;
    line-height: 1.7 !important;
    word-break: break-word;
    padding-left: 10px !important;
  }
  
  :deep(.el-radio__inner),
  :deep(.el-checkbox__inner) {
    vertical-align: top;
  }
}

.option-item {
  margin: 0;
  padding: 16px 20px;
  background: var(--bg-color);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color-lighter);
  transition: all var(--transition-fast);
  
  &:hover {
    border-color: var(--color-primary-light);
    background: var(--color-primary-light);
  }
  
  .option-key {
    font-weight: 600;
    color: var(--color-primary);
    margin-right: 8px;
  }
}

.question-essay {
  margin-bottom: 32px;
  
  .essay-input {
    :deep(.el-textarea__inner) {
      border-radius: var(--radius-md);
      padding: 16px;
      font-size: 15px;
      line-height: 1.8;
    }
  }
}

.question-nav-buttons {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid var(--border-color-lighter);
}

.nav-btn {
  min-width: 120px;
  
  .el-icon {
    margin: 0 6px;
  }
}

.nav-indicator {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
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
    background: var(--color-primary-light);
    color: var(--color-primary);
  }
  
  &.answered {
    background: var(--color-primary-light);
    color: var(--color-primary);
    border: 1px solid var(--color-primary);
  }
  
  &.current {
    background: var(--color-primary);
    color: white;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
    
    &.answered {
      background: var(--color-primary);
      border-color: var(--color-primary);
    }
  }
}

.card-summary {
  padding-top: 16px;
  border-top: 1px solid var(--border-color-lighter);
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.summary-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  
  &.answered {
    background: var(--color-primary-light);
    border: 2px solid var(--color-primary);
  }
  
  &.unanswered {
    background: var(--bg-color);
    border: 2px solid var(--border-color);
  }
}

/* 响应式调整 */
@media (max-width: 1024px) {
  .exam-body {
    flex-direction: column;
  }
  
  .question-nav {
    width: 100%;
    max-height: 80px;
  }
  
  .nav-grid {
    grid-template-columns: repeat(auto-fill, minmax(32px, 1fr));
  }
  
  .answer-card {
    width: 100%;
    max-height: 200px;
  }
  
  .card-grid {
    grid-template-columns: repeat(10, 1fr);
  }
}
</style>
