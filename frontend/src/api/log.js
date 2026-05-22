import request from '@/utils/request'

export function getLogs(params) {
  return request({
    url: '/logs',
    method: 'get',
    params
  })
}
