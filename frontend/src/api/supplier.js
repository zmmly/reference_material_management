import request from '@/utils/request'

export function getSupplierList(params) {
  return request.get('/basic/supplier', { params })
}

export function getAllSuppliers() {
  return request.get('/basic/supplier/all')
}

export function getSupplier(id) {
  return request.get(`/basic/supplier/${id}`)
}

export function createSupplier(data) {
  return request.post('/basic/supplier', data)
}

export function updateSupplier(id, data) {
  return request.put(`/basic/supplier/${id}`, data)
}

export function deleteSupplier(id) {
  return request.delete(`/basic/supplier/${id}`)
}
