# 前端架构设计

## 1. 项目结构
```
frontend/
├── public/                 # 静态资源
├── src/
│   ├── api/               # API接口
│   │   ├── auth.js        # 认证相关
│   │   ├── product.js     # 商品相关
│   │   ├── cart.js        # 购物车相关
│   │   ├── order.js       # 订单相关
│   │   └── analytics.js   # 分析相关
│   ├── components/        # 公共组件
│   │   ├── common/        # 通用组件
│   │   ├── layout/        # 布局组件
│   │   └── business/      # 业务组件
│   ├── views/            # 页面组件
│   │   ├── auth/         # 认证页面
│   │   ├── product/      # 商品页面
│   │   ├── cart/         # 购物车页面
│   │   ├── order/        # 订单页面
│   │   └── admin/        # 管理后台
│   ├── stores/           # Pinia状态管理
│   │   ├── auth.js       # 认证状态
│   │   ├── cart.js       # 购物车状态
│   │   ├── product.js    # 商品状态
│   │   └── user.js       # 用户状态
│   ├── router/           # 路由配置
│   ├── utils/            # 工具函数
│   │   ├── request.js    # 请求封装
│   │   ├── auth.js       # 认证工具
│   │   └── common.js     # 通用工具
│   ├── assets/           # 静态资源
│   ├── App.vue           # 根组件
│   └── main.js           # 入口文件
├── vite.config.js        # Vite配置
└── package.json
```

## 2. 路由配置

```javascript
// src/router/index.js
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
  // 管理后台路由
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresRole: ['ADMIN', 'SALES'] },
    children: [
      {
        path: 'products',
        name: 'AdminProducts',
        component: () => import('@/views/admin/ProductManagement.vue'),
        meta: { title: '商品管理' }
      },
      {
        path: 'analytics',
        name: 'AdminAnalytics',
        component: () => import('@/views/admin/Analytics.vue'),
        meta: { title: '数据分析', requiresRole: ['ADMIN'] }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManagement.vue'),
        meta: { title: '用户管理', requiresRole: ['ADMIN'] }
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
    const userRole = authStore.user?.role
    if (!userRole || !to.meta.requiresRole.includes(userRole)) {
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
```

## 3. Axios请求封装

```javascript
// src/utils/request.js
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

// 创建axios实例
const request = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    
    // 添加token
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    
    // 处理GET请求参数
    if (config.method === 'get' && config.params) {
      let url = config.url + '?'
      for (const propName of Object.keys(config.params)) {
        const value = config.params[propName]
        if (value !== undefined && value !== null && value !== '') {
          url += `${propName}=${encodeURIComponent(value)}&`
        }
      }
      url = url.substring(0, url.length - 1)
      config.params = {}
      config.url = url
    }
    
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const { data } = response
    
    // 统一响应格式处理
    if (data.code === 200) {
      return data.data
    } else {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message || '请求失败'))
    }
  },
  (error) => {
    const { response } = error
    
    if (response) {
      switch (response.status) {
        case 401:
          // token过期，清除登录状态
          const authStore = useAuthStore()
          authStore.logout()
          router.push('/login')
          ElMessage.error('登录已过期，请重新登录')
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }
    
    return Promise.reject(error)
  }
)

export default request
```

## 4. Pinia状态管理

### 认证状态管理
```javascript
// src/stores/auth.js
import { defineStore } from 'pinia'
import { login, register, logout, refreshToken, getProfile } from '@/api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    refreshToken: localStorage.getItem('refreshToken') || null
  }),
  
  getters: {
    isAuthenticated: (state) => !!state.token,
    userRole: (state) => state.user?.role
  },
  
  actions: {
    async login(credentials) {
      try {
        const response = await login(credentials)
        this.token = response.accessToken
        this.refreshToken = response.refreshToken
        this.user = response.user
        
        // 持久化存储
        localStorage.setItem('token', this.token)
        localStorage.setItem('refreshToken', this.refreshToken)
        localStorage.setItem('user', JSON.stringify(this.user))
        
        return response
      } catch (error) {
        this.logout()
        throw error
      }
    },
    
    async register(userInfo) {
      const response = await register(userInfo)
      return response
    },
    
    async refreshAuth() {
      if (!this.refreshToken) {
        throw new Error('No refresh token available')
      }
      
      try {
        const response = await refreshToken(this.refreshToken)
        this.token = response.accessToken
        this.refreshToken = response.refreshToken
        
        localStorage.setItem('token', this.token)
        localStorage.setItem('refreshToken', this.refreshToken)
        
        return response
      } catch (error) {
        this.logout()
        throw error
      }
    },
    
    async getCurrentUser() {
      if (!this.token) {
        throw new Error('Not authenticated')
      }
      
      try {
        const user = await getProfile()
        this.user = user
        localStorage.setItem('user', JSON.stringify(user))
        return user
      } catch (error) {
        this.logout()
        throw error
      }
    },
    
    logout() {
      this.token = null
      this.user = null
      this.refreshToken = null
      
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      
      // 调用后端登出
      if (this.token) {
        logout().catch(() => {}) // 忽略登出错误
      }
    }
  }
})
```

### 购物车状态管理
```javascript
// src/stores/cart.js
import { defineStore } from 'pinia'
import { getCart, addToCart, updateCartItem, removeCartItem, clearCart } from '@/api/cart'

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: [],
    totalItems: 0,
    totalAmount: 0,
    isLoading: false
  }),
  
  getters: {
    itemCount: (state) => state.items.reduce((total, item) => total + item.quantity, 0),
    cartTotal: (state) => state.items.reduce((total, item) => total + (item.price * item.quantity), 0)
  },
  
  actions: {
    async loadCart() {
      this.isLoading = true
      try {
        const cartData = await getCart()
        this.items = cartData.items || []
        this.totalItems = cartData.totalItems || 0
        this.totalAmount = cartData.totalAmount || 0
      } catch (error) {
        console.error('加载购物车失败:', error)
        this.items = []
        this.totalItems = 0
        this.totalAmount = 0
      } finally {
        this.isLoading = false
      }
    },
    
    async addItem(product, quantity = 1) {
      try {
        await addToCart({
          productId: product.id,
          quantity: quantity,
          price: product.price
        })
        
        // 重新加载购物车
        await this.loadCart()
      } catch (error) {
        throw error
      }
    },
    
    async updateItem(itemId, quantity) {
      if (quantity <= 0) {
        await this.removeItem(itemId)
        return
      }
      
      try {
        await updateCartItem(itemId, { quantity })
        await this.loadCart()
      } catch (error) {
        throw error
      }
    },
    
    async removeItem(itemId) {
      try {
        await removeCartItem(itemId)
        await this.loadCart()
      } catch (error) {
        throw error
      }
    },
    
    async clear() {
      try {
        await clearCart()
        this.items = []
        this.totalItems = 0
        this.totalAmount = 0
      } catch (error) {
        throw error
      }
    }
  }
})
```

## 5. 核心组件设计

### 商品列表组件
```vue
<!-- src/views/product/ProductList.vue -->
<template>
  <div class="product-list">
    <div class="filters">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索商品..."
        style="width: 300px"
        @input="handleSearch"
      >
        <template #append>
          <el-button :icon="Search" />
        </template>
      </el-input>
      
      <el-select v-model="categoryFilter" placeholder="分类筛选" @change="handleFilter">
        <el-option label="全部" value="" />
        <el-option
          v-for="category in categories"
          :key="category.id"
          :label="category.name"
          :value="category.id"
        />
      </el-select>
      
      <el-select v-model="sortBy" placeholder="排序" @change="handleSort">
        <el-option label="最新" value="newest" />
        <el-option label="价格从低到高" value="price_asc" />
        <el-option label="价格从高到低" value="price_desc" />
        <el-option label="销量最高" value="sales" />
      </el-select>
    </div>
    
    <div class="product-grid">
      <el-card
        v-for="product in products"
        :key="product.id"
        class="product-card"
        :body-style="{ padding: '20px' }"
      >
        <div class="product-image">
          <img :src="product.mainImage" :alt="product.name" />
          <div v-if="product.salesCount > 100" class="hot-badge">热销</div>
        </div>
        
        <div class="product-info">
          <h3 class="product-name">{{ product.name }}</h3>
          <p class="product-description">{{ product.description }}</p>
          
          <div class="product-price">
            <span class="current-price">¥{{ product.price }}</span>
            <span v-if="product.originalPrice" class="original-price">
              ¥{{ product.originalPrice }}
            </span>
          </div>
          
          <div class="product-actions">
            <el-button type="primary" @click="addToCart(product)">
              加入购物车
            </el-button>
            <el-button @click="$router.push(`/products/${product.id}`)">
              查看详情
            </el-button>
          </div>
        </div>
      </el-card>
    </div>
    
    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
    
    <!-- 推荐商品区域 -->
    <ProductRecommendations v-if="showRecommendations" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { useCartStore } from '@/stores/cart'
import { getProducts, searchProducts } from '@/api/product'
import ProductRecommendations from '@/components/business/ProductRecommendations.vue'

const cartStore = useCartStore()

// 响应式数据
const products = ref([])
const categories = ref([])
const searchKeyword = ref('')
const categoryFilter = ref('')
const sortBy = ref('newest')
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const loading = ref(false)

// 计算属性
const showRecommendations = computed(() => 
  products.value.length > 0 && currentPage.value === 1
)

// 生命周期
onMounted(() => {
  loadProducts()
  loadCategories()
})

// 方法
const loadProducts = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      categoryId: categoryFilter.value,
      sort: sortBy.value,
      keyword: searchKeyword.value
    }
    
    const response = await getProducts(params)
    products.value = response.items
    total.value = response.total
  } catch (error) {
    console.error('加载商品失败:', error)
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  // 加载分类数据
}

const handleSearch = () => {
  currentPage.value = 1
  loadProducts()
}

const handleFilter = () => {
  currentPage.value = 1
  loadProducts()
}

const handleSort = () => {
  currentPage.value = 1
  loadProducts()
}

const handleSizeChange = (newSize) => {
  pageSize.value = newSize
  currentPage.value = 1
  loadProducts()
}

const handlePageChange = (newPage) => {
  currentPage.value = newPage
  loadProducts()
}

const addToCart = async (product) => {
  try {
    await cartStore.addItem(product)
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error('加入购物车失败')
  }
}
</script>

<style scoped>
.product-list {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.filters {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  align-items: center;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.product-card {
  transition: transform 0.3s ease;
}

.product-card:hover {
  transform: translateY(-5px);
}

.product-image {
  position: relative;
  margin-bottom: 15px;
}

.product-image img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 8px;
}

.hot-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: #f56c6c;
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.product-name {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 8px;
  line-height: 1.4;
}

.product-description {
  color: #666;
  font-size: 14px;
  margin-bottom: 10px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-price {
  margin-bottom: 15px;
}

.current-price {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
}

.original-price {
  font-size: 14px;
  color: #999;
  text-decoration: line-through;
  margin-left: 8px;
}

.product-actions {
  display: flex;
  gap: 10px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}
</style>
```

### 购物车组件
```vue
<!-- src/views/cart/Cart.vue -->
<template>
  <div class="cart-page">
    <div class="cart-header">
      <h2>购物车</h2>
      <div class="cart-stats">
        <span>共 {{ cartStore.itemCount }} 件商品</span>
        <span class="total-amount">总计: ¥{{ cartStore.cartTotal.toFixed(2) }}</span>
      </div>
    </div>
    
    <div v-if="cartStore.items.length === 0" class="empty-cart">
      <el-empty description="购物车空空如也">
        <el-button type="primary" @click="$router.push('/products')">
          去购物
        </el-button>
      </el-empty>
    </div>
    
    <div v-else class="cart-content">
      <el-table :data="cartStore.items" style="width: 100%">
        <el-table-column width="60">
          <template #header>
            <el-checkbox v-model="selectAll" @change="handleSelectAll" />
          </template>
          <template #default="{ row }">
            <el-checkbox v-model="selectedItems" :value="row.id" />
          </template>
        </el-table-column>
        
        <el-table-column label="商品" width="400">
          <template #default="{ row }">
            <div class="product-cell">
              <img :src="row.product.mainImage" :alt="row.product.name" />
              <div class="product-info">
                <h4>{{ row.product.name }}</h4>
                <p class="sku">SKU: {{ row.product.sku }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="单价" width="120">
          <template #default="{ row }">
            ¥{{ row.price.toFixed(2) }}
          </template>
        </el-table-column>
        
        <el-table-column label="数量" width="150">
          <template #default="{ row }">
            <el-input-number
              v-model="row.quantity"
              :min="1"
              :max="row.product.stock"
              @change="(val) => updateQuantity(row.id, val)"
            />
          </template>
        </el-table-column>
        
        <el-table-column label="小计" width="120">
          <template #default="{ row }">
            ¥{{ (row.price * row.quantity).toFixed(2) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeItem(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="cart-actions">
        <el-button @click="continueShopping">继续购物</el-button>
        <el-button type="danger" @click="clearCart">清空购物车</el-button>
        <el-button type="primary" @click="goToCheckout" :disabled="selectedItems.length === 0">
          去结算 ({{ selectedItems.length }})
        </el-button>
      </div>
    </div>
    
    <!-- 推荐商品 -->
    <ProductRecommendations v-if="cartStore.items.length > 0" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProductRecommendations from '@/components/business/ProductRecommendations.vue'

const router = useRouter()
const cartStore = useCartStore()

const selectedItems = ref([])

// 计算属性
const selectAll = computed({
  get: () => selectedItems.value.length === cartStore.items.length && cartStore.items.length > 0,
  set: (value) => {
    selectedItems.value = value ? cartStore.items.map(item => item.id) : []
  }
})

// 生命周期
onMounted(() => {
  cartStore.loadCart()
})

// 方法
const handleSelectAll = (value) => {
  selectedItems.value = value ? cartStore.items.map(item => item.id) : []
}

const updateQuantity = async (itemId, quantity) => {
  try {
    await cartStore.updateItem(itemId, quantity)
  } catch (error) {
    ElMessage.error('更新数量失败')
  }
}

const removeItem = async (itemId) => {
  try {
    await ElMessageBox.confirm('确定要删除这个商品吗？', '提示', {
      type: 'warning'
    })
    
    await cartStore.removeItem(itemId)
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const clearCart = async () => {
  try {
    await ElMessageBox.confirm('确定要清空购物车吗？', '提示', {
      type: 'warning'
    })
    
    await cartStore.clear()
    ElMessage.success('购物车已清空')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清空失败')
    }
  }
}

const continueShopping = () => {
  router.push('/products')
}

const goToCheckout = () => {
  router.push('/checkout')
}
</script>

<style scoped>
.cart-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.cart-stats {
  display: flex;
  gap: 20px;
  align-items: center;
}

.total-amount {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
}

.empty-cart {
  text-align: center;
  padding: 60px 0;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 15px;
}

.product-cell img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}

.product-info h4 {
  margin: 0 0 5px 0;
  font-size: 14px;
  line-height: 1.4;
}

.sku {
  color: #999;
  font-size: 12px;
  margin: 0;
}

.cart-actions {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}
</style>
```