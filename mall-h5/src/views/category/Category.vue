<template>
  <div class="category-page">
    <!-- 搜索栏 -->
    <div class="search-bar" @click="$router.push('/product/list')">
      <div class="search-input">
        <van-icon name="search" size="16" color="#999" />
        <span>搜索商品</span>
      </div>
    </div>

    <div class="content">
      <!-- 左侧分类导航 -->
      <div class="sidebar">
        <div
          v-for="cat in tree"
          :key="cat.id"
          class="side-item"
          :class="{ active: activeId === cat.id }"
          @click="activeId = cat.id"
        >
          <div class="side-line" v-if="activeId === cat.id"></div>
          <span class="side-emoji">{{ getEmoji(cat.name) }}</span>
          <span class="side-name">{{ cat.name }}</span>
        </div>
      </div>

      <!-- 右侧内容 -->
      <div class="right-panel">
        <div v-if="activeCat" class="right-content">
          <!-- Banner -->
          <div class="banner-card">
            <div class="banner-gradient" :style="{ background: getBannerGradient(activeCat.name) }">
              <span class="banner-emoji">{{ getEmoji(activeCat.name) }}</span>
              <span class="banner-title">{{ activeCat.name }}</span>
            </div>
          </div>

          <!-- 子分类 -->
          <div class="sub-section" v-if="activeCat.children?.length">
            <div class="sub-header">
              <span class="sub-title">全部分类</span>
            </div>
            <div class="sub-grid">
              <div
                v-for="sub in activeCat.children"
                :key="sub.id"
                class="sub-card"
                @click="$router.push('/product/list')"
              >
                <div class="sub-icon-wrap">
                  <img
                    v-if="sub.imageUrl && !imgErrorMap[sub.id]"
                    :src="sub.imageUrl"
                    class="sub-real-img"
                    @error="imgErrorMap[sub.id] = true"
                  />
                  <template v-else>
                    <div class="sub-icon-box" :style="{ background: getIconGradient(sub.name) }">
                      <span class="sub-emoji">{{ getEmoji(sub.name) }}</span>
                    </div>
                  </template>
                </div>
                <span class="sub-card-name">{{ sub.name }}</span>
              </div>
            </div>
          </div>
          <van-empty v-else description="暂无子分类" />
        </div>
        <van-empty v-else description="暂无分类数据" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getCategoryTree } from '../../api/product.js'

const tree = ref([])
const activeId = ref(null)
const imgErrorMap = ref({})

const activeCat = computed(() => tree.value.find((c) => c.id === activeId.value))

onMounted(async () => {
  try {
    const data = await getCategoryTree()
    tree.value = Array.isArray(data) ? data : []
    if (tree.value.length) activeId.value = tree.value[0].id
  } catch (e) {
    console.error('获取分类失败:', e)
  }
})

/* ========== Emoji 映射表 ========== */
const emojiMap = {
  '手机': '📱', '电脑': '💻', '平板': '📲', '数码': '📷',
  '家电': '📺', '电视': '📺', '空调': '❄️', '冰箱': '🧊',
  '洗衣机': '🌀', '厨卫': '🍳', '男装': '👔', '女装': '👗',
  '童装': '🧒', '内衣': '👙', '鞋靴': '👟', '箱包': '👜',
  '美妆': '💄', '护肤': '🧴', '个护': '🪒', '香水': '🧪',
  '食品': '🍎', '饮料': '🧃', '生鲜': '🥩', '零食': '🍪',
  '水果': '🍊', '茶酒': '🍵', '母婴': '🍼', '玩具': '🧸',
  '乐器': '🎸', '用品': '🧴', '图书': '📚', '文具': '✏️',
  '办公': '📎', '家居': '🛋️', '家装': '🔨', '家具': '🪑',
  '灯具': '💡', '家纺': '🛏️', '宠物': '🐱', '园艺': '🌱',
  '汽车': '🚗', '车品': '🚙', '户外': '⛺', '运动': '⚽',
  '渔具': '🎣', '珠宝': '💍', '眼镜': '👓', '手表': '⌚',
  '医药': '💊', '保健': '🏥', '器械': '🩺', '计生': '💝',
  '礼品': '🎁', '鲜花': '🌹', '卡券': '🎫', '二手': '♻️',
  '动漫': '🎌', '周边': '🎭', '艺术': '🎨', '收藏': '🏺',
  '五金': '🔧', '电子': '🔌', '电工': '⚡', '水暖': '🚿',
}

function getEmoji(name) {
  if (!name) return '📦'
  for (const key in emojiMap) {
    if (name.includes(key)) return emojiMap[key]
  }
  return '📦'
}

/* ========== 精美渐变色表 ========== */
const gradientMap = {
  '手机': 'linear-gradient(135deg, #5B86E5 0%, #36D1DC 100%)',
  '电脑': 'linear-gradient(135deg, #7F7FD5 0%, #86A8E7 50%, #91EAE4 100%)',
  '数码': 'linear-gradient(135deg, #4776E6 0%, #8E54E9 100%)',
  '家电': 'linear-gradient(135deg, #FF6B6B 0%, #FFE66D 100%)',
  '电视': 'linear-gradient(135deg, #FF9A9E 0%, #FECFEF 100%)',
  '空调': 'linear-gradient(135deg, #A1C4FD 0%, #C2E9FB 100%)',
  '男装': 'linear-gradient(135deg, #667EEA 0%, #764BA2 100%)',
  '女装': 'linear-gradient(135deg, #F093FB 0%, #F5576C 100%)',
  '童装': 'linear-gradient(135deg, #4FACFE 0%, #00F2FE 100%)',
  '鞋靴': 'linear-gradient(135deg, #43E97B 0%, #38F9D7 100%)',
  '箱包': 'linear-gradient(135deg, #FA709A 0%, #FEE140 100%)',
  '美妆': 'linear-gradient(135deg, #FF9A9E 0%, #FECFEF 100%)',
  '护肤': 'linear-gradient(135deg, #FBC2EB 0%, #A6C1EE 100%)',
  '食品': 'linear-gradient(135deg, #F6D365 0%, #FDA085 100%)',
  '饮料': 'linear-gradient(135deg, #84FAB0 0%, #8FD3F4 100%)',
  '生鲜': 'linear-gradient(135deg, #43E97B 0%, #38F9D7 100%)',
  '水果': 'linear-gradient(135deg, #FFECD2 0%, #FCB69F 100%)',
  '母婴': 'linear-gradient(135deg, #FF9A9E 0%, #FECFEF 100%)',
  '玩具': 'linear-gradient(135deg, #A18CD1 0%, #FBC2EB 100%)',
  '乐器': 'linear-gradient(135deg, #667EEA 0%, #764BA2 100%)',
  '家居': 'linear-gradient(135deg, #30CFD0 0%, #330867 100%)',
  '家装': 'linear-gradient(135deg, #5B86E5 0%, #36D1DC 100%)',
  '家纺': 'linear-gradient(135deg, #FA709A 0%, #FEE140 100%)',
  '图书': 'linear-gradient(135deg, #F093FB 0%, #F5576C 100%)',
  '文具': 'linear-gradient(135deg, #4FACFE 0%, #00F2FE 100%)',
  '办公': 'linear-gradient(135deg, #667EEA 0%, #764BA2 100%)',
  '汽车': 'linear-gradient(135deg, #FF6B6B 0%, #FFE66D 100%)',
  '车品': 'linear-gradient(135deg, #F6D365 0%, #FDA085 100%)',
  '户外': 'linear-gradient(135deg, #43E97B 0%, #38F9D7 100%)',
  '运动': 'linear-gradient(135deg, #FA709A 0%, #FEE140 100%)',
  '珠宝': 'linear-gradient(135deg, #FBC2EB 0%, #A6C1EE 100%)',
  '眼镜': 'linear-gradient(135deg, #84FAB0 0%, #8FD3F4 100%)',
  '手表': 'linear-gradient(135deg, #5B86E5 0%, #36D1DC 100%)',
  '医药': 'linear-gradient(135deg, #30CFD0 0%, #330867 100%)',
  '保健': 'linear-gradient(135deg, #A1C4FD 0%, #C2E9FB 100%)',
  '礼品': 'linear-gradient(135deg, #FF9A9E 0%, #FECFEF 100%)',
  '鲜花': 'linear-gradient(135deg, #F093FB 0%, #F5576C 100%)',
  '宠物': 'linear-gradient(135deg, #F6D365 0%, #FDA085 100%)',
  '五金': 'linear-gradient(135deg, #667EEA 0%, #764BA2 100%)',
  '电子': 'linear-gradient(135deg, #4FACFE 0%, #00F2FE 100%)',
}

function getIconGradient(name) {
  if (!name) return 'linear-gradient(135deg, #667EEA 0%, #764BA2 100%)'
  for (const key in gradientMap) {
    if (name.includes(key)) return gradientMap[key]
  }
  return 'linear-gradient(135deg, #667EEA 0%, #764BA2 100%)'
}

function getBannerGradient(name) {
  return getIconGradient(name)
}
</script>

<style scoped>
.category-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding-bottom: var(--tabbar-height);
}

.search-bar {
  padding: 10px 16px;
  background: var(--bg-card);
}

.search-input {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--bg-page);
  padding: 8px 14px;
  border-radius: var(--radius-xl);
  color: var(--text-tertiary);
  font-size: 13px;
}

.content {
  flex: 1;
  display: flex;
  overflow: hidden;
  background: var(--bg-page);
}

/* ========== 左侧导航 ========== */
.sidebar {
  width: 92px;
  background: var(--bg-card);
  overflow-y: auto;
  flex-shrink: 0;
}

.side-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 14px 6px;
  font-size: 12px;
  color: var(--text-secondary);
  transition: all 0.2s;
}

.side-item.active {
  color: var(--mall-primary);
  font-weight: 700;
  background: var(--bg-page);
}

.side-line {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  background: var(--mall-primary-gradient);
  border-radius: 0 2px 2px 0;
}

.side-emoji {
  font-size: 22px;
  line-height: 1;
}

.side-name {
  line-height: 1.3;
}

/* ========== 右侧 ========== */
.right-panel {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.right-content {
  animation: fadeIn 0.3s ease-out;
}

/* Banner */
.banner-card {
  margin-bottom: 16px;
}

.banner-gradient {
  width: 100%;
  height: 100px;
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.banner-emoji {
  font-size: 36px;
  line-height: 1;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.15));
}

.banner-title {
  color: white;
  font-size: 16px;
  font-weight: 700;
  text-shadow: 0 1px 3px rgba(0,0,0,0.2);
}

/* 子分类区域 */
.sub-section {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 16px;
  box-shadow: var(--shadow-sm);
}

.sub-header {
  margin-bottom: 16px;
}

.sub-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.sub-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px 12px;
}

.sub-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

/* 图标外框 */
.sub-icon-wrap {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* 真实图片 */
.sub-real-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Emoji 图标盒 */
.sub-icon-box {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sub-emoji {
  font-size: 32px;
  line-height: 1;
  filter: drop-shadow(0 2px 3px rgba(0,0,0,0.15));
}

.sub-card-name {
  font-size: 12px;
  color: var(--text-secondary);
  text-align: center;
  line-height: 1.3;
}
</style>
