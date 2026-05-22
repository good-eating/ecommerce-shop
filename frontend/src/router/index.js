import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录', guestOnly: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册', guestOnly: true }
  },
  {
    path: '/products',
    name: 'Products',
    component: () => import('@/views/product/ProductList.vue'),
    meta: { title: '商品列表' }
  },
  {
    path: '/products/:id',
    name: 'ProductDetail',
    component: () => import('@/views/product/ProductDetail.vue'),
    meta: { title: '商品详情' }
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('@/views/cart/Cart.vue'),
    meta: { title: '购物车', requiresAuth: true }
  },
  {
    path: '/checkout',
    name: 'Checkout',
    component: () => import('@/views/order/Checkout.vue'),
    meta: { title: '结算', requiresAuth: true }
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('@/views/order/OrderList.vue'),
    meta: { title: '我的订单', requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/user/Profile.vue'),
    meta: { title: '个人信息', requiresAuth: true }
  },
  // 管理后台路由
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresRole: ['ADMIN', 'SALES'] },
    children: [
      {
        path: '',
        redirect: '/admin/products'
      },
      {
        path: 'products',
        name: 'AdminProducts',
        component: () => import('@/views/admin/ProductManagement.vue'),
        meta: { title: '商品管理', requiresRole: ['SALES'] }
      },
      {
        path: 'categories',
        name: 'AdminCategories',
        component: () => import('@/views/admin/CategoryManagement.vue'),
        meta: { title: '分类管理', requiresRole: ['ADMIN', 'SALES'] }
      },
      {
        path: 'logs',
        name: 'AdminLogs',
        component: () => import('@/views/admin/LogManagement.vue'),
        meta: { title: '浏览日志', requiresRole: ['SALES'] }
      },
      {
        path: 'sales-monitor',
        name: 'AdminSalesMonitor',
        component: () => import('@/views/admin/SalesMonitor.vue'),
        meta: { title: '销售监控', requiresRole: ['SALES'] }
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('@/views/admin/OrderManagement.vue'),
        meta: { title: '订单管理', requiresRole: ['SALES'] }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManagement.vue'),
        meta: { title: '用户管理', requiresRole: ['ADMIN'] }
      },
      {
        path: 'analytics',
        name: 'AdminAnalytics',
        component: () => import('@/views/admin/Analytics.vue'),
        meta: { title: '数据分析', requiresRole: ['ADMIN'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 电商购物网站`
  }
  
  // 检查是否需要认证
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
    return
  }
  
  // 检查角色权限
  if (to.meta.requiresRole) {
    const userRole = authStore.userRole
    if (!userRole || !to.meta.requiresRole.includes(userRole)) {
      // 角色不匹配时，重定向到该角色可访问的页面
      if (userRole === 'ADMIN') {
        next('/admin/users')
        return
      }
      if (userRole === 'SALES') {
        next('/admin/products')
        return
      }
      next('/home')
      return
    }
  }
  
  // 已登录用户不能访问guestOnly页面
  if (to.meta.guestOnly && authStore.isAuthenticated) {
    next('/home')
    return
  }
  
  next()
})

export default router