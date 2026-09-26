<template>
  <div class="coupon-page">
    <van-nav-bar title="我的优惠券" left-arrow fixed placeholder @click-left="$router.back()" />

    <div v-if="loading" class="loading-wrap">
      <van-loading type="spinner" color="#ff6600" />
    </div>

    <div v-else-if="!list.length" class="empty-wrap">
      <van-empty description="暂无优惠券" />
      <van-button round type="primary" color="#ff6600" class="go-btn" @click="$router.push('/index')">
        去逛逛
      </van-button>
    </div>

    <div v-else class="coupon-list">
      <div
        v-for="item in list"
        :key="item.id"
        class="coupon-card"
        :class="{ used: item.couponStatus === 2, expired: isExpired(item) }"
      >
        <div class="coupon-left">
          <div class="coupon-amount">
            <span class="symbol">¥</span>
            <span class="num">{{ item.amount }}</span>
          </div>
          <div class="coupon-type">
            {{ item.couponType === 2 ? `满${item.conditionAmount}可用` : '无门槛' }}
          </div>
        </div>
        <div class="coupon-right">
          <div class="coupon-name">{{ item.couponName }}</div>
          <div class="coupon-range">{{ item.rangeDesc || '全场通用' }}</div>
          <div class="coupon-time">有效期至 {{ formatDate(item.expireTime) }}</div>
        </div>
        <div class="coupon-status">
          <span v-if="item.couponStatus === 2" class="status-tag used">已使用</span>
          <span v-else-if="isExpired(item)" class="status-tag expired">已过期</span>
          <span v-else class="status-tag valid">未使用</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { showToast } from 'vant'
import { getUserCouponList, getCouponById } from '../../api/user.js'

const list = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const couponUsers = await getUserCouponList() || []
    const uniqueIds = [...new Set(couponUsers.map(cu => cu.couponId))]
    const details = await Promise.all(
      uniqueIds.map(id => getCouponById(id).catch(() => null))
    )
    const detailMap = new Map()
    details.forEach(d => { if (d) detailMap.set(d.id, d) })

    list.value = couponUsers.map(cu => {
      const info = detailMap.get(cu.couponId)
      return {
        id: cu.id,
        couponId: cu.couponId,
        couponStatus: cu.couponStatus,
        expireTime: cu.expireTime,
        ...info,
      }
    }).filter(item => item.couponName)
  } catch (e) {
    showToast('加载失败')
  } finally {
    loading.value = false
  }
})

const formatDate = (str) => {
  if (!str) return ''
  const d = new Date(str)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const isExpired = (item) => {
  if (!item.expireTime) return false
  return new Date(item.expireTime) < new Date()
}
</script>

<style scoped>
.coupon-page {
  background: var(--bg-page);
  min-height: 100vh;
}

.loading-wrap {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
}

.go-btn {
  margin-top: 16px;
  width: 120px;
}

.coupon-list {
  padding: 12px;
}

.coupon-card {
  display: flex;
  align-items: center;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  margin-bottom: 10px;
  overflow: hidden;
  position: relative;
}

.coupon-left {
  width: 100px;
  padding: 16px 0;
  text-align: center;
  background: linear-gradient(135deg, #ff6b00 0%, #ff9500 100%);
  color: white;
  flex-shrink: 0;
}

.coupon-amount .symbol {
  font-size: 14px;
}

.coupon-amount .num {
  font-size: 28px;
  font-weight: 800;
}

.coupon-type {
  font-size: 11px;
  margin-top: 4px;
  opacity: 0.9;
}

.coupon-right {
  flex: 1;
  padding: 12px 14px;
}

.coupon-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.coupon-range {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.coupon-time {
  font-size: 11px;
  color: var(--text-tertiary);
}

.coupon-status {
  padding-right: 14px;
  flex-shrink: 0;
}

.status-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: var(--radius-xl);
}

.status-tag.valid {
  color: #ff6600;
  background: #fff0e0;
}

.status-tag.used {
  color: #999;
  background: #f0f0f0;
}

.status-tag.expired {
  color: #999;
  background: #f0f0f0;
}

.coupon-card.used .coupon-left,
.coupon-card.expired .coupon-left {
  background: linear-gradient(135deg, #ccc 0%, #bbb 100%);
}
</style>
