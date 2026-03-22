import request from '@/utils/request'

export function getOperationLogList(params) {
  return request.get('/system/operation-log', { params })
}
