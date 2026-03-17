import request from '@/utils/request'

export function getStockCheckList(params) {
  return request.get('/stock-check', { params })
}

export function getStockCheck(id) {
  return request.get(`/stock-check/${id}`)
}

export function getStockCheckItems(id) {
  return request.get(`/stock-check/${id}/items`)
}

export function createStockCheck(data) {
  return request.post('/stock-check', data)
}

export function checkStockCheckItem(checkId, itemId, actualQuantity, remarks) {
  return request.put(`/stock-check/${checkId}/items/${itemId}`, null, { params: { actualQuantity, remarks } })
}

export function completeStockCheck(id) {
  return request.put(`/stock-check/${id}/complete`)
}

export function adjustStockCheckItem(itemId, reason) {
  return request.put(`/stock-check/items/${itemId}/adjust`, null, { params: { reason } })
}
