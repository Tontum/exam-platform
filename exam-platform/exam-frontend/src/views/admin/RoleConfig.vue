<!--
  RoleConfig.vue — 管理后台：角色权限配置页
  管理员为某个项目配置各角色的菜单可见性和操作按钮权限
  对接 exam-project-service /api/project/{projectId}/config 接口
  设计风格：现代简约 + 清新蓝白配
-->
<template>
  <div class="role-config-page">
    <!-- 返回按钮 -->
    <div class="back-button" @click="$router.back()">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回项目管理</span>
    </div>

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h2>角色权限配置</h2>
        <p class="subtitle">配置各角色在本项目中可用的工具菜单和操作按钮</p>
      </div>
    </div>

    <!-- 角色 Tab 切换 -->
    <div class="role-tabs">
      <div 
        v-for="role in roles" 
        :key="role.value"
        class="tab-item"
        :class="{ active: activeRole === role.value }"
        @click="activeRole = role.value; fetchConfigs()"
      >
        <div class="tab-icon" :class="role.value">
          <el-icon><User /></el-icon>
        </div>
        <span class="tab-label">{{ role.label }}</span>
      </div>
    </div>

    <!-- 当前角色下的工具权限列表 -->
    <div class="config-card" v-loading="loading">
      <div class="config-header">
        <el-icon><Setting /></el-icon>
        <span>工具权限配置</span>
      </div>
      
      <el-table :data="configs" stripe style="width: 100%">
        <el-table-column prop="toolName" label="工具名称" min-width="120">
          <template #default="{ row }">
            <div class="tool-cell">
              <div class="tool-icon">
                <el-icon><Document /></el-icon>
              </div>
              <span>{{ row.toolName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="toolCode" label="工具编码" width="120" />
        <el-table-column label="菜单可见" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.isEnabled" :active-value="1" :inactive-value="0" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="允许发布" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.allowPublish" :active-value="1" :inactive-value="0" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="允许删除" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.allowDelete" :active-value="1" :inactive-value="0" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="允许批阅" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.allowReview" :active-value="1" :inactive-value="0" size="small" />
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <div class="empty-state" v-if="configs.length === 0 && !loading">
        <el-empty description="暂无配置数据" />
      </div>
    </div>

    <!-- 底部保存 -->
    <div class="bottom-bar">
      <el-button @click="$router.back()">取消</el-button>
      <el-button type="primary" @click="saveConfigs" :loading="saving" class="save-btn">
        <el-icon><Check /></el-icon>
        保存权限配置
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listConfigs, batchUpdateConfigs } from '@/api/project'
import type { ConfigVO } from '@/api/project'

const route = useRoute()
const projectId = route.params.projectId as string

// 角色列表
const roles = [
  { value: '3', label: '老师 (teacher)' },
  { value: '2', label: '校长 (principal)' },
  { value: '1', label: '管理员 (admin)' },
]

// ==================== 状态 ====================
const activeRole = ref('3')
const loading = ref(false)
const saving = ref(false)
const configs = ref<ConfigVO[]>([])

// ==================== 数据加载 ====================
async function fetchConfigs() {
  loading.value = true
  try {
    configs.value = await listConfigs(projectId, Number(activeRole.value))
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
    }))
    await batchUpdateConfigs(projectId, updates)
    ElMessage.success('权限配置保存成功')
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.role-config-page {
  max-width: 1100px;
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

/* 角色 Tab 切换 */
.role-tabs {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
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
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  
  &.teacher {
    background: var(--color-primary-light);
    color: var(--color-primary);
  }
  
  &.principal {
    background: var(--color-success-light);
    color: var(--color-success);
  }
  
  &.admin {
    background: var(--color-warning-light);
    color: var(--color-warning);
  }
}

.tab-label {
  font-size: 15px;
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
  margin-bottom: 20px;
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

.tool-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tool-icon {
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

/* 空状态 */
.empty-state {
  padding: 60px 0;
  text-align: center;
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
  .role-tabs {
    flex-direction: column;
  }
}
</style>
