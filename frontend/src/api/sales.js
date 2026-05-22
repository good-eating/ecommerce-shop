import request from '@/utils/request'

export function getSalesStats() {
  return request({
    url: '/sales-monitor/stats',
    method: 'get'
  })
}

export function getLowStockProducts() {
  return request({
    url: '/sales-monitor/low-stock-products',
    method: 'get'
  })
}

export function getRecentOrders() {
  return request({
    url: '/sales-monitor/recent-orders',
    method: 'get'
  })
}

export function getAllProducts() {
  return request({
    url: '/sales-monitor/all-products',
    method: 'get'
  })
}
