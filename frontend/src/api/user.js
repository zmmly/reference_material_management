import request from '@/utils/request'

export function getUserList(params) {
  return request.get('/system/user', { params })
}

export function getAllUsers() {
  return request.get('/system/user/all')
}

export function createUser(data) {
  return request.post('/system/user', data)
}

export function updateUser(id, data) {
  return request.put(`/system/user/${id}`, data)
}

export function updateUserStatus(id, status) {
  return request.put(`/system/user/${id}/status`, null, { params: { status } })
}

export function resetPassword(id) {
  return request.put(`/system/user/${id}/reset-password`)
}
