<template>
  <div class="service-page">
    <van-nav-bar title="客服中心" left-arrow fixed placeholder @click-left="$router.back()" />

    <!-- 客服信息卡片 -->
    <div class="service-header sp-card">
      <div class="avatar-box">
        <img src="/static/missing-face.png" alt="客服" />
        <div class="status-dot"></div>
      </div>
      <div class="service-info">
        <div class="name">精选商城客服</div>
        <div class="desc">在线时间 09:00 - 22:00</div>
      </div>
      <div class="contact-btn" @click="callService">
        <van-icon name="phone-o" size="18" color="#ff6600" />
        <span>拨打热线</span>
      </div>
    </div>

    <!-- 常见问题 -->
    <div class="faq-card sp-card">
      <div class="section-title">常见问题</div>
      <div
        v-for="(item, index) in faqList"
        :key="index"
        class="faq-item"
        @click="sendFaq(item.answer)"
      >
        <van-icon name="question-o" size="16" color="#ff6600" />
        <span class="faq-text">{{ item.question }}</span>
        <van-icon name="arrow" size="14" color="#ccc" />
      </div>
    </div>

    <!-- 聊天区域 -->
    <div class="chat-card sp-card">
      <div class="section-title">在线咨询</div>
      <div class="chat-box" ref="chatBox">
        <div
          v-for="(msg, index) in messages"
          :key="index"
          class="msg-row"
          :class="msg.type"
        >
          <template v-if="msg.type === 'receive'">
            <img src="/static/missing-face.png" class="msg-avatar" />
            <div class="msg-bubble receive">
              <div class="msg-content">{{ msg.content }}</div>
            </div>
          </template>
          <template v-else>
            <div class="msg-bubble send">
              <div class="msg-content">{{ msg.content }}</div>
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- 底部输入 -->
    <div class="input-bar">
      <van-field
        v-model="inputText"
        placeholder="请输入您的问题..."
        class="input-field"
        @keyup.enter="sendMessage"
      />
      <van-button
        type="primary"
        size="small"
        color="#ff6600"
        :disabled="!inputText.trim() || sending"
        @click="sendMessage"
      >
        {{ sending ? '回复中' : '发送' }}
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { showToast } from 'vant'
import { getApiBaseUrl } from '../../utils/config.js'

const inputText = ref('')
const chatBox = ref(null)
const sending = ref(false)

const messages = ref([
  { type: 'receive', content: '您好！欢迎来到精选商城，我是您的专属客服小选，请问有什么可以帮您？' }
])

// 常见问题与问题原文都取自后端知识库（service-ai 的 ai/faq-knowledge.yml）。
// 保留快捷入口是为了让用户少打字；即使不点，直接输入同样能命中。
// 增删知识库条目时需同步本列表，否则快捷提问会答非所问。
const faqList = [
  { question: '订单什么时候发货？', prompt: '订单什么时候发货？' },
  { question: '如何申请退换货？', prompt: '我想退货，怎么申请？' },
  { question: '优惠券怎么使用？', prompt: '优惠券怎么使用？' },
  { question: '可以开发票吗？', prompt: '可以开发票吗？' },
  { question: '支持哪些支付方式？', prompt: '支持哪些支付方式？' }
]

/** 读取登录凭据；未登录为 null。后端 /api/ai/auth/** 由网关强制校验。 */
const getToken = () => localStorage.getItem('token')

const scrollToBottom = () => {
  nextTick(() => {
    if (chatBox.value) {
      chatBox.value.scrollTop = chatBox.value.scrollHeight
    }
  })
}

/** 取最后一条回复对象，流式过程中持续往它追加内容。 */
const lastReply = () => {
  const list = messages.value
  return list.length ? list[list.length - 1] : null
}

/**
 * 解析 SSE 字符流。
 *
 * 服务端事件形如：
 *   event:delta
 *   data:{"content":"您"}
 *
 * 这里刻意不复用项目的 axios 封装：axios 走 XHR，无法逐块读取响应体，
 * 用它就只能等整段回答生成完才显示，逐字效果会消失。
 */
const readSseStream = async (response) => {
  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const handleBlock = (block) => {
    let eventName = ''
    const dataLines = []
    for (const line of block.split('\n')) {
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trim())
      }
    }
    if (!dataLines.length) return

    let payload
    try {
      payload = JSON.parse(dataLines.join('\n'))
    } catch {
      return
    }

    if (eventName === 'delta') {
      const reply = lastReply()
      if (reply) reply.content += payload.content || ''
      scrollToBottom()
    } else if (eventName === 'error') {
      const reply = lastReply()
      if (reply) reply.content = payload.message || '抱歉，服务暂时不可用，请稍后再试。'
      scrollToBottom()
    }
    // done 事件仅表示收尾，无需额外处理
  }

  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    let sep = buffer.indexOf('\n\n')
    while (sep !== -1) {
      handleBlock(buffer.slice(0, sep))
      buffer = buffer.slice(sep + 2)
      sep = buffer.indexOf('\n\n')
    }
  }
  // 处理末尾可能残留的事件块
  if (buffer.trim()) handleBlock(buffer)
}

/** 发送一条提问并流式接收回答。 */
const ask = async (text) => {
  const question = (text || '').trim()
  if (!question || sending.value) return

  messages.value.push({ type: 'send', content: question })
  messages.value.push({ type: 'receive', content: '' })
  scrollToBottom()
  sending.value = true

  try {
    const query = new URLSearchParams({ message: question })
    const response = await fetch(`${getApiBaseUrl()}/api/ai/auth/chat/stream?${query}`, {
      headers: { token: getToken() || '' }
    })

    if (response.status === 401 || response.status === 403) {
      const reply = lastReply()
      if (reply) reply.content = '请先登录后再咨询订单相关问题哦～'
      showToast('请先登录')
      return
    }
    if (!response.ok || !response.body) {
      throw new Error(`HTTP ${response.status}`)
    }

    await readSseStream(response)
  } catch (error) {
    const reply = lastReply()
    if (reply && !reply.content) {
      reply.content = '抱歉，客服暂时无法响应，请稍后再试或拨打热线 400-123-4567。'
    }
    console.error('[客服] 对话失败', error)
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

const sendMessage = () => {
  if (sending.value) return
  const text = inputText.value
  inputText.value = ''
  ask(text)
}

const sendFaq = (prompt) => {
  ask(prompt)
}

const callService = () => {
  showToast('客服热线：400-123-4567')
}
</script>

<style scoped>
.service-page {
  background: var(--bg-page);
  min-height: 100vh;
  padding-bottom: 70px;
}

/* 头部 */
.service-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  margin: 10px 12px;
}

.avatar-box {
  position: relative;
  width: 56px;
  height: 56px;
  flex-shrink: 0;
}

.avatar-box img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #ffe4d0;
}

.status-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 12px;
  height: 12px;
  background: #52c41a;
  border-radius: 50%;
  border: 2px solid white;
}

.service-info {
  flex: 1;
}

.name {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.desc {
  font-size: 12px;
  color: var(--text-tertiary);
}

.contact-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  background: #fff5f0;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.contact-btn span {
  font-size: 11px;
  color: #ff6600;
}

/* 常见问题 */
.faq-card {
  margin: 0 12px 10px;
  padding: 14px 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 12px;
  color: var(--text-primary);
}

.faq-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid var(--divider);
  cursor: pointer;
}

.faq-item:last-child {
  border-bottom: none;
}

.faq-text {
  flex: 1;
  font-size: 14px;
  color: var(--text-secondary);
}

/* 聊天 */
.chat-card {
  margin: 0 12px 10px;
  padding: 14px 16px;
}

.chat-box {
  max-height: 320px;
  overflow-y: auto;
  padding: 8px 0;
}

.msg-row {
  display: flex;
  margin-bottom: 14px;
}

.msg-row.receive {
  justify-content: flex-start;
}

.msg-row.send {
  justify-content: flex-end;
}

.msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  margin-right: 10px;
  object-fit: cover;
}

.msg-bubble {
  max-width: 75%;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}

/*
 * 保留模型回答里的换行。AI 回复是多行结构（列表、分段），
 * 默认的 white-space:normal 会把 \n 折叠成空格，整段挤成一坨。
 * 这里用 pre-line 而非 pre-wrap：保留换行、折叠多余空格，
 * 比改模板走 v-html 更安全——模型输出不可信，插 HTML 会有 XSS 风险。
 */
.msg-content {
  white-space: pre-line;
}

.msg-bubble.receive {
  background: #f5f5f5;
  color: var(--text-primary);
  border-top-left-radius: 2px;
}

.msg-bubble.send {
  background: #ff6600;
  color: white;
  border-top-right-radius: 2px;
}

/* 底部输入 */
.input-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: white;
  border-top: 1px solid var(--divider);
  z-index: 100;
}

.input-field {
  flex: 1;
  padding: 8px 12px;
  background: #f5f5f5;
  border-radius: 20px;
  border: none;
}

:deep(.input-field .van-field__control) {
  font-size: 14px;
}
</style>
