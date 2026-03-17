import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/store/modules/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/views/layout/index.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue') },
      // 基础数据
      { path: 'basic/material', name: 'Material', component: () => import('@/views/reference-material/index.vue') },
      { path: 'basic/category', name: 'Category', component: () => import('@/views/basic/category/index.vue') },
      { path: 'basic/location', name: 'Location', component: () => import('@/views/basic/location/index.vue') },
      { path: 'basic/metadata', name: 'Metadata', component: () => import('@/views/basic/metadata/index.vue') },
      // 库存管理
      { path: 'stock', name: 'Stock', component: () => import('@/views/stock/index.vue') },
      { path: 'stock-in', name: 'StockIn', component: () => import('@/views/stock-in/index.vue') },
      { path: 'stock-out', name: 'StockOut', component: () => import('@/views/stock-out/index.vue') },
      { path: 'stock-out/apply', name: 'StockOutApply', component: () => import('@/views/stock-out/apply.vue') },
      // 采购管理
      { path: 'purchase', name: 'Purchase', component: () => import('@/views/purchase/index.vue') },
      // 盘点管理
      { path: 'stock-check', name: 'StockCheck', component: () => import('@/views/stock-check/index.vue') },
      // 预警中心
      { path: 'alert', name: 'Alert', component: () => import('@/views/alert/index.vue') },
      // 系统管理
      { path: 'system/user', name: 'User', component: () => import('@/views/system/user/index.vue') },
      { path: 'system/role', name: 'Role', component: () => import('@/views/system/role/index.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const token = getToken()
  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    // 如果有token但没有userInfo，尝试获取用户信息
    if (token) {
      const userStore = useUserStore()
      if (!userStore.userInfo) {
        try {
          await userStore.fetchUserInfo()
        } catch (e) {
          // 获取用户信息失败，跳转到登录页
          next('/login')
          return
        }
      }
    }
    next()
  }
})

export default router
