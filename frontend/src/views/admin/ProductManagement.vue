<template>
  <div class="product-management">
    <div class="page-header">
      <h2>商品管理</h2>
      <el-button type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon>
        添加商品
      </el-button>
    </div>

    <el-card>
      <div style="margin-bottom: 16px; display: flex; gap: 12px; align-items: center;">
        <el-select v-model="filterCategoryId" placeholder="选择分类" clearable style="width: 200px;" @change="loadProducts">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-input v-model="filterKeyword" placeholder="搜索商品名称" clearable style="width: 250px;" @clear="loadProducts" @keyup.enter="loadProducts" />
        <el-checkbox v-model="showDeleted" @change="loadProducts">显示已下架商品</el-checkbox>
        <el-button type="primary" @click="loadProducts">搜索</el-button>
      </div>
      <el-table :data="products" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="图片" width="80">
          <template #default="{ row }">
            <img v-if="row.image" :src="row.image" class="table-image" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="200" />
        <el-table-column prop="sku" label="SKU" width="120" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            ¥{{ row.price }}
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="100" />
        <el-table-column prop="salesCount" label="销量" width="100" />
        <el-table-column label="分类" width="140">
          <template #default="{ row }">
            {{ categories.find(c => c.id === row.categoryId)?.name || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showEditDialog(row)">
              编辑
            </el-button>
            <el-button v-if="row.status === 1" link type="warning" @click="handleToggleStatus(row, 0)">
              下架
            </el-button>
            <el-button v-else link type="success" @click="handleToggleStatus(row, 1)">
              上架
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadProducts"
        />
      </div>
    </el-card>

    <!-- 添加/编辑商品对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑商品' : '添加商品'"
      width="600px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="SKU" prop="sku">
          <el-input v-model="form.sku" />
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="商品图片">
          <el-upload
            class="image-uploader"
            action=""
            :show-file-list="false"
            :http-request="handleUpload"
            :before-upload="beforeUpload"
            accept=".jpg,.jpeg,.png,.gif"
          >
            <img v-if="form.image" :src="form.image" class="uploaded-image" />
            <el-icon v-else class="uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="原价" prop="originalPrice">
          <el-input-number v-model="form.originalPrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="商品分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%;" clearable>
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :precision="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProducts, getAllProducts, createProduct, updateProduct, deleteProduct, updateProductStatus } from '@/api/product'
import { getCategories } from '@/api/category'
import { uploadImage } from '@/api/upload'

const products = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const categories = ref([])
const filterCategoryId = ref(null)
const filterKeyword = ref('')
const showDeleted = ref(false)

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()

const form = ref({
  name: '',
  sku: '',
  description: '',
  price: 0,
  originalPrice: 0,
  stock: 0
})

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  sku: [{ required: true, message: '请输入SKU', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

onMounted(() => {
  loadProducts()
  getCategories().then(data => categories.value = data).catch(() => {})
})

const loadProducts = async () => {
  loading.value = true
  try {
    const response = await getAllProducts({
      page: currentPage.value,
      size: pageSize.value,
      categoryId: filterCategoryId.value,
      keyword: filterKeyword.value || undefined,
      status: showDeleted.value ? undefined : 1
    })
    products.value = response.items
    total.value = response.total
  } catch (error) {
    console.error('加载商品失败:', error)
    ElMessage.error('加载商品失败')
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  isEdit.value = false
  form.value = {
    name: '',
    sku: '',
    description: '',
    image: '',
    categoryId: null,
    price: 0,
    originalPrice: 0,
    stock: 0
  }
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    if (isEdit.value) {
      await updateProduct(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createProduct(form.value)
      ElMessage.success('添加成功')
    }

    dialogVisible.value = false
    loadProducts()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error(error.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除商品"${row.name}"吗？`, '提示', {
      type: 'warning'
    })

    await deleteProduct(row.id)
    ElMessage.success('删除成功')
    loadProducts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleToggleStatus = async (row, newStatus) => {
  const action = newStatus === 1 ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确定要${action}商品"${row.name}"吗？`, '提示', {
      type: 'warning'
    })

    await updateProductStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    loadProducts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }
  return true
}

const handleUpload = async (options) => {
  try {
    const url = await uploadImage(options.file)
    form.value.image = url
    options.onSuccess()
    ElMessage.success('图片上传成功')
  } catch (error) {
    options.onError(error)
    ElMessage.error('图片上传失败: ' + (error.message || '未知错误'))
  }
}
</script>

<style scoped>
.product-management {
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

.image-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 148px;
  height: 148px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-uploader:hover {
  border-color: #409eff;
}

.uploader-icon {
  font-size: 28px;
  color: #8c939d;
}

.uploaded-image {
  width: 148px;
  height: 148px;
  object-fit: cover;
}

.table-image {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
}
</style>
