import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/index' },
  { path: '/index', name: 'Index', component: () => import('../views/index/Index.vue'), meta: { tabBar: true, title: '首页' } },
  { path: '/category', name: 'Category', component: () => import('../views/category/Category.vue'), meta: { tabBar: true, title: '分类' } },
  { path: '/cart', name: 'Cart', component: () => import('../views/cart/Cart.vue'), meta: { tabBar: true, title: '购物车' } },
  { path: '/user', name: 'User', component: () => import('../views/user/User.vue'), meta: { tabBar: true, title: '我的' } },
  { path: '/product/:skuId', name: 'ProductDetail', component: () => import('../views/product/Detail.vue'), meta: { title: '商品详情' } },
  { path: '/product/list', name: 'ProductList', component: () => import('../views/product/List.vue'), meta: { title: '商品列表' } },
  { path: '/order/trade', name: 'OrderTrade', component: () => import('../views/order/Trade.vue'), meta: { title: '确认订单' } },
  { path: '/order/list', name: 'OrderList', component: () => import('../views/order/OrderList.vue'), meta: { title: '我的订单' } },
  { path: '/login', name: 'Login', component: () => import('../views/user/Login.vue'), meta: { title: '登录' } },
  { path: '/address', name: 'Address', component: () => import('../views/user/Address.vue'), meta: { title: '收货地址' } },
  { path: '/address/edit', name: 'AddressEdit', component: () => import('../views/user/AddressEdit.vue'), meta: { title: '编辑地址' } },
  { path: '/collect', name: 'CollectList', component: () => import('../views/user/CollectList.vue'), meta: { title: '我的收藏' } },
  { path: '/browse', name: 'BrowseList', component: () => import('../views/user/BrowseList.vue'), meta: { title: '我的足迹' } },
  { path: '/coupon', name: 'CouponList', component: () => import('../views/user/CouponList.vue'), meta: { title: '我的优惠券' } },
  { path: '/service', name: 'ServiceCenter', component: () => import('../views/user/ServiceCenter.vue'), meta: { title: '客服中心' } },
  { path: '/message', name: 'MessageList', component: () => import('../views/user/MessageList.vue'), meta: { title: '消息通知' } },
  { path: '/set', name: 'Set', component: () => import('../views/user/Set.vue'), meta: { title: '设置' } },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title || '精选商城'
  if (to.name === 'Login' && localStorage.getItem('token')) {
    next({ name: 'User', replace: true })
    return
  }
  next()
})

export default router
