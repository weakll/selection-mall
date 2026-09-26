<template>
  <div class="order-page">
    <van-nav-bar title="我的订单" left-arrow fixed placeholder @click-left="$router.back()" />

    <van-tabs v-model:active="active" @change="load" sticky offset-top="46" class="order-tabs">
      <van-tab v-for="t in tabs" :key="t.value" :title="t.name">
        <div class="order-list">
          <div v-for="order in list" :key="order.id" class="order-card sp-card">
            <div class="order-header">
              <span class="order-no">订单号：{{ order.orderNo }}</span>
              <span class="status" :class="'status-' + order.orderStatus">
                {{ statusText(order.orderStatus) }}
              </span>
            </div>

            <div class="order-goods">
              <div v-for="item in order.orderItemList" :key="item.id" class="goods-row">
                <img :src="item.thumbImg" alt="" class="goods-img" />
                <div class="goods-info">
                  <div class="name">{{ item.skuName }}</div>
                  <div class="meta">
                    <span class="sp-price">{{ item.skuPrice }}</span>
                    <span class="num">x{{ item.skuNum }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="order-footer">
              <span class="total-label">合计：</span>
              <span class="total-price">¥{{ order.totalAmount }}</span>
            </div>

            <div class="order-actions" v-if="order.orderStatus === 0">
              <van-button size="small" round plain hairline @click="cancelOrder(order.orderNo)">
                取消订单
              </van-button>
              <van-button size="small" round type="danger" class="pay-btn" @click="pay(order.orderNo)">
                去支付
              </van-button>
            </div>
          </div>

          <van-empty v-if="!loading && !list.length" description="暂无订单" />
        </div>
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { showToast } from 'vant'
import { getOrderList, directPay } from '../../api/order.js'

const active = ref(0)
const list = ref([])
const loading = ref(false)

const tabs = [
  { name: '全部', value: null },
  { name: '待付款', value: 0 },
  { name: '待发货', value: 1 },
  { name: '待收货', value: 2 },
  { name: '已完成', value: 3 },
]

const load = async () => {
  loading.value = true
  try {
    const res = await getOrderList(1, 10, tabs[active.value].value)
    list.value = res.list || []
  } catch (e) {}
  loading.value = false
}

const statusText = (s) => {
  const map = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', '-1': '已取消' }
  return map[s] || '未知'
}

const pay = async (orderNo) => {
  try {
    await directPay(orderNo)
    showToast({ message: '支付成功', icon: 'success' })
    load()
  } catch (e) {}
}

const cancelOrder = (orderNo) => {
  showToast('已取消')
  // 实际调用取消接口
}

onMounted(() => load())
</script>

<style scoped>
.order-page {
  background: var(--bg-page);
  min-height: 100vh;
}

.order-tabs :deep(.van-tabs__wrap) {
  background: var(--bg-card);
}

.order-list {
  padding: 10px;
}

.order-card {
  padding: 14px;
  margin-bottom: 10px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.order-no {
  font-size: 12px;
  color: var(--text-tertiary);
}

.status {
  font-size: 12px;
  font-weight: 600;
}

.status-0 {
  color: var(--mall-primary);
}

.status-1 {
  color: var(--mall-warning);
}

.status-2 {
  color: #2196f3;
}

.status-3 {
  color: var(--mall-success);
}

.goods-row {
  display: flex;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--divider);
}

.goods-row:last-child {
  border-bottom: none;
}

.goods-img {
  width: 70px;
  height: 70px;
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

.name {
  font-size: 13px;
  color: var(--text-primary);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.num {
  font-size: 12px;
  color: var(--text-tertiary);
}

.order-footer {
  text-align: right;
  padding: 10px 0;
  border-top: 1px solid var(--divider);
  margin-top: 4px;
}

.total-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.total-price {
  font-size: 16px;
  color: var(--mall-primary);
  font-weight: 700;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 8px;
}

.pay-btn {
  background: var(--mall-primary-gradient) !important;
  border: none !important;
}
</style>
