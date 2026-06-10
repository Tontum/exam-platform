<!--
  PaperList.vue — 学员端：试卷列表页
  展示当前项目下该老师的所有试卷，按状态分组显示
  状态：未答题(0)→开始答题 | 答题中(1)→继续答题 | 已提交(2)→查看详情 | 已批阅(3)→查看成绩
  设计风格：现代简约 + 清新蓝白配
-->
<template>
  <div class="paper-list-page">
    <!-- 返回按钮 -->
    <div class="back-button" @click="$router.push(`/teacher/project/${projectId}/tools`)">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回项目工具</span>
    </div>

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h2>{{ projectName }}</h2>
        <p class="subtitle">共 {{ papers.length }} 份试卷</p>
      </div>
      <div class="header-stats">
        <div class="stat-item pending">
          <span class="stat-value">{{ papers.filter(p => p.status === 0).length }}</span>
          <span class="stat-label">待答题</span>
        </div>
        <div class="stat-item progress">
          <span class="stat-value">{{ papers.filter(p => p.status === 1).length }}</span>
          <span class="stat-label">进行中</span>
        </div>
        <div class="stat-item completed">
          <span class="stat-value">{{ papers.filter(p => p.status === 2 || p.status === 3).length }}</span>
          <span class="stat-label">已完成</span>
        </div>
      </div>
    </div>

    <!-- 试卷列表 -->
    <div class="paper-list" v-if="papers.length > 0">
      <div
        v-for="paper in papers"
        :key="paper.paperId"
        class="paper-card"
        :class="{ 'is-completed': paper.status === 3 }"
      >
        <div class="card-left">
          <div class="card-icon" :class="statusClass(paper.status)">
            <el-icon>
              <Document v-if="paper.status === 0" />
              <Edit v-if="paper.status === 1" />
              <Check v-if="paper.status === 2" />
              <CircleCheck v-if="paper.status === 3" />
            </el-icon>
          </div>
          <div class="card-info">
            <h3 class="card-title">{{ paper.paperTitle }}</h3>
            <div class="card-meta">
              <span class="meta-item">
                <el-icon><Clock /></el-icon>
                {{ paper.durationMinutes }} 分钟
              </span>
              <span class="meta-item">
                <el-icon><Trophy /></el-icon>
                总分 {{ paper.totalScore }} 分
              </span>
              <span class="meta-item">
                <el-icon><Aim /></el-icon>
                及格 {{ paper.passScore }} 分
              </span>
            </div>
            <div class="card-details">
              <span class="detail-item">
                题目数：{{ paper.questionCount }} 道
              </span>
              <span class="detail-item">
                发布者：{{ paper.publisherName }}
              </span>
            </div>
          </div>
        </div>

        <div class="card-right">
          <!-- 状态标签 -->
          <el-tag
            :type="statusTagType(paper.status)"
            size="large"
            effect="light"
            class="status-tag"
          >
            {{ statusLabel(paper.status) }}
          </el-tag>

          <!-- 操作按钮 -->
          <el-button
            v-if="paper.status === 0"
            type="primary"
            size="large"
            @click="startExam(paper.paperId)"
            class="action-btn"
          >
            <el-icon><Edit /></el-icon>
            开始答题
          </el-button>

          <el-button
            v-if="paper.status === 1"
            type="warning"
            size="large"
            @click="continueExam(paper.paperId)"
            class="action-btn"
          >
            <el-icon><VideoPlay /></el-icon>
            继续答题
          </el-button>

          <el-button
            v-if="paper.status === 2"
            type="info"
            size="large"
            @click="viewDetail(paper.paperId)"
            class="action-btn"
          >
            <el-icon><View /></el-icon>
            查看详情
          </el-button>

          <el-button
            v-if="paper.status === 3"
            type="success"
            size="large"
            @click="viewScore(paper.paperId)"
            class="action-btn"
          >
            <el-icon><DataLine /></el-icon>
            查看成绩
          </el-button>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="empty-state" v-else>
      <el-empty description="暂无试卷">
        <template #image>
          <div class="empty-icon">
            <el-icon><Document /></el-icon>
          </div>
        </template>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listMyPapers, type MyPaperVO } from '@/api/paper'

const route = useRoute()
const router = useRouter()

const projectName = ref('')
const projectId = route.params.projectId as string
const papers = ref<MyPaperVO[]>([])

// 状态标签样式
function statusTagType(status: number): 'info' | 'warning' | '' | 'success' {
  const map: Record<number, 'info' | 'warning' | '' | 'success'> = {
    0: 'info',
    1: 'warning',
    2: '',
    3: 'success',
  }
  return map[status] || 'info'
}

// 状态文案
function statusLabel(status: number): string {
  const labels: Record<number, string> = {
    0: '未答题',
    1: '正在答题',
    2: '已提交',
    3: '已批阅',
  }
  return labels[status] || '未知'
}

// 状态样式类
function statusClass(status: number): string {
  const classes: Record<number, string> = {
    0: 'status-pending',
    1: 'status-progress',
    2: 'status-submitted',
    3: 'status-reviewed',
  }
  return classes[status] || ''
}

onMounted(async () => {
  try {
    papers.value = await listMyPapers(projectId)
  } catch (e: any) {
    ElMessage.error(e.message || '加载试卷列表失败')
  }
})

function startExam(paperId: number) { router.push(`/teacher/exam/${paperId}`) }
function continueExam(paperId: number) { router.push(`/teacher/exam/${paperId}`) }
function viewDetail(paperId: number) { router.push(`/teacher/exam/${paperId}`) }
function viewScore(paperId: number) { router.push(`/teacher/score/${paperId}`) }
</script>

<style scoped lang="scss">
.paper-list-page {
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

.header-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  text-align: center;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  min-width: 80px;
  
  &.pending {
    background: var(--color-info-light);
    .stat-value { color: var(--color-info); }
  }
  
  &.progress {
    background: var(--color-warning-light);
    .stat-value { color: var(--color-warning); }
  }
  
  &.completed {
    background: var(--color-success-light);
    .stat-value { color: var(--color-success); }
  }
  
  .stat-value {
    display: block;
    font-size: 24px;
    font-weight: 600;
    line-height: 1.2;
  }
  
  .stat-label {
    font-size: 12px;
    color: var(--text-secondary);
    margin-top: 4px;
  }
}

/* 试卷列表 */
.paper-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.paper-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-lighter);
  transition: all var(--transition-normal);
  
  &:hover {
    box-shadow: var(--shadow-md);
    border-color: var(--color-primary-light);
  }
  
  &.is-completed {
    border-left: 4px solid var(--color-success);
  }
}

.card-left {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  flex: 1;
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
  
  &.status-pending {
    background: var(--color-info-light);
    color: var(--color-info);
  }
  
  &.status-progress {
    background: var(--color-warning-light);
    color: var(--color-warning);
  }
  
  &.status-submitted {
    background: var(--color-primary-light);
    color: var(--color-primary);
  }
  
  &.status-reviewed {
    background: var(--color-success-light);
    color: var(--color-success);
  }
}

.card-info {
  flex: 1;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
  line-height: 1.4;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 8px;
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

.card-details {
  display: flex;
  gap: 20px;
}

.detail-item {
  font-size: 13px;
  color: var(--text-secondary);
}

.card-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  margin-left: 24px;
}

.status-tag {
  font-size: 14px;
  font-weight: 500;
}

.action-btn {
  min-width: 120px;
  
  .el-icon {
    margin-right: 6px;
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
  
  .header-stats {
    width: 100%;
    justify-content: space-between;
  }
  
  .paper-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
  }
  
  .card-right {
    flex-direction: row;
    align-items: center;
    margin-left: 0;
    width: 100%;
    justify-content: space-between;
  }
}
</style>
