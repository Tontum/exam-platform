<!--
  ProjectList.vue — 学员端/管理端：项目列表页
  老师/校长登录后首先看到的页面，展示所有可参加的项目
  校长可以创建校级项目
  设计风格：现代简约 + 清新蓝白配
-->
<template>
  <div class="project-list-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h2>我的项目</h2>
        <p class="subtitle">选择项目进入培训考核</p>
      </div>
      <div class="header-actions">
        <el-button v-if="userStore.role === 'principal'" type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          创建校级项目
        </el-button>
        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-value">{{ projects.length }}</span>
            <span class="stat-label">参与项目</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 项目卡片列表 -->
    <div class="project-grid" v-if="projects.length > 0">
      <div
        v-for="project in projects"
        :key="project.id"
        class="project-card"
        @click="enterProject(project.id)"
      >
        <div class="card-header">
          <div class="card-icon">
            <el-icon><Folder /></el-icon>
          </div>
          <el-tag 
            size="small" 
            :type="project.status === 1 ? 'success' : 'info'"
            effect="light"
          >
            {{ project.status === 1 ? '进行中' : project.status === 2 ? '已结束' : '未开始' }}
          </el-tag>
        </div>
        
        <div class="card-body">
          <h3 class="card-title">{{ project.name }}</h3>
          <p class="card-desc">{{ project.description }}</p>
        </div>
        
        <div class="card-footer">
          <div class="card-meta">
            <div class="meta-item">
              <el-icon><Location /></el-icon>
              <span>{{ project.province }} {{ project.city }}</span>
            </div>
          </div>
        </div>
        
        <div class="card-action">
          <span>进入项目</span>
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="empty-state" v-else>
      <el-empty description="暂无可用项目">
        <template #image>
          <div class="empty-icon">
            <el-icon><Folder /></el-icon>
          </div>
        </template>
      </el-empty>
    </div>
  </div>

  <!-- 创建项目对话框 -->
  <el-dialog v-model="dialogVisible" title="创建校级项目" width="600px">
    <el-form :model="form" label-width="80px">
      <el-form-item label="项目名称" required>
        <el-input v-model="form.name" placeholder="请输入项目名称" />
      </el-form-item>
      <el-form-item label="项目描述">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入项目描述" />
      </el-form-item>
      <el-form-item label="选择老师">
        <div class="teacher-select">
          <el-checkbox v-model="selectAllTeachers" @change="toggleSelectAll">全选本校老师</el-checkbox>
          <el-checkbox-group v-model="form.teacherIds" class="teacher-checkbox-group">
            <el-checkbox v-for="teacher in teachers" :key="teacher.id" :label="teacher.id">
              {{ teacher.realName }} ({{ teacher.school }})
            </el-checkbox>
          </el-checkbox-group>
          <p class="hint" v-if="form.teacherIds.length > 0">已选择 {{ form.teacherIds.length }} 位老师</p>
          <p class="hint" v-else>不选择则默认关联本校所有老师</p>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleCreate" :loading="submitting">创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyProjects, createProject, type ProjectVO } from '@/api/project'
import { useUserStore } from '@/stores/user'
import { get } from '@/api/index'

const router = useRouter()
const userStore = useUserStore()

// 项目数据
const projects = ref<ProjectVO[]>([])

// 创建项目对话框
const dialogVisible = ref(false)
const submitting = ref(false)
const form = reactive({
  name: '',
  description: '',
  teacherIds: [] as number[]
})

// 老师列表
const teachers = ref<any[]>([])
const selectAllTeachers = ref(false)

onMounted(async () => {
  // 调用 API 获取当前用户参与的项目
  try {
    projects.value = await getMyProjects()
  } catch (e) {
    console.error('获取项目列表失败', e)
  }
})

/** 进入项目 → 根据角色跳转到对应工具页 */
function enterProject(projectId: string) {
  const prefix = userStore.role === 'admin' ? '/admin'
    : userStore.role === 'principal' ? '/principal'
    : '/teacher'
  router.push(`${prefix}/project/${projectId}/tools`)
}

/** 打开创建项目对话框 */
async function openCreateDialog() {
  form.name = ''
  form.description = ''
  form.teacherIds = []
  selectAllTeachers.value = false
  
  // 获取本校老师列表
  try {
    teachers.value = await get('/user/teachers')
  } catch (e) {
    console.error('获取老师列表失败', e)
    teachers.value = []
  }
  
  dialogVisible.value = true
}

/** 全选/取消全选老师 */
function toggleSelectAll(val: boolean) {
  if (val) {
    form.teacherIds = teachers.value.map(t => t.id)
  } else {
    form.teacherIds = []
  }
}

/** 创建校级项目 */
async function handleCreate() {
  if (!form.name.trim()) {
    ElMessage.warning('项目名称不能为空')
    return
  }
  
  submitting.value = true
  try {
    await createProject({
      name: form.name,
      description: form.description,
      type: 2 // 校级项目
    })
    ElMessage.success('项目创建成功')
    dialogVisible.value = false
    // 刷新项目列表
    projects.value = await getMyProjects()
  } catch (e: any) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.project-list-page {
  max-width: 1200px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
  padding: 24px;
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.header-content {
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.teacher-select {
  width: 100%;
}

.teacher-checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
  padding: 12px;
  background: var(--bg-color);
  border-radius: var(--radius-md);
  margin-top: 8px;
}

.hint {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 8px;
}

.header-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  text-align: center;
  
  .stat-value {
    display: block;
    font-size: 28px;
    font-weight: 600;
    color: var(--color-primary);
    line-height: 1.2;
  }
  
  .stat-label {
    font-size: 12px;
    color: var(--text-secondary);
    margin-top: 4px;
  }
}

/* 项目卡片网格 */
.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.project-card {
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  padding: 24px;
  cursor: pointer;
  transition: all var(--transition-normal);
  border: 1px solid var(--border-color-lighter);
  position: relative;
  overflow: hidden;
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-lg);
    border-color: var(--color-primary-light);
    
    .card-action {
      background: var(--color-primary);
      color: white;
    }
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.card-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, var(--color-primary-light) 0%, #D9ECFF 100%);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 24px;
}

.card-body {
  margin-bottom: 20px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
  line-height: 1.4;
}

.card-desc {
  font-size: 14px;
  color: var(--text-regular);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  margin-bottom: 16px;
}

.card-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  
  .el-icon {
    font-size: 14px;
  }
}

.progress-section {
  background: var(--bg-color);
  padding: 12px;
  border-radius: var(--radius-md);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.progress-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.progress-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
}

.card-action {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  margin: 0 -24px -24px;
  background: var(--bg-color);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-fast);
  border-top: 1px solid var(--border-color-lighter);
  
  .el-icon {
    transition: transform var(--transition-fast);
  }
  
  &:hover .el-icon {
    transform: translateX(4px);
  }
}

/* 空状态 */
.empty-state {
  padding: 80px 0;
  text-align: center;
  
  .empty-icon {
    width: 120px;
    height: 120px;
    margin: 0 auto 24px;
    background: var(--color-primary-light);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--color-primary);
    font-size: 48px;
  }
}

/* 响应式调整 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 20px;
  }
  
  .project-grid {
    grid-template-columns: 1fr;
  }
}
</style>
