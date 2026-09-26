<template>
  <div class="login-page">
    <!-- 顶部装饰 -->
    <div class="login-header">
      <div class="logo-area">
        <img src="/static/temp/h1.png" alt="logo" class="logo" />
        <h1 class="app-name">精选商城</h1>
        <p class="slogan">品质生活，精选好物</p>
      </div>
    </div>

    <!-- 登录表单 -->
    <div class="form-card sp-card">
      <div class="tabs">
        <span class="tab active">登录</span>
        <span class="tab">注册</span>
      </div>

      <div class="input-group">
        <div class="input-wrap">
          <van-icon name="user-circle-o" size="18" color="#999" />
          <input v-model="form.username" type="text" placeholder="请输入手机号" />
        </div>
        <div class="input-wrap">
          <van-icon name="lock" size="18" color="#999" />
          <input v-model="form.password" type="password" placeholder="请输入密码" />
        </div>
      </div>

      <van-button
        class="sp-btn-primary submit-btn"
        block
        round
        :loading="loading"
        @click="onLogin"
      >
        登 录
      </van-button>

      <div class="extra">
        <span>忘记密码？</span>
        <span class="link" @click="onRegister">没有账号，立即注册</span>
      </div>
    </div>

    <!-- 底部 -->
    <div class="login-footer">
      <p>登录即代表您同意《用户协议》和《隐私政策》</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore } from '../../store/user.js'
import { login, register } from '../../api/user.js'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({ username: '', password: '' })

const onLogin = async () => {
  if (!form.username || !form.password) {
    showToast('请填写完整信息')
    return
  }
  loading.value = true
  try {
    const token = await login({ username: form.username, password: form.password })
    userStore.setToken(token)
    showToast({ message: '登录成功', icon: 'success' })
    router.replace('/user')
  } catch (e) {}
  loading.value = false
}

const onRegister = async () => {
  if (!form.username || !form.password) {
    showToast('请填写完整信息')
    return
  }
  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      nickName: '用户' + form.username.slice(-4),
    })
    showToast('注册成功，请登录')
  } catch (e) {}
  loading.value = false
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: var(--bg-page);
  display: flex;
  flex-direction: column;
}

.login-header {
  background: var(--mall-primary-gradient);
  padding: 50px 0 70px;
  border-radius: 0 0 var(--radius-xl) var(--radius-xl);
  text-align: center;
}

.logo-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: white;
}

.logo {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: white;
  padding: 4px;
  margin-bottom: 12px;
}

.app-name {
  font-size: 24px;
  font-weight: 800;
  margin-bottom: 4px;
}

.slogan {
  font-size: 13px;
  opacity: 0.9;
}

.form-card {
  margin: -40px 20px 0;
  padding: 24px;
  position: relative;
  z-index: 1;
}

.tabs {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
}

.tab {
  font-size: 16px;
  color: var(--text-tertiary);
  font-weight: 600;
  padding-bottom: 8px;
  position: relative;
}

.tab.active {
  color: var(--mall-primary);
}

.tab.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 3px;
  background: var(--mall-primary-gradient);
  border-radius: 2px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 24px;
}

.input-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--bg-page);
  padding: 12px 16px;
  border-radius: var(--radius-md);
}

.input-wrap input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 14px;
  outline: none;
  color: var(--text-primary);
}

.submit-btn {
  height: 46px;
  font-size: 16px;
}

.extra {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  font-size: 13px;
  color: var(--text-tertiary);
}

.link {
  color: var(--mall-primary);
}

.login-footer {
  margin-top: auto;
  padding: 20px;
  text-align: center;
  font-size: 11px;
  color: var(--text-tertiary);
}
</style>
