<template>
  <router-view v-slot="{ Component }">
    <keep-alive :include="['Index', 'Category', 'Cart', 'User']">
      <component :is="Component" />
    </keep-alive>
  </router-view>
  <TabBar v-if="showTabBar" />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import TabBar from './components/TabBar.vue'

const route = useRoute()
const showTabBar = computed(() => route.meta.tabBar)
</script>
