import request from '@/utils/request'

export function getAcceptanceList(params) {
  return request.get('/purchase-acceptance', { params })
}

export function getAcceptance(id) {
  return request.get(`/purchase-acceptance/${id}`)
}

export function startAcceptance(id) {
  return request.post(`/purchase-acceptance/${id}/start`)
}

export function submitAcceptance(id, data) {
  return request.post(`/purchase-acceptance/${id}/submit`, data)
}

export function exportAcceptance(params) {
  return request.get('/purchase-acceptance/export', {
    params,
    responseType: 'blob'
  })
}
