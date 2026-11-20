<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminCategoryList, createCategory, updateCategory, deleteCategory, type Category } from '@/api'

const loading = ref(false)
const categoryList = ref<Category[]>([])
const showDialog = ref(false)
const isEdit = ref(false)
const editingCategory = ref<Partial<Category>>({
  name: '',
  description: '',
  icon: '',
  sortOrder: 0,
  status: 1
})

// 获取分类列表
const fetchCategoryList = async () => {
  loading.value = true
  try {
    // 使用管理员接口获取所有分类（包括禁用的）
    const res: any = await getAdminCategoryList()
    if (res && Array.isArray(res)) {
      categoryList.value = res.sort((a: Category, b: Category) => a.sortOrder - b.sortOrder)
    }
  } catch (error) {
    console.error('获取分类列表失败:', error)
    ElMessage.error('获取分类列表失败')
  } finally {
    loading.value = false
  }
}

// 打开创建对话框
const openCreateDialog = () => {
  isEdit.value = false
  editingCategory.value = {
    name: '',
    description: '',
    icon: '',
    sortOrder: categoryList.value.length + 1,
    status: 1
  }
  showDialog.value = true
}

// 打开编辑对话框
const openEditDialog = (category: Category) => {
  isEdit.value = true
  editingCategory.value = { ...category }
  showDialog.value = true
}

// 保存分类
const handleSave = async () => {
  if (!editingCategory.value.name) {
    ElMessage.warning('请输入分类名称')
    return
  }

  try {
    if (isEdit.value && editingCategory.value.id) {
      await updateCategory(editingCategory.value.id, editingCategory.value)
      ElMessage.success('更新成功')
    } else {
      await createCategory(editingCategory.value)
      ElMessage.success('创建成功')
    }
    showDialog.value = false
    fetchCategoryList()
  } catch (error) {
    console.error('保存分类失败:', error)
    ElMessage.error('保存失败')
  }
}

// 删除分类
const handleDelete = async (category: Category) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除分类 "${category.name}" 吗？`,
      '确认删除',
      { type: 'warning' }
    )
    await deleteCategory(category.id)
    ElMessage.success('删除成功')
    fetchCategoryList()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 切换状态
const handleToggleStatus = async (category: Category) => {
  const newStatus = category.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  try {
    await updateCategory(category.id, { status: newStatus })
    ElMessage.success(`${action}成功`)
    fetchCategoryList()
  } catch (error) {
    console.error(`${action}失败:`, error)
    ElMessage.error(`${action}失败`)
  }
}

onMounted(() => {
  fetchCategoryList()
})
</script>

<template>
  <div class="category-management">
    <div class="page-header">
      <div class="header-title">
        <el-icon :size="28" color="var(--color-primary)"><Folder /></el-icon>
        <h2>分类管理</h2>
      </div>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新建分类
      </el-button>
    </div>

    <el-card class="list-card" shadow="never">
      <el-table :data="categoryList" v-loading="loading" stripe>
        <el-table-column type="index" width="60" label="序号" align="center" />
        <el-table-column label="分类名称" prop="name" min-width="150" />
        <el-table-column label="描述" prop="description" min-width="200" show-overflow-tooltip />
        <el-table-column label="图标" prop="icon" width="120" />
        <el-table-column label="排序" prop="sortOrder" width="80" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="活动数" prop="activityCount" width="100" align="center" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button 
              :type="row.status === 1 ? 'info' : 'success'" 
              size="small" 
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="primary" size="small" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
        <!-- 空数据状态 -->
        <template #empty>
          <el-empty description="暂无分类数据" :image-size="80" />
        </template>
      </el-table>
    </el-card>

    <!-- 编辑/创建对话框 -->
    <el-dialog
      v-model="showDialog"
      :title="isEdit ? '编辑分类' : '新建分类'"
      width="500px"
    >
      <el-form :model="editingCategory" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="editingCategory.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input 
            v-model="editingCategory.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入分类描述" 
          />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="editingCategory.icon" placeholder="请输入图标名称（如：icon-art）" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="editingCategory.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editingCategory.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.category-management {
  padding: 0;
}

.page-header {
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.list-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
}
</style>
