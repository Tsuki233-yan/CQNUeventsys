<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useActivityStore, useUserStore } from '@/stores'
import { getActivityDetail, uploadActivityCover, getCategoryList, type Category } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const activityStore = useActivityStore()
const userStore = useUserStore()

const isEdit = ref(false)
const eventId = ref<number | null>(null)
const loading = ref(false)

const eventFormRef = ref()
const eventForm = ref({
  title: '',
  description: '',
  categoryId: null as number | null,
  location: '',
  startTime: '',
  endTime: '',
  registrationStartTime: '',
  registrationEndTime: '',
  maxParticipants: 100,
  coverImage: '',
  tags: [] as string[]
  // 注意：所有新活动默认状态为1(待审核)，不需要传递needReview字段
})

const coverImageFile = ref<File | null>(null)
const uploadingCover = ref(false)
const categories = ref<Category[]>([])

// 预设标签列表
const predefinedTags = ref([
  '社团',
  '学术',
  '体育',
  '文艺',
  '志愿',
  '讲座',
  '比赛',
  '聚会',
  '培训',
  '展览'
])

const rules = {
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择活动类型', trigger: 'change' }],
  location: [{ required: true, message: '请输入活动地点', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  description: [{ required: true, message: '请输入活动详情', trigger: 'blur' }]
}

// 获取分类列表
const fetchCategories = async () => {
  try {
    const res: any = await getCategoryList()
    if (res && Array.isArray(res)) {
      categories.value = res.filter((c: Category) => c.status === 1)
    }
  } catch (error) {
    console.error('获取分类列表失败:', error)
  }
}

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  
  // 获取分类列表
  await fetchCategories()
  
  // 检查是否为编辑模式
  const id = route.query.id
  if (id) {
    isEdit.value = true
    eventId.value = Number(id)
    await fetchEventDetail(eventId.value)
  }
})

// 将ISO时间字符串（UTC）转换为本地时间字符串（用于datetime-local输入）
const formatDateTimeLocal = (isoString: string | null | undefined): string => {
  if (!isoString) return ''
  // 确保时间字符串以 Z 结尾表示 UTC，如果没有则添加
  const utcString = isoString.endsWith('Z') ? isoString : isoString + 'Z'
  // 将 UTC 时间字符串转换为 Date 对象（浏览器会自动转换为本地时间）
  const date = new Date(utcString)
  // 转换为本地时间字符串 YYYY-MM-DDTHH:mm:ss 格式
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

const fetchEventDetail = async (id: number) => {
  loading.value = true
  try {
    const res: any = await getActivityDetail(id)
    if (res) {
      eventForm.value = {
        title: res.title,
        description: res.description,
        categoryId: res.categoryId,
        location: res.location,
        startTime: formatDateTimeLocal(res.startTime),
        endTime: formatDateTimeLocal(res.endTime),
        registrationStartTime: formatDateTimeLocal(res.registrationStartTime),
        registrationEndTime: formatDateTimeLocal(res.registrationEndTime),
        maxParticipants: res.maxParticipants,
        coverImage: res.coverImage || '',
        tags: res.tags ? res.tags.split(',').filter((t: string) => t.trim()) : []
      }
    }
  } catch (error) {
    console.error('获取活动详情失败:', error)
  }
  loading.value = false
}

// 处理封面图上传
const handleCoverImageSelect = async (file: File) => {
  if (!file) return false
  
  // 检查文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    return false
  }
  
  // 检查文件大小（10MB，与后端限制一致）
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过10MB')
    return false
  }
  
  coverImageFile.value = file
  // 本地预览
  eventForm.value.coverImage = URL.createObjectURL(file)
  return false // 阻止默认上传行为
}

// 上传封面图到服务器
const uploadCoverImage = async (activityId: number): Promise<string | null> => {
  if (!coverImageFile.value) return eventForm.value.coverImage || null
  
  uploadingCover.value = true
  try {
    const res: any = await uploadActivityCover(activityId, coverImageFile.value)
    // 后端返回可访问的URL
    return res || null
  } catch (error: any) {
    console.error('封面上传失败:', error)
    console.error('错误详情:', error.response?.data || error.message)
    ElMessage.error('封面上传失败: ' + (error.response?.data?.message || error.message || '未知错误'))
  } finally {
    uploadingCover.value = false
  }
  return null
}

// 移除封面图
const removeCoverImage = () => {
  coverImageFile.value = null
  eventForm.value.coverImage = ''
}

/**
 * 获取活动封面图片URL
 * 后端返回格式:
 * - 新上传图片: "/uploads/images/activity_{id}/cover/{filename}"
 * - 测试数据: "/images/activity_{id}/{filename}"
 * 静态资源受 context-path 影响，需要加 /api 前缀
 */
const getImageUrl = (url: string | undefined): string => {
  if (!url) return ''
  // 本地预览URL (blob:) 直接返回
  if (url.startsWith('blob:')) {
    return url
  }
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

// 将本地时间转换为ISO格式（考虑时区）
const toISOString = (dateTimeLocal: string): string => {
  if (!dateTimeLocal) return ''
  const date = new Date(dateTimeLocal)
  return date.toISOString()
}

const submitForm = async () => {
  const valid = await eventFormRef.value?.validate().catch(() => false)
  if (!valid) return
  
  // 时间顺序验证
  const { registrationStartTime, registrationEndTime, startTime, endTime } = eventForm.value
  
  // 验证报名开始时间 < 报名结束时间
  if (registrationStartTime && registrationEndTime) {
    const regStart = new Date(registrationStartTime)
    const regEnd = new Date(registrationEndTime)
    if (regStart >= regEnd) {
      ElMessage.error('报名开始时间必须早于报名结束时间')
      return
    }
  }
  
  // 验证报名结束时间 <= 活动开始时间
  if (registrationEndTime && startTime) {
    const regEnd = new Date(registrationEndTime)
    const actStart = new Date(startTime)
    if (regEnd > actStart) {
      ElMessage.error('报名结束时间必须小于等于活动开始时间')
      return
    }
  }
  
  // 验证活动开始时间 < 活动结束时间
  if (startTime && endTime) {
    const actStart = new Date(startTime)
    const actEnd = new Date(endTime)
    if (actStart >= actEnd) {
      ElMessage.error('活动开始时间必须早于活动结束时间')
      return
    }
  }
  
  // 先创建/更新活动（不带封面）
  const data = {
    ...eventForm.value,
    coverImage: undefined as string | undefined, // 暂时不传封面，后面单独上传
    tags: eventForm.value.tags.join(','), // 将标签数组转换为逗号分隔的字符串
    startTime: toISOString(eventForm.value.startTime),
    endTime: toISOString(eventForm.value.endTime),
    registrationStartTime: eventForm.value.registrationStartTime
      ? toISOString(eventForm.value.registrationStartTime)
      : undefined,
    registrationEndTime: eventForm.value.registrationEndTime
      ? toISOString(eventForm.value.registrationEndTime)
      : undefined,
    categoryId: eventForm.value.categoryId ?? undefined
  }
  
  if (isEdit.value && eventId.value) {
    // 编辑模式：如果有新封面则上传
    if (coverImageFile.value) {
      const coverUrl = await uploadCoverImage(eventId.value)
      if (coverUrl) {
        data.coverImage = coverUrl
      }
    } else {
      data.coverImage = eventForm.value.coverImage || undefined
    }
    
    const success = await activityStore.updateActivityInfo(eventId.value, data)
    if (success) {
      // 刷新活动详情，确保状态更新
      await activityStore.fetchActivityDetail(eventId.value)
      router.push(`/event/${eventId.value}`)
    }
  } else {
    // 创建模式：先创建活动，再上传封面
    const newActivityId = await activityStore.createNewActivity(data)
    if (newActivityId) {
      // 如果有封面图，上传封面
      if (coverImageFile.value) {
        const coverUrl = await uploadCoverImage(newActivityId)
        if (coverUrl) {
          // 更新活动封面
          await activityStore.updateActivityInfo(newActivityId, { coverImage: coverUrl })
        } else {
        }
      }
      router.push('/events')
    } else {
      ElMessage.error('活动创建失败')
    }
  }
}

const cancel = () => {
  router.push('/events')
}
</script>

<template>
  <div v-loading="loading" class="create-event-container">
    <div class="page-header">
      <div class="header-title">
        <el-icon :size="28" color="var(--color-primary)"><Plus /></el-icon>
        <h2>{{ isEdit ? '编辑活动' : '创建新活动' }}</h2>
      </div>
    </div>
    
    <el-card class="form-card" shadow="never">
      <template #header>
        <h2>{{ isEdit ? '编辑活动' : '创建新活动' }}</h2>
      </template>
      
      <el-form ref="eventFormRef" :model="eventForm" :rules="rules" label-width="120px">
        <!-- 封面图上传 -->
        <el-form-item label="活动封面">
          <div class="cover-upload-section">
            <div v-if="eventForm.coverImage" class="cover-preview">
              <el-image :src="getImageUrl(eventForm.coverImage)" fit="cover" class="cover-image" />
              <el-button 
                type="danger" 
                size="small" 
                class="remove-cover-btn"
                @click="removeCoverImage"
              >
                删除
              </el-button>
            </div>
            <el-upload
              v-else
              class="cover-uploader"
              :show-file-list="false"
              :before-upload="handleCoverImageSelect"
              accept="image/*"
            >
              <div class="upload-placeholder">
                <el-icon :size="32" color="var(--border-color)"><Plus /></el-icon>
                <span>点击上传封面</span>
                <span class="upload-tip">支持jpg/png/gif，不超过10MB（与后端限制一致）</span>
              </div>
            </el-upload>
          </div>
        </el-form-item>
        
        <el-form-item label="活动标题" prop="title">
          <el-input v-model="eventForm.title" placeholder="请输入活动标题" />
        </el-form-item>
        
        <el-form-item label="活动类型" prop="categoryId">
          <el-select v-model="eventForm.categoryId" placeholder="请选择活动类型" style="width: 100%">
            <el-option 
              v-for="category in categories" 
              :key="category.id" 
              :label="category.name" 
              :value="category.id" 
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="活动标签">
          <el-select
            v-model="eventForm.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请输入或选择标签"
            style="width: 100%"
          >
            <el-option
              v-for="tag in predefinedTags"
              :key="tag"
              :label="tag"
              :value="tag"
            />
          </el-select>
          <div class="form-tip">可输入自定义标签或选择预设标签，按回车确认</div>
        </el-form-item>
        
        <el-form-item label="活动地点" prop="location">
          <el-input v-model="eventForm.location" placeholder="请输入活动地点" />
        </el-form-item>
        
        <el-form-item label="报名开始时间">
          <el-date-picker
            v-model="eventForm.registrationStartTime"
            type="datetime"
            placeholder="选择报名开始时间"
            style="width: 100%"
            :disabled="isEdit"
          />
        </el-form-item>
        
        <el-form-item label="报名结束时间">
          <el-date-picker
            v-model="eventForm.registrationEndTime"
            type="datetime"
            placeholder="选择报名结束时间"
            style="width: 100%"
            :disabled="isEdit"
          />
        </el-form-item>
        
        <el-form-item label="活动开始时间" prop="startTime">
          <el-date-picker
            v-model="eventForm.startTime"
            type="datetime"
            placeholder="选择活动开始时间"
            style="width: 100%"
            :disabled="isEdit"
          />
        </el-form-item>
        
        <el-form-item label="活动结束时间" prop="endTime">
          <el-date-picker
            v-model="eventForm.endTime"
            type="datetime"
            placeholder="选择活动结束时间"
            style="width: 100%"
            :disabled="isEdit"
          />
        </el-form-item>
        
        <el-form-item label="人数限制">
          <el-input-number v-model="eventForm.maxParticipants" :min="1" :max="10000" />
        </el-form-item>
        
        <el-form-item>
          <el-alert
            title="活动创建后将进入待审核状态，管理员审核通过后即可展示"
            type="info"
            :closable="false"
            show-icon
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="活动详情" prop="description">
          <el-input
            v-model="eventForm.description"
            type="textarea"
            :rows="6"
            placeholder="请输入活动详情描述"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="submitForm">
            {{ isEdit ? '保存修改' : '创建活动' }}
          </el-button>
          <el-button @click="cancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.create-event-container {
  padding: 0;
  width: 100%;
  max-width: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 0 4px;
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

.form-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
  background: var(--bg-primary);
  max-width: 700px;
  margin: 0 auto;
}

.form-card :deep(.el-card__header) {
  border-bottom: 1px solid var(--border-color-light);
}

.form-card h2 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: 20px;
}

.form-tip {
  margin-left: 10px;
  color: var(--text-muted);
  font-size: 12px;
}

.cover-upload-section {
  width: 100%;
}

.cover-preview {
  position: relative;
  width: 300px;
  height: 180px;
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-cover-btn {
  position: absolute;
  top: 8px;
  right: 8px;
}

.cover-uploader {
  width: 300px;
  height: 180px;
  border: 2px dashed var(--border-color);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: border-color 0.3s;
}

.cover-uploader:hover {
  border-color: var(--color-primary);
}

/* 覆盖 el-upload 内部样式 */
.cover-uploader :deep(.el-upload) {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-uploader :deep(.el-upload-dragger) {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: var(--text-regular);
  text-align: center;
  padding: 20px;
}

.upload-placeholder span {
  line-height: 1.4;
}

.upload-tip {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.4;
}
</style>
