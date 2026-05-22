<template>
  <div class="user-management">
    <div class="page-header">
      <h2>用户管理</h2>
      <el-button type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon>
        添加用户
      </el-button>
    </div>

    <el-card>
      <el-table :data="users" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="角色" width="200">
          <template #default="{ row }">
            <el-tag v-for="role in getUserRoleCodes(row.id)" :key="role" :type="role === 'ADMIN' ? 'danger' : role === 'SALES' ? 'warning' : 'info'" size="small" style="margin-right: 4px">
              {{ role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
            <el-button link type="primary" @click="showResetPasswordDialog(row)">重置密码</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadUsers"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '添加用户'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="角色">
          <el-tag type="warning">销售(SALES)</el-tag>
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">默认分配销售人员角色</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="重置密码" width="400px">
      <el-form :model="passwordForm" ref="passwordFormRef" label-width="100px">
        <el-form-item label="新密码" prop="newPassword" :rules="[{ required: true, message: '请输入新密码' }]">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleResetPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminUsers, getRoles, createAdminUser, updateAdminUser, deleteAdminUser, resetUserPassword, assignUserRoles, getUserRoles } from '@/api/admin'

const users = ref([])
const allRoles = ref([])
const userRolesMap = ref({})
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()

const passwordDialogVisible = ref(false)
const passwordFormRef = ref()
const passwordForm = ref({ newPassword: '' })
const currentUserId = ref(null)

const form = ref({
  username: '',
  email: '',
  phone: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }]
}

onMounted(() => {
  loadUsers()
  loadRoles()
})

const loadUsers = async () => {
  loading.value = true
  try {
    const data = await getAdminUsers({ page: currentPage.value, size: pageSize.value, roleCode: 'SALES', status: 1 })
    users.value = data.items
    total.value = data.total
    loadUserRoles(data.items)
  } catch (error) {
    console.error('加载用户失败:', error)
    ElMessage.error('加载用户失败')
  } finally {
    loading.value = false
  }
}

const loadRoles = async () => {
  try {
    allRoles.value = await getRoles()
  } catch (error) {
    console.error('加载角色失败:', error)
  }
}

const loadUserRoles = async (userList) => {
  for (const user of userList) {
    try {
      const roles = await getUserRoles(user.id)
      userRolesMap.value[user.id] = roles.map(r => r.code)
    } catch (e) {
      userRolesMap.value[user.id] = []
    }
  }
}

const getUserRoleCodes = (userId) => {
  return userRolesMap.value[userId] || []
}

const showAddDialog = () => {
  isEdit.value = false
  form.value = { username: '', email: '', phone: '', password: '' }
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  form.value = { ...row, password: '' }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    if (isEdit.value) {
      const data = { ...form.value }
      if (!data.password) delete data.password
      await updateAdminUser(form.value.id, data)
      ElMessage.success('更新成功')
    } else {
      const user = await createAdminUser(form.value)
      const salesRole = allRoles.value.find(r => r.code === 'SALES')
      if (salesRole) {
        await assignUserRoles(user.id, [salesRole.id])
      }
      ElMessage.success('添加成功')
    }

    dialogVisible.value = false
    loadUsers()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error(error.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户"${row.username}"吗？`, '提示', { type: 'warning' })
    await deleteAdminUser(row.id)
    ElMessage.success('删除成功')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const showResetPasswordDialog = (row) => {
  currentUserId.value = row.id
  passwordForm.value = { newPassword: '' }
  passwordDialogVisible.value = true
}

const handleResetPassword = async () => {
  try {
    await passwordFormRef.value.validate()
    submitting.value = true
    await resetUserPassword(currentUserId.value, passwordForm.value.newPassword)
    ElMessage.success('密码重置成功')
    passwordDialogVisible.value = false
  } catch (error) {
    ElMessage.error('密码重置失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.user-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
