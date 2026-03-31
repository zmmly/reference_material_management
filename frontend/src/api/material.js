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

// 下载导入模板
export function downloadMaterialTemplate() {
  return request.get('/materials/template', {
    responseType: 'blob'
  })
}

// 预览导入数据
export function previewMaterialImport(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/materials/import/preview', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 确认导入
export function confirmMaterialImport(items) {
  return request.post('/materials/import/confirm', { items })
}
