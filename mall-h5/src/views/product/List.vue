<template>
  <div class="list-page">
    <div class="search-header">
      <van-icon name="arrow-left" size="20" color="#333" @click="$router.back()" />
      <div class="search-box">
        <van-icon name="search" size="16" color="#999" />
        <input v-model="keyword" placeholder="搜索商品" @keyup.enter="onSearch" />
      </div>
      <span class="search-btn" @click="onSearch">搜索</span>
    </div>

    <div class="product-grid" v-if="list.length">
      <div
        v-for="item in list"
        :key="item.id"
        class="product-card animate-fade-in"
        @click="$router.push(`/product/${item.id}`)"
      >
        <div class="img-wrap">
          <img :src="item.thumbImg || '/static/errorImage.jpg'" alt="" />
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

    <van-empty v-else description="暂无商品" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getProductList } from '../../api/product.js'

const keyword = ref('')
const list = ref([])

const load = async () => {
  try {
    const res = await getProductList(1, 20, { keyword: keyword.value })
    list.value = res.list || []
  } catch (e) {}
}

const onSearch = () => load()
onMounted(() => load())
</script>

<style scoped>
.list-page {
  padding-bottom: 20px;
  background: var(--bg-page);
  min-height: 100vh;
}

.search-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: var(--bg-card);
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--bg-page);
  padding: 8px 14px;
  border-radius: var(--radius-xl);
}

.search-box input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 14px;
  outline: none;
}

.search-btn {
  font-size: 14px;
  color: var(--mall-primary);
  font-weight: 600;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  padding: 10px;
}

.product-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
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
