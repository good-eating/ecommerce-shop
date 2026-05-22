import request from '@/utils/request'

export function getRecommendations(limit = 10) {
  return request({
    url: '/recommendations',
    method: 'get',
    params: { limit }
  })
}

export function getPopularProducts(limit = 10) {
  return request({
    url: '/recommendations/popular',
    method: 'get',
    params: { limit }
  })
}