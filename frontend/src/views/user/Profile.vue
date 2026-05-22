<template>
  <AppLayout>
    <div class="profile-page">
      <div class="page-header">
        <h2>个人信息</h2>
      </div>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-card class="avatar-card">
            <div class="avatar-section">
              <el-avatar :size="120" :src="profile.avatar || undefined">
                {{ profile.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <h3>{{ profile.username }}</h3>
              <el-tag :type="profile.role === 'ADMIN' ? 'danger' : profile.role === 'SALES' ? 'warning' : 'info'" size="small">
                {{ roleLabel }}
              </el-tag>
              <p class="member-since">注册时间：{{ profile.createdAt }}</p>
            </div>
          </el-card>
        </el-col>

        <el-col :span="16">
          <el-card>
            <template #header>
              <span>编辑信息</span>
            </template>
            <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" class="profile-form">
              <el-form-item label="用户名">
                <el-input v-model="form.username" disabled />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="form.email" />
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="form.phone" />
              </el-form-item>
              <el-form-item label="年龄" prop="age">
                <el-input-number v-model="form.age" :min="1" :max="150" />
              </el-form-item>
              <el-form-item label="性别">
                <el-radio-group v-model="form.gender">
                  <el-radio :value="0">未知</el-radio>
                  <el-radio :value="1">男</el-radio>
                  <el-radio :value="2">女</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="城市" prop="city">
                <el-input v-model="form.city" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="submitting" @click="handleSubmit">保存修改</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import AppLayout from '@/components/layout/AppLayout.vue'
import { getProfile, updateProfile } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const profile = ref({})
const submitting = ref(false)
const formRef = ref()

const form = ref({
  username: '',
  email: '',
  phone: '',
  age: 0,
  gender: 0,
  city: ''
})

const rules = {
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }]
}

const roleLabel = computed(() => {
  const map = { ADMIN: '管理员', SALES: '销售员', CUSTOMER: '用户' }
  return map[profile.value.role] || profile.value.role
})

onMounted(() => {
  loadProfile()
})

const loadProfile = async () => {
  try {
    const data = await getProfile()
    profile.value = data
    form.value = {
      username: data.username || '',
      email: data.email || '',
      phone: data.phone || '',
      age: data.age || 0,
      gender: data.gender ?? 0,
      city: data.city || ''
    }
  } catch (error) {
    console.error('加载个人信息失败:', error)
    ElMessage.error('加载个人信息失败')
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true
    await updateProfile(form.value)
    ElMessage.success('保存成功')
    loadProfile()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error(error.message || '保存失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.avatar-card {
  text-align: center;
}

.avatar-section {
  padding: 20px 0;
}

.avatar-section h3 {
  margin: 15px 0 10px;
  font-size: 18px;
}

.member-since {
  margin-top: 10px;
  color: #909399;
  font-size: 13px;
}

.profile-form {
  max-width: 500px;
}
</style>
