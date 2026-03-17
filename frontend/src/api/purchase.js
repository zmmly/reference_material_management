import request from '@/utils/request'

export function getPurchaseList(params) {
  return request.get('/purchase', { params })
}

export function getAllPurchaseList(params) {
  return request.get('/purchase/all', { params })
}

export function getPurchase(id) {
  return request.get(`/purchase/${id}`)
}

export function applyPurchase(data) {
  return request.post('/purchase', data)
}

export function approvePurchase(id, approved, rejectReason) {
  return request.put(`/purchase/${id}/approve`, null, { params: { approved, rejectReason } })
}

export function cancelPurchase(id) {
  return request.put(`/purchase/${id}/cancel`)
}

export function receivePurchase(id) {
  return request.put(`/purchase/${id}/receive`)
}
