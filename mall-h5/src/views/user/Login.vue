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
        <span class="tab" :class="{ active: mode === 'login' }" @click="switchMode('login')">登录</span>
        <span class="tab" :class="{ active: mode === 'register' }" @click="switchMode('register')">注册</span>
      </div>

      <div class="input-group">
        <div class="input-wrap">
          <van-icon name="user-circle-o" size="18" color="#999" />
          <input v-model="form.username" type="text" placeholder="请输入手机号" />
        </div>
        <div v-if="mode !== 'forgot'" class="input-wrap">
          <van-icon name="lock" size="18" color="#999" />
          <input v-model="form.password" type="password" placeholder="请输入密码" />
        </div>
        <div v-if="mode === 'forgot'" class="input-wrap">
          <van-icon name="lock" size="18" color="#999" />
          <input v-model="form.password" type="password" placeholder="请输入新密码" />
        </div>
        <div v-if="mode === 'register' || mode === 'forgot'" class="input-row">
          <div class="input-wrap code-input">
            <van-icon name="comment-o" size="18" color="#999" />
            <input v-model="form.code" type="text" maxlength="6" placeholder="请输入验证码" />
          </div>
          <van-button class="code-btn" plain type="primary" :disabled="codeSending" @click="sendCode">
            {{ codeSending ? `${codeCountdown}s` : '获取验证码' }}
          </van-button>
        </div>
      </div>

      <van-button
        class="sp-btn-primary submit-btn"
        block
        round
        :loading="loading"
        @click="mode === 'login' ? onLogin() : mode === 'register' ? onRegister() : onResetPassword()"
      >
        {{ mode === 'login' ? '登 录' : mode === 'register' ? '注 册' : '修改密码' }}
      </van-button>

      <div class="extra">
        <span v-if="mode === 'login'" class="link" @click="switchMode('forgot')">忘记密码？</span>
        <span v-else class="link" @click="switchMode('login')">返回登录</span>
        <span v-if="mode === 'login'" class="link" @click="switchMode('register')">没有账号，立即注册</span>
        <span v-else class="link" @click="switchMode('login')">已有账号，立即登录</span>
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
import { login, register, resetPassword, sendCode as requestSendCode } from '../../api/user.js'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const mode = ref('login')
const codeSending = ref(false)
const codeCountdown = ref(60)
let countdownTimer

const form = reactive({ username: '', password: '', code: '' })

const switchMode = (nextMode) => {
  mode.value = nextMode
}

const sendCode = () => {
  if (!/^1\d{10}$/.test(form.username)) {
    showToast('请输入正确的手机号')
    return
  }
  codeSending.value = true
  codeCountdown.value = 60
  requestSendCode(form.username).then(() => {
    showToast('验证码已发送，请查收短信')
    countdownTimer = window.setInterval(() => {
      codeCountdown.value -= 1
      if (codeCountdown.value <= 0) {
        window.clearInterval(countdownTimer)
        codeSending.value = false
      }
    }, 1000)
  }).catch(() => {
    codeSending.value = false
  })
}

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
  if (!form.username || !form.password || !form.code) {
    showToast('请填写完整信息')
    return
  }
  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      nickName: '用户' + form.username.slice(-4),
      code: form.code,
    })
    showToast('注册成功，请登录')
    mode.value = 'login'
  } catch (e) {}
  loading.value = false
}

const onResetPassword = async () => {
  if (!form.username || !form.password || !form.code) {
    showToast('请填写完整信息')
    return
  }
  loading.value = true
  try {
    await resetPassword({
      username: form.username,
      password: form.password,
      code: form.code,
    })
    showToast('密码修改成功，请登录')
    form.password = ''
    form.code = ''
    mode.value = 'login'
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

.input-row {
  display: flex;
  gap: 10px;
}

.code-input {
  flex: 1;
}

.code-btn {
  width: 104px;
  height: 46px;
  flex-shrink: 0;
  border-radius: var(--radius-md);
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
