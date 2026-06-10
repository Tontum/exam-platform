<!--
  AdminProjectTools.vue — 管理后台：管理员项目工具页
  管理员点击某个项目后进入此页面，展示该项目的管理工具
  管理员可以配置工具、管理成员等
  设计风格：现代简约 + 清新蓝白配
-->
<template>
  <div class="project-tools-page">
    <!-- 返回按钮 -->
    <div class="back-button" @click="$router.push('/admin/projects')">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回项目列表</span>
    </div>

    <!-- 项目信息头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="project-icon">
          <el-icon><Folder /></el-icon>
        </div>
        <div class="project-info">
          <h2>{{ project.name }}</h2>
          <p class="subtitle">{{ project.description }}</p>
        </div>
      </div>
      <div class="header-meta">
        <div class="meta-item">
          <el-icon><Location /></el-icon>
          <span>{{ project.province }} {{ project.city }}</span>
        </div>
        <div class="meta-item">
          <el-icon><Calendar /></el-icon>
          <span>{{ project.createdAt }}</span>
        </div>
      </div>
    </div>

    <!-- 管理工具列表 -->
    <div class="tools-section">
      <h3 class="section-title">项目管理</h3>
      
      <div class="tools-grid">
        <!-- 成员管理 -->
        <div class="tool-card" @click="openMemberDialog">
          <div class="tool-icon member">
            <el-icon><User /></el-icon>
          </div>
          <div class="tool-info">
            <h4 class="tool-name">成员管理</h4>
            <p class="tool-desc">添加或移除项目成员</p>
          </div>
          <div class="tool-action">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
        
        <!-- 工具配置 -->
        <div class="tool-card" @click="goToToolConfig">
          <div class="tool-icon config">
            <el-icon><Setting /></el-icon>
          </div>
          <div class="tool-info">
            <h4 class="tool-name">工具配置</h4>
            <p class="tool-desc">配置项目启用的工具和权限</p>
          </div>
          <div class="tool-action">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
        
        <!-- 编辑项目 -->
        <div class="tool-card" @click="openEditDialog">
          <div class="tool-icon edit">
            <el-icon><Edit /></el-icon>
          </div>
          <div class="tool-info">
            <h4 class="tool-name">编辑项目</h4>
            <p class="tool-desc">修改项目名称、描述等信息</p>
          </div>
          <div class="tool-action">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
        
        <!-- 删除项目 -->
        <div class="tool-card delete" @click="handleDelete">
          <div class="tool-icon delete">
            <el-icon><Delete /></el-icon>
          </div>
          <div class="tool-info">
            <h4 class="tool-name">删除项目</h4>
            <p class="tool-desc">删除此项目及所有相关数据</p>
          </div>
          <div class="tool-action">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 成员管理对话框 -->
    <el-dialog v-model="memberDialogVisible" title="成员管理" width="600px">
      <div class="member-section">
        <h4>选择要加入项目的用户</h4>
        <el-checkbox-group v-model="selectedUserIds">
          <div v-for="user in allUsers" :key="user.id" class="user-item">
            <el-checkbox :label="user.id">
              {{ user.realName }} ({{ user.role === 2 ? '校长' : '老师' }}) - {{ user.school }}
            </el-checkbox>
          </div>
        </el-checkbox-group>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="memberDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveMembers" :loading="savingMembers">
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑项目对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑项目" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="项目名称" required>
          <el-input v-model="editForm.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="请输入项目描述" />
        </el-form-item>
        <el-form-item label="省份">
          <el-input v-model="editForm.province" placeholder="如：河南省" />
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="editForm.city" placeholder="如：郑州市" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleUpdateProject" :loading="updating">
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProject, updateProject, deleteProject, addUsersToProject, type ProjectVO } from '@/api/project'
import { get } from '@/api/index'

const route = useRoute()
const router = useRouter()
const projectId = route.params.projectId as string

// 项目信息
const project = ref<ProjectVO>({
  id: projectId,
  name: '',
  description: '',
  creatorId: '0',
  province: '',
  city: '',
  status: 0,
  createdAt: ''
})

// 成员管理状态
const memberDialogVisible = ref(false)
const allUsers = ref<any[]>([])
const selectedUserIds = ref<string[]>([])
const savingMembers = ref(false)

// 编辑项目状态
const editDialogVisible = ref(false)
const updating = ref(false)
const editForm = reactive({
  name: '',
  description: '',
  province: '',
  city: ''
})

onMounted(async () => {
  // 调用 API 获取项目信息
  try {
    project.value = await getProject(projectId)
  } catch (e) {
    console.error('获取项目信息失败', e)
    ElMessage.error('获取项目信息失败')
  }
})

/** 打开成员管理对话框 */
async function openMemberDialog() {
  selectedUserIds.value = []
  
  // 获取所有用户列表
  try {
    const res = await get('/user/list', { page: 1, size: 100 })
    allUsers.value = res.records || []
  } catch (e) {
    // 如果接口不存在，使用 mock 数据
    allUsers.value = [
      { id: '2', realName: '李校长', role: 2, school: '郑州一中' },
      { id: '3', realName: '张老师', role: 3, school: '郑州一中' },
      { id: '4', realName: '王老师', role: 3, school: '郑州二中' },
      { id: '5', realName: '赵老师', role: 3, school: '洛阳一高' },
      { id: '6', realName: '刘老师', role: 3, school: '洛阳一高' },
      { id: '7', realName: '陈老师', role: 3, school: '开封高中' },
      { id: '8', realName: '杨老师', role: 3, school: '开封高中' },
      { id: '9', realName: '黄老师', role: 3, school: '新乡一中' },
      { id: '10', realName: '周老师', role: 3, school: '新乡一中' },
      { id: '11', realName: '吴老师', role: 3, school: '安阳一中' },
      { id: '12', realName: '孙老师', role: 3, school: '安阳一中' },
      { id: '13', realName: '王校长', role: 2, school: '洛阳一高' },
      { id: '14', realName: '张校长', role: 2, school: '开封一中' },
    ]
  }
  
  memberDialogVisible.value = true
}

/** 保存成员 */
async function handleSaveMembers() {
  if (!projectId || selectedUserIds.value.length === 0) {
    ElMessage.warning('请选择要加入项目的用户')
    return
  }
  
  savingMembers.value = true
  try {
    await addUsersToProject(projectId, selectedUserIds.value)
    ElMessage.success(`已将 ${selectedUserIds.value.length} 个用户加入项目`)
    memberDialogVisible.value = false
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    savingMembers.value = false
  }
}

/** 跳转到工具配置页 */
function goToToolConfig() {
  router.push(`/admin/project/${projectId}/config`)
}

/** 打开编辑对话框 */
function openEditDialog() {
  editForm.name = project.value.name
  editForm.description = project.value.description || ''
  editForm.province = project.value.province || ''
  editForm.city = project.value.city || ''
  editDialogVisible.value = true
}

/** 更新项目 */
async function handleUpdateProject() {
  if (!editForm.name.trim()) {
    ElMessage.warning('项目名称不能为空')
    return
  }
  
  updating.value = true
  try {
    await updateProject(projectId, editForm)
    ElMessage.success('项目已更新')
    editDialogVisible.value = false
    // 重新获取项目信息
    project.value = await getProject(projectId)
  } catch (e: any) {
    ElMessage.error(e.message || '更新失败')
  } finally {
    updating.value = false
  }
}

/** 删除项目 */
async function handleDelete() {
  try {
    await ElMessageBox.confirm(
      '确认删除该项目？此操作不可撤销。',
      '确认删除',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
    await deleteProject(projectId)
    ElMessage.success('项目已删除')
    router.push('/admin/projects')
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}
</script>

<style scoped lang="scss">
.project-tools-page {
  max-width: 1000px;
  margin: 0 auto;
}

/* 返回按钮 */
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
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.project-icon {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, var(--color-primary-light) 0%, #D9ECFF 100%);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 28px;
  flex-shrink: 0;
}

.project-info {
  h2 {
    font-size: 24px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 8px;
  }
  
  .subtitle {
    color: var(--text-secondary);
    font-size: 14px;
    line-height: 1.5;
  }
}

.header-meta {
  display: flex;
  gap: 20px;
  flex-shrink: 0;
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

/* 工具区域 */
.tools-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 20px;
  padding-left: 12px;
  border-left: 4px solid var(--color-primary);
}

/* 工具网格 */
.tools-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tool-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-lighter);
  cursor: pointer;
  transition: all var(--transition-normal);
  
  &:hover {
    box-shadow: var(--shadow-md);
    border-color: var(--color-primary-light);
    transform: translateX(4px);
  }
  
  &.delete:hover {
    border-color: var(--color-danger);
    .tool-icon.delete {
      background: var(--color-danger-light);
      color: var(--color-danger);
    }
  }
}

.tool-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
  
  &.member {
    background: var(--color-primary-light);
    color: var(--color-primary);
  }
  
  &.config {
    background: var(--color-success-light);
    color: var(--color-success);
  }
  
  &.edit {
    background: var(--color-warning-light);
    color: var(--color-warning);
  }
  
  &.delete {
    background: var(--color-info-light);
    color: var(--color-info);
  }
}

.tool-info {
  flex: 1;
}

.tool-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.tool-desc {
  font-size: 14px;
  color: var(--text-secondary);
}

.tool-action {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
  
  .tool-card:hover & {
    color: var(--color-primary);
    transform: translateX(4px);
  }
}

/* 成员管理样式 */
.member-section {
  h4 {
    font-size: 16px;
    font-weight: 500;
    color: var(--text-primary);
    margin-bottom: 16px;
  }
}

.user-item {
  padding: 8px 0;
  border-bottom: 1px solid var(--border-color-lighter);
  
  &:last-child {
    border-bottom: none;
  }
  
  .el-checkbox {
    width: 100%;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 20px;
  }
  
  .header-meta {
    width: 100%;
    justify-content: flex-start;
  }
  
  .tool-card {
    flex-wrap: wrap;
  }
}
</style>
