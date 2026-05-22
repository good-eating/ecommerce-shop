<template>
  <AppLayout>
    <div class="product-detail-page" v-if="product">
      <el-row :gutter="40">
        <el-col :span="12">
          <div class="product-gallery">
            <img :src="product.image || '/placeholder.jpg'" :alt="product.name" />
          </div>
        </el-col>

        <el-col :span="12">
          <div class="product-info">
            <h1 class="product-name">{{ product.name }}</h1>
            <p class="product-sku">SKU: {{ product.sku }}</p>

            <div class="product-price">
              <span class="current-price">¥{{ product.price }}</span>
              <span v-if="product.originalPrice" class="original-price">
                ¥{{ product.originalPrice }}
              </span>
            </div>

            <div class="product-stock">
              <span>库存: {{ product.stock }} 件</span>
              <span class="sales">已售: {{ product.salesCount }} 件</span>
            </div>

            <div class="product-description">
              <h3>商品描述</h3>
              <p>{{ product.description || '暂无描述' }}</p>
            </div>

            <div class="quantity-selector">
              <span>数量:</span>
              <el-input-number v-model="quantity" :min="1" :max="product.stock" />
            </div>

            <div class="product-actions">
              <el-button type="primary" size="large" @click="addToCart">
                <el-icon><ShoppingCart /></el-icon>
                加入购物车
              </el-button>
              <el-button type="danger" size="large" @click="buyNow">
                立即购买
              </el-button>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 推荐商品 -->
      <div class="recommendations" v-if="recommendations.length > 0">
        <h2>买过这个的也买过</h2>
        <el-row :gutter="20">
          <el-col :span="6" v-for="item in recommendations" :key="item.id">
            <el-card class="product-card" :body-style="{ padding: '0' }">
              <div class="product-link" @click="goToProduct(item.id)">
                <div class="product-image">
                  <img :src="item.image || '/placeholder.jpg'" :alt="item.name" />
                </div>
                <div class="product-info-small">
                  <h4>{{ item.name }}</h4>
                  <p class="price">¥{{ item.price }}</p>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppLayout from '@/components/layout/AppLayout.vue'
import { getProductById } from '@/api/product'
import { getRecommendations } from '@/api/recommendation'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const product = ref(null)
const quantity = ref(1)
const recommendations = ref([])

onMounted(() => {
  loadProduct()
  loadRecommendations()
})

watch(() => route.params.id, () => {
  quantity.value = 1
  loadProduct()
  loadRecommendations()
})

const loadProduct = async () => {
  try {
    const id = route.params.id
    const data = await getProductById(id)
    product.value = data
  } catch (error) {
    console.error('加载商品详情失败:', error)
    ElMessage.error('商品不存在')
    router.push('/products')
  }
}

const loadRecommendations = async () => {
  try {
    const data = await getRecommendations(4)
    recommendations.value = data.filter(item => item.id !== parseInt(route.params.id))
  } catch (error) {
    console.error('加载推荐商品失败:', error)
  }
}

const addToCart = async () => {
  try {
    await cartStore.addItem(product.value, quantity.value)
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error('加入购物车失败')
  }
}

const buyNow = async () => {
  try {
    await cartStore.addItem(product.value, quantity.value)
    router.push('/checkout')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const goToProduct = (productId) => {
  router.push(`/products/${productId}`)
}
</script>

<style scoped>
.product-detail-page {
  padding: 20px 0;
}

.product-gallery {
  border-radius: 8px;
  overflow: hidden;
}

.product-gallery img {
  width: 100%;
  height: 400px;
  object-fit: cover;
}

.product-info {
  padding: 20px;
}

.product-name {
  font-size: 24px;
  margin-bottom: 8px;
}

.product-sku {
  color: #999;
  margin-bottom: 16px;
}

.product-price {
  margin-bottom: 16px;
}

.current-price {
  font-size: 28px;
  color: #f56c6c;
  font-weight: bold;
}

.original-price {
  font-size: 16px;
  color: #999;
  text-decoration: line-through;
  margin-left: 12px;
}

.product-stock {
  margin-bottom: 20px;
  color: #666;
}

.sales {
  margin-left: 20px;
}

.product-description {
  margin-bottom: 20px;
}

.product-description h3 {
  margin-bottom: 8px;
}

.quantity-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.product-actions {
  display: flex;
  gap: 12px;
}

.recommendations {
  margin-top: 40px;
  padding-top: 40px;
  border-top: 1px solid #e4e7ed;
}

.recommendations h2 {
  margin-bottom: 20px;
}

.product-card {
  cursor: pointer;
  transition: transform 0.3s;
}

.product-card:hover {
  transform: translateY(-5px);
}

.product-link {
  cursor: pointer;
  display: block;
}

.product-image {
  height: 150px;
  overflow: hidden;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info-small {
  padding: 12px;
}

.product-info-small h4 {
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price {
  color: #f56c6c;
  font-weight: bold;
}
</style>
