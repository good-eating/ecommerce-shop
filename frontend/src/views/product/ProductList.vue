<template>
  <AppLayout>
    <div class="product-list-page">
      <div class="filters">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索商品..."
          style="width: 300px"
          clearable
          @input="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>

        <el-select v-model="sortBy" placeholder="排序" @change="handleSort">
          <el-option label="最新" value="newest" />
          <el-option label="价格从低到高" value="price_asc" />
          <el-option label="价格从高到低" value="price_desc" />
          <el-option label="销量最高" value="sales" />
        </el-select>
      </div>

      <el-row :gutter="20">
        <el-col :span="6" v-for="product in products" :key="product.id">
          <el-card class="product-card" :body-style="{ padding: '0' }">
            <div class="product-image" @click="$router.push(`/products/${product.id}`)">
              <img :src="product.image || '/placeholder.jpg'" :alt="product.name" />
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
                <el-button type="primary" @click.stop="addToCart(product)">
                  加入购物车
                </el-button>
                <el-button @click.stop="$router.push(`/products/${product.id}`)">
                  查看详情
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[12, 24, 36]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AppLayout from '@/components/layout/AppLayout.vue'
import { getProducts } from '@/api/product'
import { useCartStore } from '@/stores/cart'

const cartStore = useCartStore()

const products = ref([])
const searchKeyword = ref('')
const sortBy = ref('newest')
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const loading = ref(false)

onMounted(() => {
  loadProducts()
})

const loadProducts = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      sort: sortBy.value,
      keyword: searchKeyword.value
    }

    const response = await getProducts(params)
    products.value = response.items
    total.value = response.total
  } catch (error) {
    console.error('加载商品失败:', error)
    ElMessage.error('加载商品失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
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
.product-list-page {
  padding: 20px 0;
}

.filters {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  align-items: center;
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

.product-description {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-price {
  margin-bottom: 12px;
}

.current-price {
  font-size: 18px;
  color: #f56c6c;
  font-weight: bold;
}

.original-price {
  font-size: 14px;
  color: #999;
  text-decoration: line-through;
  margin-left: 8px;
}

.product-actions {
  display: flex;
  gap: 8px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}
</style>
