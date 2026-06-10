<!--
  PaperCreate.vue — 校长创建试卷页面
  填写试卷基本信息：名称、时长、总分、及格分
  设计风格：现代简约 + 清新蓝白配
-->
<template>
  <div class="paper-create-page">
    <!-- 返回按钮 -->
    <div class="back-button" @click="$router.back()">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回试卷列表</span>
    </div>

    <!-- 页面头部 -->
    <div class="page-header">
      <h2>创建试卷</h2>
      <p class="subtitle">填写试卷基本信息，创建后可以添加题目</p>
    </div>

    <!-- 表单 -->
    <div class="form-card">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" class="paper-form">
        <el-form-item label="试卷名称" prop="title">
          <el-input v-model="form.title" placeholder="请输入试卷名称" maxlength="200" show-word-limit />
        </el-form-item>

        <el-form-item label="试卷描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入试卷描述（选填）" maxlength="500" show-word-limit />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="总分" prop="totalScore">
              <el-input-number v-model="form.totalScore" :min="1" :max="1000" :step="10" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="及格分" prop="passScore">
              <el-input-number v-model="form.passScore" :min="1" :max="form.totalScore" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="答题时间" prop="durationMinutes">
              <el-input-number v-model="form.durationMinutes" :min="5" :max="300" :step="5" style="width: 100%" />
              <span class="unit">分钟</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="试卷类型">
          <el-radio-group v-model="form.paperType">
            <el-radio :value="1">普通考核</el-radio>
            <el-radio :value="2">阶段考核</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </div>

    <!-- 底部操作栏 -->
    <div class="bottom-bar">
      <el-button @click="$router.back()">取消</el-button>
      <el-button type="primary" @click="handleCreate" :loading="submitting">
        <el-icon><Check /></el-icon>
        创建试卷
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createPaper } from '@/api/paper'

const route = useRoute()
const router = useRouter()
const projectId = route.query.projectId as string || route.params.projectId as string

const formRef = ref<FormInstance>()
const submitting = ref(false)

// 表单数据
const form = reactive({
  title: '',
  description: '',
  totalScore: 100,
  passScore: 60,
  durationMinutes: 60,
  paperType: 1
})

// 表单验证规则
const rules: FormRules = {
  title: [
    { required: true, message: '请输入试卷名称', trigger: 'blur' }
  ],
  totalScore: [
    { required: true, message: '请输入总分', trigger: 'blur' }
  ],
  passScore: [
    { required: true, message: '请输入及格分', trigger: 'blur' }
  ],
  durationMinutes: [
    { required: true, message: '请输入答题时间', trigger: 'blur' }
  ]
}

// 创建试卷
async function handleCreate() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    if (form.passScore > form.totalScore) {
      ElMessage.warning('及格分不能大于总分')
      return
    }
    
    submitting.value = true
    try {
      const paper = await createPaper({
        title: form.title,
        description: form.description,
        paperType: form.paperType,
        totalScore: form.totalScore,
        passScore: form.passScore,
        durationMinutes: form.durationMinutes,
        projectId: projectId
      })
      ElMessage.success('试卷创建成功')
      // 跳转到题目编辑页面
      router.push(`/principal/paper/${paper.id}/questions`)
    } catch (e: any) {
      ElMessage.error(e.message || '创建失败')
    } finally {
      submitting.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.paper-create-page {
  max-width: 800px;
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

.page-header {
  margin-bottom: 24px;
  
  h2 {
    font-size: 24px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 8px;
  }
  
  .subtitle {
    color: var(--text-secondary);
    font-size: 14px;
  }
}

.form-card {
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 32px;
}

.paper-form {
  :deep(.el-form-item) {
    margin-bottom: 24px;
  }
  
  :deep(.el-form-item__label) {
    font-weight: 500;
    color: var(--text-primary);
  }
  
  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner) {
    border-radius: var(--radius-md);
  }
}

.unit {
  font-size: 12px;
  color: var(--text-secondary);
  margin-left: 8px;
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
