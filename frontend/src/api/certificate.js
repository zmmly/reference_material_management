import request from '@/utils/request'

export function getCertificateList(params) {
  return request.get('/basic/certificate', { params })
}

export function getCertificate(id) {
  return request.get(`/basic/certificate/${id}`)
}

export function queryCertificate(materialId, batchNo) {
  return request.get('/basic/certificate/query', { params: { materialId, batchNo } })
}

export function createCertificate(data) {
  return request.post('/basic/certificate', data)
}

export function updateCertificate(id, data) {
  return request.put(`/basic/certificate/${id}`, data)
}

export function deleteCertificate(id) {
  return request.delete(`/basic/certificate/${id}`)
}
