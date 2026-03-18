import request from '@/utils/request'

export function getBackupList(params) {
  return request.get('/system/backup', { params })
}

export function createBackup() {
  return request.post('/system/backup')
}

export function downloadBackup(id) {
  return request.get(`/system/backup/${id}/download`, {
    responseType: 'blob'
  })
}

export function deleteBackup(id) {
  return request.delete(`/system/backup/${id}`)
}
