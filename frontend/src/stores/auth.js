import { defineStore } from 'pinia'
import { login, register, getProfile } from '@/api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
    userRole: (state) => state.user?.role
  },

  actions: {
    async login(credentials) {
      try {
        const response = await login(credentials)
        this.token = response.accessToken
        this.user = response.user

        // 持久化存储
        localStorage.setItem('token', this.token)
        localStorage.setItem('user', JSON.stringify(this.user))

        return response
      } catch (error) {
        this.logout()
        throw error
      }
    },

    async register(userInfo) {
      const response = await register(userInfo)
      return response
    },

    async getCurrentUser() {
      if (!this.token) {
        throw new Error('Not authenticated')
      }

      try {
        const user = await getProfile()
        this.user = user
        localStorage.setItem('user', JSON.stringify(user))
        return user
      } catch (error) {
        this.logout()
        throw error
      }
    },

    logout() {
      // 直接清除本地token，不依赖后端API（后端黑名单非必需）
      this.token = null
      this.user = null

      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
