<template>
  <div class="cart-page">
    <van-nav-bar title="购物车" fixed placeholder>
      <template #right>
        <span class="clear-btn" v-if="list.length" @click="onClear">清空</span>
      </template>
    </van-nav-bar>

    <!-- 未登录 -->
    <div v-if="!userStore.token" class="state-box">
      <div class="state-icon">
        <van-icon name="user-circle-o" size="64" color="#ddd" />
      </div>
      <p class="state-title">登录后查看购物车</p>
      <p class="state-desc">同步电脑与手机购物车中的商品</p>
      <van-button class="sp-btn-primary" round block @click="$router.push('/login')">
        立即登录
      </van-button>
    </div>

    <!-- 空购物车 -->
    <div v-else-if="!list.length" class="state-box">
      <img src="/static/emptyCart.jpg" class="empty-img" alt="empty" />
      <p class="state-title">购物车空空如也</p>
      <p class="state-desc">发现心仪好物，赶紧加入购物车吧</p>
      <van-button class="sp-btn-primary" round block @click="$router.push('/index')">
        去逛逛
      </van-button>
    </div>

    <!-- 购物车列表 -->
    <div v-else class="cart-list">
      <div class="cart-header">
        <van-checkbox :model-value="isAllChecked" @update:model-value="toggleAll" shape="round">
          全选
        </van-checkbox>
        <span class="edit">共 {{ list.length }} 件商品</span>
      </div>

      <van-swipe-cell v-for="item in list" :key="item.id" class="cart-cell">
        <div class="cart-item">
          <van-checkbox
            :model-value="item.isChecked === 1"
            @update:model-value="check(item.skuId, $event ? 1 : 0)"
            shape="round"
          />
          <div class="item-img" @click="$router.push(`/product/${item.skuId}`)">
            <img :src="item.imgUrl || '/static/errorImage.jpg'" alt="" />
          </div>
          <div class="item-info">
            <div class="name">{{ item.skuName }}</div>
            <div class="bottom">
              <span class="sp-price">{{ item.cartPrice }}</span>
              <van-stepper
                v-model="item.skuNum"
                min="1"
                :max="99"
                theme="round"
                button-size="22"
                disable-input
              />
            </div>
          </div>
        </div>
        <template #right>
          <van-button square text="删除" type="danger" class="delete-btn" @click="del(item.skuId)" />
        </template>
      </van-swipe-cell>
    </div>

    <!-- 底部结算 -->
    <van-submit-bar
      v-if="list.length"
      :price="Math.round(totalPrice * 100)"
      button-text="去结算"
      @submit="onSubmit"
      class="cart-submit"
    >
      <van-checkbox :model-value="isAllChecked" @update:model-value="toggleAll" shape="round">
        全选
      </van-checkbox>
    </van-submit-bar>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { useUserStore } from '../../store/user.js'
import { getCartList, deleteCart, checkCart, allCheckCart, clearCart } from '../../api/cart.js'

const router = useRouter()
const userStore = useUserStore()
const list = ref([])

const load = async () => {
  try {
    list.value = (await getCartList()) || []
  } catch (e) {}
}

onMounted(() => {
  if (userStore.token) load()
})

const isAllChecked = computed(
  () => list.value.length > 0 && list.value.every((i) => i.isChecked === 1)
)
const totalPrice = computed(() =>
  list.value
    .filter((i) => i.isChecked === 1)
    .reduce((sum, i) => sum + i.cartPrice * i.skuNum, 0)
)

const check = async (skuId, v) => {
  await checkCart(skuId, v)
  load()
}
const toggleAll = async (v) => {
  await allCheckCart(v ? 1 : 0)
  load()
}
const del = async (skuId) => {
  await deleteCart(skuId)
  load()
}
const onClear = () => {
  showConfirmDialog({ title: '提示', message: '确定清空购物车？' })
    .then(() => {
      clearCart().then(() => {
        showToast('已清空')
        load()
      })
    })
    .catch(() => {})
}
const onSubmit = () => router.push('/order/trade')
</script>

<style scoped>
.cart-page {
  padding-bottom: calc(var(--tabbar-height) + 60px + 50px);
  background: var(--bg-page);
  min-height: 100vh;
}

.clear-btn {
  font-size: 13px;
  color: var(--mall-primary);
}

.state-box {
  text-align: center;
  padding: 60px 40px 0;
}

.state-icon {
  margin-bottom: 16px;
}

.empty-img {
  width: 160px;
  height: 160px;
  object-fit: cover;
  border-radius: var(--radius-md);
  margin-bottom: 16px;
}

.state-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.state-desc {
  font-size: 13px;
  color: var(--text-tertiary);
  margin-bottom: 24px;
}

/* 列表 */
.cart-list {
  padding: 0 12px;
}

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 4px;
  font-size: 13px;
}

.edit {
  color: var(--text-tertiary);
}

.cart-cell {
  margin-bottom: 10px;
  border-radius: var(--radius-md);
  overflow: hidden;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px;
  background: var(--bg-card);
}

.item-img {
  width: 90px;
  height: 90px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  flex-shrink: 0;
  background: var(--bg-page);
}

.item-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 90px;
}

.name {
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

.delete-btn {
  height: 100%;
}

/* 结算栏：抬高避免被TabBar盖住 */
.cart-submit {
  bottom: calc(var(--tabbar-height) + env(safe-area-inset-bottom)) !important;
  z-index: 200 !important;
}

/* Vant SubmitBar 按钮样式覆盖 */
.cart-submit :deep(.van-submit-bar__button) {
  background: var(--mall-primary-gradient) !important;
  border: none !important;
  border-radius: var(--radius-xl) !important;
}
</style>
