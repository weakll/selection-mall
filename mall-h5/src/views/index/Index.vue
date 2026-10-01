<template>
  <div class="index-page">
    <!-- 顶部搜索栏 -->
    <div class="search-header">
      <div class="brand">
        <img src="/static/temp/h1.png" alt="logo" class="logo" />
        <span class="brand-name">精选商城</span>
      </div>
      <div class="search-box" @click="$router.push('/product/list')">
        <van-icon name="search" size="16" color="#999" />
        <span>搜索商品</span>
      </div>
    </div>

    <!-- Banner 轮播 -->
    <div class="banner-wrap">
      <van-swipe class="banner" :autoplay="4000" indicator-color="white" :height="180">
        <van-swipe-item v-for="i in 3" :key="i">
          <img :src="`/static/temp/banner${i}.jpg`" alt="banner" class="banner-img" />
        </van-swipe-item>
      </van-swipe>
    </div>

    <!-- 分类宫格 -->
    <div class="category-section sp-card">
      <div class="cat-grid">
        <div
          v-for="cat in categoryList.slice(0, 8)"
          :key="cat.id"
          class="cat-item"
          @click="goCategory(cat.id)"
        >
          <div class="cat-img-wrap">
            <img :src="cat.imageUrl || '/static/errorImage.jpg'" alt="" />
          </div>
          <span class="cat-name">{{ cat.name }}</span>
        </div>
      </div>
    </div>

    <!-- 热销推荐 -->
    <div class="hot-section">
      <div class="section-header">
        <div class="sp-section-title">
          <van-icon name="fire" color="#ff6b00" />
          热销推荐
        </div>
        <span class="sub">精选好物，限时特惠</span>
      </div>
      <div class="product-grid">
        <div
          v-for="item in productList"
          :key="item.id"
          class="product-card animate-fade-in"
          @click="$router.push(`/product/${item.id}`)"
        >
          <div class="img-wrap">
            <img :src="getImageUrl(item)" :alt="item.skuName" @error="onImgError" />
            <div v-if="item.saleNum > 100" class="tag">热卖</div>
          </div>
          <div class="content">
            <div class="name">{{ item.skuName }}</div>
            <div class="meta">
              <span class="sp-price">{{ item.salePrice }}</span>
              <span class="stock" :class="{ out: (item.stockNum || 0) <= 0 }">
                {{ (item.stockNum || 0) > 0 ? `库存 ${item.stockNum}` : '缺货' }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getIndexData } from '../../api/product.js'

const router = useRouter()
const categoryList = ref([])
const productList = ref([])

onMounted(async () => {
  try {
    const data = await getIndexData()
    categoryList.value = data.categoryList || []
    productList.value = data.productSkuList || []
    console.log('首页商品数据:', productList.value)
  } catch (e) {
    console.error(e)
  }
})

// 处理图片URL：如果是相对路径则拼上MinIO基础地址
const getImageUrl = (item) => {
  const localImages = {
    1: '/static/products/phone-main.jpg',
    2: '/static/products/laptop-main.jpg',
    3: '/static/products/watch-main.jpg',
    4: '/static/products/shoe-main.jpg'
  }
  if (localImages[item.productId]) return localImages[item.productId]
  const url = item.thumbImg
  if (!url) return '/static/errorImage.jpg'
  if (url.startsWith('http')) return url
  // 相对路径，需要拼接（根据实际情况修改）
  return url
}

const onImgError = (e) => {
  e.target.src = '/static/errorImage.jpg'
}

const goCategory = (id) => {
  router.push({ path: '/category', query: { id } })
}
</script>

<style scoped>
.index-page {
  padding-bottom: calc(var(--tabbar-height) + 20px);
}

/* 搜索头部 */
.search-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: var(--bg-card);
}

.brand {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.logo {
  width: 28px;
  height: 28px;
  border-radius: 50%;
}

.brand-name {
  font-size: 16px;
  font-weight: 800;
  background: var(--mall-primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--bg-page);
  padding: 8px 14px;
  border-radius: var(--radius-xl);
  color: var(--text-tertiary);
  font-size: 13px;
}

/* Banner */
.banner-wrap {
  padding: 0 12px;
}

.banner {
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 分类 */
.category-section {
  margin: 12px;
  padding: 16px 8px;
}

.cat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px 4px;
}

.cat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.cat-img-wrap {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--bg-page);
  padding: 4px;
}

.cat-img-wrap img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.cat-name {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
}

/* 热销 */
.hot-section {
  padding: 0 12px;
  margin-top: 4px;
}

.section-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}

.sub {
  font-size: 12px;
  color: var(--text-tertiary);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.product-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  transition: transform 0.2s, box-shadow 0.2s;
}

.product-card:active {
  transform: scale(0.98);
}

.img-wrap {
  position: relative;
  width: 100%;
  padding-top: 100%;
  background: #f8f8f8;
}

.img-wrap img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.tag {
  position: absolute;
  top: 8px;
  left: 8px;
  background: var(--mall-primary-gradient);
  color: white;
  font-size: 10px;
  padding: 2px 8px;
  border-radius: var(--radius-xl);
  font-weight: 700;
}

.content {
  padding: 10px;
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
  min-height: 39px;
}

.meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.stock {
  font-size: 11px;
  color: var(--mall-success);
}

.stock.out {
  color: var(--mall-danger);
}
</style>
