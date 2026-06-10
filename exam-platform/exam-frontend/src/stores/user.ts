/**
 * 用户全局状态管理（Pinia Store）
 * 存储当前登录用户信息、角色、权限列表
 * 当前阶段使用 mock 数据，后续接入登录接口
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 用户信息类型
interface UserInfo {
  id: string
  name: string
  role: 'teacher' | 'principal' | 'admin' // 角色编码
  schoolId: string
  schoolName: string
  scope: string      // 'ALL' | 'PROVINCE' | ''
  province: string   // 管理员所属省份
}

export const useUserStore = defineStore('user', () => {
  // ---- 状态 ----
  const userInfo = ref<UserInfo | null>(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const token = ref<string>(localStorage.getItem('token') || '')

  // ---- 计算属性 ----
  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role)

  // ---- 方法 ----
  /**
   * 设置登录 Token
   * TODO: 后续接入实际登录接口
   */
  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  /**
   * 设置用户信息
   */
  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
    localStorage.setItem('userRole', info.role)
  }

  /**
   * 退出登录，清除所有状态
   */
  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('userRole')
    window.location.href = '/'
  }

  return {
    userInfo,
    token,
    isLoggedIn,
    role,
    setToken,
    setUserInfo,
    logout,
  }
})
