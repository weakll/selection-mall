<template>
  <div class="detail-page">
    <van-nav-bar title="商品详情" left-arrow fixed placeholder @click-left="$router.back()">
      <template #right>
        <van-icon name="share-o" size="20" color="#333" />
      </template>
    </van-nav-bar>

    <!-- 商品轮播 -->
    <div class="gallery">
      <van-swipe :autoplay="5000" indicator-color="white">
        <van-swipe-item v-for="(url, i) in detail.sliderUrlList" :key="i">
          <img :src="url" :alt="detail.productSku?.skuName || '商品图片'" class="gallery-img" @error="onImageError" />
        </van-swipe-item>
      </van-swipe>
    </div>

    <!-- 价格信息 -->
    <div class="info-panel sp-card">
      <div class="price-row" v-if="detail.productSku">
        <div class="price-main">
          <span class="symbol">¥</span>
          <span class="amount">{{ detail.productSku.salePrice }}</span>
          <span class="original">¥{{ detail.productSku.marketPrice }}</span>
        </div>
        <div class="sales">已售 {{ detail.productSku.saleNum || 0 }}+</div>
      </div>
      <div class="stock-row" v-if="detail.productSku">
        <van-tag :type="detail.productSku.stockNum > 0 ? 'success' : 'danger'" round>
          {{ detail.productSku.stockNum > 0 ? `库存 ${detail.productSku.stockNum} 件` : '暂时缺货' }}
        </van-tag>
      </div>
      <h1 class="title" v-if="detail.productSku">{{ detail.productSku.skuName }}</h1>
      <div class="tags">
        <span class="tag-item">正品保证</span>
        <span class="tag-item">七天无理由</span>
        <span class="tag-item">极速发货</span>
      </div>
    </div>

    <!-- 规格选择 -->
    <div class="spec-panel sp-card" v-if="specList.length">
      <div class="spec-title">选择规格</div>
      <div class="spec-groups">
        <div v-for="(group, gIdx) in specList" :key="gIdx" class="spec-group">
          <div class="group-label">{{ group.name }}</div>
          <div class="spec-options">
            <span
              v-for="(opt, oIdx) in group.options"
              :key="oIdx"
              class="spec-btn"
              :class="{ active: selectedSpecs[gIdx] === oIdx }"
              @click="selectSpec(gIdx, oIdx)"
            >
              {{ opt }}
            </span>
          </div>
        </div>
      </div>
      <div class="selected-text" v-if="selectedSpecText">
        已选：{{ selectedSpecText }}
      </div>
    </div>

    <!-- 详情图 -->
    <div class="detail-imgs" v-if="detail.detailsImageUrlList?.length">
      <div class="section-label">商品详情</div>
      <img v-for="(url, i) in detail.detailsImageUrlList" :key="i" :src="url" alt="商品详情图" @error="onImageError" />
    </div>

    <!-- 底部操作栏 -->
    <van-action-bar>
      <van-action-bar-icon icon="chat-o" text="客服" />
      <van-action-bar-icon icon="cart-o" text="购物车" @click="$router.push('/cart')" />
      <div class="custom-action-icon" @click="onToggleCollect">
        <van-icon
          :name="collected ? 'star' : 'star-o'"
          :color="collected ? '#ff6600' : '#333'"
          size="18"
        />
        <span class="custom-action-text" :style="{ color: collected ? '#ff6600' : '#333' }">{{ collected ? '已收藏' : '收藏' }}</span>
      </div>
      <van-action-bar-button type="warning" text="加入购物车" @click="onAddCart" />
      <van-action-bar-button
        type="danger"
        text="立即购买"
        @click="onBuy"
        :disabled="(detail.productSku?.stockNum || 0) <= 0"
      />
    </van-action-bar>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getProductDetail } from '../../api/product.js'
import { addToCart } from '../../api/cart.js'
import { collect, cancelCollect, isCollected, addBrowseHistory } from '../../api/user.js'

const route = useRoute()
const router = useRouter()
const detail = ref({})
const specList = ref([])
const selectedSpecs = ref([])
const collected = ref(false)
const token = localStorage.getItem('token')
const isLogin = !!token

onMounted(async () => {
  try {
    detail.value = await getProductDetail(route.params.skuId)
    parseSpecList()
    if (isLogin) {
      collected.value = await isCollected(route.params.skuId)
      addBrowseHistory(route.params.skuId).catch(() => {})
    }
  } catch (e) {
    console.error(e)
  }
})

// 解析规格数据，兼容多种后端格式
const parseSpecList = () => {
  const raw = detail.value.specValueList
  if (!raw || !raw.length) return

  // 格式1: 字符串数组 ["红色", "蓝色"]
  if (typeof raw[0] === 'string') {
    specList.value = [{ name: '规格', options: raw }]
    selectedSpecs.value = [0]
    return
  }

  // 格式2: 对象数组 [{ value: "颜色", detail: ["红","蓝"] }]
  if (raw[0]?.value && Array.isArray(raw[0]?.detail)) {
    specList.value = raw.map(item => ({
      name: item.value,
      options: item.detail
    }))
    selectedSpecs.value = raw.map(() => 0)
    return
  }

  // 格式3: 对象数组 [{ name: "颜色", value: ["红","蓝"] }]
  if (raw[0]?.name && Array.isArray(raw[0]?.value)) {
    specList.value = raw.map(item => ({
      name: item.name,
      options: item.value
    }))
    selectedSpecs.value = raw.map(() => 0)
    return
  }

  // 格式4: 简单对象数组 [{ value: "红色" }, { value: "蓝色" }]
  if (raw[0]?.value && typeof raw[0]?.value === 'string') {
    specList.value = [{ name: '规格', options: raw.map(item => item.value) }]
    selectedSpecs.value = [0]
    return
  }

  // 未知格式：直接控制台输出方便调试
  console.warn('未知规格格式:', raw)
}

// 当前选中的规格文本描述
const selectedSpecText = computed(() => {
  if (!specList.value.length) return ''
  const texts = specList.value.map((group, idx) => {
    const optIdx = selectedSpecs.value[idx] ?? 0
    return group.options[optIdx]
  }).filter(Boolean)
  return texts.join('，')
})

const selectSpec = (groupIdx, optIdx) => {
  selectedSpecs.value[groupIdx] = optIdx
}

const onImageError = (event) => {
  if (event.target.src.endsWith('/static/errorImage.jpg')) return
  event.target.src = '/static/errorImage.jpg'
}

const onAddCart = async () => {
  try {
    await addToCart(route.params.skuId, 1)
    showToast({ message: '已加入购物车', icon: 'success' })
  } catch (e) {}
}

const onBuy = () => {
  router.push(`/order/trade?skuId=${route.params.skuId}&buyNow=1`)
}

const onToggleCollect = async () => {
  if (!isLogin) {
    showToast('请先登录')
    router.push('/login')
    return
  }
  try {
    if (collected.value) {
      await cancelCollect(route.params.skuId)
      collected.value = false
      showToast({ message: '已取消收藏', icon: 'success' })
    } else {
      await collect(route.params.skuId)
      collected.value = true
      showToast({ message: '收藏成功', icon: 'success' })
    }
  } catch (e) {
    showToast(e?.message || '操作失败，请检查网络')
    console.error(e)
  }
}
</script>

<style scoped>
.detail-page {
  padding-bottom: calc(var(--tabbar-height) + 20px);
  background: var(--bg-page);
}

.gallery {
  background: var(--bg-card);
}

.gallery-img {
  width: 100%;
  height: 375px;
  object-fit: cover;
}

.info-panel {
  margin: 12px;
  padding: 16px;
}

.price-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 10px;
}

.price-main {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.symbol {
  font-size: 14px;
  color: var(--mall-primary);
  font-weight: 700;
}

.amount {
  font-size: 28px;
  color: var(--mall-primary);
  font-weight: 800;
  line-height: 1;
}

.original {
  font-size: 13px;
  color: var(--text-tertiary);
  text-decoration: line-through;
  margin-left: 6px;
}

.sales {
  font-size: 12px;
  color: var(--text-tertiary);
}

.stock-row {
  margin-bottom: 10px;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.5;
  margin-bottom: 12px;
}

.tags {
  display: flex;
  gap: 8px;
}

.tag-item {
  font-size: 11px;
  color: var(--mall-primary-light);
  background: var(--bg-hover);
  padding: 3px 10px;
  border-radius: var(--radius-sm);
}

/* 规格 */
.spec-panel {
  margin: 0 12px 12px;
  padding: 16px;
}

.spec-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 14px;
}

.spec-groups {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.spec-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.group-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.spec-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.spec-btn {
  padding: 8px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--text-secondary);
  transition: all 0.2s;
  background: var(--bg-card);
}

.spec-btn.active {
  border-color: var(--mall-primary);
  color: var(--mall-primary);
  background: var(--bg-hover);
  font-weight: 600;
}

.selected-text {
  margin-top: 14px;
  font-size: 13px;
  color: var(--text-secondary);
  padding-top: 12px;
  border-top: 1px solid var(--divider);
}

/* 详情图 */
.detail-imgs {
  background: var(--bg-card);
  margin: 0 12px 12px;
  border-radius: var(--radius-md);
  overflow: hidden;
}

.section-label {
  padding: 16px;
  font-size: 15px;
  font-weight: 700;
  border-bottom: 1px solid var(--divider);
}

.detail-imgs img {
  width: 100%;
  display: block;
}

/* 自定义收藏按钮（van-action-bar-icon 不触发 click） */
.custom-action-icon {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-width: 48px;
  height: 100%;
  font-size: 10px;
  text-align: center;
  background: var(--bg-card);
  cursor: pointer;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
}
.custom-action-icon:active {
  opacity: 0.7;
}
.custom-action-text {
  margin-top: 4px;
  font-size: 10px;
}
</style>
