<template>
  <el-container class="app-layout">
    <AppHeader />
    <el-main class="main-content">
      <slot />
    </el-main>
    <el-footer class="app-footer">
      <p>© 2026 电商购物网站 - All Rights Reserved</p>
    </el-footer>
  </el-container>
</template>

<script setup>
import AppHeader from './AppHeader.vue'
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  padding: 20px;
}

.app-footer {
  text-align: center;
  color: #909399;
  padding: 20px;
}
</style>
