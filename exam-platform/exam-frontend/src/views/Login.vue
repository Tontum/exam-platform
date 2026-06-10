<!--
  Login.vue — 登录页面
  用户输入用户名和密码进行登录
  登录成功后跳转到对应角色的首页
  设计风格：现代简约 + 清新蓝白配
-->
<template>
  <div class="login-page">
    <div class="login-container">
      <!-- 左侧品牌区域 -->
      <div class="login-brand">
        <div class="brand-content">
          <div class="brand-icon">
            <el-icon><Reading /></el-icon>
          </div>
          <h1 class="brand-title">教师培训在线考试平台</h1>
          <p class="brand-subtitle">面向全国教师的在线培训与考核系统</p>
        </div>
      </div>

      <!-- 右侧登录表单 -->
      <div class="login-form-wrapper">
        <div class="form-header">
          <h2>用户登录</h2>
          <p>请输入您的账号和密码</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              size="large"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              登录
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 注册链接 -->
        <div class="register-link">
          <span>还没有账号？</span>
          <router-link to="/register" class="link">老师注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { post } from '@/api/index'

const router = useRouter()
const userStore = useUserStore()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)

// 进入登录页时清除旧的登录状态，防止路由守卫用旧值跳转
onMounted(() => {
  localStorage.removeItem('userRole')
  localStorage.removeItem('userInfo')
})

// 登录表单
const loginForm = reactive({
  username: '',
  password: ''
})

// 表单验证规则
const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

// 登录
async function handleLogin() {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      const res = await post<any>('/auth/login', loginForm)
      
      // 清除旧的 localStorage 数据，防止角色残留
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('userRole')
      
      // 保存 Token 和用户信息
      userStore.setToken(res.token)
      userStore.setUserInfo({
        id: res.userId,
        name: res.realName,
        role: res.role === 1 ? 'admin' : res.role === 2 ? 'principal' : 'teacher',
        schoolId: res.schoolId || '',
        schoolName: res.schoolName || '',
        scope: res.scope || '',
        province: res.province || ''
      })
      
      // 保存用户ID到localStorage
      localStorage.setItem('userId', String(res.userId))
      
      ElMessage.success(`欢迎回来，${res.realName}`)
      
      // 根据角色跳转到对应首页
      const rolePath = res.role === 1 ? '/admin' : res.role === 2 ? '/principal' : '/teacher'
      router.push(rolePath)
    } catch (e: any) {
      ElMessage.error(e.message || '登录失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-container {
  display: flex;
  width: 900px;
  min-height: 500px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

/* 左侧品牌区域 */
.login-brand {
  flex: 1;
  background: linear-gradient(135deg, var(--color-primary) 0%, #66B1FF 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.brand-content {
  text-align: center;
  color: white;
}

.brand-icon {
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  font-size: 40px;
}

.brand-title {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 12px;
}

.brand-subtitle {
  font-size: 16px;
  opacity: 0.9;
}

/* 右侧登录表单 */
.login-form-wrapper {
  flex: 1;
  padding: 60px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-header {
  margin-bottom: 40px;
  
  h2 {
    font-size: 28px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 8px;
  }
  
  p {
    font-size: 14px;
    color: var(--text-secondary);
  }
}

.login-form {
  :deep(.el-form-item) {
    margin-bottom: 24px;
  }
  
  :deep(.el-input__wrapper) {
    border-radius: 8px;
  }
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 8px;
}

/* 注册链接 */
.register-link {
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
  color: var(--text-secondary);
  
  .link {
    color: var(--color-primary);
    text-decoration: none;
    font-weight: 500;
    margin-left: 4px;
    
    &:hover {
      text-decoration: underline;
    }
  }
}

/* 响应式调整 */
@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
    width: 100%;
    min-height: auto;
  }
  
  .login-brand {
    padding: 30px;
  }
  
  .login-form-wrapper {
    padding: 30px 20px;
  }
}
</style>
