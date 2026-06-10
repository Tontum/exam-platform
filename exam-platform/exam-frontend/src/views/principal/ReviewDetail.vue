<!--
  ReviewDetail.vue — 管理端：批阅详情页
  校长逐题查看老师答案，客观题自动判分可修正，主观题手动打分+评语
  设计风格：现代简约 + 清新蓝白配
-->
<template>
  <div class="review-detail-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <div class="back-button" @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回批阅列表</span>
        </div>
        <div class="student-info">
          <div class="student-avatar">
            <el-icon><User /></el-icon>
          </div>
          <div class="student-details">
            <h3>{{ studentName }}</h3>
            <span class="school">{{ school }}</span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <div class="score-summary">
          <span class="score-label">当前总分</span>
          <span class="score-value">{{ currentTotal }}</span>
        </div>
      </div>
    </div>

    <!-- 逐题批阅卡片 -->
    <div class="question-list" v-if="answers.length > 0">
      <div v-for="(item, index) in answers" :key="item.questionId" class="review-card">
        <div class="card-header">
          <div class="header-left">
            <span class="question-index">第 {{ index + 1 }} 题</span>
            <el-tag :type="questionTypeTag(item.type)" size="small" effect="light">
              {{ questionTypeLabel(item.type) }}
            </el-tag>
          </div>
          <div class="header-right">
            <span class="max-score">满分 {{ item.maxScore }} 分</span>
          </div>
        </div>

        <div class="card-body">
          <!-- 题干 -->
          <div class="question-stem">{{ item.stem }}</div>

          <!-- 客观题：显示老师选择、正确答案、得分 -->
          <div class="answer-section" v-if="item.type !== 'essay'">
            <div class="answer-row">
              <span class="answer-label">老师答案：</span>
              <span :class="{ wrong: !item.autoCorrect }">{{ item.userAnswer || '未作答' }}</span>
            </div>
            <div class="answer-row">
              <span class="answer-label">正确答案：</span>
              <span class="correct-answer">{{ item.correctAnswer }}</span>
            </div>
            <div class="answer-row" v-if="!item.autoCorrect">
              <span class="answer-label">自动判定：</span>
              <el-tag type="danger" size="small" effect="light">错误</el-tag>
            </div>
          </div>

          <!-- 主观题：显示老师作答 + 打分区 -->
          <div class="answer-section" v-if="item.type === 'essay'">
            <div class="answer-row">
              <span class="answer-label">老师作答：</span>
            </div>
            <div class="essay-content">{{ item.userAnswer || '未作答' }}</div>
          </div>

          <!-- 批阅打分区 -->
          <div class="review-area">
            <div class="review-header">
              <el-icon><Edit /></el-icon>
              <span>批阅打分</span>
            </div>
            <div class="review-content">
              <el-input-number
                v-model="item.gotScore"
                :min="0"
                :max="item.maxScore"
                size="large"
                style="width: 140px"
              />
              <span class="score-hint">/ {{ item.maxScore }} 分</span>
            </div>
          </div>

          <!-- 评语 -->
          <div class="comment-area">
            <div class="comment-header">
              <el-icon><ChatDotSquare /></el-icon>
              <span>批阅评语</span>
              <span class="comment-hint">（选填）</span>
            </div>
            <el-input
              v-model="item.reviewComment"
              type="textarea"
              :rows="2"
              placeholder="可填写评语..."
              class="comment-input"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="empty-state" v-if="answers.length === 0">
      <el-empty description="暂无答题数据" />
    </div>

    <!-- 底部操作栏 -->
    <div class="bottom-bar" v-if="answers.length > 0">
      <el-button @click="$router.back()" size="large">
        <el-icon><Back /></el-icon>
        暂存
      </el-button>
      <el-button type="primary" @click="submitReview" :loading="submitting" size="large" class="submit-btn">
        <el-icon><Check /></el-icon>
        确认批阅完成
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()

// 答案数据类型
interface AnswerItem {
  questionId: number
  type: 'single' | 'multiple' | 'judge' | 'essay'
  stem: string
  userAnswer: string
  correctAnswer: string
  maxScore: number
  gotScore: number
  autoCorrect: boolean
  reviewComment: string
}

const studentName = ref('') // 从 API 获取
const school = ref('') // 从 API 获取
const submitting = ref(false)

// 当前批阅的答案列表
const answers = ref<AnswerItem[]>([]) // 从 API 获取

// 当前总分
const currentTotal = computed(() => answers.value.reduce((sum, a) => sum + a.gotScore, 0))

// 题目类型标签
function questionTypeTag(type: string) {
  const map: Record<string, string> = { single: 'primary', multiple: 'success', judge: 'warning', essay: 'info' }
  return map[type] || 'info'
}

function questionTypeLabel(type: string) {
  const map: Record<string, string> = { single: '单选题', multiple: '多选题', judge: '判断题', essay: '主观题' }
  return map[type] || type
}

/** 提交批阅结果 */
async function submitReview() {
  await ElMessageBox.confirm(
    `当前总分为 ${currentTotal.value} 分，确认完成批阅？`,
    '确认批阅',
    { type: 'info' }
  )
  submitting.value = true
  // TODO: 调用 API POST /api/principal/review/submit  body: { paperId, userId, answers }
  setTimeout(() => {
    ElMessage.success('批阅完成')
    submitting.value = false
    window.history.back()
  }, 500)
}
</script>

<style scoped lang="scss">
.review-detail-page {
  max-width: 900px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 24px;
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: var(--bg-color);
  border-radius: var(--radius-md);
  color: var(--text-regular);
  font-size: 14px;
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 1px solid var(--border-color-lighter);
  
  &:hover {
    color: var(--color-primary);
    border-color: var(--color-primary-light);
    background: var(--color-primary-light);
  }
}

.student-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.student-avatar {
  width: 48px;
  height: 48px;
  background: var(--color-primary-light);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 24px;
}

.student-details {
  h3 {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 4px;
  }
  
  .school {
    font-size: 14px;
    color: var(--text-secondary);
  }
}

.header-right {
  .score-summary {
    text-align: center;
    padding: 12px 24px;
    background: var(--color-primary-light);
    border-radius: var(--radius-md);
  }
  
  .score-label {
    display: block;
    font-size: 12px;
    color: var(--text-secondary);
    margin-bottom: 4px;
  }
  
  .score-value {
    font-size: 28px;
    font-weight: 700;
    color: var(--color-primary);
  }
}

/* 题目列表 */
.question-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-card {
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  transition: all var(--transition-normal);
  
  &:hover {
    box-shadow: var(--shadow-md);
  }
}

.card-header {
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

.question-index {
  font-weight: 600;
  color: var(--text-primary);
}

.header-right {
  .max-score {
    font-size: 14px;
    color: var(--text-secondary);
  }
}

.card-body {
  padding: 24px;
}

.question-stem {
  font-size: 16px;
  line-height: 1.8;
  color: var(--text-primary);
  margin-bottom: 20px;
  padding: 16px;
  background: var(--bg-color);
  border-radius: var(--radius-md);
}

/* 答案区域 */
.answer-section {
  margin-bottom: 24px;
}

.answer-row {
  display: flex;
  align-items: baseline;
  margin-bottom: 12px;
  font-size: 14px;
  
  .answer-label {
    color: var(--text-secondary);
    margin-right: 12px;
    white-space: nowrap;
  }
  
  .wrong {
    color: var(--color-danger);
    font-weight: 600;
  }
  
  .correct-answer {
    color: var(--color-success);
    font-weight: 600;
  }
}

.essay-content {
  padding: 16px;
  background: var(--bg-color);
  border-radius: var(--radius-md);
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-primary);
  margin-top: 8px;
}

/* 批阅打分区域 */
.review-area {
  margin-bottom: 20px;
  padding: 20px;
  background: var(--color-primary-light);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-primary-light);
}

.review-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  
  .el-icon {
    color: var(--color-primary);
  }
}

.review-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.score-hint {
  font-size: 16px;
  color: var(--text-secondary);
}

/* 评语区域 */
.comment-area {
  padding: 20px;
  background: var(--bg-color);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color-lighter);
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  
  .el-icon {
    color: var(--color-primary);
  }
  
  .comment-hint {
    font-size: 13px;
    color: var(--text-secondary);
    font-weight: 400;
  }
}

.comment-input {
  :deep(.el-textarea__inner) {
    border-radius: var(--radius-md);
    padding: 12px 16px;
  }
}

/* 空状态 */
.empty-state {
  padding: 80px 0;
  text-align: center;
}

/* 底部操作栏 */
.bottom-bar {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
  padding: 24px;
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.submit-btn {
  min-width: 160px;
  
  .el-icon {
    margin-right: 6px;
  }
}

/* 响应式调整 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 20px;
  }
  
  .header-left {
    flex-direction: column;
    gap: 16px;
  }
  
  .header-right {
    width: 100%;
  }
  
  .score-summary {
    width: 100%;
  }
}
</style>
