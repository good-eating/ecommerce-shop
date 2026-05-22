import request from '@/utils/request'

export function getCart() {
  return request({
    url: '/cart',
    method: 'get'
  })
}

export function addToCart(productId, quantity = 1) {
  return request({
    url: '/cart/items',
    method: 'post',
    params: { productId, quantity }
  })
}

export function updateCartItem(cartItemId, quantity) {
  return request({
    url: `/cart/items/${cartItemId}`,
    method: 'put',
    params: { quantity }
  })
}

export function removeCartItem(cartItemId) {
  return request({
    url: `/cart/items/${cartItemId}`,
    method: 'delete'
  })
}

export function clearCart() {
  return request({
    url: '/cart',
    method: 'delete'
  })
}