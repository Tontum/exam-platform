<!--
  PaperManage.vue — 管理端：试卷管理列表
  校长查看自己发布的试卷，支持创建、编辑、发布、下线操作
  设计风格：现代简约 + 清新蓝白配
-->
<template>
  <div class="paper-manage-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h2>试卷管理</h2>
        <p class="subtitle">管理和发布试卷，查看试卷状态</p>
      </div>
      <el-button type="primary" @click="$router.push(`/principal/paper/create/${projectId}`)" class="create-btn">
        <el-icon><Plus /></el-icon>
        创建试卷
      </el-button>
    </div>

    <!-- 搜索筛选区 -->
    <div class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="试卷名称">
          <el-input 
            v-model="filterForm.name" 
            placeholder="搜索试卷名称" 
            clearable 
            class="filter-input"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已下线" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchPapers">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="resetFilter">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon draft">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ papers.filter(p => p.status === 0).length }}</span>
          <span class="stat-label">草稿</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon published">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ papers.filter(p => p.status === 1).length }}</span>
          <span class="stat-label">已发布</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon offline">
          <el-icon><CircleClose /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ papers.filter(p => p.status === 2).length }}</span>
          <span class="stat-label">已下线</span>
        </div>
      </div>
    </div>

    <!-- 试卷表格 -->
    <div class="table-card">
      <el-table :data="papers" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="100" show-overflow-tooltip />
        <el-table-column prop="title" label="试卷名称" min-width="180">
          <template #default="{ row }">
            <div class="paper-name-cell">
              <span class="paper-name">{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="基本信息" width="200">
          <template #default="{ row }">
            <div class="paper-info-cell">
              <span class="info-item">
                <el-icon><Clock /></el-icon>
                {{ row.durationMinutes }}分钟
              </span>
              <span class="info-item">
                <el-icon><Trophy /></el-icon>
                {{ row.totalScore }}分
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="questionCount" label="题目数" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="light">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <!-- 草稿状态：编辑题目 -->
              <el-button
                v-if="row.status === 0"
                type="primary" link size="small"
                @click="editQuestions(row.id)"
              >
                <el-icon><Edit /></el-icon>
                编辑题目
              </el-button>
              <!-- 草稿状态：发布 -->
              <el-button
                v-if="row.status === 0"
                type="success" link size="small"
                @click="handlePublishPaper(row.id)"
              >
                <el-icon><Upload /></el-icon>
                发布
              </el-button>
              <!-- 已发布：下线 -->
              <el-button
                v-if="row.status === 1"
                type="warning" link size="small"
                @click="handleClosePaper(row.id)"
              >
                <el-icon><Download /></el-icon>
                下线
              </el-button>
              <!-- 已发布/已下线：查看统计 -->
              <el-button
                v-if="row.status !== 0"
                type="info" link size="small"
                @click="$router.push(`/principal/statistics/${row.id}`)"
              >
                <el-icon><DataLine /></el-icon>
                统计
              </el-button>
              <!-- 删除 -->
              <el-button type="danger" link size="small" @click="handleDeletePaper(row.id)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next, jumper"
          @current-change="fetchPapers"
        />
      </div>
    </div>

    <!-- 空状态 -->
    <div class="empty-state" v-if="papers.length === 0 && !loading">
      <el-empty description="暂无试卷">
        <template #image>
          <div class="empty-icon">
            <el-icon><Document /></el-icon>
          </div>
        </template>
        <el-button type="primary" @click="$router.push(`/principal/paper/create/${projectId}`)">
          创建第一份试卷
        </el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPapers, publishPaper, closePaper, deletePaper, type PaperVO } from '@/api/paper'

const route = useRoute()
const router = useRouter()
const projectId = route.params.projectId as string

const loading = ref(false)
const papers = ref<PaperVO[]>([])
const page = ref(1)
const pageSize = ref(15)
const total = ref(0)

const filterForm = reactive({ name: '', status: undefined as number | undefined })

// 状态标签类型
function statusTagType(status: number): 'success' | 'info' | 'warning' {
  const map: Record<number, 'success' | 'info' | 'warning'> = {
    0: 'info',
    1: 'success',
    2: 'warning',
  }
  return map[status] || 'info'
}

// 状态文案
function statusLabel(status: number): string {
  const labels: Record<number, string> = {
    0: '草稿',
    1: '已发布',
    2: '已下线',
  }
  return labels[status] || '未知'
}

onMounted(() => fetchPapers())

/** 获取试卷列表 */
async function fetchPapers() {
  loading.value = true
  try {
    const res = await listPapers(projectId, {
      name: filterForm.name || undefined,
      status: filterForm.status,
      page: page.value,
      size: pageSize.value
    })
    papers.value = res.records
    total.value = Number(res.total)
  } catch (e: any) {
    ElMessage.error(e.message || '加载试卷列表失败')
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filterForm.name = ''
  filterForm.status = undefined
  fetchPapers()
}

function editQuestions(paperId: string) {
  router.push(`/principal/paper/${paperId}/questions`)
}

/** 发布试卷 */
async function handlePublishPaper(paperId: string) {
  try {
    await ElMessageBox.confirm('确认发布该试卷？发布后老师将可以看到并答题。', '确认发布', { type: 'info' })
    await publishPaper(paperId)
    ElMessage.success('试卷已发布')
    fetchPapers()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '发布失败')
    }
  }
}

/** 下线试卷 */
async function handleClosePaper(paperId: string) {
  try {
    await ElMessageBox.confirm('确认下线该试卷？下线后老师将无法继续答题。', '确认下线', { type: 'warning' })
    await closePaper(paperId)
    ElMessage.success('试卷已下线')
    fetchPapers()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '下线失败')
    }
  }
}

async function handleDeletePaper(paperId: string) {
  try {
    await ElMessageBox.confirm('确认删除该试卷？此操作不可撤销。', '确认删除', { type: 'error' })
    await deletePaper(paperId)
    ElMessage.success('试卷已删除')
    fetchPapers()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}
</script>

<style scoped lang="scss">
.paper-manage-page {
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

/* 搜索筛选区 */
.filter-card {
  padding: 20px 24px;
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
}

.filter-input {
  width: 240px;
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
  
  &.draft {
    background: var(--color-info-light);
    color: var(--color-info);
  }
  
  &.published {
    background: var(--color-success-light);
    color: var(--color-success);
  }
  
  &.offline {
    background: var(--color-warning-light);
    color: var(--color-warning);
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

.paper-name-cell {
  .paper-name {
    font-weight: 500;
    color: var(--text-primary);
  }
}

.paper-info-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  
  .el-icon {
    font-size: 14px;
  }
}

.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  
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

/* 响应式调整 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
  }
  
  .stats-cards {
    grid-template-columns: 1fr;
  }
  
  .filter-form {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-input {
    width: 100%;
  }
}
</style>
