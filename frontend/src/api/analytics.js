import request from '@/utils/request'

export function getAnalyticsStats() {
  return request({
    url: '/admin/analytics/stats',
    method: 'get'
  })
}

export function getTopProducts(limit = 10) {
  return request({
    url: '/admin/analytics/top-products',
    method: 'get',
    params: { limit }
  })
}

export function getSalesTrend(period = 'week') {
  return request({
    url: '/admin/analytics/sales-trend',
    method: 'get',
    params: { period }
  })
}

export function getSalesByCategory() {
  return request({
    url: '/admin/analytics/sales-by-category',
    method: 'get'
  })
}

export function getSalesPerformance() {
  return request({
    url: '/admin/analytics/sales-performance',
    method: 'get'
  })
}

export function getOrderStatusDistribution() {
  return request({
    url: '/admin/analytics/order-status-distribution',
    method: 'get'
  })
}

export function getTodayOverview() {
  return request({
    url: '/admin/analytics/today-overview',
    method: 'get'
  })
}

export function getUserList() {
  return request({
    url: '/admin/analytics/user-list',
    method: 'get'
  })
}

export function getProductList() {
  return request({
    url: '/admin/analytics/product-list',
    method: 'get'
  })
}

export function getUserCityDistribution() {
  return request({
    url: '/admin/analytics/user-profile-city',
    method: 'get'
  })
}

export function getUserPurchasingPower() {
  return request({
    url: '/admin/analytics/user-purchasing-power',
    method: 'get'
  })
}

export function getUserCategoryPreference() {
  return request({
    url: '/admin/analytics/user-category-preference',
    method: 'get'
  })
}

export function getSalesPrediction(days = 7) {
  return request({
    url: '/admin/analytics/sales-prediction',
    method: 'get',
    params: { days }
  })
}

export function getSalesAnomalyDetection(days = 30) {
  return request({
    url: '/admin/analytics/sales-anomaly-detection',
    method: 'get',
    params: { days }
  })
}

export function getLoginLogs(limit = 50) {
  return request({
    url: '/admin/analytics/login-logs',
    method: 'get',
    params: { limit }
  })
}

export function getOperationLogs(params) {
  return request({
    url: '/admin/operation-logs',
    method: 'get',
    params
  })
}

export function getTodayOrdersDetail() {
  return request({
    url: '/admin/analytics/today-orders-detail',
    method: 'get'
  })
}

export function getTodaySalesDetail() {
  return request({
    url: '/admin/analytics/today-sales-detail',
    method: 'get'
  })
}