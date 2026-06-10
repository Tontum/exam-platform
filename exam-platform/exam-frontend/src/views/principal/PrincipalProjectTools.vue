<!--
  PrincipalProjectTools.vue — 管理端：校长项目工具页
  校长点击某个项目后进入此页面，展示该项目下校长可用的工具
  校长可以发布试卷、批阅试卷、查看统计等
  设计风格：现代简约 + 清新蓝白配
-->
<template>
  <div class="project-tools-page">
    <!-- 返回按钮 -->
    <div class="back-button" @click="$router.push('/principal/projects')">
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

    <!-- 工具列表 -->
    <div class="tools-section">
      <h3 class="section-title">项目工具</h3>
      
      <div class="tools-grid" v-if="tools.length > 0">
        <div
          v-for="tool in tools"
          :key="tool.code"
          class="tool-card"
          :class="{ disabled: !tool.isEnabled }"
          @click="enterTool(tool)"
        >
          <div class="tool-icon" :class="tool.code">
            <el-icon>
              <Document v-if="tool.code === 'paper'" />
              <Edit v-if="tool.code === 'homework'" />
              <VideoPlay v-if="tool.code === 'video'" />
              <ChatDotRound v-if="tool.code === 'chat'" />
              <ChatLineSquare v-if="tool.code === 'qa'" />
              <Reading v-if="tool.code === 'article'" />
              <Monitor v-if="tool.code === 'live'" />
            </el-icon>
          </div>
          
          <div class="tool-info">
            <h4 class="tool-name">{{ tool.name }}</h4>
            <p class="tool-desc">{{ tool.description }}</p>
          </div>
          
          <div class="tool-status">
            <el-tag v-if="tool.isEnabled" type="success" size="small" effect="light">
              可用
            </el-tag>
            <el-tag v-else type="info" size="small" effect="light">
              未启用
            </el-tag>
          </div>
          
          <div class="tool-action" v-if="tool.isEnabled">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div class="empty-state" v-else>
        <el-empty description="暂无可用工具">
          <template #image>
            <div class="empty-icon">
              <el-icon><Box /></el-icon>
            </div>
          </template>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProject, listProjectTools, type ProjectVO, type ToolVO } from '@/api/project'

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

// 工具列表
const tools = ref<ToolVO[]>([])

onMounted(async () => {
  // 调用 API 获取项目信息
  try {
    project.value = await getProject(projectId)
  } catch (e) {
    console.error('获取项目信息失败', e)
    ElMessage.error('获取项目信息失败')
  }
  
  // 调用 API 获取校长角色已启用的工具列表
  try {
    const enabledTools = await listProjectTools(projectId, 2) // role=2 校长
    
    // 所有工具定义（校长可用的工具）
    const allTools = [
      { code: 'paper', name: '试卷工具', description: '发布试卷、批阅试卷' },
      { code: 'homework', name: '作业工具', description: '布置作业、批改作业' },
      { code: 'video', name: '视频工具', description: '管理视频资源' },
      { code: 'article', name: '文章工具', description: '发布文章' },
      { code: 'chat', name: '交流工具', description: '管理讨论区' },
      { code: 'qa', name: '问答工具', description: '管理问答' },
      { code: 'live', name: '直播工具', description: '管理直播' },
    ]
    
    // 合并：已启用的工具标记为可用，未启用的标记为不可用
    const enabledCodes = new Set(enabledTools.map(t => t.code))
    tools.value = allTools.map(tool => ({
      ...tool,
      isEnabled: enabledCodes.has(tool.code)
    }))
  } catch (e) {
    console.error('获取工具列表失败', e)
    ElMessage.error('获取工具列表失败')
  }
})

/** 进入工具 */
function enterTool(tool: ToolVO) {
  if (!tool.isEnabled) {
    ElMessage.warning('该工具暂未启用')
    return
  }
  
  // 根据工具类型跳转到对应页面
  switch (tool.code) {
    case 'paper':
      router.push(`/principal/papers/${projectId}`)
      break
    case 'homework':
      // TODO: 跳转到作业管理
      ElMessage.info('作业功能开发中')
      break
    case 'video':
      // TODO: 跳转到视频管理
      ElMessage.info('视频功能开发中')
      break
    default:
      ElMessage.info('功能开发中')
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
  
  &.disabled {
    opacity: 0.6;
    cursor: not-allowed;
    
    &:hover {
      transform: none;
      box-shadow: none;
      border-color: var(--border-color-lighter);
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
  
  &.paper {
    background: var(--color-primary-light);
    color: var(--color-primary);
  }
  
  &.homework {
    background: var(--color-success-light);
    color: var(--color-success);
  }
  
  &.video {
    background: var(--color-warning-light);
    color: var(--color-warning);
  }
  
  &.article {
    background: var(--color-info-light);
    color: var(--color-info);
  }
  
  &.chat, &.qa {
    background: #F0F9FF;
    color: #0EA5E9;
  }
  
  &.live {
    background: #FDF2F8;
    color: #EC4899;
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

.tool-status {
  flex-shrink: 0;
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
  
  .header-meta {
    width: 100%;
    justify-content: flex-start;
  }
  
  .tool-card {
    flex-wrap: wrap;
  }
}
</style>
