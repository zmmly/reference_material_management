import request from '@/utils/request'

export function getCategoryTree() {
  return request.get('/basic/category/tree')
}

export function createCategory(data) {
  return request.post('/basic/category', data)
}

export function updateCategory(id, data) {
  return request.put(`/basic/category/${id}`, data)
}

export function deleteCategory(id) {
  return request.delete(`/basic/category/${id}`)
}
