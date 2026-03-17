import request from '@/utils/request'

export function getUserList(params) {
  return request.get('/users', { params })
}

export function createUser(data) {
  return request.post('/users', data)
}

export function updateUser(id, data) {
  return request.put(`/users/${id}`, data)
}

export function updateUserStatus(id, status) {
  return request.put(`/users/${id}/status`, null, { params: { status } })
}

export function resetPassword(id) {
  return request.put(`/users/${id}/reset-password`)
}
