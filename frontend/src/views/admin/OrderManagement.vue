<template>
  <div class="order-management">
    <div class="page-header">
      <h2>订单管理</h2>
    </div>

    <el-card>
      <el-tabs v-model="activeTab" @tab-change="loadOrders">
        <el-tab-pane label="待发货" name="paid">
          <el-table :data="orders" v-loading="loading" empty-text="暂无待发货订单">
            <el-table-column prop="orderNo" label="订单号" min-width="180" />
            <el-table-column label="下单时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="商品" min-width="200">
              <template #default="{ row }">
                <div v-for="item in row.items" :key="item.id" class="order-item-info">
                  {{ item.productName }} x {{ item.quantity }}
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="payAmount" label="实付金额" width="120">
              <template #default="{ row }">
                ¥{{ row.payAmount }}
              </template>
            </el-table-column>
            <el-table-column label="收货地址" min-width="200">
              <template #default="{ row }">
                {{ row.shippingAddress || '未填写' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag type="success">{{ row.statusText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="handleShip(row.id)">
                  发货
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="已发货/已收货" name="shipped">
          <el-table :data="orders" v-loading="loading" empty-text="暂无已发货订单">
            <el-table-column prop="orderNo" label="订单号" min-width="180" />
            <el-table-column label="下单时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="商品" min-width="200">
              <template #default="{ row }">
                <div v-for="item in row.items" :key="item.id" class="order-item-info">
                  {{ item.productName }} x {{ item.quantity }}
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="payAmount" label="实付金额" width="120">
              <template #default="{ row }">
                ¥{{ row.payAmount }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.status === 2" type="primary">已发货</el-tag>
                <el-tag v-else type="success">已收货</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="收货时间" width="180">
              <template #default="{ row }">
                {{ row.receivedTime ? formatDate(row.receivedTime) : '未收货' }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadOrders"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPaidOrders, getShippedOrders, shipOrder } from '@/api/salesOrder'

const activeTab = ref('paid')
const orders = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

onMounted(() => {
  loadOrders()
})

const loadOrders = async () => {
  loading.value = true
  try {
    let response
    if (activeTab.value === 'paid') {
      response = await getPaidOrders({
        page: currentPage.value,
        size: pageSize.value
      })
    } else {
      response = await getShippedOrders({
        page: currentPage.value,
        size: pageSize.value
      })
    }
    orders.value = response.items
    total.value = response.total
  } catch (error) {
    console.error('加载订单失败:', error)
    ElMessage.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

const handleShip = async (orderId) => {
  try {
    await ElMessageBox.confirm('确定要发货该订单吗？', '提示', {
      type: 'warning'
    })

    await shipOrder(orderId)
    ElMessage.success('发货成功')
    loadOrders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('发货失败')
    }
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}
</script>

<style scoped>
.order-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.order-item-info {
  padding: 2px 0;
}
</style>