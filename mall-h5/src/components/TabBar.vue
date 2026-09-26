<template>
  <div class="tab-bar">
    <router-link
      v-for="item in tabs"
      :key="item.path"
      :to="item.path"
      class="tab-item"
      :class="{ active: $route.path === item.path }"
    >
      <div class="icon-box">
        <van-icon :name="item.icon" size="22" />
        <div v-if="item.path === '/cart' && cartCount > 0" class="badge">{{ cartCount }}</div>
      </div>
      <span class="label">{{ item.name }}</span>
    </router-link>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCartList } from '../api/cart.js'
import { useUserStore } from '../store/user.js'

const userStore = useUserStore()
const cartCount = ref(0)

const tabs = [
  { name: '首页', path: '/index', icon: 'wap-home-o' },
  { name: '分类', path: '/category', icon: 'apps-o' },
  { name: '购物车', path: '/cart', icon: 'shopping-cart-o' },
  { name: '我的', path: '/user', icon: 'user-o' },
]

onMounted(async () => {
  if (userStore.token) {
    try {
      const list = await getCartList()
      cartCount.value = (list || []).reduce((sum, i) => sum + i.skuNum, 0)
    } catch (e) {}
  }
})
</script>

<style scoped>
.tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: var(--tabbar-height);
  background: var(--tabbar-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  display: flex;
  align-items: center;
  border-top: 1px solid rgba(0, 0, 0, 0.04);
  z-index: 100;
  padding-bottom: env(safe-area-inset-bottom);
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  text-decoration: none;
  font-size: 10px;
  gap: 3px;
  transition: color 0.2s;
  position: relative;
}

.tab-item.active {
  color: var(--mall-primary);
}

.tab-item.active .icon-box {
  transform: scale(1.1);
}

.icon-box {
  position: relative;
  transition: transform 0.2s;
}

.label {
  font-weight: 500;
}

.badge {
  position: absolute;
  top: -6px;
  right: -10px;
  min-width: 16px;
  height: 16px;
  background: var(--mall-danger);
  color: white;
  font-size: 10px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  font-weight: 700;
}
</style>
