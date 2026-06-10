<!--
  QuestionEdit.vue — 校长编辑题目页面
  添加/编辑试卷中的题目，支持单选、多选、判断、主观题
  设计风格：现代简约 + 清新蓝白配
-->
<template>
  <div class="question-edit-page">
    <!-- 返回按钮 -->
    <div class="back-button" @click="$router.back()">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回试卷列表</span>
    </div>

    <!-- 试卷信息头部 -->
    <div class="paper-header">
      <div class="paper-info">
        <h2>{{ paper.title }}</h2>
        <div class="paper-meta">
          <span><el-icon><Clock /></el-icon> {{ paper.durationMinutes }}分钟</span>
          <span><el-icon><Trophy /></el-icon> 总分 {{ paper.totalScore }}</span>
          <span><el-icon><Aim /></el-icon> 及格 {{ paper.passScore }}</span>
          <span><el-icon><Document /></el-icon> {{ paper.questionCount }} 题</span>
        </div>
      </div>
      <el-tag :type="paper.status === 0 ? 'info' : 'success'" size="large">
        {{ paper.status === 0 ? '草稿' : '已发布' }}
      </el-tag>
    </div>

    <!-- 题目列表 -->
    <div class="questions-section">
      <div class="section-header">
        <h3>题目列表</h3>
        <el-button type="primary" @click="showAddQuestion = true" v-if="paper.status === 0">
          <el-icon><Plus /></el-icon>
          添加题目
        </el-button>
      </div>

      <!-- 题目卡片 -->
      <div v-if="questions.length > 0" class="question-list">
        <div v-for="(q, index) in questions" :key="q.id" class="question-card">
          <div class="question-header">
            <div class="question-index">
              <span class="index">{{ index + 1 }}</span>
              <el-tag size="small" :type="questionTypeTag(q.questionType)">
                {{ questionTypeLabel(q.questionType) }}
              </el-tag>
              <span class="score">{{ q.score }}分</span>
            </div>
            <el-button type="danger" link size="small" @click="removeQuestion(q.id)" v-if="paper.status === 0">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          
          <div class="question-title">{{ q.title }}</div>
          
          <!-- 选项列表 -->
          <div v-if="q.options && q.options.length > 0" class="options-list">
            <div v-for="opt in q.options" :key="opt.id" class="option-item" :class="{ correct: opt.isCorrect === 1 }">
              <span class="option-label">{{ opt.optionLabel }}</span>
              <span class="option-content">{{ opt.optionContent }}</span>
              <el-icon v-if="opt.isCorrect === 1" class="correct-icon"><CircleCheck /></el-icon>
            </div>
          </div>
          
          <div v-if="q.analysis" class="question-analysis">
            <el-icon><InfoFilled /></el-icon>
            <span>解析：{{ q.analysis }}</span>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <el-empty description="暂无题目">
          <el-button type="primary" @click="showAddQuestion = true" v-if="paper.status === 0">
            添加第一道题
          </el-button>
        </el-empty>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="bottom-bar" v-if="paper.status === 0">
      <el-button @click="$router.back()">返回</el-button>
      <el-button type="success" @click="handlePublish" :loading="publishing" :disabled="questions.length === 0">
        <el-icon><Upload /></el-icon>
        发布试卷 ({{ questions.length }} 题)
      </el-button>
    </div>

    <!-- 添加题目对话框 -->
    <el-dialog v-model="showAddQuestion" title="添加题目" width="700px" :close-on-click-modal="false">
      <el-form :model="questionForm" :rules="questionRules" ref="questionFormRef" label-width="80px">
        <el-form-item label="题目类型" prop="questionType">
          <el-radio-group v-model="questionForm.questionType">
            <el-radio :value="1">单选题</el-radio>
            <el-radio :value="2">多选题</el-radio>
            <el-radio :value="3">判断题</el-radio>
            <el-radio :value="4">主观题</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="题干" prop="title">
          <el-input v-model="questionForm.title" type="textarea" :rows="3" placeholder="请输入题目内容" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分值" prop="score">
              <el-input-number v-model="questionForm.score" :min="1" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否必答">
              <el-switch v-model="questionForm.isRequired" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 选项（单选/多选/判断题） -->
        <el-form-item v-if="questionForm.questionType !== 4" label="选项">
          <div class="options-editor">
            <div v-for="(opt, index) in questionForm.options" :key="index" class="option-row">
              <el-input v-model="opt.optionLabel" style="width: 60px" placeholder="A" />
              <el-input v-model="opt.optionContent" style="flex: 1" placeholder="选项内容" />
              <el-checkbox v-model="opt.isCorrect" :true-value="1" :false-value="0">正确答案</el-checkbox>
              <el-button type="danger" link @click="removeOption(index)" :disabled="questionForm.options.length <= 2">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button type="primary" link @click="addOption" v-if="questionForm.questionType !== 3">
              <el-icon><Plus /></el-icon>
              添加选项
            </el-button>
          </div>
        </el-form-item>

        <!-- 判断题默认选项 -->
        <el-form-item v-if="questionForm.questionType === 3" label="正确答案">
          <el-radio-group v-model="questionForm.options[0].isCorrect">
            <el-radio :value="1">对</el-radio>
            <el-radio :value="0">错</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="解析">
          <el-input v-model="questionForm.analysis" type="textarea" :rows="2" placeholder="题目解析（选填）" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showAddQuestion = false">取消</el-button>
        <el-button type="primary" @click="handleAddQuestion" :loading="adding">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getPaperDetail, addQuestion, deleteQuestion as deleteQuestionApi, publishPaper, type PaperVO, type QuestionVO } from '@/api/paper'

const route = useRoute()
const router = useRouter()
const paperId = route.params.paperId as string

// 试卷信息
const paper = ref<PaperVO>({
  id: paperId,
  title: '',
  description: '',
  paperType: 1,
  totalScore: 100,
  passScore: 60,
  questionCount: 0,
  durationMinutes: 60,
  status: 0,
  publisherName: '',
  projectId: '',
  projectName: '',
  createdAt: '',
  questions: []
})

// 题目列表
const questions = ref<QuestionVO[]>([])

// 添加题目对话框
const showAddQuestion = ref(false)
const adding = ref(false)
const publishing = ref(false)
const questionFormRef = ref<FormInstance>()

// 题目表单
const questionForm = reactive({
  questionType: 1,
  title: '',
  score: 10,
  isRequired: 1,
  analysis: '',
  options: [
    { optionLabel: 'A', optionContent: '', isCorrect: 0, sortOrder: 1 },
    { optionLabel: 'B', optionContent: '', isCorrect: 0, sortOrder: 2 },
    { optionLabel: 'C', optionContent: '', isCorrect: 0, sortOrder: 3 },
    { optionLabel: 'D', optionContent: '', isCorrect: 0, sortOrder: 4 }
  ]
})

// 题目验证规则
const questionRules: FormRules = {
  title: [{ required: true, message: '请输入题干', trigger: 'blur' }],
  score: [{ required: true, message: '请输入分值', trigger: 'blur' }]
}

// 加载试卷详情
onMounted(async () => {
  try {
    const detail = await getPaperDetail(paperId)
    paper.value = detail
    questions.value = detail.questions || []
  } catch (e) {
    console.error('获取试卷详情失败', e)
    ElMessage.error('获取试卷详情失败')
  }
})

// 题目类型标签
function questionTypeLabel(type: number): string {
  const map: Record<number, string> = { 1: '单选', 2: '多选', 3: '判断', 4: '主观' }
  return map[type] || '未知'
}

function questionTypeTag(type: number): 'primary' | 'success' | 'warning' | 'info' {
  const map: Record<number, 'primary' | 'success' | 'warning' | 'info'> = { 1: 'primary', 2: 'success', 3: 'warning', 4: 'info' }
  return map[type] || 'info'
}

// 添加选项
function addOption() {
  const labels = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']
  const nextIndex = questionForm.options.length
  questionForm.options.push({
    optionLabel: labels[nextIndex] || String(nextIndex + 1),
    optionContent: '',
    isCorrect: 0,
    sortOrder: nextIndex + 1
  })
}

// 删除选项
function removeOption(index: number) {
  questionForm.options.splice(index, 1)
}

// 监听题型切换，判断题自动重置为 2 个选项（对/错）
watch(() => questionForm.questionType, (newType) => {
  if (newType === 3) {
    questionForm.options = [
      { optionLabel: '对', optionContent: '对', isCorrect: 0, sortOrder: 1 },
      { optionLabel: '错', optionContent: '错', isCorrect: 0, sortOrder: 2 }
    ]
  } else if (questionForm.options.length === 2 && questionForm.options[0].optionLabel === '对') {
    // 从判断题切换回选择题时，恢复默认 4 个选项
    questionForm.options = [
      { optionLabel: 'A', optionContent: '', isCorrect: 0, sortOrder: 1 },
      { optionLabel: 'B', optionContent: '', isCorrect: 0, sortOrder: 2 },
      { optionLabel: 'C', optionContent: '', isCorrect: 0, sortOrder: 3 },
      { optionLabel: 'D', optionContent: '', isCorrect: 0, sortOrder: 4 }
    ]
  }
})

// 添加题目
async function handleAddQuestion() {
  if (!questionFormRef.value) return
  
  await questionFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    // 验证选项
    if (questionForm.questionType !== 4) {
      const hasCorrect = questionForm.options.some(o => o.isCorrect === 1)
      if (!hasCorrect) {
        ElMessage.warning('请至少选择一个正确答案')
        return
      }
      const hasEmpty = questionForm.options.some(o => !o.optionContent.trim())
      if (hasEmpty) {
        ElMessage.warning('请填写所有选项内容')
        return
      }
    }
    
    adding.value = true
    try {
      await addQuestion(paperId, {
        title: questionForm.title,
        questionType: questionForm.questionType,
        score: questionForm.score,
        isRequired: questionForm.isRequired,
        analysis: questionForm.analysis,
        options: questionForm.questionType !== 4 ? questionForm.options : undefined
      })
      ElMessage.success('题目添加成功')
      showAddQuestion.value = false
      // 重新加载试卷详情
      const detail = await getPaperDetail(paperId)
      paper.value = detail
      questions.value = detail.questions || []
      // 重置表单
      resetQuestionForm()
    } catch (e: any) {
      ElMessage.error(e.message || '添加失败')
    } finally {
      adding.value = false
    }
  })
}

// 重置题目表单
function resetQuestionForm() {
  questionForm.questionType = 1
  questionForm.title = ''
  questionForm.score = 10
  questionForm.isRequired = 1
  questionForm.analysis = ''
  questionForm.options = [
    { optionLabel: 'A', optionContent: '', isCorrect: 0, sortOrder: 1 },
    { optionLabel: 'B', optionContent: '', isCorrect: 0, sortOrder: 2 },
    { optionLabel: 'C', optionContent: '', isCorrect: 0, sortOrder: 3 },
    { optionLabel: 'D', optionContent: '', isCorrect: 0, sortOrder: 4 }
  ]
}

// 删除题目
async function removeQuestion(questionId: string) {
  try {
    await ElMessageBox.confirm('确认删除该题目？', '确认删除')
    await deleteQuestionApi(paperId, questionId)
    ElMessage.success('题目已删除')
    // 重新加载
    const detail = await getPaperDetail(paperId)
    paper.value = detail
    questions.value = detail.questions || []
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}

// 发布试卷
async function handlePublish() {
  try {
    await ElMessageBox.confirm(
      `确认发布试卷？发布后将有 ${questions.value.length} 道题，老师可以开始答题。`,
      '确认发布',
      { type: 'info' }
    )
    publishing.value = true
    await publishPaper(paperId)
    ElMessage.success('试卷发布成功')
    paper.value.status = 1
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '发布失败')
    }
  } finally {
    publishing.value = false
  }
}
</script>

<style scoped lang="scss">
.question-edit-page {
  max-width: 900px;
  margin: 0 auto;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  margin-bottom: 24px;
  background: var(--bg-color-card);
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

.paper-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  margin-bottom: 24px;
}

.paper-info {
  h2 {
    font-size: 24px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 12px;
  }
}

.paper-meta {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: var(--text-secondary);
  
  span {
    display: flex;
    align-items: center;
    gap: 6px;
  }
  
  .el-icon {
    font-size: 16px;
  }
}

.questions-section {
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  
  h3 {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
  }
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-card {
  padding: 20px;
  border: 1px solid var(--border-color-lighter);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  
  &:hover {
    border-color: var(--color-primary-light);
    box-shadow: var(--shadow-sm);
  }
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.question-index {
  display: flex;
  align-items: center;
  gap: 12px;
  
  .index {
    width: 28px;
    height: 28px;
    background: var(--color-primary);
    color: white;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 600;
  }
  
  .score {
    font-size: 14px;
    color: var(--text-secondary);
  }
}

.question-title {
  font-size: 16px;
  color: var(--text-primary);
  margin-bottom: 16px;
  line-height: 1.6;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: var(--bg-color);
  border-radius: var(--radius-sm);
  border: 1px solid transparent;
  
  &.correct {
    background: var(--color-success-light);
    border-color: var(--color-success);
  }
}

.option-label {
  width: 24px;
  height: 24px;
  background: var(--color-primary-light);
  color: var(--color-primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.option-content {
  flex: 1;
  font-size: 14px;
  color: var(--text-primary);
}

.correct-icon {
  color: var(--color-success);
  font-size: 18px;
}

.question-analysis {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px;
  background: var(--color-info-light);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--text-secondary);
  
  .el-icon {
    margin-top: 2px;
    flex-shrink: 0;
  }
}

.options-editor {
  width: 100%;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.empty-state {
  padding: 60px 0;
}

.bottom-bar {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
  padding: 24px;
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}
</style>
