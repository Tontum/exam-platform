<!--
  Statistics.vue — 管理端：数据统计页
  按试卷维度展示平均分、合格率、各分数段人数
  支持按区域（省/市/县/校）对比
  设计风格：现代简约 + 清新蓝白配
-->
<template>
  <div class="statistics-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h2>数据统计</h2>
        <p class="subtitle">查看试卷统计数据，分析区域对比情况</p>
      </div>
    </div>

    <!-- 筛选区 -->
    <div class="filter-card">
      <div class="filter-header">
        <el-icon><DataLine /></el-icon>
        <span>筛选条件</span>
      </div>
      <el-form :inline="true" class="filter-form">
        <el-form-item label="选择试卷">
          <el-select v-model="selectedPaperId" placeholder="选择试卷" @change="fetchData" style="width: 280px">
            <el-option v-for="p in paperOptions" :key="p.paperId" :label="p.paperName" :value="p.paperId" />
          </el-select>
        </el-form-item>
        <el-form-item label="区域维度">
          <el-select v-model="regionLevel" placeholder="按区域统计" @change="fetchData" style="width: 160px">
            <el-option label="省级" value="province" />
            <el-option label="市级" value="city" />
            <el-option label="县级" value="district" />
            <el-option label="校级" value="school" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <!-- 空状态 -->
    <div class="empty-state" v-if="!selectedPaperId">
      <el-empty description="请选择试卷查看统计数据">
        <template #image>
          <div class="empty-icon">
            <el-icon><DataLine /></el-icon>
          </div>
        </template>
      </el-empty>
    </div>

    <template v-if="selectedPaperId">
      <!-- 总览卡片 -->
      <div class="overview-cards">
        <div class="overview-card">
          <div class="card-icon participants">
            <el-icon><User /></el-icon>
          </div>
          <div class="card-info">
            <span class="card-value">{{ overview.participantCount }}</span>
            <span class="card-label">参与人数</span>
          </div>
        </div>
        <div class="overview-card">
          <div class="card-icon submitted">
            <el-icon><Document /></el-icon>
          </div>
          <div class="card-info">
            <span class="card-value">{{ overview.submittedCount }}</span>
            <span class="card-label">已提交</span>
          </div>
        </div>
        <div class="overview-card">
          <div class="card-icon reviewed">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="card-info">
            <span class="card-value">{{ overview.reviewedCount }}</span>
            <span class="card-label">已批阅</span>
          </div>
        </div>
        <div class="overview-card">
          <div class="card-icon average">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="card-info">
            <span class="card-value">{{ overview.avgScore }}</span>
            <span class="card-label">平均分</span>
          </div>
        </div>
        <div class="overview-card">
          <div class="card-icon pass-rate">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="card-info">
            <span class="card-value">{{ overview.passRate }}%</span>
            <span class="card-label">合格率</span>
          </div>
        </div>
      </div>

      <!-- 分数段分布 -->
      <div class="section-card">
        <div class="section-header">
          <el-icon><Histogram /></el-icon>
          <h3>分数段分布</h3>
        </div>
        <div class="score-dist">
          <div v-for="seg in scoreSegments" :key="seg.label" class="dist-bar-wrap">
            <span class="dist-label">{{ seg.label }}</span>
            <div class="dist-bar-bg">
              <div class="dist-bar" :style="{ width: seg.width + '%' }"></div>
            </div>
            <span class="dist-count">{{ seg.count }} 人</span>
          </div>
        </div>
      </div>

      <!-- 区域对比表 -->
      <div class="section-card">
        <div class="section-header">
          <el-icon><MapLocation /></el-icon>
          <h3>区域{{ regionLabel }}对比</h3>
        </div>
        <el-table :data="regionData" stripe style="width: 100%">
          <el-table-column prop="regionName" :label="regionLabel" min-width="140">
            <template #default="{ row }">
              <div class="region-cell">
                <el-icon><Location /></el-icon>
                <span>{{ row.regionName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="participantCount" label="参与人数" width="100" />
          <el-table-column prop="avgScore" label="平均分" width="100">
            <template #default="{ row }">
              <span class="score-highlight">{{ row.avgScore }}</span>
            </template>
          </el-table-column>
          <el-table-column label="合格率" width="120">
            <template #default="{ row }">
              <div class="pass-rate-cell">
                <el-progress :percentage="row.passRate" :stroke-width="8" :show-text="false" />
                <span class="pass-rate-text">{{ row.passRate }}%</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="maxScore" label="最高分" width="80" />
          <el-table-column prop="minScore" label="最低分" width="80" />
        </el-table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'

const paperOptions = ref([
  { paperId: 2, paperName: '学科专业知识考核' },
  { paperId: 3, paperName: '课堂教学技能测试' },
])
const selectedPaperId = ref<number | null>(null)
const regionLevel = ref('city')

const regionLabel = computed(() => {
  const map: Record<string, string> = { province: '省', city: '市', district: '县', school: '校' }
  return map[regionLevel.value] || '区域'
})

// 总览数据
const overview = reactive({
  participantCount: 0,
  submittedCount: 0,
  reviewedCount: 0,
  avgScore: 0,
  passRate: 0,
})

// 分数段分布
const scoreSegments = ref<{ label: string; count: number; width: number }[]>([])

// 区域对比数据
const regionData = ref<any[]>([])

function fetchData() {
  if (!selectedPaperId.value) return
  // TODO: 调用 API GET /api/principal/statistics  params: { paperId, regionLevel }
  overview.participantCount = 0 // 从 API 获取
  overview.submittedCount = 0 // 从 API 获取
  overview.reviewedCount = 0 // 从 API 获取
  overview.avgScore = 0 // 从 API 获取
  overview.passRate = 0 // 从 API 获取

  scoreSegments.value = [] // 从 API 获取
  regionData.value = [] // 从 API 获取
}
</script>

<style scoped lang="scss">
.statistics-page {
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

/* 筛选区 */
.filter-card {
  padding: 24px;
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  margin-bottom: 20px;
}

.filter-header {
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

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
}

/* 总览卡片 */
.overview-cards {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.overview-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-normal);
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }
}

.card-icon {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  
  &.participants {
    background: var(--color-primary-light);
    color: var(--color-primary);
  }
  
  &.submitted {
    background: var(--color-info-light);
    color: var(--color-info);
  }
  
  &.reviewed {
    background: var(--color-success-light);
    color: var(--color-success);
  }
  
  &.average {
    background: var(--color-warning-light);
    color: var(--color-warning);
  }
  
  &.pass-rate {
    background: var(--color-success-light);
    color: var(--color-success);
  }
}

.card-info {
  display: flex;
  flex-direction: column;
}

.card-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}

.card-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

/* 区域卡片 */
.section-card {
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 24px;
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color-lighter);
  
  .el-icon {
    color: var(--color-primary);
    font-size: 20px;
  }
  
  h3 {
    font-size: 17px;
    font-weight: 600;
    color: var(--text-primary);
  }
}

/* 分数段分布 */
.score-dist {
  .dist-bar-wrap {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;
    
    &:last-child {
      margin-bottom: 0;
    }
    
    .dist-label {
      width: 80px;
      font-size: 14px;
      color: var(--text-secondary);
      text-align: right;
    }
    
    .dist-bar-bg {
      flex: 1;
      height: 28px;
      background: var(--bg-color);
      border-radius: var(--radius-md);
      overflow: hidden;
    }
    
    .dist-bar {
      height: 100%;
      background: linear-gradient(90deg, var(--color-primary) 0%, #66B1FF 100%);
      border-radius: var(--radius-md);
      transition: width 0.5s ease;
    }
    
    .dist-count {
      width: 70px;
      font-size: 14px;
      color: var(--text-secondary);
      font-weight: 500;
    }
  }
}

/* 区域表格 */
.region-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .el-icon {
    color: var(--color-primary);
  }
}

.score-highlight {
  font-weight: 600;
  color: var(--color-primary);
}

.pass-rate-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .el-progress {
    flex: 1;
  }
  
  .pass-rate-text {
    font-size: 13px;
    font-weight: 500;
    color: var(--text-secondary);
    min-width: 40px;
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
@media (max-width: 1024px) {
  .overview-cards {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .overview-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .filter-form {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
