<template>
  <div class="sales-monitor-page">
    <h2>销售监控</h2>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card stat-card-products" shadow="hover" @click="showProductList">
          <div class="stat-item">
            <div class="stat-label">商品总数</div>
            <div class="stat-value">{{ stats.totalProducts }}</div>
            <div class="stat-tip">点击展开列表 →</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-card-sales" shadow="hover" @click="showSalesList">
          <div class="stat-item">
            <div class="stat-label">总销量</div>
            <div class="stat-value">{{ stats.totalSales }}</div>
            <div class="stat-tip">点击查看详情 →</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-card-lowstock" shadow="hover" @click="showLowStockList">
          <div class="stat-item">
            <div class="stat-label">库存不足</div>
            <div class="stat-value" style="color: #e6a23c">{{ stats.lowStockCount }}</div>
            <div class="stat-tip">点击查看详情 →</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-card-outstock" shadow="hover" @click="showOutOfStockList">
          <div class="stat-item">
            <div class="stat-label">缺货商品</div>
            <div class="stat-value" style="color: #f56c6c">{{ stats.outOfStockCount }}</div>
            <div class="stat-tip">点击查看详情 →</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>库存状态分布</span>
          </template>
          <div ref="stockChartRef" style="width: 100%; height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>订单状态分布</span>
          </template>
          <div ref="orderChartRef" style="width: 100%; height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="content-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>库存不足商品</span>
          </template>
          <el-table :data="lowStockProducts" v-loading="loading" max-height="350" size="small">
            <el-table-column prop="name" label="商品名称" min-width="150" />
            <el-table-column prop="stock" label="库存" width="80" />
            <el-table-column prop="salesCount" label="销量" width="80" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.stock === 0" type="danger" size="small">缺货</el-tag>
                <el-tag v-else type="warning" size="small">不足</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="lowStockProducts.length === 0" class="empty-data">所有商品库存充足</div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <span>最近订单</span>
          </template>
          <el-table :data="recentOrders" v-loading="loading" max-height="350" size="small">
            <el-table-column prop="orderNo" label="订单号" width="180" />
            <el-table-column prop="totalAmount" label="金额" width="100">
              <template #default="{ row }">¥{{ row.totalAmount }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.status === 0" type="info" size="small">未支付</el-tag>
                <el-tag v-else-if="row.status === 1" type="success" size="small">已支付</el-tag>
                <el-tag v-else-if="row.status === 2" type="primary" size="small">已发货</el-tag>
                <el-tag v-else type="danger" size="small">已取消</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" width="160" />
          </el-table>
          <div v-if="recentOrders.length === 0" class="empty-data">暂无订单数据</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 商品列表弹窗 -->
    <el-dialog v-model="productListVisible" title="📦 全部商品列表" width="85%" top="5vh">
      <el-table :data="allProducts" border stripe style="width: 100%" max-height="500" size="small">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="name" label="商品名称" min-width="180" />
        <el-table-column prop="sku" label="SKU" width="110" />
        <el-table-column prop="price" label="价格" width="90" align="center">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="70" align="center" />
        <el-table-column prop="salesCount" label="销量" width="70" align="center" />
        <el-table-column label="状态" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 销量排行弹窗 -->
    <el-dialog v-model="salesListVisible" title="🔥 商品销量排行" width="85%" top="5vh">
      <el-table :data="allProducts" border stripe style="width: 100%" max-height="500" size="small" @row-click="handleProductRowClick">
        <el-table-column type="index" label="排名" width="70" align="center">
          <template #default="{ $index }">
            <span :class="'rank-' + ($index + 1)">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="200" />
        <el-table-column prop="sku" label="SKU" width="110" />
        <el-table-column prop="price" label="价格" width="90" align="center">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="70" align="center" />
        <el-table-column prop="salesCount" label="销量" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.salesCount > 50 ? 'success' : row.salesCount > 10 ? 'warning' : 'info'" size="small">
              {{ row.salesCount }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 库存不足商品弹窗 -->
    <el-dialog v-model="lowStockVisible" title="⚠️ 库存不足商品" width="75%" top="5vh">
      <el-table :data="lowStockProducts" border stripe style="width: 100%" max-height="500" size="small">
        <el-table-column prop="name" label="商品名称" min-width="200" />
        <el-table-column prop="sku" label="SKU" width="110" />
        <el-table-column prop="stock" label="当前库存" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.stock === 0 ? 'danger' : 'warning'" size="small">
              {{ row.stock }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="salesCount" label="销量" width="70" align="center" />
        <el-table-column label="建议" width="150" align="center">
          <template #default="{ row }">
            <span style="color: #e6a23c; font-size: 12px;">建议补货</span>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="lowStockProducts.length === 0" class="empty-data">暂无库存不足商品</div>
    </el-dialog>

    <!-- 缺货商品弹窗 -->
    <el-dialog v-model="outOfStockVisible" title="🚫 缺货商品" width="75%" top="5vh">
      <el-table :data="outOfStockProducts" border stripe style="width: 100%" max-height="500" size="small">
        <el-table-column prop="name" label="商品名称" min-width="200" />
        <el-table-column prop="sku" label="SKU" width="110" />
        <el-table-column prop="stock" label="当前库存" width="90" align="center">
          <template #default="{ row }">
            <el-tag type="danger" size="small">{{ row.stock }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="salesCount" label="历史销量" width="80" align="center" />
        <el-table-column label="建议" width="150" align="center">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-size: 12px;">急需补货</span>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="outOfStockProducts.length === 0" class="empty-data">暂无缺货商品</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getSalesStats, getLowStockProducts, getRecentOrders, getAllProducts } from '@/api/sales'

const stats = ref({
  totalProducts: 0,
  totalSales: 0,
  lowStockCount: 0,
  outOfStockCount: 0
})
const lowStockProducts = ref([])
const recentOrders = ref([])
const allProducts = ref([])
const outOfStockProducts = ref([])
const loading = ref(false)
const stockChartRef = ref(null)
const orderChartRef = ref(null)

const productListVisible = ref(false)
const salesListVisible = ref(false)
const lowStockVisible = ref(false)
const outOfStockVisible = ref(false)

let stockChart = null
let orderChart = null

onMounted(() => {
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const [statsData, lowStockData, ordersData, productsData] = await Promise.all([
      getSalesStats(),
      getLowStockProducts(),
      getRecentOrders(),
      getAllProducts()
    ])
    stats.value = statsData
    lowStockProducts.value = lowStockData
    recentOrders.value = ordersData
    allProducts.value = productsData
    outOfStockProducts.value = productsData.filter(p => p.stock === 0 || p.stock == null)

    await nextTick()
    initCharts()
  } catch (error) {
    console.error('加载销售数据失败:', error)
  } finally {
    loading.value = false
  }
}

const showProductList = () => {
  productListVisible.value = true
}

const showSalesList = () => {
  salesListVisible.value = true
}

const showLowStockList = () => {
  lowStockVisible.value = true
}

const showOutOfStockList = () => {
  outOfStockVisible.value = true
}

const handleProductRowClick = (row) => {
  // 预留：点击商品行可跳转商品详情
}

const initCharts = () => {
  initStockChart()
  initOrderChart()
}

const initStockChart = () => {
  if (!stockChartRef.value) return

  stockChart = echarts.init(stockChartRef.value)

  const normalStock = stats.value.totalProducts - stats.value.lowStockCount - stats.value.outOfStockCount

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '库存状态',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}: {c}'
        },
        data: [
          { value: normalStock > 0 ? normalStock : 0, name: '库存正常', itemStyle: { color: '#67c23a' } },
          { value: stats.value.lowStockCount, name: '库存不足', itemStyle: { color: '#e6a23c' } },
          { value: stats.value.outOfStockCount, name: '缺货', itemStyle: { color: '#f56c6c' } }
        ]
      }
    ]
  }

  stockChart.setOption(option)
}

const initOrderChart = () => {
  if (!orderChartRef.value) return

  orderChart = echarts.init(orderChartRef.value)

  const orderStats = {
    unpaid: 0,
    paid: 0,
    shipped: 0,
    cancelled: 0
  }

  recentOrders.value.forEach(order => {
    if (order.status === 0) orderStats.unpaid++
    else if (order.status === 1) orderStats.paid++
    else if (order.status === 2) orderStats.shipped++
    else orderStats.cancelled++
  })

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: ['订单数量']
    },
    xAxis: {
      type: 'category',
      data: ['未支付', '已支付', '已发货', '已取消']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '订单数量',
        type: 'bar',
        barWidth: '50%',
        itemStyle: {
          borderRadius: [4, 4, 0, 0]
        },
        data: [
          { value: orderStats.unpaid, itemStyle: { color: '#909399' } },
          { value: orderStats.paid, itemStyle: { color: '#67c23a' } },
          { value: orderStats.shipped, itemStyle: { color: '#409eff' } },
          { value: orderStats.cancelled, itemStyle: { color: '#f56c6c' } }
        ]
      }
    ],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    }
  }

  orderChart.setOption(option)
}
</script>

<style scoped>
.sales-monitor-page {
  padding: 20px;
}

.sales-monitor-page h2 {
  margin-bottom: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.charts-row {
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  padding: 10px;
  position: relative;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-tip {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 8px;
  opacity: 0;
  transition: opacity 0.3s;
}

.stat-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 12px;
  overflow: hidden;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12) !important;
}

.stat-card:hover .stat-tip {
  opacity: 1;
}

.stat-card-products {
  border-left: 4px solid #409eff;
}

.stat-card-sales {
  border-left: 4px solid #67c23a;
}

.stat-card-lowstock {
  border-left: 4px solid #e6a23c;
}

.stat-card-outstock {
  border-left: 4px solid #f56c6c;
}

.content-row {
  margin-bottom: 20px;
}

.empty-data {
  text-align: center;
  padding: 40px;
  color: #909399;
}

.rank-1 {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}

.rank-2 {
  color: #e6a23c;
  font-weight: bold;
  font-size: 15px;
}

.rank-3 {
  color: #409eff;
  font-weight: bold;
  font-size: 14px;
}
</style>
