import request from '@/utils/request'

export function getDashboardSummary() {
  return request.get('/reports/dashboard/summary')
}

export function getCategoryStats() {
  return request.get('/reports/dashboard/category-stats')
}

export function getLocationStats() {
  return request.get('/reports/dashboard/location-stats')
}

export function getExpiryStats() {
  return request.get('/reports/dashboard/expiry-stats')
}

export function getInOutTrend(startDate, endDate) {
  return request.get('/reports/in-out-trend', { params: { startDate, endDate } })
}
