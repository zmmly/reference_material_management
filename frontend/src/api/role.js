import request from '@/utils/request'

export function getRoleList(params) {
  return request.get('/system/role', { params })
}

export function getAllRoles() {
  return request.get('/system/role/all')
}

export function createRole(data) {
  return request.post('/system/role', data)
}

export function updateRole(id, data) {
  return request.put(`/system/role/${id}`, data)
}

export function deleteRole(id) {
  return request.delete(`/system/role/${id}`)
}
