<template>
  <div class="user-page">
    <!-- 用户信息卡片 -->
    <div class="user-header">
      <div class="bg-pattern"></div>
      <div class="user-content">
        <div class="avatar-ring">
          <img :src="userInfo?.avatar || '/static/missing-face.png'" alt="avatar" />
        </div>
        <div class="user-meta">
          <div v-if="userInfo" class="nickname">{{ userInfo.nickName }}</div>
          <div v-else class="nickname" @click="$router.push('/login')">点击登录</div>
          <div class="level" v-if="userInfo">
            <van-icon name="gem" size="12" />
            <span>黄金会员</span>
          </div>
        </div>
        <van-icon name="setting-o" size="20" color="#fff" class="setting" @click="$router.push('/set')" />
      </div>

      <!-- 数据概览 -->
      <div class="stats-bar">
        <div class="stat-item" @click="$router.push('/collect')">
          <div class="stat-num">{{ collectCount }}</div>
          <div class="stat-label">收藏</div>
        </div>
        <div class="stat-item" @click="$router.push('/browse')">
          <div class="stat-num">{{ browseCount }}</div>
          <div class="stat-label">足迹</div>
        </div>
        <div class="stat-item" @click="$router.push('/coupon')">
          <div class="stat-num">{{ couponCount }}</div>
          <div class="stat-label">优惠券</div>
        </div>
      </div>
    </div>

    <!-- 订单入口 -->
    <div class="order-card sp-card">
      <div class="card-header" @click="$router.push('/order/list')">
        <span class="title">我的订单</span>
        <span class="more">
          查看全部 <van-icon name="arrow" size="12" />
        </span>
      </div>
      <div class="order-grid">
        <div
          v-for="(t, i) in orderTabs"
          :key="i"
          class="grid-item"
          @click="$router.push({ path: '/order/list', query: { state: i } })"
        >
          <div class="icon-bg">
            <van-icon :name="t.icon" size="22" color="#fff" />
          </div>
          <span>{{ t.name }}</span>
        </div>
      </div>
    </div>

    <!-- 功能列表 -->
    <div class="menu-card sp-card">
      <div class="menu-item" @click="$router.push('/address')">
        <div class="menu-left">
          <div class="menu-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
            <van-icon name="location-o" size="18" color="#fff" />
          </div>
          <span>收货地址</span>
        </div>
        <van-icon name="arrow" color="#ccc" />
      </div>
      <div class="menu-item" @click="$router.push('/service')">
        <div class="menu-left">
          <div class="menu-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
            <van-icon name="service-o" size="18" color="#fff" />
          </div>
          <span>客服中心</span>
        </div>
        <van-icon name="arrow" color="#ccc" />
      </div>
      <div class="menu-item" @click="$router.push('/message')">
        <div class="menu-left">
          <div class="menu-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
            <van-icon name="bullhorn-o" size="18" color="#fff" />
          </div>
          <span>消息通知</span>
        </div>
        <div class="menu-right">
          <van-badge v-if="unreadMsgCount > 0" :content="unreadMsgCount" color="#ff4d4f" />
          <van-icon name="arrow" color="#ccc" />
        </div>
      </div>
    </div>

    <!-- 退出登录 -->
    <div v-if="userStore.token" class="logout-wrap">
      <van-button block round plain hairline type="danger" @click="onLogout">
        退出登录
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { showConfirmDialog } from 'vant'
import { useUserStore } from '../../store/user.js'
import { getUserInfo, getCollectList, getBrowseHistoryList, getUserCouponList } from '../../api/user.js'

const userStore = useUserStore()
const userInfo = ref(null)
const collectCount = ref(0)
const browseCount = ref(0)
const couponCount = ref(0)
const unreadMsgCount = ref(0)

const orderTabs = [
  { name: '待付款', icon: 'balance-pay' },
  { name: '待发货', icon: 'send-gift-o' },
  { name: '待收货', icon: 'logistics' },
  { name: '已完成', icon: 'comment-o' },
]

const calcUnreadMsg = () => {
  try {
    const readIds = JSON.parse(localStorage.getItem('mall_msg_read')) || []
    const readSet = new Set(readIds)
    // 模拟6条消息
    const total = 6
    unreadMsgCount.value = total - readSet.size
  } catch {
    unreadMsgCount.value = 6
  }
}

onMounted(async () => {
  if (userStore.token) {
    try {
      userInfo.value = await getUserInfo()
    } catch (e) {}
    try {
      const list = await getCollectList()
      collectCount.value = list?.length || 0
    } catch (e) {}
    try {
      const list = await getBrowseHistoryList()
      browseCount.value = list?.length || 0
    } catch (e) {}
    try {
      const list = await getUserCouponList()
      couponCount.value = list?.length || 0
    } catch (e) {}
  }
  calcUnreadMsg()
})

const onLogout = () => {
  showConfirmDialog({ title: '提示', message: '确定退出登录？' })
    .then(() => {
      userStore.logout()
      userInfo.value = null
    })
    .catch(() => {})
}
</script>

<style scoped>
.user-page {
  padding-bottom: calc(var(--tabbar-height) + 20px);
  background: var(--bg-page);
  min-height: 100vh;
}

/* 头部 */
.user-header {
  position: relative;
  background: var(--mall-primary-gradient);
  padding: 20px 16px 50px;
  border-radius: 0 0 var(--radius-xl) var(--radius-xl);
  overflow: hidden;
}

.bg-pattern {
  position: absolute;
  inset: 0;
  opacity: 0.1;
  background-image: radial-gradient(circle at 20% 50%, white 2px, transparent 2px),
    radial-gradient(circle at 80% 30%, white 1.5px, transparent 1.5px);
  background-size: 40px 40px;
}

.user-content {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar-ring {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  padding: 3px;
  background: rgba(255, 255, 255, 0.3);
}

.avatar-ring img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid white;
}

.user-meta {
  flex: 1;
}

.nickname {
  color: white;
  font-size: 18px;
  font-weight: 700;
}

.level {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 10px;
  border-radius: var(--radius-xl);
  color: white;
  font-size: 11px;
}

.setting {
  position: relative;
}

/* 统计 */
.stats-bar {
  position: relative;
  display: flex;
  justify-content: space-around;
  margin-top: 20px;
}

.stat-item {
  text-align: center;
  color: white;
  cursor: pointer;
}

.stat-num {
  font-size: 20px;
  font-weight: 700;
}

.stat-label {
  font-size: 12px;
  opacity: 0.9;
  margin-top: 2px;
}

/* 订单卡片 */
.order-card {
  margin: -30px 12px 12px;
  padding: 16px;
  position: relative;
  z-index: 1;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.title {
  font-size: 15px;
  font-weight: 700;
}

.more {
  font-size: 12px;
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  gap: 2px;
}

.order-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.icon-bg {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--mall-primary-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(255, 107, 0, 0.3);
}

/* 菜单 */
.menu-card {
  margin: 0 12px 12px;
  padding: 4px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
}

.menu-left {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: var(--text-primary);
}

.menu-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.menu-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logout-wrap {
  margin: 20px 12px;
}
</style>
