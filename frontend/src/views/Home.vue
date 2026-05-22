<template>
  <AppLayout>
    <div class="home-page">
      <!-- Banner -->
      <div class="banner">
        <h1>欢迎来到电商购物网站</h1>
        <p>发现优质商品，享受便捷购物体验</p>
        <el-button type="primary" size="large" @click="$router.push('/products')">
          开始购物
        </el-button>
      </div>

      <!-- 热门商品 -->
      <div class="section">
        <h2 class="section-title">热门商品</h2>
        <el-row :gutter="20">
          <el-col :span="6" v-for="product in popularProducts" :key="product.id">
            <el-card class="product-card" :body-style="{ padding: '0' }" @click="$router.push(`/products/${product.id}`)">
              <div class="product-image">
                <img :src="product.image || '/placeholder.jpg'" :alt="product.name" />
              </div>
              <div class="product-info">
                <h3 class="product-name">{{ product.name }}</h3>
                <p class="product-price">¥{{ product.price }}</p>
                <el-button type="primary" @click.stop="addToCart(product)">
                  加入购物车
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 推荐商品 -->
      <div class="section" v-if="recommendations.length > 0">
        <h2 class="section-title">最近许多人买过</h2>
        <el-row :gutter="20">
          <el-col :span="6" v-for="product in recommendations" :key="product.id">
            <el-card class="product-card" :body-style="{ padding: '0' }" @click="$router.push(`/products/${product.id}`)">
              <div class="product-image">
                <img :src="product.image || '/placeholder.jpg'" :alt="product.name" />
              </div>
              <div class="product-info">
                <h3 class="product-name">{{ product.name }}</h3>
                <p class="product-price">¥{{ product.price }}</p>
                <el-button type="primary" @click.stop="addToCart(product)">
                  加入购物车
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AppLayout from '@/components/layout/AppLayout.vue'
import { getPopularProducts } from '@/api/recommendation'
import { getRecommendations } from '@/api/recommendation'
import { useCartStore } from '@/stores/cart'

const cartStore = useCartStore()

const popularProducts = ref([])
const recommendations = ref([])

onMounted(() => {
  loadPopularProducts()
  loadRecommendations()
})

const loadPopularProducts = async () => {
  try {
    const data = await getPopularProducts(8)
    popularProducts.value = data
  } catch (error) {
    console.error('加载热门商品失败:', error)
  }
}

const loadRecommendations = async () => {
  try {
    const data = await getRecommendations(4)
    recommendations.value = data
  } catch (error) {
    console.error('加载推荐商品失败:', error)
  }
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
.home-page {
  padding: 20px 0;
}

.banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-align: center;
  padding: 60px 20px;
  border-radius: 8px;
  margin-bottom: 40px;
}

.banner h1 {
  font-size: 36px;
  margin-bottom: 16px;
}

.banner p {
  font-size: 18px;
  margin-bottom: 24px;
  opacity: 0.9;
}

.section {
  margin-bottom: 40px;
}

.section-title {
  font-size: 24px;
  margin-bottom: 20px;
  color: #333;
}

.product-card {
  margin-bottom: 20px;
  cursor: pointer;
  transition: transform 0.3s;
}

.product-card:hover {
  transform: translateY(-5px);
}

.product-image {
  height: 200px;
  overflow: hidden;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info {
  padding: 16px;
}

.product-name {
  font-size: 16px;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-price {
  font-size: 18px;
  color: #f56c6c;
  font-weight: bold;
  margin-bottom: 12px;
}
</style>
