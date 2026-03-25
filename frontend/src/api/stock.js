import request from '@/utils/request'

export function getStockList(params) {
  return request.get('/stocks', { params })
}

export function getStock(id) {
  return request.get(`/stocks/${id}`)
}

export function getStockInList(params) {
  return request.get('/stock-in', { params })
}

export function createStockIn(data) {
  return request.post('/stock-in', data)
}

export function exportStockIn(params) {
  return request.get('/stock-in/export', {
    params,
    responseType: 'blob'
  })
}

export function getStockOutList(params) {
  return request.get('/stock-out', { params })
}

export function applyStockOut(data) {
  return request.post('/stock-out', data)
}

export function batchApplyStockOut(data) {
  return request.post('/stock-out/batch', data)
}

export function approveStockOut(id, approved, rejectReason) {
  return request.put(`/stock-out/${id}/approve`, null, { params: { approved, rejectReason } })
}

export function cancelStockOut(id) {
  return request.put(`/stock-out/${id}/cancel`)
}

// 下载入库导入模板
export function downloadStockInTemplate() {
  return request.get('/stock-in/template', {
    responseType: 'blob'
  })
}

// 预览入库导入数据
export function previewStockInImport(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/stock-in/import/preview', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 确认入库导入
export function confirmStockInImport(items) {
  return request.post('/stock-in/import/confirm', { items })
}
