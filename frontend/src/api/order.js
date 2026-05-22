import request from '@/utils/request'

export function createOrder(data) {
  return request({
    url: '/orders',
    method: 'post',
    data
  })
}

export function getOrders(params) {
  return request({
    url: '/orders',
    method: 'get',
    params
  })
}

export function getOrderById(orderId) {
  return request({
    url: `/orders/${orderId}`,
    method: 'get'
  })
}

export function cancelOrder(orderId) {
  return request({
    url: `/orders/${orderId}/cancel`,
    method: 'post'
  })
}

export function payOrder(orderId) {
  return request({
    url: `/orders/${orderId}/pay`,
    method: 'post'
  })
}

export function confirmReceipt(orderId) {
  return request({
    url: `/orders/${orderId}/confirm-receipt`,
    method: 'post'
  })
}