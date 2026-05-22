import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { CLIENT_ID, TOKEN_KEY } from '@/constants'
import type { ApiResponse } from '@/types/api'

const http: AxiosInstance = axios.create({
  baseURL: '/dev-api',
  timeout: 30000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers.clientid = CLIENT_ID
  return config
})

http.interceptors.response.use(
  (response: AxiosResponse) => {
    const payload = response.data as ApiResponse<unknown> & { rows?: unknown[]; total?: number }

    if (Array.isArray(payload.rows)) {
      return response
    }

    if (payload.code !== undefined && payload.code !== 200) {
      ElMessage.error(payload.msg || '请求失败')
      return Promise.reject(new Error(payload.msg || '请求失败'))
    }

    return response
  },
  (error) => {
    const message = error.response?.data?.msg || error.message || '网络错误'
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login'
      }
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  },
)

export function unwrap<T>(response: AxiosResponse<ApiResponse<T>>): T {
  return response.data.data
}

export default http
