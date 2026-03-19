import request from '@/utils/request'

export function getStockCheckList(params) {
  return request.get('/stock-check', { params })
}

export function getStockCheck(id) {
  return request.get(`/stock-check/${id}`)
}

export function getStockCheckGroups(id) {
  return request.get(`/stock-check/${id}/groups`)
}

export function createStockCheck(data) {
  return request.post('/stock-check', data)
}

export function checkStockGroup(checkId, data) {
  return request.put(`/stock-check/${checkId}/check`, data)
}

export function completeStockCheck(id) {
  return request.put(`/stock-check/${id}/complete`)
}

export function adjustStockCheckGroup(groupId, reason) {
  return request.put(`/stock-check/groups/${groupId}/adjust`, null, { params: { reason } })
}
