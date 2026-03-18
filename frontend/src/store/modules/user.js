import { defineStore } from 'pinia'
import { login, getUserInfo } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'

const USER_INFO_KEY = 'user_info'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    userInfo: JSON.parse(localStorage.getItem(USER_INFO_KEY) || 'null'),
    needChangePassword: false
  }),

  actions: {
    async login(loginForm) {
      const res = await login(loginForm)
      this.token = res.data.token
      this.userInfo = res.data.user
      this.needChangePassword = res.data.needChangePassword || false
      setToken(res.data.token)
      this.saveUserInfo(res.data.user)
      return res
    },

    async fetchUserInfo() {
      if (!this.token) return
      try {
        const res = await getUserInfo()
        this.userInfo = res.data
        this.saveUserInfo(res.data)
      } catch (e) {
        this.logout()
      }
    },

    saveUserInfo(userInfo) {
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
    },

    passwordChanged() {
      this.needChangePassword = false
    },

    logout() {
      this.token = ''
      this.userInfo = null
      this.needChangePassword = false
      removeToken()
      localStorage.removeItem(USER_INFO_KEY)
    }
  }
})
