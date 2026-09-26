<template>
  <div class="address-edit-page">
    <van-nav-bar
      :title="isEdit ? '编辑地址' : '新增地址'"
      left-arrow
      fixed
      placeholder
      @click-left="$router.back()"
    />

    <div class="form-card sp-card">
      <van-cell-group inset :border="false">
        <van-field v-model="form.name" label="收货人" placeholder="请输入姓名" clearable />
        <van-field v-model="form.phone" label="手机号" placeholder="请输入手机号" type="tel" clearable />
        <van-field
          v-model="form.fullAddress"
          label="所在地区"
          placeholder="省/市/区"
          clearable
          readonly
          @click="showArea = true"
        />
        <van-field
          v-model="form.address"
          label="详细地址"
          placeholder="街道、楼牌号等"
          type="textarea"
          rows="2"
          clearable
        />
      </van-cell-group>

      <div class="default-row">
        <span>设为默认地址</span>
        <van-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" size="20" />
      </div>
    </div>

    <div class="btn-wrap">
      <van-button class="sp-btn-primary" block round @click="onSave">
        保 存
      </van-button>
      <van-button
        v-if="isEdit"
        block
        round
        plain
        hairline
        type="danger"
        style="margin-top: 12px;"
        @click="onDelete"
      >
        删除地址
      </van-button>
    </div>

    <!-- 地区选择弹窗（简化版：直接输入） -->
    <van-popup v-model:show="showArea" position="bottom" round>
      <div class="area-popup">
        <div class="area-header">
          <span>选择所在地区</span>
          <van-icon name="cross" size="18" @click="showArea = false" />
        </div>
        <div class="area-input">
          <van-field v-model="areaText" placeholder="请输入省/市/区，如：内蒙古自治区/鄂尔多斯市/康巴什区" />
        </div>
        <van-button block round type="primary" @click="confirmArea">确定</van-button>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { saveAddress, updateAddress, deleteAddress } from '../../api/user.js'

const route = useRoute()
const router = useRouter()
const isEdit = ref(false)
const showArea = ref(false)
const areaText = ref('')

const form = ref({
  id: null,
  name: '',
  phone: '',
  fullAddress: '',
  address: '',
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  isDefault: 0,
})

onMounted(() => {
  const raw = route.query.data
  if (raw) {
    try {
      const data = JSON.parse(raw)
      isEdit.value = true
      form.value = {
        id: data.id,
        name: data.name || '',
        phone: data.phone || '',
        fullAddress: data.fullAddress || '',
        address: data.address || '',
        provinceCode: data.provinceCode || '',
        cityCode: data.cityCode || '',
        districtCode: data.districtCode || '',
        isDefault: data.isDefault || 0,
      }
    } catch (e) {}
  }
})

const confirmArea = () => {
  form.value.fullAddress = areaText.value
  showArea.value = false
}

const onSave = async () => {
  if (!form.value.name || !form.value.phone || !form.value.fullAddress || !form.value.address) {
    showToast('请填写完整信息')
    return
  }
  try {
    const payload = { ...form.value }
    // 如果没有id字段（新增时）
    if (!payload.id) delete payload.id

    if (isEdit.value) {
      await updateAddress(payload)
      showToast({ message: '修改成功', icon: 'success' })
    } else {
      await saveAddress(payload)
      showToast({ message: '添加成功', icon: 'success' })
    }
    router.back()
  } catch (e) {}
}

const onDelete = () => {
  showConfirmDialog({ title: '提示', message: '确定删除该地址？' })
    .then(async () => {
      try {
        await deleteAddress(form.value.id)
        showToast('已删除')
        router.back()
      } catch (e) {}
    })
    .catch(() => {})
}
</script>

<style scoped>
.address-edit-page {
  min-height: 100vh;
  background: var(--bg-page);
  padding-bottom: 30px;
}

.form-card {
  margin: 12px;
  padding: 10px 0;
}

.default-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  margin-top: 4px;
  font-size: 14px;
  color: var(--text-primary);
}

.btn-wrap {
  margin: 24px 12px;
}

.area-popup {
  padding: 16px;
}

.area-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 12px;
}

.area-input {
  margin-bottom: 16px;
}
</style>
