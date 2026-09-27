<template>
  <div class="trade-page">
    <van-nav-bar title="确认订单" left-arrow fixed placeholder @click-left="$router.back()" />

    <!-- 地址卡片 -->
    <div class="address-card sp-card" @click="$router.push({ path: '/address', query: { from: 'trade' } })">
      <template v-if="address">
        <div class="addr-header">
          <div class="addr-name">
            <van-icon name="location" color="#ff6b00" />
            <span class="name">{{ address.name }}</span>
            <span class="phone">{{ address.phone }}</span>
          </div>
          <van-icon name="arrow" color="#ccc" />
        </div>
        <div class="addr-detail">{{ address.fullAddress }} {{ address.address }}</div>
      </template>
      <template v-else>
        <div class="addr-empty">
          <van-icon name="add-o" size="20" color="#ff6b00" />
          <span>请选择收货地址</span>
          <van-icon name="arrow" color="#ccc" />
        </div>
      </template>
    </div>

    <!-- 商品清单 -->
    <div class="goods-card sp-card">
      <div class="card-title">商品清单</div>
      <div v-for="item in trade.orderItemList" :key="item.skuId" class="goods-item">
        <img :src="item.thumbImg" alt="" class="goods-img" />
        <div class="goods-info">
          <div class="name">{{ item.skuName }}</div>
          <div class="bottom">
            <span class="sp-price">{{ item.skuPrice }}</span>
            <span class="num">x{{ item.skuNum }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 优惠券 -->
    <div class="coupon-card sp-card" @click="openCouponSheet">
      <div class="coupon-row">
        <span>优惠券</span>
        <span class="coupon-val" :class="{ active: selectedCoupon }">
          <template v-if="selectedCoupon">
            -¥{{ selectedCoupon.amount }}
          </template>
          <template v-else-if="availableCoupons.length">
            {{ availableCoupons.length }}张可用
          </template>
          <template v-else>
            暂无可用
          </template>
          <van-icon name="arrow" size="12" color="#999" />
        </span>
      </div>
    </div>

    <!-- 金额明细 -->
    <div class="amount-card sp-card">
      <div class="amount-row">
        <span>商品总额</span>
        <span class="val">¥{{ originalAmount }}</span>
      </div>
      <div class="amount-row" v-if="couponDiscount > 0">
        <span>优惠券</span>
        <span class="val" style="color: #ff6600;">-¥{{ couponDiscount }}</span>
      </div>
      <div class="amount-row">
        <span>运费</span>
        <span class="val free">免运费</span>
      </div>
      <div class="amount-divider"></div>
      <div class="amount-row total">
        <span>实付金额</span>
        <span class="val sp-price">¥{{ payAmount }}</span>
      </div>
    </div>

    <!-- 底部提交 -->
    <van-submit-bar
      :price="payAmount * 100"
      button-text="提交订单"
      :loading="submitting"
      @submit="onSubmit"
      class="trade-submit"
    >
      <template #default>
        <span class="submit-label">实付：</span>
      </template>
    </van-submit-bar>

    <!-- 优惠券选择弹窗 -->
    <van-action-sheet
      v-model:show="showCouponSheet"
      title="选择优惠券"
      :closeable="true"
    >
      <div class="coupon-list">
        <div
          v-for="c in availableCoupons"
          :key="c.couponId"
          class="coupon-option"
          :class="{ selected: selectedCoupon?.couponId === c.couponId }"
          @click="selectCoupon(c)"
        >
          <div class="coupon-info">
            <div class="coupon-name">{{ c.couponName }}</div>
            <div class="coupon-desc">
              <template v-if="c.couponType === 2">
                满{{ c.conditionAmount }}减{{ c.amount }}
              </template>
              <template v-else>
                无门槛减{{ c.amount }}元
              </template>
            </div>
          </div>
          <div class="coupon-amount">-¥{{ c.amount }}</div>
        </div>
        <div v-if="!availableCoupons.length" class="coupon-empty">暂无可用优惠券</div>
        <div class="coupon-none" @click="selectCoupon(null)">
          <span>不使用优惠券</span>
          <van-icon v-if="!selectedCoupon" name="success" color="#ff6600" />
        </div>
      </div>
    </van-action-sheet>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getTrade, buyNow, submitOrder } from '../../api/order.js'
import { getAddressList, getUserCouponList, getCouponById } from '../../api/user.js'

const route = useRoute()
const router = useRouter()
const trade = ref({})
const address = ref(null)
const couponUsers = ref([])
const couponMap = ref(new Map())
const selectedCoupon = ref(null)
const showCouponSheet = ref(false)
const submitting = ref(false)
const token = localStorage.getItem('token')
const isLogin = !!token
const submitRequestId = createRequestId()

function createRequestId() {
  return globalThis.crypto?.randomUUID?.()
    || `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

// 计算商品原始总额
const originalAmount = computed(() => {
  const list = trade.value.orderItemList || []
  return list.reduce((sum, item) => {
    return sum + (item.skuPrice || 0) * (item.skuNum || 1)
  }, 0)
})

// 计算优惠券减免金额
const couponDiscount = computed(() => {
  if (!selectedCoupon.value) return 0
  const discount = parseFloat(selectedCoupon.value.amount) || 0
  return discount > originalAmount.value ? originalAmount.value : discount
})

// 计算实付金额
const payAmount = computed(() => {
  const pay = originalAmount.value - couponDiscount.value
  return pay < 0 ? 0 : pay
})

// 可用的优惠券（未使用且满足门槛）
const availableCoupons = computed(() => {
  return couponUsers.value
    .filter(cu => cu.couponStatus === 1)
    .map(cu => {
      const info = couponMap.value.get(cu.couponId)
      return info ? { ...info, couponUserId: cu.id } : null
    })
    .filter(Boolean)
    .filter(info => {
      // 满减券需要满足门槛
      if (info.couponType === 2) {
        const condition = parseFloat(info.conditionAmount) || 0
        return originalAmount.value >= condition
      }
      return true
    })
})

const load = async () => {
  try {
    const skuId = route.query.skuId
    if (skuId) {
      trade.value = await buyNow(skuId)
    } else {
      trade.value = await getTrade()
    }
    const addrList = await getAddressList()
    address.value = addrList.find((a) => a.isDefault === 1) || addrList[0] || null

    // 加载优惠券
    if (isLogin) {
      const list = await getUserCouponList()
      couponUsers.value = list || []
      // 并行获取优惠券详情
      const uniqueIds = [...new Set(couponUsers.value.map(cu => cu.couponId))]
      const details = await Promise.all(
        uniqueIds.map(id => getCouponById(id).catch(() => null))
      )
      details.forEach(d => {
        if (d) couponMap.value.set(d.id, d)
      })
    }
  } catch (e) {
    console.error('加载订单数据失败:', e)
  }
}

onMounted(() => load())

import { onActivated } from 'vue'
onActivated(() => {
  load()
})

const openCouponSheet = () => {
  if (!isLogin) {
    showToast('请先登录')
    return
  }
  showCouponSheet.value = true
}

const selectCoupon = (coupon) => {
  selectedCoupon.value = coupon
  showCouponSheet.value = false
}

const onSubmit = async () => {
  if (!address.value) {
    showToast('请选择收货地址')
    return
  }
  if (submitting.value) {
    return
  }
  submitting.value = true
  try {
    const params = {
      requestId: submitRequestId,
      orderItemList: trade.value.orderItemList,
      userAddressId: address.value.id,
      feightFee: 0,
      remark: '',
      couponId: selectedCoupon.value?.couponId || null,
      couponAmount: couponDiscount.value || 0,
    }
    await submitOrder(params)
    showToast({ message: '下单成功', icon: 'success' })
    router.replace('/order/list')
  } catch (e) {
    console.error('提交订单失败:', e)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.trade-page {
  padding: 10px 12px calc(var(--tabbar-height) + 60px);
  background: var(--bg-page);
  min-height: 100vh;
}

/* 地址 */
.address-card {
  padding: 16px;
  margin-bottom: 10px;
}

.addr-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.addr-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.addr-name .name {
  font-size: 15px;
  font-weight: 700;
}

.addr-name .phone {
  font-size: 13px;
  color: var(--text-tertiary);
}

.addr-detail {
  font-size: 13px;
  color: var(--text-secondary);
  padding-left: 24px;
  line-height: 1.5;
}

.addr-empty {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--text-secondary);
  font-size: 14px;
}

/* 商品 */
.goods-card {
  padding: 16px;
  margin-bottom: 10px;
}

.card-title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 14px;
}

.goods-item {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--divider);
}

.goods-item:last-child {
  border-bottom: none;
}

.goods-img {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  background: var(--bg-page);
}

.goods-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.goods-info .name {
  font-size: 13px;
  color: var(--text-primary);
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.num {
  font-size: 13px;
  color: var(--text-tertiary);
}

/* 优惠券 */
.coupon-card {
  padding: 14px 16px;
  margin-bottom: 10px;
  cursor: pointer;
}

.coupon-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: var(--text-primary);
}

.coupon-val {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--text-tertiary);
  font-size: 13px;
}

.coupon-val.active {
  color: #ff6600;
  font-weight: 600;
}

/* 金额 */
.amount-card {
  padding: 16px;
}

.amount-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.amount-row .val {
  color: var(--text-primary);
  font-weight: 500;
}

.amount-row .free {
  color: var(--mall-success);
}

.amount-divider {
  height: 1px;
  background: var(--divider);
  margin: 8px 0;
}

.amount-row.total {
  font-size: 14px;
  font-weight: 700;
}

.amount-row.total .val {
  font-size: 18px;
  color: var(--mall-primary);
}

.submit-label {
  font-size: 13px;
  color: var(--text-secondary);
}

/* 优惠券弹窗 */
.coupon-list {
  padding: 12px 16px 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.coupon-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  margin-bottom: 10px;
  background: var(--bg-card);
}

.coupon-option.selected {
  border-color: #ff6600;
  background: #fff8f0;
}

.coupon-info {
  flex: 1;
}

.coupon-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.coupon-desc {
  font-size: 12px;
  color: var(--text-tertiary);
}

.coupon-amount {
  font-size: 18px;
  color: #ff6600;
  font-weight: 700;
}

.coupon-empty {
  text-align: center;
  padding: 40px 0;
  color: var(--text-tertiary);
  font-size: 14px;
}

.coupon-none {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 12px;
  border-top: 1px solid var(--divider);
  margin-top: 10px;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
}
</style>
