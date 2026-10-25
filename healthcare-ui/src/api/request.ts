import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'

export const TOKEN_KEY = 'healthcare_token'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 30000
})

service.interceptors.request.use(
  (config: AxiosRequestConfig) => {
    const token = useUserStore().token
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    const payload = response.data
    // 导出接口直接返回二进制流，原样透传
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return payload
    }
    // 统一响应结构 {code, msg, data}：拆包后交给调用方，
    // 与 api 层声明的返回类型（LoginResult / UserInfo / PageResult...）保持一致
    if (payload && typeof payload.code === 'number') {
      if (payload.code !== 200) {
        ElMessage.error(payload.msg || '操作失败')
        if (payload.code === 401) {
          const userStore = useUserStore()
          userStore.reset()
          if (!location.hash.includes('/login')) {
            location.hash = '/login'
          }
        }
        return Promise.reject(new Error(payload.msg || '操作失败'))
      }
      return payload.data
    }
    return payload
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.msg || error.message || '网络异常'
    if (status === 401) {
      useUserStore().reset()
      location.hash = '/login'
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default service
