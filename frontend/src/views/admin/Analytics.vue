<template>
  <div class="analytics-page">
    <h2>数据分析</h2>

    <!-- Tab 切换 -->
    <el-tabs v-model="activeTab" class="analytics-tabs" @tab-change="onTabChange">
      <el-tab-pane label="📊 概览大屏" name="overview">
        <el-row :gutter="20" class="stats-row">
          <el-col :span="6">
            <el-card class="stat-card stat-card-orders" shadow="hover" @click="showChartDialog('orders')">
              <div class="stat-item">
                <div class="stat-icon">📦</div>
                <div class="stat-label">今日订单</div>
                <div class="stat-value">{{ stats.todayOrders }}</div>
                <div class="stat-tip">点击查看详情 →</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card stat-card-sales" shadow="hover" @click="showChartDialog('sales')">
              <div class="stat-item">
                <div class="stat-icon">💰</div>
                <div class="stat-label">今日销售额</div>
                <div class="stat-value">¥{{ stats.todaySales }}</div>
                <div class="stat-tip">点击查看详情 →</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card stat-card-users" shadow="hover" @click="showUserList">
              <div class="stat-item">
                <div class="stat-icon">👥</div>
                <div class="stat-label">总用户数</div>
                <div class="stat-value">{{ stats.totalUsers }}</div>
                <div class="stat-tip">点击展开列表 →</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card stat-card-products" shadow="hover" @click="showProductList">
              <div class="stat-item">
                <div class="stat-icon">📱</div>
                <div class="stat-label">总商品数</div>
                <div class="stat-value">{{ stats.totalProducts }}</div>
                <div class="stat-tip">点击展开列表 →</div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" class="charts-row">
          <el-col :span="14">
            <el-card>
              <template #header><span>🔥 热销商品TOP10</span></template>
              <div ref="topProductsChartRef" style="width: 100%; height: 400px;" @click="showChartDialog('topProducts')"></div>
            </el-card>
          </el-col>
          <el-col :span="10">
            <el-card>
              <template #header>
                <span>📈 销售趋势</span>
                <el-radio-group v-model="salesTrendPeriod" size="small" style="float: right;" @change="onSalesTrendPeriodChange">
                  <el-radio-button value="day">日</el-radio-button>
                  <el-radio-button value="week">周</el-radio-button>
                  <el-radio-button value="month">月</el-radio-button>
                </el-radio-group>
              </template>
              <div ref="salesTrendChartRef" style="width: 100%; height: 400px;" @click="showChartDialog('salesTrend')"></div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" class="charts-row">
          <el-col :span="12">
            <el-card>
              <template #header><span>📊 分类销售统计</span></template>
              <div ref="categoryChartRef" style="width: 100%; height: 350px;" @click="showChartDialog('category')"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card>
              <template #header><span>📋 订单状态分布</span></template>
              <div ref="orderStatusChartRef" style="width: 100%; height: 350px;" @click="showChartDialog('orderStatus')"></div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" class="charts-row">
          <el-col :span="12">
            <el-card>
              <template #header><span>🏆 销售业绩排行</span></template>
              <div class="performance-list">
                <div v-for="(item, index) in performance" :key="index" class="performance-item" @click="showPerformanceDetail(item)">
                  <span class="rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
                  <div class="perf-user-info">
                    <span class="user-name">{{ item.userName }}</span>
                    <span class="user-id-label">ID: {{ item.userId }}</span>
                  </div>
                  <div class="perf-stats">
                    <span class="orders">订单: {{ item.orderCount }}</span>
                    <span class="amount">¥{{ item.totalAmount }}</span>
                  </div>
                  <div class="perf-bar">
                    <div class="perf-bar-inner" :style="{ width: (item.totalAmount / maxPerformanceAmount * 100) + '%' }"></div>
                  </div>
                </div>
                <div v-if="performance.length === 0" class="empty-data">暂无销售数据</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card>
              <template #header><span>📊 今日数据概览</span></template>
              <div class="summary-panel">
                <div class="summary-item summary-orders"><div class="summary-icon">📦</div><div class="summary-content"><div class="summary-value">{{ todayOverview.todayOrders }}</div><div class="summary-label">今日订单</div></div></div>
                <div class="summary-item summary-sales"><div class="summary-icon">💰</div><div class="summary-content"><div class="summary-value">¥{{ todayOverview.todaySales }}</div><div class="summary-label">今日销售额</div></div></div>
                <div class="summary-item summary-paid"><div class="summary-icon">✅</div><div class="summary-content"><div class="summary-value">{{ todayOverview.todayPaidOrders }}</div><div class="summary-label">今日已支付</div></div></div>
                <div class="summary-item summary-newusers"><div class="summary-icon">👤</div><div class="summary-content"><div class="summary-value">{{ todayOverview.todayNewUsers }}</div><div class="summary-label">今日新用户</div></div></div>
                <div class="summary-item summary-totalusers"><div class="summary-icon">👥</div><div class="summary-content"><div class="summary-value">{{ todayOverview.totalUsers }}</div><div class="summary-label">总用户数</div></div></div>
                <div class="summary-item summary-totalproducts"><div class="summary-icon">📱</div><div class="summary-content"><div class="summary-value">{{ todayOverview.totalProducts }}</div><div class="summary-label">总商品数</div></div></div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="👤 用户画像" name="profile">
        <el-row :gutter="20" class="charts-row">
          <el-col :span="8">
            <el-card>
              <template #header><span>📍 用户地域分布</span></template>
              <div ref="cityChartRef" style="width: 100%; height: 350px;"></div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card>
              <template #header><span>💰 购买力分析</span></template>
              <div ref="purchasingPowerChartRef" style="width: 100%; height: 350px;"></div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card>
              <template #header><span>🎯 用户偏好分类</span></template>
              <div ref="preferenceChartRef" style="width: 100%; height: 350px;"></div>
            </el-card>
          </el-col>
        </el-row>
        <el-row :gutter="20" class="charts-row">
          <el-col :span="24">
            <el-card>
              <template #header><span>📋 购买力分布详情</span></template>
              <div style="padding: 10px;">
                <el-table :data="purchasingPower.distribution || []" border stripe style="width: 100%">
                  <el-table-column prop="level" label="消费等级" min-width="150" />
                  <el-table-column prop="count" label="用户数量" min-width="100" align="center" />
                </el-table>
                <div style="margin-top: 10px; color: #666;">总用户数: {{ purchasingPower.totalUsers || 0 }}，总消费额: ¥{{ purchasingPower.totalSpending || 0 }}</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="📈 销售预测" name="prediction">
        <el-row :gutter="20" class="charts-row">
          <el-col :span="24">
            <el-card>
              <template #header>
                <span>📊 销售趋势预测（基于7日移动平均）</span>
                <el-button size="small" style="float: right;" @click="loadPredictionData">刷新数据</el-button>
              </template>
              <div ref="predictionChartRef" style="width: 100%; height: 450px;"></div>
            </el-card>
          </el-col>
        </el-row>
        <el-row :gutter="20" class="charts-row">
          <el-col :span="24">
            <el-card>
              <template #header><span>📋 预测数据表（未来7天）</span></template>
              <el-table :data="predictionData.predictions || []" border stripe style="width: 100%">
                <el-table-column prop="date" label="预测日期" min-width="150" />
                <el-table-column prop="amount" label="预测销售额" min-width="150" align="center">
                  <template #default="{ row }">¥{{ row.amount }}</template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="🚨 异常监控" name="anomaly">
        <el-row :gutter="20" class="charts-row">
          <el-col :span="24">
            <el-card>
              <template #header>
                <span>🚨 销售异常检测（Z-Score > 2 视为异常）</span>
                <el-tag v-if="anomalyData.totalAnomalies > 0" type="danger" style="float: right;">
                  发现 {{ anomalyData.totalAnomalies }} 个异常日
                </el-tag>
                <el-tag v-else type="success" style="float: right;">无异常</el-tag>
              </template>
              <div ref="anomalyChartRef" style="width: 100%; height: 450px;"></div>
            </el-card>
          </el-col>
        </el-row>
        <el-row :gutter="20" class="charts-row">
          <el-col :span="12">
            <el-card>
              <template #header><span>📋 异常详情</span></template>
              <el-table :data="anomalyData.anomalies || []" border stripe style="width: 100%">
                <el-table-column prop="date" label="日期" min-width="120" />
                <el-table-column prop="amount" label="销售额" min-width="100" align="center">
                  <template #default="{ row }">¥{{ row.amount }}</template>
                </el-table-column>
                <el-table-column prop="zScore" label="Z-Score" min-width="80" align="center" />
                <el-table-column prop="type" label="异常类型" min-width="80" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.type === '突增' ? 'danger' : 'warning'" size="small">{{ row.type }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="(!anomalyData.anomalies || anomalyData.anomalies.length === 0)" style="text-align:center;padding:20px;color:#999;">暂无异常数据</div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card>
              <template #header><span>📊 统计参数</span></template>
              <div class="param-list">
                <div class="param-item"><span class="param-label">均值(Mean)</span><span class="param-value">¥{{ anomalyData.mean }}</span></div>
                <div class="param-item"><span class="param-label">标准差(StdDev)</span><span class="param-value">{{ anomalyData.stdDev }}</span></div>
                <div class="param-item"><span class="param-label">异常阈值(Z-Score)</span><span class="param-value">{{ anomalyData.threshold }}</span></div>
                <div class="param-item"><span class="param-label">检测天数</span><span class="param-value">{{ anomalyData.dailyData ? anomalyData.dailyData.length : 0 }} 天</span></div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="📜 登录日志" name="loginLogs">
        <el-row :gutter="20" class="charts-row">
          <el-col :span="24">
            <el-card>
              <template #header><span>📜 用户登录日志</span></template>
              <el-table :data="loginLogs" border stripe style="width: 100%" max-height="600">
                <el-table-column prop="loginTime" label="登录时间" width="180" />
                <el-table-column prop="username" label="用户名" width="120" />
                <el-table-column prop="role" label="角色" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'SALES' ? 'warning' : 'info'" size="small">{{ row.role }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="ip" label="IP地址" width="140" />
                <el-table-column prop="status" label="状态" width="80" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="failureReason" label="失败原因" min-width="120" />
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="📋 操作日志" name="operationLogs">
        <el-row :gutter="20" class="charts-row">
          <el-col :span="24">
            <el-card>
              <template #header><span>📋 管理操作日志</span></template>
              <el-table :data="operationLogs" border stripe style="width: 100%" max-height="600">
                <el-table-column prop="createdAt" label="操作时间" width="180" />
                <el-table-column prop="username" label="操作用户" width="120" />
                <el-table-column prop="role" label="角色" width="80" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'warning'" size="small">{{ row.role }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="operation" label="操作类型" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.operation === 'create' ? 'success' : row.operation === 'delete' ? 'danger' : 'warning'" size="small">{{ row.operation }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="resource" label="资源" width="100" align="center" />
                <el-table-column prop="resourceId" label="资源ID" width="80" align="center" />
                <el-table-column prop="content" label="操作内容" min-width="200" />
                <el-table-column prop="ip" label="IP地址" width="140" />
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>

    <!-- 图表弹窗 -->
    <el-dialog v-model="chartDialogVisible" :title="chartDialogTitle" width="75%" top="5vh" class="chart-dialog" destroy-on-close>
      <div ref="dialogChartRef" style="width: 100%; height: 450px;"></div>
    </el-dialog>

    <!-- 销售业绩详情弹窗 -->
    <el-dialog v-model="perfDialogVisible" :title="'🏆 ' + (selectedPerformance?.userName || '') + ' 的销售详情'" width="500px">
      <div class="performance-detail">
        <div class="detail-row"><span class="detail-label">销售人员</span><span class="detail-value">{{ selectedPerformance?.userName }}</span></div>
        <div class="detail-row"><span class="detail-label">用户ID</span><span class="detail-value">{{ selectedPerformance?.userId }}</span></div>
        <div class="detail-row"><span class="detail-label">订单数量</span><span class="detail-value">{{ selectedPerformance?.orderCount }}</span></div>
        <div class="detail-row"><span class="detail-label">总销售额</span><span class="detail-value highlight">¥{{ selectedPerformance?.totalAmount }}</span></div>
        <div class="detail-row"><span class="detail-label">总销量(件)</span><span class="detail-value">{{ selectedPerformance?.totalQuantity }}</span></div>
        <div class="detail-row"><span class="detail-label">平均订单金额</span><span class="detail-value">¥{{ selectedPerformance ? (selectedPerformance.totalAmount / selectedPerformance.orderCount).toFixed(2) : '0' }}</span></div>
      </div>
    </el-dialog>

    <!-- 用户列表弹窗 -->
    <el-dialog v-model="userListVisible" title="👥 用户列表" width="80%" top="5vh">
      <el-table :data="userList" border stripe style="width: 100%" max-height="500">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" min-width="180" />
      </el-table>
    </el-dialog>

    <!-- 商品列表弹窗 -->
    <el-dialog v-model="productListVisible" title="📱 商品列表" width="85%" top="5vh">
      <el-table :data="productList" border stripe style="width: 100%" max-height="500">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="商品名称" min-width="200" />
        <el-table-column prop="sku" label="SKU" width="120" />
        <el-table-column prop="price" label="价格" width="100" align="center"><template #default="{ row }">¥{{ row.price }}</template></el-table-column>
        <el-table-column prop="stock" label="库存" width="80" align="center" />
        <el-table-column prop="salesCount" label="销量" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '上架' : '下架' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue';
import * as echarts from 'echarts';
import {
  getAnalyticsStats, getTopProducts, getSalesTrend, getSalesByCategory,
  getSalesPerformance, getOrderStatusDistribution, getTodayOverview,
  getUserList, getProductList, getUserCityDistribution, getUserPurchasingPower,
  getUserCategoryPreference, getSalesPrediction, getSalesAnomalyDetection,
  getLoginLogs, getOperationLogs, getTodayOrdersDetail, getTodaySalesDetail
} from '@/api/analytics';

const activeTab = ref('overview');
const stats = ref({ todayOrders: 0, todaySales: 0, totalUsers: 0, totalProducts: 0 });
const topProducts = ref([]);
const salesTrend = ref([]);
const categoryStats = ref([]);
const performance = ref([]);
const orderStats = ref({ unpaid: 0, paid: 0, shipped: 0, completed: 0, cancelled: 0 });
const todayOverview = ref({ todayOrders: 0, todaySales: 0, todayNewUsers: 0, todayPaidOrders: 0, totalProducts: 0, totalUsers: 0 });
const userList = ref([]);
const productList = ref([]);
const cityDistribution = ref([]);
const purchasingPower = ref({ distribution: [], totalUsers: 0, totalSpending: 0 });
const categoryPreference = ref([]);
const predictionData = ref({ history: [], predictions: [] });
const anomalyData = ref({ mean: 0, stdDev: 0, threshold: 2, dailyData: [], anomalies: [], totalAnomalies: 0 });
const loginLogs = ref([]);
const operationLogs = ref([]);

const todayOrdersDetail = ref([]);
const todaySalesDetail = ref([]);
const salesTrendPeriod = ref('week');

const chartDialogVisible = ref(false);
const chartDialogTitle = ref('');
const perfDialogVisible = ref(false);
const selectedPerformance = ref(null);
const userListVisible = ref(false);
const productListVisible = ref(false);

const topProductsChartRef = ref(null);
const salesTrendChartRef = ref(null);
const categoryChartRef = ref(null);
const orderStatusChartRef = ref(null);
const cityChartRef = ref(null);
const purchasingPowerChartRef = ref(null);
const preferenceChartRef = ref(null);
const predictionChartRef = ref(null);
const anomalyChartRef = ref(null);
const dialogChartRef = ref(null);

let topProductsChart = null, salesTrendChart = null, categoryChart = null, orderStatusChart = null;
let cityChart = null, purchasingPowerChart = null, preferenceChart = null;
let predictionChart = null, anomalyChart = null, dialogChart = null;

const maxPerformanceAmount = computed(() => {
  if (performance.value.length === 0) return 1;
  return Math.max(...performance.value.map(p => parseFloat(p.totalAmount) || 0));
});

onMounted(() => {
  loadOverviewData();
  loadProfileData();
  loadPredictionData();
  loadAnomalyData();
  loadLoginLogs();
  loadOperationLogs();
  window.addEventListener('resize', handleResize);
});

const handleResize = () => {
  const charts = [topProductsChart, salesTrendChart, categoryChart, orderStatusChart,
    cityChart, purchasingPowerChart, preferenceChart, predictionChart, anomalyChart];
  charts.forEach(c => c && c.resize());
};

const onTabChange = (tab) => {
  nextTick(() => {
    requestAnimationFrame(() => {
      setTimeout(() => {
        if (tab === 'overview') initOverviewCharts();
        else if (tab === 'profile') initProfileCharts();
        else if (tab === 'prediction') initPredictionChart();
        else if (tab === 'anomaly') initAnomalyChart();
      }, 100);
    });
  });
};

const loadOverviewData = async (period) => {
  try {
    const [statsData, productsData, trendData, categoryData, perfData, orderStatusData, overviewData] = await Promise.all([
      getAnalyticsStats(), getTopProducts(10), getSalesTrend(period || salesTrendPeriod.value), getSalesByCategory(),
      getSalesPerformance(), getOrderStatusDistribution(), getTodayOverview()
    ]);
    stats.value = statsData; topProducts.value = productsData; salesTrend.value = trendData;
    categoryStats.value = categoryData; performance.value = perfData;
    orderStats.value = orderStatusData; todayOverview.value = overviewData;
    await nextTick(); initOverviewCharts();
  } catch (e) { console.error('加载概览数据失败:', e); }
};

const loadProfileData = async () => {
  try {
    const [city, power, pref] = await Promise.all([
      getUserCityDistribution(), getUserPurchasingPower(), getUserCategoryPreference()
    ]);
    cityDistribution.value = city; purchasingPower.value = power; categoryPreference.value = pref;
    if (activeTab.value === 'profile') {
      await nextTick(); initProfileCharts();
    }
  } catch (e) { console.error('加载画像数据失败:', e); }
};

const loadPredictionData = async () => {
  try {
    predictionData.value = await getSalesPrediction(7);
    if (activeTab.value === 'prediction') {
      await nextTick(); initPredictionChart();
    }
  } catch (e) { console.error('加载预测数据失败:', e); }
};

const loadAnomalyData = async () => {
  try {
    anomalyData.value = await getSalesAnomalyDetection(30);
    if (activeTab.value === 'anomaly') {
      await nextTick(); initAnomalyChart();
    }
  } catch (e) { console.error('加载异常数据失败:', e); }
};

const loadLoginLogs = async () => {
  try { loginLogs.value = await getLoginLogs(50); } catch (e) {}
};

const loadOperationLogs = async () => {
  try { operationLogs.value = await getOperationLogs({ limit: 50 }); } catch (e) {}
};

const initOverviewCharts = () => {
  initTopProductsChart();
  initSalesTrendChart();
  initCategoryChart();
  initOrderStatusChart();
};

const initTopProductsChart = () => {
  if (!topProductsChartRef.value) return;
  topProductsChart = echarts.init(topProductsChartRef.value);
  const names = topProducts.value.map(p => p.name).reverse();
  const values = topProducts.value.map(p => p.salesCount).reverse();
  topProductsChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: p => `<strong>${p[0].name}</strong><br/>销量: ${p[0].value} 件` },
    grid: { left: '3%', right: '8%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed', color: '#eee' } } },
    yAxis: { type: 'category', data: names, axisLabel: { width: 80, overflow: 'truncate' } },
    series: [{ type: 'bar', data: values.map(v => ({ value: v, itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#667eea' }, { offset: 1, color: '#764ba2' }]), borderRadius: [0, 6, 6, 0] } })), barWidth: '55%', label: { show: true, position: 'right', formatter: '{c} 件' } }]
  });
};

const initSalesTrendChart = () => {
  if (!salesTrendChartRef.value) return;
  if (salesTrendChart) salesTrendChart.dispose();
  salesTrendChart = echarts.init(salesTrendChartRef.value);
  const xData = salesTrend.value.map(i => i.label || i.date);
  const isHourly = xData.length === 24;
  salesTrendChart.setOption({
    tooltip: { trigger: 'axis', formatter: p => `<strong>${p[0].name}</strong><br/>销售额: ¥${p[0].value}` },
    grid: { left: '3%', right: '4%', bottom: '8%', containLabel: true },
    xAxis: { type: 'category', data: xData, axisLabel: isHourly ? {} : { rotate: 45, fontSize: 10 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed', color: '#eee' } }, axisLabel: { formatter: '¥{value}' } },
    series: [{ type: 'line', smooth: true, data: salesTrend.value.map(i => parseFloat(i.amount) || 0), areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(102,126,234,0.6)' }, { offset: 1, color: 'rgba(102,126,234,0.05)' }]) }, lineStyle: { color: '#667eea', width: 3 }, itemStyle: { color: '#667eea' } }]
  });
};

const initCategoryChart = () => {
  if (!categoryChartRef.value) return;
  categoryChart = echarts.init(categoryChartRef.value);
  const colors = ['#36a2eb','#ff6384','#ffce56','#4bc0c0','#9966ff','#ff9f40','#7c3aed'];
  categoryChart.setOption({
    tooltip: { trigger: 'item', formatter: p => `<strong>${p.name}</strong><br/>销量: ${p.value} 件<br/>占比: ${p.percent}%` },
    legend: { orient: 'vertical', left: 'left' },
    series: [{ type: 'pie', radius: ['40%','70%'], center: ['55%','50%'], avoidLabelOverlap: false, itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 }, label: { show: true, formatter: '{b}: {c}' }, data: categoryStats.value.map((i, idx) => ({ value: i.totalSales || 0, name: i.categoryName || '分类'+i.categoryId, itemStyle: { color: colors[idx % colors.length] } })) }]
  });
};

const initOrderStatusChart = () => {
  if (!orderStatusChartRef.value) return;
  orderStatusChart = echarts.init(orderStatusChartRef.value);
  orderStatusChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: p => `<strong>${p[0].name}</strong><br/>数量: ${p[0].value} 单` },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['未支付','已支付','已发货','已完成','已取消'] },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed', color: '#eee' } } },
    series: [{ type: 'bar', barWidth: '50%', data: [
      { value: orderStats.value.unpaid, itemStyle: { color: '#909399' } },
      { value: orderStats.value.paid, itemStyle: { color: '#67c23a' } },
      { value: orderStats.value.shipped, itemStyle: { color: '#409eff' } },
      { value: orderStats.value.completed, itemStyle: { color: '#7c3aed' } },
      { value: orderStats.value.cancelled, itemStyle: { color: '#f56c6c' } }
    ], itemStyle: { borderRadius: [6,6,0,0] }, label: { show: true, position: 'top', formatter: '{c} 单' } }]
  });
};

const initProfileCharts = () => {
  initCityChart();
  initPurchasingPowerChart();
  initPreferenceChart();
};

const initCityChart = () => {
  if (!cityChartRef.value) return;
  cityChart = echarts.init(cityChartRef.value);
  cityChart.setOption({
    tooltip: { trigger: 'item', formatter: p => `<strong>${p.name}</strong><br/>用户数: ${p.value}` },
    series: [{ type: 'pie', radius: ['30%','60%'], center: ['50%','50%'], label: { show: true, formatter: '{b}\n{d}%' }, data: cityDistribution.value.map((i, idx) => ({ value: i.count, name: i.city, itemStyle: { color: ['#36a2eb','#ff6384','#ffce56','#4bc0c0','#9966ff','#ff9f40'][idx % 6] } })) }]
  });
};

const initPurchasingPowerChart = () => {
  if (!purchasingPowerChartRef.value) return;
  if (purchasingPowerChart) purchasingPowerChart.dispose();
  purchasingPowerChart = echarts.init(purchasingPowerChartRef.value);
  const dist = purchasingPower.value.distribution || [];
  purchasingPowerChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: p => `<strong>${p[0].name}</strong><br/>用户数: ${p[0].value}` },
    grid: { left: '5%', right: '5%', bottom: '15%', containLabel: true },
    xAxis: { type: 'category', data: dist.map(i => i.level), axisLabel: { rotate: 15, fontSize: 11 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed', color: '#eee' } } },
    series: [{ type: 'bar', barWidth: '40%', barMaxWidth: 50, data: dist.map(i => ({ value: i.count, itemStyle: { color: i.level.includes('高') ? '#f56c6c' : i.level.includes('中') ? '#e6a23c' : i.level.includes('低') ? '#909399' : '#67c23a' } })), label: { show: true, position: 'top', formatter: '{c} 人', fontSize: 11 } }]
  });
};

const initPreferenceChart = () => {
  if (!preferenceChartRef.value) return;
  preferenceChart = echarts.init(preferenceChartRef.value);
  preferenceChart.setOption({
    tooltip: { trigger: 'item', formatter: p => `<strong>${p.name}</strong><br/>购买次数: ${p.value}` },
    legend: { orient: 'vertical', left: 'left', textStyle: { fontSize: 11 } },
    series: [{ type: 'pie', radius: ['30%','60%'], center: ['55%','55%'], label: { show: true, formatter: '{b}\n{d}%', fontSize: 10 }, data: categoryPreference.value.map((i, idx) => ({ value: i.salesCount, name: i.categoryName, itemStyle: { color: ['#36a2eb','#ff6384','#ffce56','#4bc0c0','#9966ff','#ff9f40','#7c3aed'][idx % 7] } })) }]
  });
};

const initPredictionChart = () => {
  if (!predictionChartRef.value) return;
  if (predictionChart) predictionChart.dispose();
  predictionChart = echarts.init(predictionChartRef.value);
  const history = predictionData.value.history || [];
  const predictions = predictionData.value.predictions || [];

  const allDates = [...history.map(i => i.date), ...predictions.map(i => i.date)];
  const historyAmounts = history.map(i => parseFloat(i.amount));
  const predictionAmounts = [...Array(history.length).fill(null), ...predictions.map(i => parseFloat(i.amount))];

  predictionChart.setOption({
    tooltip: { trigger: 'axis', formatter: p => {
      let s = `<strong>${p[0].name}</strong><br/>`;
      p.forEach(i => { s += `${i.marker} ${i.seriesName}: ¥${parseFloat(i.value).toFixed(2)}<br/>`; });
      return s;
    }},
    legend: { data: ['历史数据', '预测数据'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: allDates, axisLabel: { rotate: 45, fontSize: 10 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed', color: '#eee' } }, axisLabel: { formatter: '¥{value}' } },
    series: [
      { name: '历史数据', type: 'line', smooth: true, data: historyAmounts, lineStyle: { color: '#667eea', width: 3 }, itemStyle: { color: '#667eea' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(102,126,234,0.6)' }, { offset: 1, color: 'rgba(102,126,234,0.05)' }]) } },
      { name: '预测数据', type: 'line', smooth: true, data: predictionAmounts, lineStyle: { color: '#f56c6c', width: 3, type: 'dashed' }, itemStyle: { color: '#f56c6c' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(245,108,108,0.4)' }, { offset: 1, color: 'rgba(245,108,108,0.02)' }]) } }
    ]
  });
};

const initAnomalyChart = () => {
  if (!anomalyChartRef.value) return;
  if (anomalyChart) anomalyChart.dispose();
  anomalyChart = echarts.init(anomalyChartRef.value);
  const data = anomalyData.value.dailyData || [];
  const dates = data.map(i => i.date);
  const amounts = data.map(i => parseFloat(i.amount));
  const meanVal = parseFloat(anomalyData.value.mean || 0);

  anomalyChart.setOption({
    tooltip: { trigger: 'axis', formatter: p => {
      let s = `<strong>${p[0].name}</strong><br/>`;
      p.forEach(i => { s += `${i.marker} ${i.seriesName}: ${typeof i.value === 'number' ? '¥' + i.value.toFixed(2) : i.value}<br/>`; });
      return s;
    }},
    legend: { data: ['每日销售额', '均值线', '异常点'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: 45, fontSize: 10 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed', color: '#eee' } }, axisLabel: { formatter: '¥{value}' } },
    series: [
      { name: '每日销售额', type: 'line', smooth: true, data: amounts, lineStyle: { color: '#409eff', width: 2 }, itemStyle: { color: '#409eff' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(64,158,255,0.5)' }, { offset: 1, color: 'rgba(64,158,255,0.05)' }]) } },
      { name: '均值线', type: 'line', data: Array(dates.length).fill(meanVal), lineStyle: { color: '#67c23a', width: 2, type: 'dashed' }, symbol: 'none' },
      { name: '异常点', type: 'scatter', data: data.map((d, i) => d.isAnomaly ? [i, parseFloat(d.amount)] : null).filter(Boolean), symbolSize: 12, itemStyle: { color: '#f56c6c' } }
    ]
  });
};

const onSalesTrendPeriodChange = async (period) => {
  salesTrend.value = await getSalesTrend(period);
  await nextTick();
  if (salesTrendChart) salesTrendChart.dispose();
  initSalesTrendChart();
};

const showChartDialog = async (type) => {
  chartDialogTitle.value = { orders: '今日订单(按小时)', sales: '今日销售额(按小时)', users: '用户统计', products: '商品统计', topProducts: '热销商品TOP10', salesTrend: '销售趋势', category: '分类统计', orderStatus: '订单状态' }[type] || '详情';
  chartDialogVisible.value = true;
  if (type === 'orders') {
    todayOrdersDetail.value = await getTodayOrdersDetail();
  } else if (type === 'sales') {
    todaySalesDetail.value = await getTodaySalesDetail();
  }
  nextTick(() => initDialogChart(type));
};

const initDialogChart = (type) => {
  if (!dialogChartRef.value) return;
  dialogChart = echarts.init(dialogChartRef.value);
  if (type === 'orders') {
    dialogChart.setOption({
      title: { text: '今日订单 (按小时)', left: 'center' },
      tooltip: { trigger: 'axis', formatter: p => '<strong>' + p[0].name + '</strong><br/>订单数: ' + p[0].value + ' 单' },
      grid: { left: '6%', right: '4%', bottom: '8%', containLabel: true },
      xAxis: { type: 'category', data: todayOrdersDetail.value.map(i => i.hour), axisLabel: { rotate: 45, fontSize: 10 } },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ type: 'bar', data: todayOrdersDetail.value.map(i => ({
        value: i.orders,
        itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#409eff'},{offset:1,color:'#66b1ff'}]) }
      })), barWidth: '60%', label: { show: true, position: 'top', formatter: '{c} 单' } }]
    });
  } else if (type === 'sales') {
    dialogChart.setOption({
      title: { text: '今日销售额 (按小时)', left: 'center' },
      tooltip: { trigger: 'axis', formatter: p => '<strong>' + p[0].name + '</strong><br/>销售额: ¥' + p[0].value },
      grid: { left: '6%', right: '4%', bottom: '8%', containLabel: true },
      xAxis: { type: 'category', data: todaySalesDetail.value.map(i => i.hour), axisLabel: { rotate: 45, fontSize: 10 } },
      yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
      series: [{ type: 'bar', data: todaySalesDetail.value.map(i => ({
        value: parseFloat(i.amount) || 0,
        itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#67c23a'},{offset:1,color:'#95d475'}]) }
      })), barWidth: '60%', label: { show: true, position: 'top', formatter: p => '¥' + p.value.toFixed(2) } }]
    });
  } else if (type === 'topProducts') {
    const names = topProducts.value.map(p => p.name).reverse();
    const values = topProducts.value.map(p => p.salesCount).reverse();
    dialogChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      xAxis: { type: 'value' }, yAxis: { type: 'category', data: names },
      series: [{ type: 'bar', data: values, itemStyle: { color: new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:'#667eea'},{offset:1,color:'#764ba2'}]) } }]
    });
  } else if (type === 'salesTrend') {
    dialogChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: salesTrend.value.map(i => i.label || i.date), axisLabel: { rotate: 45, fontSize: 10 } },
      yAxis: { type: 'value' },
      series: [{ type: 'line', smooth: true, data: salesTrend.value.map(i => parseFloat(i.amount) || 0), areaStyle: {} }]
    });
  } else if (type === 'category') {
    dialogChart.setOption({
      series: [{ type: 'pie', radius: ['30%','60%'], data: categoryStats.value.map(i => ({ value: i.totalSales || 0, name: i.categoryName })) }]
    });
  } else if (type === 'orderStatus') {
    dialogChart.setOption({
      xAxis: { type: 'category', data: ['未支付','已支付','已发货','已完成','已取消'] },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: [orderStats.value.unpaid, orderStats.value.paid, orderStats.value.shipped, orderStats.value.completed, orderStats.value.cancelled] }]
    });
  }
};

const showPerformanceDetail = (item) => { selectedPerformance.value = item; perfDialogVisible.value = true; };
const showUserList = async () => { userList.value = await getUserList(); userListVisible.value = true; };
const showProductList = async () => { productList.value = await getProductList(); productListVisible.value = true; };
</script>

<style scoped>
.analytics-page { padding: 10px; }
.analytics-page h2 { margin-bottom: 20px; color: #303133; }
.analytics-tabs { min-height: 600px; }
.stats-row, .charts-row { margin-bottom: 20px; }
.stat-card { cursor: pointer; transition: all 0.3s; }
.stat-card:hover { transform: translateY(-3px); box-shadow: 0 6px 16px rgba(0,0,0,0.12); }
.stat-card-orders { border-left: 4px solid #409eff; }
.stat-card-sales { border-left: 4px solid #67c23a; }
.stat-card-users { border-left: 4px solid #e6a23c; }
.stat-card-products { border-left: 4px solid #7c3aed; }
.stat-item { text-align: center; padding: 10px 0; }
.stat-icon { font-size: 28px; margin-bottom: 8px; }
.stat-label { font-size: 13px; color: #909399; margin-bottom: 4px; }
.stat-value { font-size: 24px; font-weight: bold; color: #303133; }
.stat-tip { font-size: 12px; color: #c0c4cc; margin-top: 6px; }
.performance-list { max-height: 400px; overflow-y: auto; }
.performance-item { display: flex; align-items: center; gap: 10px; padding: 10px; border-bottom: 1px solid #f0f0f0; cursor: pointer; transition: background 0.2s; }
.performance-item:hover { background: #f5f7fa; }
.rank { width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 12px; color: #fff; flex-shrink: 0; }
.rank-1 { background: linear-gradient(135deg, #f5af19, #f12711); }
.rank-2 { background: linear-gradient(135deg, #bdc3c7, #2c3e50); }
.rank-3 { background: linear-gradient(135deg, #c94b4b, #4b134f); }
.rank-4, .rank-5, .rank-6, .rank-7, .rank-8, .rank-9, .rank-10 { background: #909399; }
.perf-user-info { flex: 1; display: flex; flex-direction: column; }
.user-name { font-weight: 500; font-size: 14px; }
.user-id-label { font-size: 11px; color: #999; }
.perf-stats { text-align: right; font-size: 13px; white-space: nowrap; }
.perf-stats .orders { color: #666; margin-right: 8px; }
.perf-stats .amount { font-weight: bold; color: #e6a23c; }
.perf-bar { width: 60px; height: 6px; background: #f0f0f0; border-radius: 3px; overflow: hidden; }
.perf-bar-inner { height: 100%; background: linear-gradient(90deg, #667eea, #764ba2); border-radius: 3px; transition: width 0.5s; }
.empty-data { text-align: center; padding: 30px; color: #999; font-size: 14px; }
.summary-panel { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.summary-item { display: flex; align-items: center; gap: 10px; padding: 12px; border-radius: 8px; }
.summary-orders { background: #ecf5ff; } .summary-sales { background: #f0f9eb; } .summary-paid { background: #fdf6ec; }
.summary-newusers { background: #fef0f0; } .summary-totalusers { background: #f5f0ff; } .summary-totalproducts { background: #e6f7ff; }
.summary-icon { font-size: 24px; }
.summary-content { flex: 1; }
.summary-value { font-size: 18px; font-weight: bold; color: #303133; }
.summary-label { font-size: 12px; color: #909399; }
.performance-detail { padding: 10px; }
.detail-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f0f0f0; }
.detail-label { color: #666; font-size: 14px; }
.detail-value { font-weight: 500; color: #303133; }
.detail-value.highlight { color: #e6a23c; font-size: 16px; }
.param-list { padding: 10px; }
.param-item { display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.param-label { color: #666; font-size: 14px; }
.param-value { font-weight: 500; color: #303133; font-size: 16px; }
:deep(.chart-dialog .el-dialog__body) { padding: 20px; }
</style>