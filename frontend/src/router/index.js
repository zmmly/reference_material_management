import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

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

router.beforeEach((to, from, next) => {
  const token = getToken()
  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
