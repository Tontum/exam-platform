<!--
  ProjectManage.vue — 管理后台：项目管理页
  管理员查看所有项目，可创建、编辑项目
  对接 exam-project-service（端口 8087）
  设计风格：现代简约 + 清新蓝白配
-->
<template>
  <div class="project-manage-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h2>项目管理</h2>
        <p class="subtitle">管理和配置培训项目</p>
      </div>
      <el-button type="primary" @click="openCreateDialog" class="create-btn">
        <el-icon><Plus /></el-icon>
        创建项目
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select v-model="filterProvince" placeholder="选择省份" clearable @change="onFilterProvinceChange" style="width: 140px">
        <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
      </el-select>
      <el-select v-model="filterCity" placeholder="选择城市" clearable :disabled="!filterProvince" @change="fetchProjects" style="width: 140px">
        <el-option v-for="c in filterCities" :key="c" :label="c" :value="c" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="项目状态" clearable @change="fetchProjects" style="width: 120px">
        <el-option label="未开始" :value="0" />
        <el-option label="进行中" :value="1" />
        <el-option label="已结束" :value="2" />
      </el-select>
      <el-input
        v-model="filterKeyword"
        placeholder="搜索项目名称"
        clearable
        @clear="fetchProjects"
        @keyup.enter="fetchProjects"
        style="width: 200px"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button @click="fetchProjects">搜索</el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon total">
          <el-icon><Folder /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ projects.length }}</span>
          <span class="stat-label">总项目数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon active">
          <el-icon><VideoPlay /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ projects.filter(p => p.status === 1).length }}</span>
          <span class="stat-label">进行中</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon ended">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ projects.filter(p => p.status === 2).length }}</span>
          <span class="stat-label">已结束</span>
        </div>
      </div>
    </div>

    <!-- 项目表格 -->
    <div class="table-card">
      <el-table :data="projects" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="项目名称" min-width="200">
          <template #default="{ row }">
            <div class="project-name-cell">
              <div class="project-icon">
                <el-icon><Folder /></el-icon>
              </div>
              <span class="project-name">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="light">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="province" label="省份" width="100" />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="350" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link size="small" @click="openMemberDialog(row)">
                <el-icon><User /></el-icon>
                成员管理
              </el-button>
              <el-button type="success" link size="small" @click="configTools(row.id)">
                <el-icon><Setting /></el-icon>
                工具配置
              </el-button>
              <el-button type="warning" link size="small" @click="openEditDialog(row)">
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row.id)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next, jumper"
          @current-change="fetchProjects"
        />
      </div>
    </div>

    <!-- 空状态 -->
    <div class="empty-state" v-if="projects.length === 0 && !loading">
      <el-empty description="暂无项目">
        <template #image>
          <div class="empty-icon">
            <el-icon><Folder /></el-icon>
          </div>
        </template>
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          创建第一个项目
        </el-button>
      </el-empty>
    </div>

    <!-- 创建/编辑项目对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑项目' : '创建项目'" width="600px" class="project-dialog">
      <el-form :model="form" label-width="80px" class="dialog-form">
        <el-form-item label="项目名称" required>
          <el-input v-model="form.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入项目描述" />
        </el-form-item>
        <el-form-item label="项目类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">省级项目（分配给多个学校）</el-radio>
            <el-radio :value="2">校级项目（仅限单个学校）</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <!-- 省级项目：选择多个学校 -->
        <el-form-item v-if="form.type === 1" label="选择学校">
          <div class="school-select">
            <el-select v-model="selectedProvince" placeholder="选择省份" clearable style="width: 30%" @change="onProvinceChange">
              <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
            </el-select>
            <el-select v-model="selectedCity" placeholder="选择城市" clearable :disabled="!selectedProvince" style="width: 30%" @change="onCityChange">
              <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
            </el-select>
          </div>
          <el-checkbox-group v-model="form.schoolIds" class="school-checkbox-group">
            <el-checkbox v-for="school in schools" :key="school.id" :label="school.id">
              {{ school.name }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        
        <!-- 校级项目：选择单个学校 -->
        <el-form-item v-if="form.type === 2" label="选择学校">
          <div class="school-select">
            <el-select v-model="selectedProvince" placeholder="选择省份" clearable style="width: 30%" @change="onProvinceChange">
              <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
            </el-select>
            <el-select v-model="selectedCity" placeholder="选择城市" clearable :disabled="!selectedProvince" style="width: 30%" @change="onCityChange">
              <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
            </el-select>
            <el-select v-model="form.schoolId" placeholder="选择学校" clearable filterable :disabled="!selectedCity" style="width: 40%">
              <el-option v-for="school in schools" :key="school.id" :label="school.name" :value="school.id" />
            </el-select>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 成员管理对话框 -->
    <el-dialog v-model="memberDialogVisible" title="成员管理" width="600px" class="project-dialog">
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listProjects, createProject, updateProject, deleteProject, addUsersToProject } from '@/api/project'
import type { ProjectVO } from '@/api/project'
import { get } from '@/api/index'
import { onMounted } from 'vue'

const router = useRouter()

// ==================== 列表状态 ====================
const loading = ref(false)
const projects = ref<ProjectVO[]>([])
const page = ref(1)
const pageSize = ref(15)
const total = ref(0)

// ==================== 筛选状态 ====================
const filterProvince = ref('')
const filterCity = ref('')
const filterCities = ref<string[]>([])
const filterStatus = ref<number | null>(null)
const filterKeyword = ref('')

// ==================== 对话框状态 ====================
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const editingId = ref<string | null>(null)
const form = reactive({ 
  name: '', 
  description: '', 
  province: '', 
  city: '', 
  type: 1,
  schoolId: null as number | null,
  schoolIds: [] as number[]
})

// ==================== 学校选择 ====================
const provinces = ref<string[]>([])
const cities = ref<string[]>([])
const schools = ref<any[]>([])
const selectedProvince = ref('')
const selectedCity = ref('')

// ==================== 成员管理状态 ====================
const memberDialogVisible = ref(false)
const currentProjectId = ref<number | null>(null)
const allUsers = ref<any[]>([])
const selectedUserIds = ref<number[]>([])
const savingMembers = ref(false)

// ==================== 状态映射 ====================
/** 后端 status: 0=未开始、1=进行中、2=已结束 */
function statusLabel(status: number): string {
  switch (status) {
    case 0: return '未开始'
    case 1: return '进行中'
    case 2: return '已结束'
    default: return '未知'
  }
}

function statusTagType(status: number): 'info' | 'success' | 'warning' {
  switch (status) {
    case 0: return 'info'
    case 1: return 'success'
    case 2: return 'warning'
    default: return 'info'
  }
}

// ==================== 数据加载 ====================
/** 省份筛选变化 */
function onFilterProvinceChange() {
  filterCity.value = ''
  if (filterProvince.value) {
    get<string[]>(`/school/cities?province=${encodeURIComponent(filterProvince.value)}`).then(res => {
      filterCities.value = res
    })
  } else {
    filterCities.value = []
  }
  fetchProjects()
}

async function fetchProjects() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: pageSize.value }
    if (filterProvince.value) params.province = filterProvince.value
    if (filterCity.value) params.city = filterCity.value
    if (filterStatus.value !== null) params.status = filterStatus.value
    if (filterKeyword.value) params.keyword = filterKeyword.value

    const res = await listProjects(params)
    projects.value = res.records
    total.value = Number(res.total)
  } catch (e: any) {
    ElMessage.error(e.message || '加载项目列表失败')
  } finally {
    loading.value = false
  }
}
fetchProjects()

// ==================== 学校选择 ====================
async function fetchProvinces() {
  try {
    provinces.value = await get('/school/provinces')
  } catch (e) {
    console.error('获取省份列表失败', e)
  }
}

async function onProvinceChange() {
  selectedCity.value = ''
  form.schoolId = null
  form.schoolIds = []
  cities.value = []
  schools.value = []
  
  if (selectedProvince.value) {
    try {
      cities.value = await get('/school/cities', { province: selectedProvince.value })
    } catch (e) {
      console.error('获取城市列表失败', e)
    }
  }
}

async function onCityChange() {
  form.schoolId = null
  form.schoolIds = []
  schools.value = []
  
  if (selectedCity.value) {
    try {
      schools.value = await get('/school/list', { province: selectedProvince.value, city: selectedCity.value })
    } catch (e) {
      console.error('获取学校列表失败', e)
    }
  }
}

// ==================== 对话框操作 ====================
function openCreateDialog() {
  isEdit.value = false
  editingId.value = null
  form.name = ''
  form.description = ''
  form.province = ''
  form.city = ''
  form.type = 1
  form.schoolId = null
  form.schoolIds = []
  selectedProvince.value = ''
  selectedCity.value = ''
  schools.value = []
  dialogVisible.value = true
  fetchProvinces()
}

function openEditDialog(row: ProjectVO) {
  isEdit.value = true
  editingId.value = row.id
  form.name = row.name
  form.description = row.description || ''
  form.province = row.province || ''
  form.city = row.city || ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.name.trim()) {
    ElMessage.warning('项目名称不能为空')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value && editingId.value) {
      await updateProject(editingId.value, form)
      ElMessage.success('项目已更新')
    } else {
      await createProject(form)
      ElMessage.success('项目创建成功')
    }
    dialogVisible.value = false
    fetchProjects()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

// ==================== 导航 ====================
function configRoles(projectId: string) {
  router.push(`/admin/project/${projectId}/roles`)
}

function configTools(projectId: string) {
  router.push(`/admin/project/${projectId}/tools`)
}

// ==================== 成员管理 ====================
async function openMemberDialog(row: ProjectVO) {
  currentProjectId.value = row.id
  selectedUserIds.value = []
  
  // 获取所有用户列表
  try {
    const res = await get('/user/list', { page: 1, size: 100 })
    allUsers.value = res.records || []
  } catch (e) {
    // 如果接口不存在，使用 mock 数据
    allUsers.value = [
      { id: 2, realName: '李校长', role: 2, school: '郑州一中' },
      { id: 3, realName: '张老师', role: 3, school: '郑州一中' },
      { id: 4, realName: '王老师', role: 3, school: '郑州二中' },
      { id: 5, realName: '赵老师', role: 3, school: '洛阳一高' },
      { id: 6, realName: '刘老师', role: 3, school: '洛阳一高' },
      { id: 7, realName: '陈老师', role: 3, school: '开封高中' },
      { id: 8, realName: '杨老师', role: 3, school: '开封高中' },
      { id: 9, realName: '黄老师', role: 3, school: '新乡一中' },
      { id: 10, realName: '周老师', role: 3, school: '新乡一中' },
      { id: 11, realName: '吴老师', role: 3, school: '安阳一中' },
      { id: 12, realName: '孙老师', role: 3, school: '安阳一中' },
      { id: 13, realName: '王校长', role: 2, school: '洛阳一高' },
      { id: 14, realName: '张校长', role: 2, school: '开封一中' },
    ]
  }
  
  memberDialogVisible.value = true
}

async function handleSaveMembers() {
  if (!currentProjectId.value || selectedUserIds.value.length === 0) {
    ElMessage.warning('请选择要加入项目的用户')
    return
  }
  
  savingMembers.value = true
  try {
    await addUsersToProject(currentProjectId.value, selectedUserIds.value)
    ElMessage.success(`已将 ${selectedUserIds.value.length} 个用户加入项目`)
    memberDialogVisible.value = false
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    savingMembers.value = false
  }
}

// ==================== 删除操作 ====================
async function handleDelete(projectId: string) {
  try {
    await ElMessageBox.confirm(
      '确认删除该项目？此操作不可撤销。',
      '确认删除',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
    await deleteProject(projectId)
    ElMessage.success('项目已删除')
    fetchProjects()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}

// ==================== 初始化 ====================
onMounted(async () => {
  try {
    provinces.value = await get('/school/provinces')
  } catch (e) {
    console.error('获取省份列表失败', e)
  }
})
</script>

<style scoped lang="scss">
.project-manage-page {
  max-width: 1200px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
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

.create-btn {
  min-width: 120px;
  
  .el-icon {
    margin-right: 6px;
  }
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px 24px;
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  
  &.total {
    background: var(--color-primary-light);
    color: var(--color-primary);
  }
  
  &.active {
    background: var(--color-success-light);
    color: var(--color-success);
  }
  
  &.ended {
    background: var(--color-info-light);
    color: var(--color-info);
  }
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

/* 表格卡片 */
.table-card {
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 20px;
}

.project-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.project-icon {
  width: 36px;
  height: 36px;
  background: var(--color-primary-light);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 18px;
}

.project-name {
  font-weight: 500;
  color: var(--text-primary);
}

.action-buttons {
  display: flex;
  gap: 8px;
  
  .el-button {
    padding: 4px 8px;
    
    .el-icon {
      margin-right: 4px;
    }
  }
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
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

/* 对话框样式 */
.project-dialog {
  :deep(.el-dialog) {
    border-radius: var(--radius-lg);
  }
}

.dialog-form {
  padding: 20px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
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

.school-select {
  display: flex;
  gap: 8px;
  width: 100%;
  margin-bottom: 12px;
}

.school-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  max-height: 200px;
  overflow-y: auto;
  padding: 8px;
  background: var(--bg-color);
  border-radius: var(--radius-md);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
  }
  
  .stats-cards {
    grid-template-columns: 1fr;
  }
}
</style>
