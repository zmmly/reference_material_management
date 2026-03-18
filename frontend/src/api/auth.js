import request from '@/utils/request'

export function getCaptcha() {
  return request.get('/auth/captcha')
}

export function login(data) {
  return request.post('/auth/login', data)
}

export function getUserInfo() {
  return request.get('/auth/user-info')
}

export function changePassword(data) {
  return request.post('/auth/change-password', data)
}
