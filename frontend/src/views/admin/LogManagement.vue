<template>
  <div class="log-management">
    <div class="page-header">
      <h2>浏览日志</h2>
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户ID">
          <el-input v-model="searchForm.userId" placeholder="用户ID" style="width: 120px" />
        </el-form-item>
        <el-form-item label="路径">
          <el-input v-model="searchForm.path" placeholder="路径关键字" style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadLogs">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-card>
      <el-table :data="logs" v-loading="loading" max-height="600">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column prop="method" label="方法" width="80" />
        <el-table-column prop="path" label="路径" min-width="250" />
        <el-table-column prop="params" label="参数" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="duration" label="耗时(ms)" width="100" />
        <el-table-column prop="statusCode" label="状态码" width="90">
          <template #default="{ row }">
            <el-tag :type="row.statusCode === 200 ? 'success' : 'danger'" size="small">
              {{ row.statusCode }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="180" />
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadLogs"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getLogs } from '@/api/log'

const logs = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const searchForm = ref({
  userId: '',
  path: ''
})

onMounted(() => {
  loadLogs()
})

const loadLogs = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (searchForm.value.userId) params.userId = searchForm.value.userId
    if (searchForm.value.path) params.path = searchForm.value.path

    const data = await getLogs(params)
    logs.value = data.items
    total.value = data.total
  } catch (error) {
    console.error('加载日志失败:', error)
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.value = { userId: '', path: '' }
  currentPage.value = 1
  loadLogs()
}
</script>

<style scoped>
.log-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
