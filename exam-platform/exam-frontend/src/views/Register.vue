<!--
  Register.vue — 老师注册页面
  老师填写信息进行注册，注册成功后自动登录
  设计风格：现代简约 + 清新蓝白配
-->
<template>
  <div class="register-page">
    <div class="register-container">
      <!-- 左侧品牌区域 -->
      <div class="register-brand">
        <div class="brand-content">
          <div class="brand-icon">
            <el-icon><Reading /></el-icon>
          </div>
          <h1 class="brand-title">教师培训在线考试平台</h1>
          <p class="brand-subtitle">老师注册入口</p>
        </div>
      </div>

      <!-- 右侧注册表单 -->
      <div class="register-form-wrapper">
        <div class="form-header">
          <h2>老师注册</h2>
          <p>请填写以下信息完成注册</p>
        </div>

        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="register-form"
          label-position="top"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="registerForm.username"
              placeholder="请输入用户名（4-20个字符）"
              prefix-icon="User"
              size="large"
            />
          </el-form-item>

          <el-form-item label="真实姓名" prop="realName">
            <el-input
              v-model="registerForm.realName"
              placeholder="请输入真实姓名"
              prefix-icon="UserFilled"
              size="large"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码（至少6位）"
              prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>

          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="省份" prop="province">
                <el-select
                  v-model="selectedProvince"
                  placeholder="选择省份"
                  size="large"
                  style="width: 100%"
                  @change="onProvinceChange"
                >
                  <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="城市" prop="city">
                <el-select
                  v-model="selectedCity"
                  placeholder="选择城市"
                  size="large"
                  style="width: 100%"
                  :disabled="!selectedProvince"
                  @change="onCityChange"
                >
                  <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="区县" prop="county">
                <el-select
                  v-model="selectedCounty"
                  placeholder="选择区县"
                  size="large"
                  style="width: 100%"
                  :disabled="!selectedCity"
                  @change="onCountyChange"
                >
                  <el-option v-for="co in counties" :key="co" :label="co" :value="co" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="学校" prop="schoolId">
            <el-select
              v-model="registerForm.schoolId"
              placeholder="选择学校"
              size="large"
              style="width: 100%"
              :disabled="!selectedCity"
              filterable
            >
              <el-option v-for="s in schools" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="手机号（选填）" prop="phone">
            <el-input
              v-model="registerForm.phone"
              placeholder="请输入手机号"
              prefix-icon="Phone"
              size="large"
            />
          </el-form-item>

          <el-form-item label="邮箱（选填）" prop="email">
            <el-input
              v-model="registerForm.email"
              placeholder="请输入邮箱"
              prefix-icon="Message"
              size="large"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="register-btn"
              :loading="loading"
              @click="handleRegister"
            >
              注册
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <span>已有账号？</span>
          <router-link to="/login" class="login-link">立即登录</router-link>
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
import { post, get } from '@/api/index'

const router = useRouter()
const userStore = useUserStore()
const registerFormRef = ref<FormInstance>()
const loading = ref(false)

// 注册表单
const registerForm = reactive({
  username: '',
  realName: '',
  password: '',
  confirmPassword: '',
  schoolId: null as number | null,
  phone: '',
  email: ''
})

// 地区选择
const selectedProvince = ref('')
const selectedCity = ref('')
const selectedCounty = ref('')

// 地区和学校列表
const provinces = ref<string[]>([])
const cities = ref<string[]>([])
const counties = ref<string[]>([])
const schools = ref<any[]>([])

// 加载省份列表
onMounted(async () => {
  try {
    const res = await get<string[]>('/school/provinces')
    provinces.value = res
  } catch (e) {
    console.error('获取省份列表失败', e)
  }
})

// 省份变化
async function onProvinceChange() {
  selectedCity.value = ''
  selectedCounty.value = ''
  registerForm.schoolId = null
  cities.value = []
  counties.value = []
  schools.value = []
  
  if (selectedProvince.value) {
    try {
      const res = await get<string[]>('/school/cities', { province: selectedProvince.value })
      cities.value = res
    } catch (e) {
      console.error('获取城市列表失败', e)
    }
  }
}

// 城市变化
async function onCityChange() {
  selectedCounty.value = ''
  registerForm.schoolId = null
  counties.value = []
  schools.value = []
  
  if (selectedCity.value) {
    try {
      const res = await get<string[]>('/school/counties', { province: selectedProvince.value, city: selectedCity.value })
      counties.value = res
      // 同时加载该城市的学校
      await loadSchools()
    } catch (e) {
      console.error('获取区县列表失败', e)
    }
  }
}

// 区县变化
async function onCountyChange() {
  registerForm.schoolId = null
  await loadSchools()
}

// 加载学校列表
async function loadSchools() {
  try {
    const params: any = { province: selectedProvince.value, city: selectedCity.value }
    if (selectedCounty.value) {
      params.county = selectedCounty.value
    }
    const res = await get<any[]>('/school/list', params)
    schools.value = res
  } catch (e) {
    console.error('获取学校列表失败', e)
  }
}

// 表单验证规则
const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度为 4-20 个字符', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' }
  ],
  schoolId: [
    { required: true, message: '请选择学校', trigger: 'change' }
  ]
}

// 注册
async function handleRegister() {
  if (!registerFormRef.value) return
  
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      const res = await post<any>('/auth/register', registerForm)
      
      // 保存 Token 和用户信息
      userStore.setToken(res.token)
      userStore.setUserInfo({
        id: res.userId,
        name: res.realName,
        role: 'teacher',
        schoolId: res.schoolId || '',
        schoolName: res.schoolName || '',
        scope: res.scope || '',
        province: res.province || ''
      })
      
      // 保存用户ID到localStorage
      localStorage.setItem('userId', String(res.userId))
      
      ElMessage.success('注册成功！')
      
      // 跳转到学员端首页
      router.push('/teacher')
    } catch (e: any) {
      ElMessage.error(e.message || '注册失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.register-container {
  display: flex;
  width: 900px;
  min-height: 600px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

/* 左侧品牌区域 */
.register-brand {
  flex: 0 0 300px;
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
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 12px;
}

.brand-subtitle {
  font-size: 16px;
  opacity: 0.9;
}

/* 右侧注册表单 */
.register-form-wrapper {
  flex: 1;
  padding: 40px;
  overflow-y: auto;
}

.form-header {
  margin-bottom: 30px;
  
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

.register-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }
  
  :deep(.el-form-item__label) {
    font-weight: 500;
    color: var(--text-primary);
  }
  
  :deep(.el-input__wrapper) {
    border-radius: 8px;
  }
}

.register-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 8px;
}

.form-footer {
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
  color: var(--text-secondary);
  
  .login-link {
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
  .register-container {
    flex-direction: column;
    width: 100%;
    min-height: auto;
  }
  
  .register-brand {
    flex: 0 0 auto;
    padding: 30px;
  }
  
  .register-form-wrapper {
    padding: 30px 20px;
  }
}
</style>
