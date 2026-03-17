import request from '@/utils/request'

export function getLocationList(params) {
  return request.get('/locations', { params })
}

export function getAllLocations() {
  return request.get('/locations/all')
}

export function createLocation(data) {
  return request.post('/locations', data)
}

export function updateLocation(id, data) {
  return request.put(`/locations/${id}`, data)
}
