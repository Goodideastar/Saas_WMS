import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken, removeUserInfo } from '@/utils/auth'
import router from '@/router'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000
})

service.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      if (res.code === 401) {
        removeToken()
        removeUserInfo()
        ElMessage.error('登录已过期，请重新登录')
        router.push('/login?expired=1')
        return Promise.reject(new Error('登录已过期'))
      } else if (res.code === 403) {
        ElMessage.error('权限不足，无法执行此操作')
      } else if (!(res.code >= 4001 && res.code <= 4004)) {
        // 4001-4004 为登录流程业务码（验证码/凭据错误），由调用方统一提示
        ElMessage.error(res.message || '请求失败')
      }
      // reject 的错误附带 code/msg，供调用方按业务码分支处理
      return Promise.reject(Object.assign(new Error(res.message || '请求失败'), {
        code: res.code,
        msg: res.message
      }))
    }
    return res
  },
  error => {
    if (error.response) {
      switch (error.response.status) {
        case 401:
          removeToken()
          removeUserInfo()
          ElMessage.error('登录已过期，请重新登录')
          router.push('/login?expired=1')
          break
        case 403:
          ElMessage.error('权限不足')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default service
