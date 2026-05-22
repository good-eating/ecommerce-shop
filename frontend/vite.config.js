import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api/v1')
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => `/api/v1${path}`
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets'
  }
})