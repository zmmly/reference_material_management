import request from '@/utils/request'

export function getLocationList(params) {
  return request.get('/basic/location', { params })
}

export function getAllLocations() {
  return request.get('/basic/location/all')
}

export function createLocation(data) {
  return request.post('/basic/location', data)
}

export function updateLocation(id, data) {
  return request.put(`/basic/location/${id}`, data)
}
