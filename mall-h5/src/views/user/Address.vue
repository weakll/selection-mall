<template>
  <div class="address-page">
    <van-nav-bar title="收货地址" left-arrow fixed placeholder @click-left="$router.back()" />

    <div class="address-list" v-if="list.length">
      <div
        v-for="item in list"
        :key="item.id"
        class="address-card sp-card"
        :class="{ default: item.isDefault === 1 }"
        @click="onSelect(item)"
      >
        <div class="addr-top">
          <div class="name-phone">
            <span class="name">{{ item.name }}</span>
            <span class="phone">{{ item.phone }}</span>
            <van-tag v-if="item.isDefault === 1" type="danger" round size="small">默认</van-tag>
          </div>
          <div class="full-addr">{{ item.fullAddress }} {{ item.address }}</div>
        </div>
        <div class="addr-bottom">
          <van-radio
            :checked="item.isDefault === 1"
            @click.stop="setDefault(item)"
          >
            设为默认
          </van-radio>
          <div class="actions">
            <span class="edit" @click.stop="onEdit(item)">
              <van-icon name="edit" size="14" /> 编辑
            </span>
            <span class="delete" @click.stop="onDelete(item.id)">
              <van-icon name="delete-o" size="14" /> 删除
            </span>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <van-empty description="暂无收货地址" />
    </div>

    <div class="bottom-btn">
      <van-button class="sp-btn-primary" block round @click="onAdd">
        <van-icon name="plus" /> 新建地址
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { getAddressList, deleteAddress, updateAddress } from '../../api/user.js'

const route = useRoute()
const router = useRouter()
const list = ref([])

const load = async () => {
  try {
    list.value = (await getAddressList()) || []
  } catch (e) {}
}

onMounted(() => load())

const onSelect = (item) => {
  // 如果是从订单页选择地址，则返回并带上地址数据
  if (route.query.from === 'trade') {
    router.back()
  }
}

const onAdd = () => {
  router.push('/address/edit')
}

const onEdit = (item) => {
  router.push({
    path: '/address/edit',
    query: { data: JSON.stringify(item) }
  })
}

const onDelete = (id) => {
  showConfirmDialog({ title: '提示', message: '确定删除该地址？' })
    .then(async () => {
      try {
        await deleteAddress(id)
        showToast('已删除')
        load()
      } catch (e) {}
    })
    .catch(() => {})
}

const setDefault = async (item) => {
  if (item.isDefault === 1) return
  try {
    await updateAddress({ ...item, isDefault: 1 })
    showToast('已设为默认')
    load()
  } catch (e) {}
}
</script>

<style scoped>
.address-page {
  min-height: 100vh;
  background: var(--bg-page);
  padding-bottom: 80px;
}

.address-list {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.address-card {
  padding: 14px 16px;
  position: relative;
}

.address-card.default {
  border: 1px solid rgba(255, 107, 0, 0.2);
}

.addr-top {
  margin-bottom: 12px;
}

.name-phone {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.phone {
  font-size: 13px;
  color: var(--text-secondary);
}

.full-addr {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.addr-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px solid var(--divider);
}

.actions {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--text-secondary);
}

.actions .edit,
.actions .delete {
  display: flex;
  align-items: center;
  gap: 4px;
}

.empty-state {
  padding-top: 60px;
}

.bottom-btn {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: var(--bg-card);
  border-top: 1px solid var(--divider);
}
</style>
