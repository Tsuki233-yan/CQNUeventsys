<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, setAdmin, cancelAdmin, resetPassword, deleteUser, updateUser, banUser, unbanUser, type UserListParams } from '@/api/admin'
import type { User } from '@/api/auth'

const loading = ref(false)
const userList = ref<User[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const roleFilter = ref<number | ''>('')
const statusFilter = ref<number | ''>('')
// 身份筛选（真实用户身份绑定）：0-学生，1-教师，-1-未绑定身份
const userTypeFilter = ref<number | ''>('')
const showEditDialog = ref(false)
const editingUser = ref<Partial<User>>({})

// 获取用户列表
const fetchUserList = async () => {
  loading.value = true
  try {
    const params: UserListParams = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      role: roleFilter.value !== '' ? roleFilter.value : undefined,
      status: statusFilter.value !== '' ? statusFilter.value : undefined,
      userType: userTypeFilter.value !== '' ? userTypeFilter.value : undefined
    }
    const res: any = await getUserList(params)
    // 后端返回分页格式 { records: [], total: number, ... }
    if (res && Array.isArray(res.records)) {
      userList.value = res.records
      total.value = res.total || 0
    } else if (Array.isArray(res)) {
      // 兼容直接返回数组的情况
      userList.value = res
      total.value = res.length
    } else {
      userList.value = res?.list || []
      total.value = res?.total || 0
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    userList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchUserList()
}

// 分页
const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchUserList()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  fetchUserList()
}

// 设置/取消管理员
const handleToggleAdmin = async (user: User) => {
  const isAdmin = user.role === 1
  const action = isAdmin ? '取消管理员' : '设为管理员'
  
  try {
    await ElMessageBox.confirm(
      `确定要${action}用户 "${user.nickname}" 吗？`,
      '确认操作',
      { type: 'warning' }
    )
    
    if (isAdmin) {
      await cancelAdmin(user.id)
    } else {
      await setAdmin(user.id)
    }
    
    ElMessage.success(`${action}成功`)
    fetchUserList()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

// 重置密码
const handleResetPassword = async (user: User) => {
  try {
    const { value: newPassword } = (await ElMessageBox.prompt(
      `请输入用户 "${user.nickname}" 的新密码：`,
      '重置密码',
      {
        confirmButtonText: '确认重置',
        cancelButtonText: '取消',
        inputPattern: /^.{6,20}$/,
        inputErrorMessage: '密码长度应在6-20位之间'
      }
    )) as unknown as { value: string }
    
    await resetPassword(user.id, newPassword)
    ElMessage.success('密码重置成功')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('密码重置失败')
    }
  }
}

// 获取角色标签
const getRoleLabel = (role: number) => {
  return role === 1 ? '管理员' : '普通用户'
}

const getRoleType = (role: number) => {
  return role === 1 ? 'danger' : 'info'
}

// 获取状态标签
const getStatusLabel = (status: number) => {
  return status === 1 ? '正常' : '禁用'
}

const getStatusType = (status: number) => {
  return status === 1 ? 'success' : 'danger'
}

// 获取身份标签（真实用户身份绑定：学生绑学号、教师绑工号；管理员豁免绑定）
const getIdentityLabel = (user: User) => {
  if (user.role === 1) return '管理员'
  if (user.userType === 1) return '教师'
  if (user.userType === 0 && user.studentId) return '学生'
  return '未绑定'
}

const getIdentityTagType = (user: User) => {
  const label = getIdentityLabel(user)
  if (label === '管理员') return 'danger'
  if (label === '教师') return 'success'
  if (label === '学生') return 'primary'
  return 'warning'
}

// 学号/工号展示值：教师显示工号，学生显示学号，未绑定显示 -
const getIdNumber = (user: User) => {
  if (user.userType === 1) return user.teacherId || '-'
  return user.studentId || '-'
}

// 删除用户
const handleDeleteUser = async (user: User) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 "${user.nickname}" 吗？此操作不可恢复！`,
      '确认删除',
      { type: 'error', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    
    await deleteUser(user.id)
    ElMessage.success('删除成功')
    fetchUserList()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 打开编辑对话框
const openEditDialog = (user: User) => {
  editingUser.value = { ...user }
  showEditDialog.value = true
}

// 验证手机号格式
const validatePhone = (phone: string | undefined): boolean => {
  if (!phone) return true // 手机号可选，为空时不验证
  const phoneRegex = /^1[3-9]\d{9}$/
  return phoneRegex.test(phone)
}

// 保存用户信息
const handleSaveUser = async () => {
  if (!editingUser.value.id) return
  
  // 验证手机号
  if (editingUser.value.phone && !validatePhone(editingUser.value.phone)) {
    ElMessage.warning('请输入正确的手机号码')
    return
  }
  
  const updateData: Partial<User> = {
    nickname: editingUser.value.nickname,
    email: editingUser.value.email,
    phone: editingUser.value.phone,
    realName: editingUser.value.realName,
    college: editingUser.value.college,
    major: editingUser.value.major,
    grade: editingUser.value.grade,
    gender: editingUser.value.gender,
    bio: editingUser.value.bio
  }
  
  try {
    await updateUser(editingUser.value.id, updateData)
    ElMessage.success('更新成功')
    showEditDialog.value = false
    fetchUserList()
  } catch (error: any) {
    console.error('更新用户失败:', error)
    const errorMsg = error?.message || error?.response?.data?.message || '更新失败'
    ElMessage.error(errorMsg)
  }
}

// 封禁/解封用户
const handleToggleBan = async (user: User) => {
  const isBanned = user.status === 0
  const action = isBanned ? '解封' : '封禁'
  const confirmText = isBanned 
    ? `确定要解封用户 "${user.nickname}" 吗？` 
    : `确定要封禁用户 "${user.nickname}" 吗？封禁后该用户将无法登录系统。`
  
  try {
    await ElMessageBox.confirm(
      confirmText,
      `确认${action}`,
      { type: isBanned ? 'info' : 'warning', confirmButtonText: action, cancelButtonText: '取消' }
    )
    
    if (isBanned) {
      await unbanUser(user.id)
    } else {
      await banUser(user.id)
    }
    
    ElMessage.success(`${action}成功`)
    fetchUserList()
  } catch (error: any) {
    if (error === 'cancel') return
    console.error(`${action}失败:`, error)
    const errorMsg = error?.response?.data?.message || error?.message || `${action}失败`
    ElMessage.error(errorMsg)
  }
}

onMounted(() => {
  fetchUserList()
})
</script>

<template>
  <div class="user-management">
    <div class="page-header">
      <div class="header-title">
        <el-icon :size="28" color="var(--primary-color)"><UserFilled /></el-icon>
        <h2>用户管理</h2>
      </div>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" class="search-form">
        <el-form-item label="关键词">
          <el-input
            v-model="searchKeyword"
            placeholder="用户名/昵称/学号/工号/邮箱"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="roleFilter" placeholder="全部角色" clearable style="width: 120px">
            <el-option label="普通用户" :value="0" />
            <el-option label="管理员" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="身份">
          <el-select v-model="userTypeFilter" placeholder="全部身份" clearable style="width: 120px">
            <el-option label="学生" :value="0" />
            <el-option label="教师" :value="1" />
            <el-option label="未绑定" :value="-1" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="禁用" :value="0" />
            <el-option label="正常" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户列表 -->
    <el-card class="list-card" shadow="never">
      <el-table :data="userList" v-loading="loading" stripe>
        <el-table-column type="index" width="60" label="序号" />
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar :size="40" :src="row.avatar">
                {{ row.nickname?.charAt(0) || row.username?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="user-detail">
                <div class="nickname">{{ row.nickname || row.username }}</div>
                <div class="username">{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <!-- 身份列：学生/教师/管理员/未绑定（真实用户身份绑定） -->
        <el-table-column label="身份" width="90">
          <template #default="{ row }">
            <el-tag :type="getIdentityTagType(row)" size="small">
              {{ getIdentityLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 学号/工号列：教师显示工号，学生显示学号 -->
        <el-table-column label="学号/工号" width="130">
          <template #default="{ row }">
            {{ getIdNumber(row) }}
          </template>
        </el-table-column>
        <el-table-column label="学院" prop="college" min-width="150" show-overflow-tooltip />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role)" size="small">
              {{ getRoleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="380" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                :type="row.role === 1 ? 'danger' : 'primary'"
                size="small"
                @click="handleToggleAdmin(row)"
              >
                {{ row.role === 1 ? '取消管理员' : '设为管理员' }}
              </el-button>
              <el-button type="warning" size="small" @click="handleResetPassword(row)">
                重置密码
              </el-button>
              <el-button 
                :type="row.status === 0 ? 'success' : 'danger'" 
                size="small" 
                @click="handleToggleBan(row)"
              >
                {{ row.status === 0 ? '解封' : '封禁' }}
              </el-button>
              <el-button type="info" size="small" @click="openEditDialog(row)">
                编辑
              </el-button>
              <el-button type="danger" size="small" @click="handleDeleteUser(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
        <!-- 空数据状态 -->
        <template #empty>
          <el-empty description="暂无用户数据" :image-size="80" />
        </template>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 编辑用户对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑用户信息"
      width="600px"
    >
      <el-form :model="editingUser" label-width="80px">
        <el-form-item label="昵称">
          <el-input v-model="editingUser.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editingUser.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editingUser.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="editingUser.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="学院">
          <el-input v-model="editingUser.college" placeholder="请输入学院" />
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="editingUser.major" placeholder="请输入专业" />
        </el-form-item>
        <el-form-item label="年级">
          <el-input v-model="editingUser.grade" placeholder="请输入年级" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editingUser.gender">
            <el-radio :label="0">女</el-radio>
            <el-radio :label="1">男</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input
            v-model="editingUser.bio"
            type="textarea"
            :rows="3"
            placeholder="请输入个人简介"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-management {
  padding: 0;
}

.page-header {
  margin-bottom: 24px;
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

.search-card {
  margin-bottom: 20px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.list-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-detail {
  display: flex;
  flex-direction: column;
}

.nickname {
  font-weight: 600;
  color: var(--text-primary);
}

.username {
  font-size: 12px;
  color: var(--text-secondary);
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
