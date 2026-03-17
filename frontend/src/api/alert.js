import request from '@/utils/request'

export function getAlertList(params) {
  return request.get('/alerts', { params })
}

export function getAlertStats() {
  return request.get('/alerts/stats')
}

export function getAlertConfigs() {
  return request.get('/alerts/configs')
}

export function updateAlertConfig(type, threshold, enabled) {
  return request.put(`/alerts/configs/${type}`, null, { params: { threshold, enabled } })
}

export function handleAlert(id, remark) {
  return request.put(`/alerts/${id}/handle`, null, { params: { remark } })
}

export function ignoreAlert(id) {
  return request.put(`/alerts/${id}/ignore`)
}

export function triggerAlertCheck() {
  return request.post('/alerts/check')
}
