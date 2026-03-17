import request from '@/utils/request'

export function getMaterialList(params) {
  return request.get('/materials', { params })
}

export function getAllMaterials() {
  return request.get('/materials/all')
}

export function getMaterial(id) {
  return request.get(`/materials/${id}`)
}

export function createMaterial(data) {
  return request.post('/materials', data)
}

export function updateMaterial(id, data) {
  return request.put(`/materials/${id}`, data)
}

export function deleteMaterial(id) {
  return request.delete(`/materials/${id}`)
}
