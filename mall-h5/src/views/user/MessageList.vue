<template>
  <div class="message-page">
    <van-nav-bar title="消息通知" left-arrow fixed placeholder @click-left="$router.back()">
      <template #right>
        <span class="read-all" @click="markAllRead">全部已读</span>
      </template>
    </van-nav-bar>

    <div class="msg-list">
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="msg-card"
        :class="{ unread: !isRead(msg.id) }"
        @click="readMsg(msg)"
      >
        <div class="msg-icon" :style="{ background: getIconBg(msg.type) }">
          <van-icon :name="getIconName(msg.type)" size="20" color="#fff" />
        </div>
        <div class="msg-body">
          <div class="msg-header">
            <span class="msg-title">{{ msg.title }}</span>
            <span class="msg-time">{{ msg.time }}</span>
          </div>
          <div class="msg-content">{{ msg.content }}</div>
        </div>
        <div v-if="!isRead(msg.id)" class="unread-dot"></div>
      </div>
    </div>

    <div v-if="allRead" class="all-read-tip">— 没有更多消息了 —</div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'

const router = useRouter()
const READ_KEY = 'mall_msg_read'

// 模拟消息数据
const messages = ref([
  {
    id: 1,
    type: 'system',
    title: '系统公告',
    content: '欢迎来到精选商城！新用户注册即送3张优惠券，全场通用，快来选购心仪商品吧~',
    time: '今天 09:30',
    link: '/coupon'
  },
  {
    id: 2,
    type: 'order',
    title: '订单发货',
    content: '您购买的商品已发货，物流单号：SF1234567890，请注意查收。',
    time: '昨天 14:20',
    link: '/order/list'
  },
  {
    id: 3,
    type: 'activity',
    title: '限时活动',
    content: '618年中大促火热进行中！全场满199减50，更多好物低至5折起。',
    time: '昨天 10:00',
    link: '/index'
  },
  {
    id: 4,
    type: 'system',
    title: '优惠券到账',
    content: '新人专属优惠券已发放到您的账户，包含5元、10元现金券及满50减15券。',
    time: '06-05 16:45',
    link: '/coupon'
  },
  {
    id: 5,
    type: 'order',
    title: '订单签收',
    content: '您的订单已完成签收，如对商品满意，欢迎留下好评。',
    time: '06-04 11:30',
    link: '/order/list'
  },
  {
    id: 6,
    type: 'activity',
    title: '会员福利',
    content: '恭喜您升级为黄金会员！尊享专属折扣、生日礼遇及优先发货特权。',
    time: '06-03 09:00',
    link: null
  }
])

const readIds = ref(new Set(getReadIds()))

function getReadIds() {
  try {
    return JSON.parse(localStorage.getItem(READ_KEY)) || []
  } catch {
    return []
  }
}

function saveReadIds() {
  localStorage.setItem(READ_KEY, JSON.stringify([...readIds.value]))
}

const isRead = (id) => readIds.value.has(id)

const allRead = computed(() => messages.value.every(m => isRead(m.id)))

const readMsg = (msg) => {
  if (!readIds.value.has(msg.id)) {
    readIds.value.add(msg.id)
    saveReadIds()
  }
  if (msg.link) {
    router.push(msg.link)
  }
}

const markAllRead = () => {
  messages.value.forEach(m => readIds.value.add(m.id))
  saveReadIds()
  showToast({ message: '已全部标为已读', icon: 'success' })
}

const getIconName = (type) => {
  const map = { system: 'bullhorn-o', order: 'logistics', activity: 'gift-o' }
  return map[type] || 'bell-o'
}

const getIconBg = (type) => {
  const map = {
    system: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    order: 'linear-gradient(135deg, #ff6b00 0%, #ff9500 100%)',
    activity: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
  }
  return map[type] || '#999'
}
</script>

<style scoped>
.message-page {
  background: var(--bg-page);
  min-height: 100vh;
}

.read-all {
  font-size: 13px;
  color: #ff6600;
}

.msg-list {
  padding: 12px;
}

.msg-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  margin-bottom: 10px;
  position: relative;
  cursor: pointer;
  transition: background 0.2s;
}

.msg-card.unread {
  background: #fff8f0;
}

.msg-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.msg-body {
  flex: 1;
  min-width: 0;
}

.msg-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.msg-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.msg-time {
  font-size: 11px;
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.msg-content {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background: #ff4d4f;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 6px;
}

.all-read-tip {
  text-align: center;
  padding: 30px 0 50px;
  font-size: 13px;
  color: var(--text-tertiary);
}
</style>
