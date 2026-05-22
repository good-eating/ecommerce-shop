<template>
  <el-container class="admin-layout">
    <el-aside width="200px" class="admin-aside">
      <div class="admin-logo">
        <span>管理后台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="admin-menu"
        router
      >
        <el-menu-item index="/admin/products" v-if="isSales">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/categories" v-if="isSales">
          <el-icon><Folder /></el-icon>
          <span>分类管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/sales-monitor" v-if="isSales">
          <el-icon><DataLine /></el-icon>
          <span>销售监控</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders" v-if="isSales">
          <el-icon><Truck /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/logs" v-if="isSales">
          <el-icon><Document /></el-icon>
          <span>浏览日志</span>
        </el-menu-item>
        <el-menu-item index="/admin/users" v-if="isAdmin">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/analytics" v-if="isAdmin">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据分析</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div class="header-left">
          <el-button text @click="$router.push('/home')">
            <el-icon><Back /></el-icon>
            返回前台
          </el-button>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :icon="UserFilled" />
              <span>{{ authStore.user?.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeMenu = computed(() => route.path)

const isAdmin = computed(() => authStore.userRole === 'ADMIN')
const isSales = computed(() => authStore.userRole === 'SALES')

const handleCommand = (command) => {
  if (command === 'logout') {
    authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.admin-aside {
  background-color: #304156;
  color: #fff;
}

.admin-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #1f2d3d;
}

.admin-menu {
  border-right: none;
  background-color: #304156;
}

.admin-menu :deep(.el-menu-item) {
  color: #bfcbd9;
}

.admin-menu :deep(.el-menu-item:hover),
.admin-menu :deep(.el-menu-item.is-active) {
  background-color: #263445;
  color: #409eff;
}

.admin-header {
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.admin-main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
