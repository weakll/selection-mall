import axios from 'axios'
import { showToast } from 'vant'
import { getApiBaseUrl } from '../utils/config.js'

function createRequestInstance() {
  return axios.create({
    baseURL: getApiBaseUrl(),
    timeout: 15000
  })
}

let request = createRequestInstance()

// 暴露重新创建实例的方法（切换baseUrl后调用）
export function reloadRequest() {
  request = createRequestInstance()
  setupInterceptors()
}

function setupInterceptors() {
  request.interceptors.request.use(config => {
    // 自动补全 /api 前缀（确保能匹配网关路由 /**/product/**）
    if (config.url && !config.url.startsWith('/api')) {
      config.url = '/api' + config.url
    }

    const token = localStorage.getItem('token')
    if (token) {
      config.headers.token = token
    }
    return config
  }, error => Promise.reject(error))

  request.interceptors.response.use(response => {
    const res = response.data
    if (res.code !== 200) {
      showToast(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res.data
  }, error => {
    const message = error.response?.data?.message || error.message
    showToast(message && message !== 'Request failed with status code 500' ? message : '网络异常')
    return Promise.reject(error)
  })
}

setupInterceptors()

export default request
