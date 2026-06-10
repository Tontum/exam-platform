/**
 * Axios 请求封装
 * 统一拦截请求/响应，注入 Token、处理错误
 * 当前阶段：接口位置预留，前端直接使用 mock 数据或后续对接后端
 */

import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'

// 创建 axios 实例，配置基地址和超时
const instance: AxiosInstance = axios.create({
  baseURL: '/api', // 通过 Vite 代理转发到后端网关
  timeout: 15000, // 15 秒超时
  headers: {
    'Content-Type': 'application/json',
  },
})

// ---- 请求拦截器 ----
instance.interceptors.request.use(
  (config) => {
    // 从 localStorage 获取登录 Token，注入请求头
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// ---- 响应拦截器 ----
instance.interceptors.response.use(
  (response: AxiosResponse) => {
    // 后端统一响应格式：{ code: 200, data: {...}, message: 'success' }
    const res = response.data
    if (res.code !== 200) {
      // 业务错误（如参数校验失败），弹出提示
      console.error(`[API Error] ${res.message}`)
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  (error) => {
    // HTTP 错误处理（401 未登录、403 无权限、500 服务器错误等）
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        // Token 过期，清除登录状态，跳转登录页
        localStorage.removeItem('token')
        window.location.href = '/login'
      } else if (status === 403) {
        console.error('[API Error] 无权限访问')
      } else if (status >= 500) {
        console.error('[API Error] 服务器内部错误')
      }
    }
    return Promise.reject(error)
  }
)

export default instance

/**
 * 通用的 GET / POST / PUT / DELETE 请求方法封装
 * 后续各业务模块 API 直接引用这些方法
 */

export function get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
  return instance.get(url, { params, ...config })
}

export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return instance.post(url, data, config)
}

export function put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return instance.put(url, data, config)
}

export function del<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return instance.delete(url, config)
}
