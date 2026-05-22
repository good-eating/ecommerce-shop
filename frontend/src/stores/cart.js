import { defineStore } from 'pinia'
import { getCart, addToCart, updateCartItem, removeCartItem, clearCart } from '@/api/cart'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: [],
    totalItems: 0,
    totalAmount: 0,
    isLoading: false
  }),

  getters: {
    itemCount: (state) => state.items?.reduce((total, item) => total + item.quantity, 0) || 0,
    cartTotal: (state) => state.items?.reduce((total, item) => total + (item.price * item.quantity), 0) || 0
  },

  actions: {
    async loadCart() {
      const authStore = useAuthStore()
      if (!authStore.isAuthenticated) {
        this.items = []
        this.totalItems = 0
        this.totalAmount = 0
        return
      }
      this.isLoading = true
      try {
        const cartData = await getCart()
        this.items = cartData.items || []
        this.totalItems = cartData.totalItems || 0
        this.totalAmount = cartData.totalAmount || 0
      } catch (error) {
        console.error('加载购物车失败:', error)
        this.items = []
        this.totalItems = 0
        this.totalAmount = 0
      } finally {
        this.isLoading = false
      }
    },

    async addItem(product, quantity = 1) {
      const authStore = useAuthStore()
      if (!authStore.isAuthenticated) {
        router.push('/login')
        throw new Error('请先登录')
      }
      try {
        await addToCart(product.id, quantity)
        await this.loadCart()
      } catch (error) {
        throw error
      }
    },

    async updateItem(itemId, quantity) {
      if (quantity <= 0) {
        await this.removeItem(itemId)
        return
      }

      try {
        await updateCartItem(itemId, quantity)
        await this.loadCart()
      } catch (error) {
        throw error
      }
    },

    async removeItem(itemId) {
      try {
        await removeCartItem(itemId)
        await this.loadCart()
      } catch (error) {
        throw error
      }
    },

    async clear() {
      try {
        await clearCart()
        this.items = []
        this.totalItems = 0
        this.totalAmount = 0
      } catch (error) {
        throw error
      }
    }
  }
})
