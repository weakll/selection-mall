<template>
  <div class="collect-page">
    <van-nav-bar title="我的收藏" left-arrow fixed placeholder @click-left="$router.back()" />

    <div v-if="loading" class="loading-wrap">
      <van-loading type="spinner" color="#ff6600" />
    </div>

    <div v-else-if="!list.length" class="empty-wrap">
      <van-empty description="暂无收藏商品" />
      <van-button round type="primary" color="#ff6600" class="go-btn" @click="$router.push('/index')">
        去逛逛
      </van-button>
    </div>

    <div v-else class="goods-list">
      <div
        v-for="item in list"
        :key="item.id"
        class="goods-item"
        @click="$router.push(`/product/${item.skuId}`)"
      >
        <div class="img-box">
          <img
            v-if="item.productSku?.thumbImg"
            :src="item.productSku.thumbImg"
            alt=""
            @error="$event.target.style.display='none'"
          />
          <div v-else class="img-placeholder">
            <van-icon name="photo-o" size="32" color="#ccc" />
          </div>
        </div>
        <div class="info-box">
          <div class="name">{{ item.productSku?.skuName || '商品已下架' }}</div>
          <div class="price-row">
            <span class="price">¥{{ item.productSku?.salePrice || '-' }}</span>
            <span class="market" v-if="item.productSku?.marketPrice">¥{{ item.productSku.marketPrice }}</span>
          </div>
          <div class="time">收藏于 {{ formatDate(item.createTime) }}</div>
        </div>
        <div class="action-box">
          <van-button
            size="small"
            round
            plain
            color="#ff6600"
            @click.stop="onCancel(item.skuId)"
          >
            取消收藏
          </van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { showToast, showConfirmDialog } from 'vant'
import { getCollectList, cancelCollect } from '../../api/user.js'

const list = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const data = await getCollectList()
    list.value = data || []
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

const onCancel = async (skuId) => {
  try {
    await showConfirmDialog({ title: '提示', message: '确定取消收藏该商品？' })
    await cancelCollect(skuId)
    list.value = list.value.filter(item => item.skuId !== skuId)
    showToast({ message: '已取消收藏', icon: 'success' })
  } catch (e) {
    // 用户取消对话框不处理
  }
}
</script>

<style scoped>
.collect-page {
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

.goods-list {
  padding: 12px;
}

.goods-item {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 12px;
  margin-bottom: 10px;
}

.img-box {
  width: 88px;
  height: 88px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  flex-shrink: 0;
  background: #f0f0f0;
}

.img-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.info-box {
  flex: 1;
  min-width: 0;
}

.name {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 8px;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 4px;
}

.price {
  font-size: 16px;
  color: var(--mall-primary);
  font-weight: 700;
}

.market {
  font-size: 12px;
  color: var(--text-tertiary);
  text-decoration: line-through;
}

.time {
  font-size: 11px;
  color: var(--text-tertiary);
}

.action-box {
  flex-shrink: 0;
}
</style>
