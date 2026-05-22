import request from '@/utils/request'

export function getProducts(params) {
  return request({
    url: '/products',
    method: 'get',
    params
  })
}

export function getAllProducts(params) {
  return request({
    url: '/products/admin/all',
    method: 'get',
    params
  })
}

export function getProductById(id) {
  return request({
    url: `/products/${id}`,
    method: 'get'
  })
}

export function createProduct(data) {
  return request({
    url: '/products',
    method: 'post',
    data
  })
}

export function updateProduct(id, data) {
  return request({
    url: `/products/${id}`,
    method: 'put',
    data
  })
}

export function deleteProduct(id) {
  return request({
    url: `/products/${id}`,
    method: 'delete'
  })
}

export function updateProductStatus(id, status) {
  return request({
    url: `/products/${id}/status`,
    method: 'patch',
    data: { status }
  })
}

export function getTopProducts(limit = 10) {
  return request({
    url: '/products/top',
    method: 'get',
    params: { limit }
  })
}