import request from '@/utils/request'

export function getAdminUsers(params) {
  return request({
    url: '/admin/users',
    method: 'get',
    params
  })
}

export function getRoles() {
  return request({
    url: '/admin/users/roles',
    method: 'get'
  })
}

export function createAdminUser(data) {
  return request({
    url: '/admin/users',
    method: 'post',
    data
  })
}

export function updateAdminUser(id, data) {
  return request({
    url: `/admin/users/${id}`,
    method: 'put',
    data
  })
}

export function deleteAdminUser(id) {
  return request({
    url: `/admin/users/${id}`,
    method: 'delete'
  })
}

export function resetUserPassword(id, newPassword) {
  return request({
    url: `/admin/users/${id}/reset-password`,
    method: 'put',
    data: { password: newPassword }
  })
}

export function assignUserRoles(id, roleIds) {
  return request({
    url: `/admin/users/${id}/roles`,
    method: 'post',
    data: roleIds
  })
}

export function getUserRoles(id) {
  return request({
    url: `/admin/users/${id}/roles`,
    method: 'get'
  })
}
