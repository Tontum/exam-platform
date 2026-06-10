import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// Vite 构建配置
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'), // @ 别名指向 src 目录
    },
  },
  server: {
    port: 3000,
    open: false,
    // 代理配置：将 /api 请求转发到后端网关
    proxy: {
      '/api': {
        target: 'http://localhost:8087',  // exam-project-service（开发阶段直连，后续切 gateway:8080）
        changeOrigin: true,
      },
    },
  },
})
