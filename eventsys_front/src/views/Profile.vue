<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore, useActivityStore } from '@/stores'
import { getMyFavorites } from '@/api/favorite'
import { getMyCreatedActivities, getMyCheckInRecords, deleteActivity, getActivityDetail } from '@/api/activity'
import { uploadAvatar, updateUserInfo, bindIdentity, type UpdateUserInfoRequest } from '@/api/auth'
import { getMyReviews } from '@/api/review'
import type { Activity, Review } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const activityStore = useActivityStore()

const activeTab = ref('info')
const loading = ref(false)
const myEvents = ref<Activity[]>([])
const participatedEvents = ref<Activity[]>([])
const favoriteEvents = ref<Activity[]>([])
const checkInRecords = ref<any[]>([])
const checkInActivityMap = ref<Record<number, Activity>>({}) // 缓存签到记录对应的活动信息
const myReviews = ref<Review[]>([])

const userInfo = computed(() => userStore.userInfo)

/**
 * 身份绑定相关状态（真实用户身份绑定：学生绑学号、教师绑工号）
 * 校验规则为模拟规则，可替换为学籍接口对接
 */
// 身份展示文案：管理员豁免绑定；其余按 userType 判定
const identityLabel = computed(() => {
  const u = userInfo.value
  if (!u) return '未绑定'
  if (u.role === 1) return '管理员'
  if (u.userType === 1) return '教师'
  if (u.userType === 0 && u.studentId) return '学生'
  return '未绑定'
})

// 身份标签颜色
const identityTagType = computed(() => {
  const label = identityLabel.value
  if (label === '管理员') return 'danger'
  if (label === '教师') return 'success'
  if (label === '学生') return 'primary'
  return 'warning'
})

// 是否已完成身份绑定（管理员视为已绑定，豁免校验）
const isIdentityBound = computed(() => {
  const u = userInfo.value
  if (!u) return false
  if (u.role === 1) return true
  if (u.userType === 1) return !!u.teacherId
  if (u.userType === 0) return !!u.studentId
  return false
})

// 是否需要提示完善身份认证（未绑定身份且非管理员）
const needIdentityBind = computed(() => {
  const u = userInfo.value
  return !!u && u.role !== 1 && !isIdentityBound.value
})

// 已绑定的学号/工号展示值
const boundIdNumber = computed(() => {
  const u = userInfo.value
  if (!u) return ''
  if (u.userType === 1) return u.teacherId || ''
  return u.studentId || ''
})

// 补填绑定表单
const bindFormRef = ref()
const binding = ref(false)
const bindForm = ref({
  userType: 0,
  idNumber: ''
})

// 学生学号：10 位数字，前 4 位为入学年份（2020~2026）；与后端校验规则一致
const STUDENT_ID_PATTERN = /^202[0-6]\d{6}$/
// 教师工号：6 位数字；与后端校验规则一致
const TEACHER_ID_PATTERN = /^\d{6}$/

const validateIdNumber = (_rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error(bindForm.value.userType === 1 ? '请输入工号' : '请输入学号'))
  } else if (bindForm.value.userType === 1 && !TEACHER_ID_PATTERN.test(value)) {
    callback(new Error('工号须为6位数字'))
  } else if (bindForm.value.userType === 0 && !STUDENT_ID_PATTERN.test(value)) {
    callback(new Error('学号须为10位数字，且前4位为入学年份（2020~2026）'))
  } else {
    callback()
  }
}

const bindRules = {
  userType: [{ required: true, message: '请选择身份', trigger: 'change' }],
  idNumber: [{ required: true, validator: validateIdNumber, trigger: 'blur' }]
}

// 切换身份时清空编号并重置校验提示
const handleBindTypeChange = () => {
  bindForm.value.idNumber = ''
  bindFormRef.value?.clearValidate()
}

// 提交补填身份绑定
const handleBindIdentity = async () => {
  const valid = await bindFormRef.value?.validate().catch(() => false)
  if (!valid) return

  binding.value = true
  try {
    await bindIdentity({
      userType: bindForm.value.userType,
      // 学生只提交学号，教师只提交工号
      studentId: bindForm.value.userType === 0 ? bindForm.value.idNumber.trim() : undefined,
      teacherId: bindForm.value.userType === 1 ? bindForm.value.idNumber.trim() : undefined
    })
    ElMessage.success('身份绑定成功')
    // 重新拉取用户信息，刷新身份与学号/工号展示
    await userStore.fetchUserInfo()
  } catch (error: any) {
    console.error('身份绑定失败:', error)
    // 错误提示已由响应拦截器弹出
  } finally {
    binding.value = false
  }
}

/**
 * 获取头像URL
 * 静态资源受 context-path 影响，需要加 /api 前缀
 */
const getAvatarUrl = (url: string | undefined): string => {
  if (!url) return ''
  // 如果已经是完整路径，直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  // 如果已经是 /api 开头，直接返回
  if (url.startsWith('/api/')) {
    return url
  }
  // 其他路径添加 /api 前缀
  return '/api' + url
}

// 获取我的活动数据
// 根据最新API：
// - /activities/participant/my-activities 返回我参与的活动
// - /activities/manage/my-created 返回我创建的活动
const fetchMyActivities = async () => {
  loading.value = true
  try {
    // 并行获取我参与的活动和我创建的活动
    const [participatedRes, createdRes] = await Promise.all([
      activityStore.fetchMyActivities(),
      getMyCreatedActivities()
    ])
    
    // 处理我参与的活动
    if (participatedRes && Array.isArray(participatedRes)) {
      participatedEvents.value = participatedRes
    } else {
      participatedEvents.value = []
    }
    
    // 处理我创建的活动
    if (createdRes && Array.isArray(createdRes)) {
      myEvents.value = createdRes
    } else {
      myEvents.value = []
    }
  } catch (error) {
    console.error('获取我的活动失败:', error)
    participatedEvents.value = []
    myEvents.value = []
  }
  loading.value = false
}

// 获取收藏的活动
const fetchFavorites = async () => {
  const res: any = await getMyFavorites()
  if (res) {
    favoriteEvents.value = res
  }
}

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  // 重新获取用户信息，更新统计数值
  await userStore.fetchUserInfo()
  fetchMyActivities()
  fetchFavorites()
  fetchCheckInRecords()
  fetchMyReviews()
})

// 获取签到记录
const fetchCheckInRecords = async () => {
  try {
    const res: any = await getMyCheckInRecords()
    if (res && Array.isArray(res)) {
      checkInRecords.value = res
      // 获取每个签到记录对应的活动详情
      const activityIds = [...new Set(res.map((record: any) => record.activityId).filter(Boolean))]
      await fetchCheckInActivities(activityIds)
    } else {
      checkInRecords.value = []
    }
  } catch (error) {
    console.error('获取签到记录失败:', error)
    checkInRecords.value = []
  }
}

// 获取签到记录对应的活动详情
const fetchCheckInActivities = async (activityIds: number[]) => {
  const activityMap: Record<number, Activity> = {}
  await Promise.all(
    activityIds.map(async (id) => {
      try {
        const activity = await getActivityDetail(id)
        if (activity) {
          activityMap[id] = activity as unknown as Activity
        }
      } catch (error) {
        console.error(`获取活动 ${id} 详情失败:`, error)
      }
    })
  )
  checkInActivityMap.value = activityMap
}

const uploadingAvatar = ref(false)

// 处理头像上传
const handleAvatarUpload = async (file: File) => {
  if (!file) return false
  
  // 检查文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    return false
  }
  
  // 检查文件大小（5MB）
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像大小不能超过5MB')
    return false
  }
  
  uploadingAvatar.value = true
  try {
    const res: any = await uploadAvatar(file)
    if (res) {
      // 更新本地用户信息
      userStore.updateUserInfo({ avatar: res })
      ElMessage.success('头像上传成功')
    }
  } catch (error) {
    console.error('头像上传失败:', error)
    ElMessage.error('头像上传失败')
  } finally {
    uploadingAvatar.value = false
  }
  return false // 阻止默认上传行为
}

// 验证手机号格式
const validatePhone = (phone: string | undefined): boolean => {
  if (!phone) return true // 手机号可选，为空时不验证
  const phoneRegex = /^1[3-9]\d{9}$/
  return phoneRegex.test(phone)
}

const updateProfile = async () => {
  if (!userInfo.value) return
  
  // 验证手机号
  if (userInfo.value.phone && !validatePhone(userInfo.value.phone)) {
    ElMessage.warning('请输入正确的手机号码')
    return
  }
  
  const updateData: UpdateUserInfoRequest = {
    nickname: userInfo.value.nickname,
    email: userInfo.value.email,
    phone: userInfo.value.phone,
    realName: userInfo.value.realName,
    college: userInfo.value.college,
    major: userInfo.value.major,
    grade: userInfo.value.grade,
    gender: userInfo.value.gender,
    bio: userInfo.value.bio
  }
  
  try {
    await updateUserInfo(updateData)
    // 更新本地用户信息
    userStore.updateUserInfo(updateData)
    ElMessage.success('个人信息更新成功')
  } catch (error: any) {
    console.error('更新用户信息失败:', error)
    const errorMsg = error?.message || error?.response?.data?.message || '更新失败'
    ElMessage.error(errorMsg)
  }
}

// 统一格式化为 yyyy/mm/dd hh:mm:ss（处理UTC时间）
const formatDate = (date: string) => {
  if (!date) return ''
  // 确保时间字符串以 Z 结尾表示 UTC
  const utcString = date.endsWith('Z') ? date : date + 'Z'
  const d = new Date(utcString)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${year}/${month}/${day} ${hours}:${minutes}:${seconds}`
}

// 获取我的评价
const fetchMyReviews = async () => {
  try {
    const res: any = await getMyReviews()
    if (res && Array.isArray(res)) {
      myReviews.value = res
    } else if (res && Array.isArray(res.records)) {
      myReviews.value = res.records
    } else {
      myReviews.value = []
    }
  } catch (error) {
    console.error('获取我的评价失败:', error)
    myReviews.value = []
  }
}

// 删除我创建的活动
const handleDeleteMyEvent = async (event: Activity) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除活动 "${event.title}" 吗？删除后不可恢复！`,
      '确认删除',
      { type: 'error', confirmButtonText: '删除' }
    )
    await deleteActivity(event.id)
    ElMessage.success('活动删除成功')
    // 刷新列表
    await fetchMyActivities()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}
</script>

<template>
  <div v-loading="loading" class="profile-container">
    <div class="page-header">
      <div class="header-title">
        <el-icon :size="28" color="var(--primary-color)"><UserFilled /></el-icon>
        <h2>个人中心</h2>
      </div>
    </div>
    
    <!-- 身份认证提示：未绑定身份的老账号须先补填学号/工号，绑定前不允许报名活动 -->
    <el-alert v-if="needIdentityBind" type="warning" :closable="false" class="identity-alert">
      <template #title>请先完善身份认证：绑定学号/工号后才能报名活动</template>
      <el-form ref="bindFormRef" :model="bindForm" :rules="bindRules" label-width="80px" class="bind-form">
        <el-form-item label="身份" prop="userType">
          <el-radio-group v-model="bindForm.userType" @change="handleBindTypeChange">
            <el-radio :label="0">学生</el-radio>
            <el-radio :label="1">教师</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="bindForm.userType === 1 ? '工号' : '学号'" prop="idNumber">
          <el-input
            v-model="bindForm.idNumber"
            :maxlength="bindForm.userType === 1 ? 6 : 10"
            :placeholder="bindForm.userType === 1 ? '请输入6位数字工号' : '请输入10位数字学号（前4位为入学年份2020-2026）'"
            style="width: 360px; max-width: 100%"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="binding" @click="handleBindIdentity">立即绑定</el-button>
        </el-form-item>
      </el-form>
    </el-alert>

    <el-row :gutter="24">
      <el-col :lg="6" :md="8" :sm="24">
        <el-card v-if="userInfo" class="user-card" shadow="never">
          <div class="user-avatar">
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :before-upload="handleAvatarUpload"
              :disabled="uploadingAvatar"
              accept="image/*"
            >
              <el-avatar :size="100" :src="getAvatarUrl(userInfo.avatar)" v-loading="uploadingAvatar">
                {{ userInfo.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="avatar-overlay">
                <el-icon :size="24"><Camera /></el-icon>
                <span>更换头像</span>
              </div>
            </el-upload>
          </div>
          <h3 class="user-name">{{ userInfo.nickname }}</h3>
          <p class="user-id">@{{ userInfo.username }}</p>
          <!-- 身份标签：管理员/教师/学生/未绑定 -->
          <div class="user-identity">
            <el-tag :type="identityTagType" size="small">{{ identityLabel }}</el-tag>
          </div>
          
          <el-divider />
          
          <div class="user-stats">
            <div class="stat-item">
              <span class="stat-value">{{ userInfo.organizedCount || 0 }}</span>
              <span class="stat-label">创建活动</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ userInfo.participatedCount || 0 }}</span>
              <span class="stat-label">参与活动</span>
            </div>
          </div>
          
          <el-divider />
          
          <p class="join-date">
            <el-icon><Calendar /></el-icon>
            加入时间: {{ formatDate(userInfo.createTime) }}
          </p>
        </el-card>
      </el-col>
      
      <el-col :span="18">
        <el-card>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="个人信息" name="info">
              <el-form v-if="userInfo" :model="userInfo" label-width="100px" class="profile-form">
                <el-form-item label="用户名">
                  <el-input v-model="userInfo.username" disabled />
                </el-form-item>

                <!-- 身份与学号/工号：真实用户身份绑定信息，只读展示（绑定/补填走上方身份认证提示） -->
                <el-form-item label="身份">
                  <el-tag :type="identityTagType">{{ identityLabel }}</el-tag>
                </el-form-item>
                <el-form-item label="学号/工号">
                  <el-input :model-value="boundIdNumber || '未绑定'" disabled />
                </el-form-item>

                <el-form-item label="昵称">
                  <el-input v-model="userInfo.nickname" placeholder="请输入昵称" />
                </el-form-item>
                
                <el-form-item label="真实姓名">
                  <el-input v-model="userInfo.realName" placeholder="请输入真实姓名" />
                </el-form-item>
                
                <el-form-item label="性别">
                  <el-radio-group v-model="userInfo.gender">
                    <el-radio :label="0">女</el-radio>
                    <el-radio :label="1">男</el-radio>
                  </el-radio-group>
                </el-form-item>
                
                <el-form-item label="邮箱">
                  <el-input v-model="userInfo.email" placeholder="请输入邮箱" />
                </el-form-item>
                
                <el-form-item label="手机号">
                  <el-input v-model="userInfo.phone" placeholder="请输入手机号" />
                </el-form-item>
                
                <el-form-item label="学院">
                  <el-input v-model="userInfo.college" placeholder="请输入学院" />
                </el-form-item>
                
                <el-form-item label="专业">
                  <el-input v-model="userInfo.major" placeholder="请输入专业" />
                </el-form-item>
                
                <el-form-item label="年级">
                  <el-input v-model="userInfo.grade" placeholder="请输入年级" />
                </el-form-item>
                
                <el-form-item label="个人简介">
                  <el-input v-model="userInfo.bio" type="textarea" :rows="4" placeholder="请输入个人简介" />
                </el-form-item>
                
                <el-form-item>
                  <el-button type="primary" @click="updateProfile">保存修改</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
            
            <el-tab-pane label="我创建的活动" name="created">
              <el-table v-if="myEvents.length > 0" :data="myEvents" style="width: 100%">
                <el-table-column prop="title" label="活动名称" />
                <el-table-column prop="location" label="地点" />
                <el-table-column prop="status" label="状态">
                  <template #default="scope">
                    <el-tag :type="activityStore.getStatusType(scope.row.status)">
                      {{ activityStore.getStatusText(scope.row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="currentParticipants" label="参与人数" />
                <el-table-column label="操作" width="180">
                  <template #default="scope">
                    <el-button link type="primary" @click="$router.push(`/event/${scope.row.id}`)">
                      查看
                    </el-button>
                    <el-button link type="warning" @click="$router.push(`/create-event?id=${scope.row.id}`)">
                      编辑
                    </el-button>
                    <el-button link type="danger" @click="handleDeleteMyEvent(scope.row)">
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-else description="暂无创建的活动" />
            </el-tab-pane>
            
            <el-tab-pane label="我参与的活动" name="participated">
              <el-table v-if="participatedEvents.length > 0" :data="participatedEvents" style="width: 100%">
                <el-table-column prop="title" label="活动名称" />
                <el-table-column prop="location" label="地点" />
                <el-table-column prop="status" label="状态">
                  <template #default="scope">
                    <el-tag :type="activityStore.getStatusType(scope.row.status)">
                      {{ activityStore.getStatusText(scope.row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作">
                  <template #default="scope">
                    <el-button link type="primary" @click="$router.push(`/event/${scope.row.id}`)">
                      查看
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-else description="暂无参与的活动" />
            </el-tab-pane>
            
            <el-tab-pane label="我的收藏" name="favorites">
              <el-table v-if="favoriteEvents.length > 0" :data="favoriteEvents" style="width: 100%">
                <el-table-column prop="title" label="活动名称" />
                <el-table-column prop="location" label="地点" />
                <el-table-column prop="status" label="状态">
                  <template #default="scope">
                    <el-tag :type="activityStore.getStatusType(scope.row.status)">
                      {{ activityStore.getStatusText(scope.row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作">
                  <template #default="scope">
                    <el-button link type="primary" @click="$router.push(`/event/${scope.row.id}`)">
                      查看
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-else description="暂无收藏的活动" />
            </el-tab-pane>
            
            <el-tab-pane label="我的签到记录" name="checkins">
              <el-table v-if="checkInRecords.length > 0" :data="checkInRecords" style="width: 100%">
                <el-table-column label="活动名称" min-width="200">
                  <template #default="scope">
                    {{ checkInActivityMap[scope.row.activityId]?.title || '加载中...' }}
                  </template>
                </el-table-column>
                <el-table-column label="地点" min-width="150">
                  <template #default="scope">
                    {{ checkInActivityMap[scope.row.activityId]?.location || '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="checkInTime" label="签到时间" width="160">
                  <template #default="scope">
                    {{ scope.row.checkInTime?.replace('T', ' ').substring(0, 16) }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="100">
                  <template #default="scope">
                    <el-button link type="primary" @click="$router.push(`/event/${scope.row.activityId}`)">
                      查看活动
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-else description="暂无签到记录" />
            </el-tab-pane>

            <el-tab-pane label="我的评价" name="reviews">
              <el-table v-if="myReviews.length > 0" :data="myReviews" style="width: 100%">
                <el-table-column label="活动" min-width="200">
                  <template #default="scope">
                    <el-button link type="primary" @click="$router.push(`/event/${scope.row.activityId}`)">
                      活动 #{{ scope.row.activityId }}
                    </el-button>
                  </template>
                </el-table-column>
                <el-table-column label="评分" width="200">
                  <template #default="scope">
                    <el-rate v-model="scope.row.rating" disabled show-score />
                  </template>
                </el-table-column>
                <el-table-column prop="content" label="评价内容" show-overflow-tooltip />
                <el-table-column prop="createTime" label="评价时间" width="160">
                  <template #default="scope">
                    {{ formatDate(scope.row.createTime) }}
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-else description="暂无评价记录" />
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.profile-container {
  padding: 0;
  width: 100%;
  max-width: 100%;
}

.page-header {
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 4px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
  white-space: nowrap;
}

.header-title h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  white-space: nowrap;
}

/* 身份认证提示与补填绑定表单 */
.identity-alert {
  margin-bottom: 24px;
  border-radius: var(--radius-lg, 8px);
}

.bind-form {
  margin-top: 12px;
  max-width: 520px;
}

/* 用户卡片中的身份标签 */
.user-identity {
  margin-top: 6px;
}

.user-card {
  text-align: center;
}

.user-avatar {
  display: flex;
  justify-content: center;
  margin-bottom: 15px;
}

.avatar-uploader {
  position: relative;
  cursor: pointer;
}

.avatar-uploader :deep(.el-upload) {
  position: relative;
  display: inline-block;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s;
}

.avatar-uploader:hover .avatar-overlay {
  opacity: 1;
}

.avatar-overlay span {
  font-size: 12px;
  margin-top: 4px;
}

.user-name {
  margin: 10px 0 5px;
}

.user-id {
  color: var(--text-muted);
  margin: 0;
}

.user-stats {
  display: flex;
  justify-content: space-around;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: var(--primary-color);
}

.stat-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 5px;
}

.join-date {
  color: var(--text-muted);
  font-size: 12px;
  margin: 0;
}

.profile-form {
  max-width: 500px;
  padding: 20px 0;
}
</style>
