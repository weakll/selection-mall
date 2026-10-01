<template>
  <div class="set-page">
    <van-nav-bar title="设置" left-arrow fixed placeholder @click-left="$router.back()" />

    <div class="set-group">
      <div class="group-title">服务器配置</div>
      <div class="set-item">
        <span>API 地址</span>
        <div class="input-wrap">
          <input
            v-model="apiUrl"
            placeholder="默认 /api（走Vite代理）"
            @blur="saveApiUrl"
          />
        </div>
      </div>
      <div class="tip">
        默认使用 /api（需配合 Nginx 代理）<br>
        如需直连后端，填写完整地址：http://127.0.0.1:8500
      </div>
    </div>

    <div class="set-group">
      <div class="group-title">缓存管理</div>
      <div class="set-item" @click="clearCache">
        <span>清除本地缓存</span>
        <van-icon name="arrow" color="#ccc" />
      </div>
      <div class="set-item" @click="clearToken">
        <span>清除登录状态</span>
        <van-icon name="arrow" color="#ccc" />
      </div>
    </div>

    <div class="set-group">
      <div class="group-title">关于</div>
      <div class="set-item">
        <span>当前版本</span>
        <span class="val">v1.0.0</span>
      </div>
      <div class="set-item">
        <span>精选商城</span>
      </div>
    </div>

    <div class="btn-wrap">
      <van-button block round type="danger" plain hairline @click="resetAll">
        恢复默认设置
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import { useRouter } from 'vue-router'
import { getConfig, setConfig } from '../../utils/config.js'
import { reloadRequest } from '../../api/request.js'
import { useUserStore } from '../../store/user.js'

const router = useRouter()
const userStore = useUserStore()
const apiUrl = ref('')

onMounted(() => {
  const cfg = getConfig()
  apiUrl.value = cfg.apiBaseUrl || ''
})

const saveApiUrl = () => {
  const url = apiUrl.value.trim()
  setConfig({ apiBaseUrl: url })
  reloadRequest()
  showToast(url ? `已切换至 ${url}` : '已恢复默认代理')
}

const clearCache = () => {
  showConfirmDialog({ title: '提示', message: '确定清除所有本地缓存？' })
    .then(() => {
      localStorage.removeItem('token')
      showToast('缓存已清除')
    })
    .catch(() => {})
}

const clearToken = () => {
  showConfirmDialog({ title: '提示', message: '确定退出登录？' })
    .then(() => {
      userStore.logout()
      showToast('已退出')
      router.replace('/login')
    })
    .catch(() => {})
}

const resetAll = () => {
  showConfirmDialog({ title: '提示', message: '确定恢复所有默认设置？' })
    .then(() => {
      localStorage.clear()
      apiUrl.value = ''
      reloadRequest()
      showToast('已恢复默认设置，请刷新页面')
    })
    .catch(() => {})
}
</script>

<style scoped>
.set-page {
  min-height: 100vh;
  background: var(--bg-page);
  padding-bottom: 30px;
}

.set-group {
  margin: 12px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.group-title {
  padding: 12px 16px 4px;
  font-size: 12px;
  color: var(--text-tertiary);
  font-weight: 600;
}

.set-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  font-size: 14px;
  color: var(--text-primary);
}

.set-item:not(:last-child) {
  border-bottom: 1px solid var(--divider);
}

.input-wrap {
  flex: 1;
  margin-left: 12px;
  display: flex;
  justify-content: flex-end;
}

.input-wrap input {
  width: 100%;
  max-width: 240px;
  text-align: right;
  border: none;
  background: var(--bg-page);
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--text-secondary);
  outline: none;
}

.tip {
  padding: 0 16px 12px;
  font-size: 11px;
  color: var(--text-tertiary);
  line-height: 1.5;
}

.val {
  color: var(--text-tertiary);
  font-size: 13px;
}

.btn-wrap {
  margin: 24px 12px;
}
</style>
