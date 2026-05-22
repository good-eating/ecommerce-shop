<template>
  <AppLayout>
    <div class="order-list-page">
      <h2>我的订单</h2>

      <div v-if="orders.length === 0" class="empty-orders">
        <el-empty description="暂无订单">
          <el-button type="primary" @click="$router.push('/products')">
            去购物
          </el-button>
        </el-empty>
      </div>

      <div v-else class="orders-list">
        <el-card v-for="order in orders" :key="order.id" class="order-card">
          <div class="order-header">
            <div class="order-info">
              <span class="order-no">订单号: {{ order.orderNo }}</span>
              <span class="order-date">{{ formatDate(order.createdAt) }}</span>
            </div>
            <el-tag :type="getStatusType(order.status)">
              {{ order.statusText }}
            </el-tag>
          </div>

          <div class="order-items">
            <div v-for="item in order.items" :key="item.id" class="order-item">
              <div class="item-info">
                <span class="item-name">{{ item.productName }}</span>
                <span class="item-sku">SKU: {{ item.sku }}</span>
              </div>
              <div class="item-price">
                <span>¥{{ item.priceAtPurchase }} x {{ item.quantity }}</span>
                <span class="item-subtotal">¥{{ item.subtotal }}</span>
              </div>
            </div>
          </div>

          <div class="order-footer">
            <div class="order-total">
              <span>共 {{ order.items.length }} 件商品</span>
              <span class="total-amount">
                实付: <strong>¥{{ order.payAmount }}</strong>
              </span>
            </div>
            <div class="order-actions">
              <el-button
                v-if="order.status === 0"
                type="primary"
                @click="handlePay(order.id)"
              >
                立即支付
              </el-button>
              <el-button
                v-if="order.status === 0"
                @click="handleCancel(order.id)"
              >
                取消订单
              </el-button>
              <el-button
                v-if="order.status === 2"
                type="success"
                @click="handleConfirmReceipt(order.id)"
              >
                确认收货
              </el-button>
              <el-button @click="viewDetail(order.id)">
                查看详情
              </el-button>
            </div>
          </div>
        </el-card>

        <div class="pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="loadOrders"
          />
        </div>
      </div>

      <!-- 订单详情对话框 -->
      <el-dialog v-model="detailVisible" title="订单详情" width="600px">
        <template v-if="detailOrder">
          <div class="detail-header">
            <p><strong>订单号：</strong>{{ detailOrder.orderNo }}</p>
            <p><strong>下单时间：</strong>{{ formatDate(detailOrder.createdAt) }}</p>
            <p><strong>订单状态：</strong>
              <el-tag :type="getStatusType(detailOrder.status)">{{ detailOrder.statusText }}</el-tag>
            </p>
          </div>
          <el-divider />
          <div class="detail-items">
            <div v-for="item in detailOrder.items" :key="item.id" class="detail-item">
              <div class="detail-item-info">
                <span class="detail-item-name">{{ item.productName }}</span>
                <span class="detail-item-sku">SKU: {{ item.sku }}</span>
              </div>
              <div class="detail-item-price">
                <span>¥{{ item.priceAtPurchase }} x {{ item.quantity }}</span>
                <span class="detail-item-subtotal">¥{{ item.subtotal }}</span>
              </div>
            </div>
          </div>
          <el-divider />
          <div class="detail-footer">
            <p><strong>商品总数：</strong>{{ detailOrder.items.length }} 件</p>
            <p><strong>商品总额：</strong>¥{{ detailOrder.totalAmount }}</p>
            <p v-if="detailOrder.discountAmount > 0"><strong>优惠金额：</strong>¥{{ detailOrder.discountAmount }}</p>
            <p class="detail-pay-amount"><strong>实付金额：</strong>¥{{ detailOrder.payAmount }}</p>
            <p v-if="detailOrder.shippingAddress"><strong>收货地址：</strong>{{ detailOrder.shippingAddress }}</p>
            <p v-if="detailOrder.receivedTime"><strong>收货时间：</strong>{{ formatDate(detailOrder.receivedTime) }}</p>
            <p v-if="detailOrder.remark"><strong>备注：</strong>{{ detailOrder.remark }}</p>
          </div>
        </template>
      </el-dialog>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppLayout from '@/components/layout/AppLayout.vue'
import { getOrders, cancelOrder, payOrder, getOrderById, confirmReceipt } from '@/api/order'

const orders = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const detailOrder = ref(null)

onMounted(() => {
  loadOrders()
})

const loadOrders = async () => {
  try {
    const response = await getOrders({
      page: currentPage.value,
      size: pageSize.value
    })
    orders.value = response.items
    total.value = response.total
  } catch (error) {
    console.error('加载订单失败:', error)
    ElMessage.error('加载订单失败')
  }
}

const handlePay = async (orderId) => {
  try {
    await payOrder(orderId)
    ElMessage.success('支付成功')
    loadOrders()
  } catch (error) {
    ElMessage.error('支付失败')
  }
}

const handleCancel = async (orderId) => {
  try {
    await ElMessageBox.confirm('确定要取消这个订单吗？', '提示', {
      type: 'warning'
    })

    await cancelOrder(orderId)
    ElMessage.success('订单已取消')
    loadOrders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消订单失败')
    }
  }
}

const handleConfirmReceipt = async (orderId) => {
  try {
    await ElMessageBox.confirm('确定已收到商品吗？', '提示', {
      type: 'success'
    })

    await confirmReceipt(orderId)
    ElMessage.success('已确认收货')
    loadOrders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('确认收货失败')
    }
  }
}

const viewDetail = async (orderId) => {
  try {
    const res = await getOrderById(orderId)
    detailOrder.value = res
    detailVisible.value = true
  } catch (error) {
    ElMessage.error('获取订单详情失败')
  }
}

const getStatusType = (status) => {
  const types = {
    0: 'warning',   // 待付款
    1: 'success',   // 已付款
    2: 'primary',   // 已发货
    3: 'success',   // 已完成
    4: 'info'       // 已取消
  }
  return types[status] || 'info'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}
</script>

<style scoped>
.order-list-page {
  padding: 20px 0;
}

.order-list-page h2 {
  margin-bottom: 20px;
}

.empty-orders {
  text-align: center;
  padding: 60px 0;
}

.order-card {
  margin-bottom: 20px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.order-info {
  display: flex;
  gap: 20px;
}

.order-no {
  font-weight: bold;
}

.order-date {
  color: #909399;
}

.order-items {
  margin-bottom: 16px;
}

.order-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.order-item:last-child {
  border-bottom: none;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-name {
  font-weight: 500;
}

.item-sku {
  font-size: 12px;
  color: #909399;
}

.item-price {
  display: flex;
  gap: 20px;
  align-items: center;
}

.item-subtotal {
  font-weight: bold;
  color: #f56c6c;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}

.order-total {
  display: flex;
  gap: 20px;
  align-items: center;
}

.total-amount {
  font-size: 16px;
}

.total-amount strong {
  color: #f56c6c;
  font-size: 20px;
}

.order-actions {
  display: flex;
  gap: 12px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}
</style>
