<!--
  ToolConfig.vue — 管理后台：工具开关与规则配置页
  管理员按角色配置某个工具的业务规则
  对接 exam-project-service /api/project/{projectId}/config 接口
  设计风格：现代简约 + 清新蓝白配
-->
<template>
  <div class="tool-config-page">
    <!-- 返回按钮 -->
    <div class="back-button" @click="$router.back()">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回项目管理</span>
    </div>

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h2>工具规则配置</h2>
        <p class="subtitle">按角色配置工具的业务规则</p>
      </div>
    </div>

    <!-- 工具选择 Tab -->
    <div class="tool-tabs">
      <div 
        v-for="t in tools" 
        :key="t.code"
        class="tab-item"
        :class="{ active: activeTool === t.code }"
        @click="activeTool = t.code; fetchConfigs()"
      >
        <div class="tab-icon">
          <el-icon><Document /></el-icon>
        </div>
        <span class="tab-label">{{ t.name }}</span>
      </div>
    </div>

    <!-- 规则配置 -->
    <div class="config-card" v-loading="loading">
      <div class="config-header">
        <el-icon><Setting /></el-icon>
        <span>规则配置</span>
      </div>

      <el-empty v-if="configs.length === 0 && !loading" description="暂无该工具的配置数据" />

      <template v-for="config in configs" :key="config.id">
        <div class="config-section">
          <div class="section-header">
            <div class="role-badge" :class="getRoleClass(config.role)">
              {{ roleLabel(config.role) }}
            </div>
          </div>
          
          <el-form label-width="200px" class="config-form">
            <el-form-item label="菜单可见">
              <el-switch v-model="config.isEnabled" :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item label="是否允许发布">
              <el-switch v-model="config.allowPublish" :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item label="是否允许删除">
              <el-switch v-model="config.allowDelete" :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item label="是否允许批阅" v-if="activeTool === 'paper'">
              <el-switch v-model="config.allowReview" :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item label="是否必须设置合格分" v-if="activeTool === 'paper'">
              <el-switch v-model="config.requirePassScore" :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item label="主观题是否自动给分" v-if="activeTool === 'paper'">
              <el-switch v-model="config.autoScore" :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item label="允许发布时间段-起始" v-if="activeTool === 'paper'">
              <el-time-picker v-model="config.publishTimeStart" format="HH:mm" placeholder="起始时间" />
            </el-form-item>
            <el-form-item label="允许发布时间段-截止" v-if="activeTool === 'paper'">
              <el-time-picker v-model="config.publishTimeEnd" format="HH:mm" placeholder="截止时间" />
            </el-form-item>
            <el-form-item label="每次提交试卷加分" v-if="activeTool === 'paper'">
              <el-input-number v-model="config.scorePerSubmit" :min="0" :max="100" />
            </el-form-item>
          </el-form>
        </div>
      </template>
    </div>

    <!-- 底部保存 -->
    <div class="bottom-bar">
      <el-button @click="$router.back()">取消</el-button>
      <el-button type="primary" @click="saveConfigs" :loading="saving" class="save-btn">
        <el-icon><Check /></el-icon>
        保存全部配置
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listConfigs, batchUpdateConfigs, type ConfigVO } from '@/api/project'

const route = useRoute()
const projectId = route.params.projectId as string

// ==================== 工具列表 ====================
const tools = [
  { code: 'paper', name: '试题工具' },
  { code: 'article', name: '文章工具' },
  { code: 'chat', name: '交流工具' },
  { code: 'qa', name: '问答工具' },
  { code: 'homework', name: '作业工具' },
  { code: 'live', name: '直播工具' },
  { code: 'video', name: '视频工具' },
]

// ==================== 状态 ====================
const activeTool = ref('paper')
const loading = ref(false)
const saving = ref(false)
const configs = ref<ConfigVO[]>([])

// ==================== 角色映射 ====================
function roleLabel(role: number): string {
  switch (role) {
    case 1: return '管理员'
    case 2: return '校长'
    case 3: return '老师'
    default: return `角色${role}`
  }
}

function getRoleClass(role: number): string {
  switch (role) {
    case 1: return 'admin'
    case 2: return 'principal'
    case 3: return 'teacher'
    default: return ''
  }
}

// ==================== 数据加载 ====================
async function fetchConfigs() {
  loading.value = true
  try {
    // 加载所有角色对该工具的配置
    const allConfigs: ConfigVO[] = []
    for (const role of [1, 2, 3]) {
      const roleConfigs = await listConfigs(projectId, role)
      const filtered = roleConfigs.filter(c => c.toolCode === activeTool.value)
      allConfigs.push(...filtered)
    }
    configs.value = allConfigs
  } catch (e: any) {
    ElMessage.error(e.message || '加载配置失败')
  } finally {
    loading.value = false
  }
}
fetchConfigs()

// ==================== 保存 ====================
async function saveConfigs() {
  saving.value = true
  try {
    const updates = configs.value.map(c => ({
      id: c.id,
      isEnabled: c.isEnabled,
      allowPublish: c.allowPublish,
      allowDelete: c.allowDelete,
      allowReview: c.allowReview,
      requirePassScore: c.requirePassScore,
      autoScore: c.autoScore,
      scorePerSubmit: c.scorePerSubmit,
    }))
    await batchUpdateConfigs(projectId, updates)
    ElMessage.success('配置保存成功')
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.tool-config-page {
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

/* 工具 Tab 切换 */
.tool-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  background: var(--bg-color-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition: all var(--transition-normal);
  border: 2px solid transparent;
  
  &:hover {
    border-color: var(--color-primary-light);
  }
  
  &.active {
    border-color: var(--color-primary);
    background: var(--color-primary-light);
  }
}

.tab-icon {
  width: 32px;
  height: 32px;
  background: var(--color-primary-light);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 16px;
}

.tab-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

/* 配置卡片 */
.config-card {
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 24px;
  margin-bottom: 20px;
}

.config-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color-lighter);
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  
  .el-icon {
    color: var(--color-primary);
    font-size: 20px;
  }
}

/* 配置区域 */
.config-section {
  margin-bottom: 24px;
  padding: 20px;
  background: var(--bg-color);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color-lighter);
  
  &:last-child {
    margin-bottom: 0;
  }
}

.section-header {
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color-lighter);
}

.role-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 16px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  
  &.admin {
    background: var(--color-warning-light);
    color: var(--color-warning);
  }
  
  &.principal {
    background: var(--color-success-light);
    color: var(--color-success);
  }
  
  &.teacher {
    background: var(--color-primary-light);
    color: var(--color-primary);
  }
}

.config-form {
  :deep(.el-form-item) {
    margin-bottom: 16px;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
  
  :deep(.el-form-item__label) {
    color: var(--text-regular);
  }
}

/* 底部操作栏 */
.bottom-bar {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 24px;
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.save-btn {
  min-width: 140px;
  
  .el-icon {
    margin-right: 6px;
  }
}

/* 响应式调整 */
@media (max-width: 768px) {
  .tool-tabs {
    flex-direction: column;
  }
}
</style>
