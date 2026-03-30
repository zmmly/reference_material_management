import request from '@/utils/request'

export function getMetadataByType(type) {
  return request.get(`/basic/metadata/type/${type}`)
}

export function createMetadata(data) {
  return request.post('/basic/metadata', data)
}

export function updateMetadata(id, data) {
  return request.put(`/basic/metadata/${id}`, data)
}

export function deleteMetadata(id) {
  return request.delete(`/basic/metadata/${id}`)
}
