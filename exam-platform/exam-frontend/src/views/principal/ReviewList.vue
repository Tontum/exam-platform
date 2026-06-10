<!--
  ReviewList.vue — 管理端：批阅列表
  校长选择某份已发布的试卷 → 查看所有老师答题列表 → 进入批阅
  设计风格：现代简约 + 清新蓝白配
-->
<template>
  <div class="review-list-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h2>批阅试卷</h2>
        <p class="subtitle">选择试卷，查看和批阅老师的答题情况</p>
      </div>
    </div>

    <!-- 选择试卷 -->
    <div class="select-card">
      <div class="select-header">
        <el-icon><Document /></el-icon>
        <span>选择试卷</span>
      </div>
      <el-select 
        v-model="selectedPaperId" 
        placeholder="请选择要批阅的试卷" 
        @change="fetchReviewList" 
        style="width: 100%"
        size="large"
      >
        <el-option 
          v-for="p in paperOptions" 
          :key="p.paperId" 
          :label="p.paperName" 
          :value="p.paperId" 
        />
      </el-select>
    </div>

    <!-- 统计信息 -->
    <div class="stats-cards" v-if="selectedPaperId && reviewList.length > 0">
      <div class="stat-card">
        <div class="stat-icon total">
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ reviewList.length }}</span>
          <span class="stat-label">总人数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon pending">
          <el-icon><Edit /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ reviewList.filter(r => r.status === 2).length }}</span>
          <span class="stat-label">待批阅</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon reviewed">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ reviewList.filter(r => r.status === 3).length }}</span>
          <span class="stat-label">已批阅</span>
        </div>
      </div>
    </div>

    <!-- 老师答题列表 -->
    <div class="table-card" v-if="selectedPaperId">
      <el-table :data="reviewList" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="userName" label="老师姓名" min-width="120">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-avatar">
                <el-icon><User /></el-icon>
              </div>
              <span class="user-name">{{ row.userName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="school" label="学校" min-width="160" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 3 ? 'success' : row.status === 2 ? 'warning' : 'info'" size="small" effect="light">
              {{ row.status === 3 ? '已批阅' : row.status === 2 ? '待批阅' : '答题中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="得分" width="120">
          <template #default="{ row }">
            <div class="score-cell">
              <span v-if="row.score !== null" class="score-value">{{ row.score }} <span class="score-unit">/ {{ row.totalScore }}</span></span>
              <span v-else class="not-scored">-</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              :type="row.status === 3 ? 'info' : 'primary'" 
              link size="small"
              @click="enterReview(row.userId)"
              class="review-btn"
            >
              <el-icon><View v-if="row.status === 3" /><Edit v-else /></el-icon>
              {{ row.status === 3 ? '查看' : '批阅' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next, jumper"
          @current-change="fetchReviewList"
        />
      </div>
    </div>

    <!-- 空状态 -->
    <div class="empty-state" v-if="!selectedPaperId">
      <el-empty description="请选择一份试卷查看答题列表">
        <template #image>
          <div class="empty-icon">
            <el-icon><Document /></el-icon>
          </div>
        </template>
      </el-empty>
    </div>

    <div class="empty-state" v-if="selectedPaperId && reviewList.length === 0 && !loading">
      <el-empty description="暂无答题记录" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

interface ReviewItemVo {
  userId: number
  userName: string
  school: string
  status: number
  score: number | null
  totalScore: number
  submitTime: string
}

const paperOptions = ref([
  { paperId: 2, paperName: '学科专业知识考核' },
  { paperId: 3, paperName: '课堂教学技能测试' },
])

const selectedPaperId = ref<number | null>(null)
const reviewList = ref<ReviewItemVo[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

/** 获取批阅列表 */
function fetchReviewList() {
  if (!selectedPaperId.value) return
  loading.value = true
  // TODO: 调用 API GET /api/principal/review  params: { paperId: selectedPaperId, page, pageSize }
  setTimeout(() => {
    reviewList.value = [] // 从 API 获取
    total.value = 0 // 从 API 获取
    loading.value = false
  }, 300)
}

function enterReview(userId: number) {
  router.push(`/principal/review/${selectedPaperId.value}/${userId}`)
}
</script>

<style scoped lang="scss">
.review-list-page {
  max-width: 1200px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
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

/* 选择试卷 */
.select-card {
  padding: 24px;
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  margin-bottom: 20px;
}

.select-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  
  .el-icon {
    color: var(--color-primary);
  }
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
  
  &.pending {
    background: var(--color-warning-light);
    color: var(--color-warning);
  }
  
  &.reviewed {
    background: var(--color-success-light);
    color: var(--color-success);
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

.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  background: var(--color-primary-light);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 16px;
}

.user-name {
  font-weight: 500;
  color: var(--text-primary);
}

.score-cell {
  .score-value {
    font-weight: 600;
    color: var(--color-primary);
  }
  
  .score-unit {
    font-weight: 400;
    color: var(--text-secondary);
    font-size: 12px;
  }
  
  .not-scored {
    color: var(--text-placeholder);
  }
}

.review-btn {
  .el-icon {
    margin-right: 4px;
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
  .stats-cards {
    grid-template-columns: 1fr;
  }
}
</style>
