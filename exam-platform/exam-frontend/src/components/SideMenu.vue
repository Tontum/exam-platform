<!--
  SideMenu.vue — 侧边菜单组件
  根据传入的菜单项渲染，支持折叠模式（仅显示图标）
  设计风格：现代简约 + 清新蓝白配
-->
<template>
  <el-menu
    :default-active="activeMenu"
    :collapse="collapsed"
    :collapse-transition="false"
    router
    class="side-menu"
  >
    <template v-for="item in menuItems" :key="item.path">
      <!-- 有子菜单时渲染为 sub-menu -->
      <el-sub-menu v-if="item.children?.length" :index="item.path">
        <template #title>
          <el-icon v-if="item.icon"><component :is="iconMap[item.icon]" /></el-icon>
          <span>{{ item.title }}</span>
        </template>
        <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
          {{ child.title }}
        </el-menu-item>
      </el-sub-menu>

      <!-- 单级菜单 -->
      <el-menu-item v-else :index="item.path" class="menu-item">
        <el-icon v-if="item.icon"><component :is="iconMap[item.icon]" /></el-icon>
        <template #title>{{ item.title }}</template>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
// 动态引入图标组件
import {
  Folder,
  Document,
  Edit,
  DataAnalysis,
  User,
} from '@element-plus/icons-vue'

// 图标名称 → 组件映射
const iconMap: Record<string, any> = {
  Folder,
  Document,
  Edit,
  DataAnalysis,
  User,
}

// 菜单项类型定义
export interface MenuItem {
  path: string
  title: string
  icon?: string
  children?: MenuItem[]
}

defineProps<{
  menuItems: MenuItem[]
  collapsed: boolean
}>()

const route = useRoute()

// 高亮当前活跃菜单项
const activeMenu = computed(() => {
  return route.path
})
</script>

<style scoped lang="scss">
.side-menu {
  border-right: none;
  height: 100%;
  padding: 12px 8px;
  
  /* 菜单项样式 */
  :deep(.el-menu-item) {
    height: 48px;
    line-height: 48px;
    margin-bottom: 4px;
    border-radius: var(--radius-md);
    color: var(--text-regular);
    transition: all var(--transition-fast);
    
    &:hover {
      background: var(--color-primary-light);
      color: var(--color-primary);
    }
    
    &.is-active {
      background: linear-gradient(135deg, var(--color-primary) 0%, #66B1FF 100%);
      color: white;
      font-weight: 500;
      box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
      
      .el-icon {
        color: white;
      }
    }
    
    .el-icon {
      font-size: 18px;
      margin-right: 8px;
      color: var(--text-secondary);
      transition: color var(--transition-fast);
    }
    
    &:hover .el-icon,
    &.is-active .el-icon {
      color: inherit;
    }
  }
  
  /* 子菜单样式 */
  :deep(.el-sub-menu) {
    margin-bottom: 4px;
    
    .el-sub-menu__title {
      height: 48px;
      line-height: 48px;
      border-radius: var(--radius-md);
      color: var(--text-regular);
      transition: all var(--transition-fast);
      
      &:hover {
        background: var(--color-primary-light);
        color: var(--color-primary);
      }
      
      .el-icon {
        font-size: 18px;
        margin-right: 8px;
        color: var(--text-secondary);
        transition: color var(--transition-fast);
      }
      
      &:hover .el-icon {
        color: var(--color-primary);
      }
    }
    
    .el-menu {
      padding-left: 12px;
    }
    
    .el-menu-item {
      height: 44px;
      line-height: 44px;
      font-size: 13px;
    }
  }
  
  /* 折叠模式样式 */
  &.el-menu--collapse {
    width: 64px;
    padding: 12px 4px;
    
    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      height: 48px;
      line-height: 48px;
      padding: 0 20px;
      justify-content: center;
      
      .el-icon {
        margin-right: 0;
      }
    }
  }
}

/* Element Plus 菜单在折叠时不要弹出 tooltip */
.el-menu--collapse {
  width: 64px;
}
</style>
