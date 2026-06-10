/**
 * 应用入口文件
 * 初始化 Vue 应用、挂载路由、状态管理、UI 组件库
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import './styles/global.scss'

const app = createApp(App)

// Pinia 状态管理（用户信息、权限等全局状态）
const pinia = createPinia()
app.use(pinia)

// Vue Router 路由
app.use(router)

// Element Plus UI 组件库（中文语言包）
app.use(ElementPlus, { locale: zhCn })

// 全局注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
