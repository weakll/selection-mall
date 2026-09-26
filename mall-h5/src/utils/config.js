// 全局配置管理
const CONFIG_KEY = 'mall_config'

const defaultConfig = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || '',
}

export function getConfig() {
  try {
    const raw = localStorage.getItem(CONFIG_KEY)
    return raw ? { ...defaultConfig, ...JSON.parse(raw) } : { ...defaultConfig }
  } catch {
    return { ...defaultConfig }
  }
}

export function setConfig(partial) {
  const cfg = getConfig()
  const next = { ...cfg, ...partial }
  localStorage.setItem(CONFIG_KEY, JSON.stringify(next))
  return next
}

export function getApiBaseUrl() {
  const cfg = getConfig()
  return cfg.apiBaseUrl || ''
}
