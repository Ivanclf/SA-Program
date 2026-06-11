import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截器 — 注入 Token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器 — 统一错误处理
request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const { status, data } = error.response || {}
    switch (status) {
      case 400:
        ElMessage.error(data?.message || '请求参数错误')
        break
      case 401:
        localStorage.removeItem('token')
        window.location.href = '/login'
        break
      case 403:
        window.location.href = '/403'
        break
      case 404:
        ElMessage.error('资源不存在')
        break
      default:
        ElMessage.error('服务器错误')
    }
    return Promise.reject(error)
  }
)

export default request
