import request from '@/utils/request'

export function getPaidOrders(params) {
  return request({
    url: '/sales/orders/paid',
    method: 'get',
    params
  })
}

export function shipOrder(orderId) {
  return request({
    url: `/sales/orders/${orderId}/ship`,
    method: 'post'
  })
}

export function getShippedOrders(params) {
  return request({
    url: '/sales/orders/shipped',
    method: 'get',
    params
  })
}