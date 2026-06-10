<!--
  UserManage.vue — 管理后台：用户管理页
  管理员查看、创建、禁用用户账号
  设计风格：现代简约 + 清新蓝白配
-->
<template>
  <div class="user-manage-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h2>用户管理</h2>
        <p class="subtitle">管理校长和老师账号</p>
      </div>
      <el-button type="primary" @click="openCreateDialog" class="create-btn">
        <el-icon><Plus /></el-icon>
        创建账号
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select v-model="filterProvince" placeholder="选择省份" clearable @change="onFilterProvinceChange" style="width: 140px">
        <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
      </el-select>
      <el-select v-model="filterCity" placeholder="选择城市" clearable :disabled="!filterProvince" @change="fetchUsers" style="width: 140px">
        <el-option v-for="c in filterCities" :key="c" :label="c" :value="c" />
      </el-select>
      <el-select v-model="filterRole" placeholder="选择角色" clearable @change="fetchUsers" style="width: 120px">
        <el-option label="管理员" :value="1" />
        <el-option label="校长" :value="2" />
        <el-option label="老师" :value="3" />
      </el-select>
      <el-input
        v-model="filterKeyword"
        placeholder="搜索姓名或用户名"
        clearable
        @clear="fetchUsers"
        @keyup.enter="fetchUsers"
        style="width: 200px"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button @click="fetchUsers">搜索</el-button>
    </div>

    <!-- 用户表格 -->
    <div class="table-card">
      <el-table :data="users" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="100" show-overflow-tooltip />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small">
              {{ roleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权限范围" width="120">
          <template #default="{ row }">
            <span v-if="row.role === 1">
              {{ row.scope === 'PROVINCE' ? row.province : '全国' }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="school" label="学校" min-width="150" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="light">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              :type="row.status === 1 ? 'warning' : 'success'"
              link
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="info" link size="small" @click="resetPassword(row)">
              重置密码
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchUsers"
        />
      </div>
    </div>

    <!-- 创建用户对话框 -->
    <el-dialog v-model="dialogVisible" title="创建账号" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入登录用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入初始密码" show-password />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="form.role" @change="onRoleChange">
            <el-radio v-if="isSuperAdmin" :value="1">管理员</el-radio>
            <el-radio :value="2">校长</el-radio>
            <el-radio :value="3">老师</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 管理员专属：权限范围 -->
        <template v-if="form.role === 1">
          <el-form-item label="权限范围">
            <el-radio-group v-model="form.scope">
              <el-radio value="ALL">全国</el-radio>
              <el-radio value="PROVINCE">省级</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="所属省份" v-if="form.scope === 'PROVINCE'">
            <el-select v-model="form.province" placeholder="选择省份" style="width: 100%">
              <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
            </el-select>
          </el-form-item>
        </template>

        <el-form-item label="学校" v-if="form.role !== 1">
          <div class="school-select">
            <el-select
              v-model="selectedProvince"
              placeholder="选择省份"
              clearable
              style="width: 30%"
              @change="onProvinceChange"
            >
              <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
            </el-select>
            <el-select
              v-model="selectedCity"
              placeholder="选择城市"
              clearable
              :disabled="!selectedProvince"
              style="width: 30%"
              @change="onCityChange"
            >
              <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
            </el-select>
            <el-select
              v-model="form.schoolId"
              placeholder="选择学校"
              clearable
              filterable
              :disabled="!selectedCity"
              style="width: 40%"
            >
              <el-option
                v-for="school in schools"
                :key="school.id"
                :label="school.name"
                :value="school.id"
              />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号（选填）" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="submitting">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { get, post, put } from '@/api/index'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
// 当前用户是否为超级管理员（可创建所有角色）
const isSuperAdmin = userStore.userInfo?.scope === 'ALL'

// 用户数据
const users = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 筛选
const filterProvince = ref('')
const filterCity = ref('')
const filterCities = ref<string[]>([])
const filterRole = ref<number | null>(null)
const filterKeyword = ref('')

// 对话框
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  username: '',
  password: '123456',
  realName: '',
  role: 3,
  scope: 'ALL',
  province: '',
  schoolId: null as number | null,
  phone: '',
  email: ''
})

// 学校选择
const provinces = ref<string[]>([])
const cities = ref<string[]>([])
const schools = ref<any[]>([])
const selectedProvince = ref('')
const selectedCity = ref('')

// 表单验证规则
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

// 角色映射
function roleLabel(role: number): string {
  const map: Record<number, string> = { 1: '管理员', 2: '校长', 3: '老师' }
  return map[role] || '未知'
}

function roleTagType(role: number): 'danger' | 'success' | 'primary' {
  const map: Record<number, 'danger' | 'success' | 'primary'> = { 1: 'danger', 2: 'success', 3: 'primary' }
  return map[role] || 'info'
}

// 角色切换时重置相关字段
function onRoleChange() {
  if (form.role === 1) {
    form.schoolId = null
    selectedProvince.value = ''
    selectedCity.value = ''
  } else {
    form.scope = 'ALL'
    form.province = ''
  }
}

// 地区筛选省份变化
function onFilterProvinceChange() {
  filterCity.value = ''
  if (filterProvince.value) {
    get<string[]>(`/school/cities?province=${encodeURIComponent(filterProvince.value)}`).then(res => {
      filterCities.value = res
    })
  } else {
    filterCities.value = []
  }
  fetchUsers()
}

// 加载用户列表
async function fetchUsers() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: pageSize.value }
    if (filterRole.value) params.role = filterRole.value
    if (filterKeyword.value) params.keyword = filterKeyword.value
    if (filterProvince.value) params.province = filterProvince.value
    if (filterCity.value) params.city = filterCity.value

    const res = await get('/user/list', params)
    users.value = res.records
    total.value = Number(res.total)
  } catch (e: any) {
    ElMessage.error(e.message || '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

// 加载省份列表
async function fetchProvinces() {
  try {
    provinces.value = await get('/school/provinces')
  } catch (e) {
    console.error('加载省份列表失败', e)
  }
}

// 省份变化
async function onProvinceChange() {
  selectedCity.value = ''
  form.schoolId = null
  cities.value = []
  schools.value = []
  
  if (selectedProvince.value) {
    try {
      cities.value = await get('/school/cities', { province: selectedProvince.value })
    } catch (e) {
      console.error('加载城市列表失败', e)
    }
  }
}

// 城市变化
async function onCityChange() {
  form.schoolId = null
  schools.value = []
  
  if (selectedCity.value) {
    try {
      schools.value = await get('/school/list', { province: selectedProvince.value, city: selectedCity.value })
    } catch (e) {
      console.error('加载学校列表失败', e)
    }
  }
}

// 打开创建对话框
function openCreateDialog() {
  form.username = ''
  form.password = '123456'
  form.realName = ''
  form.role = 2
  form.scope = 'ALL'
  form.province = ''
  form.schoolId = null
  form.phone = ''
  form.email = ''
  dialogVisible.value = true
}

// 创建用户
async function handleCreate() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      const data: any = {
        username: form.username,
        password: form.password,
        realName: form.realName,
        role: form.role,
        phone: form.phone,
        email: form.email
      }
      // 管理员角色带 scope 和 province
      if (form.role === 1) {
        data.scope = form.scope
        if (form.scope === 'PROVINCE') {
          data.province = form.province
        }
      } else {
        data.schoolId = form.schoolId
      }
      await post('/user', data)
      ElMessage.success('账号创建成功')
      dialogVisible.value = false
      fetchUsers()
    } catch (e: any) {
      ElMessage.error(e.message || '创建失败')
    } finally {
      submitting.value = false
    }
  })
}

// 切换用户状态
async function toggleStatus(user: any) {
  const newStatus = user.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  
  try {
    await ElMessageBox.confirm(`确认${action}用户 ${user.realName}？`, '确认操作')
    await put(`/user/${user.id}/status?status=${newStatus}`)
    ElMessage.success(`已${action}`)
    fetchUsers()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '操作失败')
    }
  }
}

// 重置密码
async function resetPassword(user: any) {
  try {
    await ElMessageBox.confirm(`确认重置 ${user.realName} 的密码为 123456？`, '重置密码')
    await put(`/user/${user.id}/password?newPassword=123456`)
    ElMessage.success('密码已重置为 123456')
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '操作失败')
    }
  }
}

// 初始化
onMounted(() => {
  fetchUsers()
  fetchProvinces()
})
</script>

<style scoped lang="scss">
.user-manage-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
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

.create-btn {
  min-width: 120px;
  
  .el-icon {
    margin-right: 6px;
  }
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px 24px;
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.table-card {
  background: var(--bg-color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 20px;
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.school-select {
  display: flex;
  gap: 8px;
  width: 100%;
}
</style>
