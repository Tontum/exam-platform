<!--
  AppLayout.vue — 三端通用布局组件
  左侧可折叠菜单 + 顶部导航条 + 中间内容区
  根据路由前缀 /teacher /principal /admin 自动切换菜单项
  设计风格：现代简约 + 清新蓝白配
-->
<template>
  <el-container class="app-layout">
    <!-- 顶部导航栏 -->
    <el-header class="app-header" height="56px">
      <div class="header-left">
        <el-icon v-if="!isFullPage" class="collapse-btn" @click="toggleSidebar">
          <Fold v-if="!isCollapsed" />
          <Expand v-else />
        </el-icon>
        <div class="header-brand">
          <div class="brand-icon">
            <el-icon><Reading /></el-icon>
          </div>
          <span class="header-title">{{ portalTitle }}</span>
        </div>
      </div>
      <div class="header-right">
        <!-- 当前登录用户信息 -->
        <el-dropdown trigger="click" v-if="userStore.isLoggedIn">
          <div class="user-info">
            <div class="user-avatar">
              <el-icon><UserFilled /></el-icon>
            </div>
            <div class="user-details">
              <span class="user-name">{{ userStore.userInfo?.name || '未知用户' }}</span>
              <span class="user-role">{{ roleLabel }}{{ userStore.userInfo?.schoolName ? ' · ' + userStore.userInfo.schoolName : '' }}</span>
            </div>
            <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <div v-else class="login-prompt" @click="handleLogin">
          <div class="user-avatar">
            <el-icon><UserFilled /></el-icon>
          </div>
          <span class="login-text">点击登录</span>
        </div>
      </div>
    </el-header>

    <el-container>
      <!-- 左侧菜单（考试页/查成绩页隐藏） -->
      <el-aside v-if="!isFullPage" :width="isCollapsed ? '64px' : '220px'" class="app-sidebar">
        <SideMenu :menu-items="currentMenu" :collapsed="isCollapsed" />
      </el-aside>

      <!-- 右侧内容区 -->
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import SideMenu from './SideMenu.vue'
import type { MenuItem } from './SideMenu.vue'
import { getMyProjects, type ProjectVO } from '@/api/project'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 侧边栏折叠状态
const isCollapsed = ref(false)

// 项目列表（学员端用）
const projects = ref<ProjectVO[]>([])

/** 根据当前登录用户角色判断所在端 */
const currentPortal = computed<'teacher' | 'principal' | 'admin'>(() => {
  const role = userStore.role
  if (role === 'admin') return 'admin'
  if (role === 'principal') return 'principal'
  return 'teacher'
})

/** 端标题映射 */
const portalTitle = computed(() => {
  const titles: Record<string, string> = {
    teacher: '在线考试平台',
    principal: '试卷管理系统',
    admin: '系统管理后台',
  }
  return titles[currentPortal.value]
})

/** 角色标签 */
const roleLabel = computed(() => {
  const info = userStore.userInfo
  if (!info) return '未知角色'
  if (info.role === 'admin') {
    return info.scope === 'PROVINCE' ? `${info.province}管理员` : '系统管理员'
  }
  const labels: Record<string, string> = {
    teacher: '学员',
    principal: '校长',
  }
  return labels[info.role] || '未知角色'
})

/** 加载项目列表 */
async function loadProjects() {
  try {
    projects.value = await getMyProjects()
  } catch (e) {
    console.error('获取项目列表失败', e)
    // 失败时使用 mock 数据
    projects.value = [
      { id: 1, name: '2025年度河南省教师素质提升培训', description: '', creatorId: 1, province: '', city: '', status: 1, createdAt: '' },
    ]
  }
}

/** 各端菜单项配置 — 所有角色都基于项目 */
const teacherMenu = computed<MenuItem[]>(() => [
  {
    path: '/teacher/projects',
    title: '项目列表',
    icon: 'Folder',
    children: projects.value.map(p => ({
      path: `/teacher/project/${p.id}/tools`,
      title: p.name,
    }))
  },
])

const principalMenu = computed<MenuItem[]>(() => [
  {
    path: '/principal/projects',
    title: '项目列表',
    icon: 'Folder',
    children: projects.value.map(p => ({
      path: `/principal/project/${p.id}/tools`,
      title: p.name,
    }))
  },
])

const adminMenu = computed<MenuItem[]>(() => [
  {
    path: '/admin/projects',
    title: '项目列表',
    icon: 'Folder',
    children: projects.value.map(p => ({
      path: `/admin/project/${p.id}/tools`,
      title: p.name,
    }))
  },
  {
    path: '/admin/users',
    title: '用户管理',
    icon: 'User',
  },
])

/** 当前端菜单 */
const currentMenu = computed<MenuItem[]>(() => {
  const menus: Record<string, MenuItem[]> = {
    teacher: teacherMenu.value,
    principal: principalMenu.value,
    admin: adminMenu.value,
  }
  return menus[currentPortal.value] || teacherMenu.value
})

/** 是否全屏页面（考试/查看成绩 — 隐藏侧边栏） */
const isFullPage = computed(() => {
  return route.path.includes('/exam/') || route.path.includes('/score/')
})

// 初始化加载项目（所有角色都需要）
onMounted(() => {
  loadProjects()
})

/** 折叠/展开侧边栏 */
function toggleSidebar() {
  isCollapsed.value = !isCollapsed.value
}

/** 切换端入口 */
function switchPortal(portal: 'teacher' | 'principal' | 'admin') {
  router.push(`/${portal}`)
}

/** 退出登录 */
function handleLogout() {
  userStore.logout()
}

/** 跳转到登录页 */
function handleLogin() {
  router.push('/login')
}
</script>

<style scoped lang="scss">
.app-layout {
  height: 100vh;
  background: var(--bg-color-page);
}

/* ---- 顶部导航 ---- */
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-color-card);
  border-bottom: 1px solid var(--border-color-lighter);
  padding: 0 24px;
  box-shadow: var(--shadow-sm);
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: var(--text-secondary);
  padding: 8px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  
  &:hover {
    color: var(--color-primary);
    background: var(--color-primary-light);
  }
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, var(--color-primary) 0%, #66B1FF 100%);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
}

.header-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  
  &:hover {
    background: var(--bg-color);
  }
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
  font-size: 18px;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.user-role {
  font-size: 12px;
  color: var(--text-secondary);
}

.dropdown-icon {
  font-size: 12px;
  color: var(--text-secondary);
  margin-left: 4px;
}

.login-prompt {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  
  &:hover {
    background: var(--bg-color);
  }
  
  .login-text {
    font-size: 14px;
    color: var(--text-secondary);
  }
}

/* ---- 侧边栏 ---- */
.app-sidebar {
  background: var(--bg-color-card);
  border-right: 1px solid var(--border-color-lighter);
  transition: width var(--transition-normal);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

/* ---- 内容区 ---- */
.app-main {
  background: var(--bg-color-page);
  padding: 24px;
  min-height: calc(100vh - var(--header-height));
  overflow-y: auto;
}
</style>
