import request from '@/utils/request'

export function getMetadataByType(type) {
  return request.get(`/metadata/${type}`)
}

export function createMetadata(data) {
  return request.post('/metadata', data)
}

export function updateMetadata(id, data) {
  return request.put(`/metadata/${id}`, data)
}
