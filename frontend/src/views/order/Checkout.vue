<template>
  <AppLayout>
    <div class="checkout-page">
      <h2>订单结算</h2>

      <el-steps :active="1" finish-status="success" simple>
        <el-step title="购物车" />
        <el-step title="确认订单" />
        <el-step title="完成支付" />
      </el-steps>

      <div class="checkout-content">
        <el-card class="order-section">
          <template #header>
            <span>收货地址</span>
          </template>
          <el-input
            v-model="shippingAddress"
            type="textarea"
            :rows="3"
            placeholder="请输入详细收货地址"
          />
        </el-card>

        <el-card class="order-section">
          <template #header>
            <span>商品清单</span>
          </template>
          <el-table :data="cartStore.items" style="width: 100%">
            <el-table-column label="商品" min-width="300">
              <template #default="{ row }">
                <div class="product-cell">
                  <span>{{ row.productName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="120">
              <template #default="{ row }">
                ¥{{ row.price }}
              </template>
            </el-table-column>
            <el-table-column label="数量" width="100">
              <template #default="{ row }">
                {{ row.quantity }}
              </template>
            </el-table-column>
            <el-table-column label="小计" width="120">
              <template #default="{ row }">
                <span class="subtotal">¥{{ (row.price * row.quantity).toFixed(2) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card class="order-section">
          <template #header>
            <span>订单备注</span>
          </template>
          <el-input
            v-model="remark"
            type="textarea"
            :rows="2"
            placeholder="请输入订单备注（选填）"
          />
        </el-card>

        <div class="order-summary">
          <div class="summary-row">
            <span>商品总额:</span>
            <span>¥{{ cartStore.cartTotal.toFixed(2) }}</span>
          </div>
          <div class="summary-row">
            <span>运费:</span>
            <span>¥0.00</span>
          </div>
          <div class="summary-row total">
            <span>应付总额:</span>
            <span class="total-amount">¥{{ cartStore.cartTotal.toFixed(2) }}</span>
          </div>
          <el-button
            type="primary"
            size="large"
            :loading="submitting"
            :disabled="!shippingAddress"
            @click="submitOrder"
          >
            提交订单
          </el-button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppLayout from '@/components/layout/AppLayout.vue'
import { useCartStore } from '@/stores/cart'
import { createOrder } from '@/api/order'

const router = useRouter()
const cartStore = useCartStore()

const shippingAddress = ref('')
const remark = ref('')
const submitting = ref(false)

onMounted(() => {
  cartStore.loadCart()

  // 如果购物车为空，跳转到购物车页面
  if (cartStore.items.length === 0) {
    ElMessage.warning('购物车为空，请先添加商品')
    router.push('/cart')
  }
})

const submitOrder = async () => {
  if (!shippingAddress.value.trim()) {
    ElMessage.warning('请输入收货地址')
    return
  }

  try {
    submitting.value = true

    const orderData = {
      shippingAddress: shippingAddress.value,
      remark: remark.value,
      paymentMethod: '模拟支付'
    }

    const order = await createOrder(orderData)

    ElMessage.success('订单创建成功')
    router.push(`/orders`)
  } catch (error) {
    console.error('创建订单失败:', error)
    ElMessage.error(error.message || '创建订单失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.checkout-page {
  padding: 20px 0;
}

.checkout-page h2 {
  margin-bottom: 20px;
}

.checkout-content {
  margin-top: 30px;
}

.order-section {
  margin-bottom: 20px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 15px;
}

.subtotal {
  color: #f56c6c;
  font-weight: bold;
}

.order-summary {
  background: #f5f7fa;
  padding: 20px;
  border-radius: 8px;
  text-align: right;
}

.summary-row {
  display: flex;
  justify-content: flex-end;
  gap: 20px;
  margin-bottom: 10px;
  font-size: 14px;
}

.summary-row.total {
  font-size: 18px;
  font-weight: bold;
  margin: 20px 0;
  padding-top: 20px;
  border-top: 1px solid #dcdfe6;
}

.total-amount {
  color: #f56c6c;
  font-size: 24px;
}
</style>
