<template>
  <AppLayout>
    <div class="cart-page">
      <h2>购物车</h2>

      <div v-if="cartStore.items.length === 0" class="empty-cart">
        <el-empty description="购物车空空如也">
          <el-button type="primary" @click="$router.push('/products')">
            去购物
          </el-button>
        </el-empty>
      </div>

      <div v-else class="cart-content">
        <el-table :data="cartStore.items" style="width: 100%">
          <el-table-column label="商品" min-width="300">
            <template #default="{ row }">
              <div class="product-cell">
                <img :src="row.productImage || '/placeholder.jpg'" :alt="row.productName" />
                <div class="product-info">
                  <h4>{{ row.productName }}</h4>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="单价" width="120">
            <template #default="{ row }">
              ¥{{ row.price }}
            </template>
          </el-table-column>

          <el-table-column label="数量" width="150">
            <template #default="{ row }">
              <el-input-number
                v-model="row.quantity"
                :min="1"
                :max="99"
                size="small"
                @change="(val) => updateQuantity(row.id, val)"
              />
            </template>
          </el-table-column>

          <el-table-column label="小计" width="120">
            <template #default="{ row }">
              <span class="subtotal">¥{{ (row.price * row.quantity).toFixed(2) }}</span>
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

        <div class="cart-summary">
          <div class="summary-info">
            <span>共 {{ cartStore.itemCount }} 件商品</span>
            <span class="total-amount">
              合计: <strong>¥{{ cartStore.cartTotal.toFixed(2) }}</strong>
            </span>
          </div>
          <div class="summary-actions">
            <el-button @click="$router.push('/products')">继续购物</el-button>
            <el-button type="danger" @click="clearCart">清空购物车</el-button>
            <el-button type="primary" size="large" @click="goToCheckout">
              去结算
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppLayout from '@/components/layout/AppLayout.vue'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const cartStore = useCartStore()

onMounted(() => {
  cartStore.loadCart()
})

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

const goToCheckout = () => {
  router.push('/checkout')
}
</script>

<style scoped>
.cart-page {
  padding: 20px 0;
}

.cart-page h2 {
  margin-bottom: 20px;
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
  margin: 0;
  font-size: 14px;
}

.subtotal {
  color: #f56c6c;
  font-weight: bold;
}

.cart-summary {
  margin-top: 30px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary-info {
  display: flex;
  gap: 20px;
  align-items: center;
}

.total-amount {
  font-size: 18px;
}

.total-amount strong {
  color: #f56c6c;
  font-size: 24px;
}

.summary-actions {
  display: flex;
  gap: 12px;
}
</style>
