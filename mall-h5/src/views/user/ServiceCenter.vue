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
        :disabled="!inputText.trim()"
        @click="sendMessage"
      >
        发送
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { showToast } from 'vant'

const inputText = ref('')
const chatBox = ref(null)

const messages = ref([
  { type: 'receive', content: '您好！欢迎来到精选商城，我是您的专属客服小选，请问有什么可以帮您？' }
])

const faqList = [
  { question: '订单什么时候发货？', answer: '亲，一般情况下订单会在付款后24小时内发货，节假日可能会有延迟，请您耐心等待~' },
  { question: '如何申请退换货？', answer: '亲，我们支持7天无理由退换货。您可以在"我的订单"中找到对应订单，点击"申请售后"即可。' },
  { question: '优惠券怎么使用？', answer: '亲，在确认订单页面可以选择可用的优惠券，系统会自动为您计算减免金额。' },
  { question: '忘记密码怎么办？', answer: '亲，您可以在登录页面点击"忘记密码"，通过手机号验证码重置密码。' },
  { question: '支持哪些支付方式？', answer: '亲，目前我们支持微信支付和支付宝支付，您可以在提交订单时选择支付方式。' }
]

const sendMessage = () => {
  const text = inputText.value.trim()
  if (!text) return

  // 用户消息
  messages.value.push({ type: 'send', content: text })
  inputText.value = ''
  scrollToBottom()

  // 模拟客服回复
  setTimeout(() => {
    const reply = getReply(text)
    messages.value.push({ type: 'receive', content: reply })
    scrollToBottom()
  }, 800)
}

const sendFaq = (answer) => {
  messages.value.push({ type: 'send', content: '我想问一下这个问题' })
  scrollToBottom()
  setTimeout(() => {
    messages.value.push({ type: 'receive', content: answer })
    scrollToBottom()
  }, 600)
}

const getReply = (text) => {
  const lower = text.toLowerCase()
  if (lower.includes('发货') || lower.includes('快递') || lower.includes('物流')) {
    return '亲，订单付款后24小时内发货，您可以在"我的订单"中查看物流信息。'
  }
  if (lower.includes('退') || lower.includes('换') || lower.includes('售后')) {
    return '亲，我们支持7天无理由退换货，请在"我的订单"中申请售后。'
  }
  if (lower.includes('优惠券') || lower.includes('折扣') || lower.includes('满减')) {
    return '亲，新用户注册即送3张优惠券，在确认订单页可以选择使用。'
  }
  if (lower.includes('支付') || lower.includes('付款') || lower.includes('钱')) {
    return '亲，我们支持微信支付和支付宝支付，请放心下单。'
  }
  if (lower.includes(' hello') || lower.includes('你好') || lower.includes('在吗')) {
    return '亲，我在的~ 请问有什么可以帮您？'
  }
  return '亲，收到您的问题，我已记录并会尽快为您处理。如有紧急问题请拨打客服热线 400-123-4567。'
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatBox.value) {
      chatBox.value.scrollTop = chatBox.value.scrollHeight
    }
  })
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
