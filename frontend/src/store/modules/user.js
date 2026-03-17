import { defineStore } from 'pinia'
import { login } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    userInfo: null
  }),

  actions: {
    async login(loginForm) {
      const res = await login(loginForm)
      this.token = res.data.token
      this.userInfo = res.data.user
      setToken(res.data.token)
      return res
    },

    logout() {
      this.token = ''
      this.userInfo = null
      removeToken()
    }
  }
})
