<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores'
import { getResultByActivityId, updateResult, deleteResult, uploadResultImages, type ActivityResult } from '@/api/result'
import { getComments, postComment, replyComment, deleteComment, type Comment } from '@/api/comment'
import { banUser, unbanUser } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MdEditor, MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activityId = Number(route.params.id)
const loading = ref(false)
const achievement = ref<ActivityResult | null>(null)
const comments = ref<Comment[]>([])
const newMessage = ref('')
const showEditDialog = ref(false)
const uploadingImages = ref(false)
const editForm = ref({
  title: '',
  content: '',
  images: [] as string[]
})

// 回复相关状态
const replyState = ref<{
  isReplying: boolean;
  parentId: number | null;
  replyToUserId: number | null;
  replyToUserName: string;
  replyContent: string;
}>({
  isReplying: false,
  parentId: null,
  replyToUserId: null,
  replyToUserName: '',
  replyContent: ''
})

// 获取成果详情
const fetchAchievementDetail = async () => {
  loading.value = true
  try {
    const res: any = await getResultByActivityId(activityId)
    if (res && res.id) {
      achievement.value = res
      // 获取到成果详情后，再获取评论（使用成果展示贴的ID）
      await fetchComments()
    }
  } catch (error: any) {
    console.error('获取成果详情失败:', error)
  }
  loading.value = false
}

// 获取评论列表
const fetchComments = async () => {
  if (!achievement.value?.id) return
  const res: any = await getComments(achievement.value.id, 2) // 2-成果展示贴类型
  if (res) {
    comments.value = res
  }
}

onMounted(() => {
  fetchAchievementDetail()
})

const goBack = () => {
  router.push('/achievements')
}

/**
 * 解析成果展示贴图片URL列表
 * 后端返回格式: ["/uploads/images/activity_1/xxx.jpg", ...] 或 ["/api/uploads/images/activity_1/xxx.jpg", ...]
 * 静态资源受 context-path 影响，需要加 /api 前缀
 */
const parseImages = (images: string | undefined): string[] => {
  if (!images) return []
  try {
    const parsed = JSON.parse(images)
    if (!Array.isArray(parsed)) return []
    // 处理每个URL，确保有 /api 前缀
    return parsed.filter(Boolean).map((url: string) => {
      if (url.startsWith('/api/')) {
        return url
      }
      return '/api' + url
    })
  } catch {
    return []
  }
}

/**
 * 获取头像URL
 * 静态资源受 context-path 影响，需要加 /api 前缀
 */
const getAvatarUrl = (url: string | undefined): string => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('/api/')) return url
  return '/api' + url
}

// 提交评论
const submitMessage = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  
  if (!newMessage.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  
  if (!achievement.value?.id) return
  
  await postComment(achievement.value.id, 2, newMessage.value) // 2-成果展示贴类型
  newMessage.value = ''
  ElMessage.success('评论成功')
  await fetchComments()
}

// 开始回复
const startReply = (comment: Comment, replyToUser?: { id: number; nickname: string }) => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  
  replyState.value = {
    isReplying: true,
    parentId: comment.id,
    replyToUserId: replyToUser?.id || null,
    replyToUserName: replyToUser?.nickname || comment.user?.nickname || '',
    replyContent: ''
  }
}

// 取消回复
const cancelReply = () => {
  replyState.value = {
    isReplying: false,
    parentId: null,
    replyToUserId: null,
    replyToUserName: '',
    replyContent: ''
  }
}

// 提交回复
const submitReply = async () => {
  if (!replyState.value.replyContent.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  
  if (!replyState.value.parentId) return
  
  await replyComment(
    replyState.value.parentId,
    replyState.value.replyContent,
    replyState.value.replyToUserId || undefined
  )
  
  ElMessage.success('回复成功')
  cancelReply()
  await fetchComments()
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

// 检查当前用户是否是创建者
const isCreator = computed(() => {
  return achievement.value?.creatorId === userStore.userInfo?.id
})

// 计算总评论数（包括顶层评论和所有回复）
const totalCommentCount = computed(() => {
  let count = comments.value.length
  comments.value.forEach(comment => {
    if (comment.children && comment.children.length > 0) {
      count += comment.children.length
    }
  })
  return count
})

// 跳转到编辑页面
const goToEditPage = () => {
  if (!achievement.value) return
  router.push(`/edit-achievement?activityId=${activityId}&resultId=${achievement.value.id}`)
}

// 处理图片上传
const handleImageUpload = async (file: File) => {
  if (!file) return false
  
  if (editForm.value.images.length >= 9) {
    ElMessage.warning('最多只能上传9张图片')
    return false
  }
  
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    return false
  }
  
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('单张图片大小不能超过10MB')
    return false
  }
  
  uploadingImages.value = true
  try {
    const res: any = await uploadResultImages(activityId, [file])
    if (res && Array.isArray(res)) {
      editForm.value.images.push(...res)
      ElMessage.success('图片上传成功')
    }
  } catch (error) {
    console.error('图片上传失败:', error)
    ElMessage.error('图片上传失败')
  } finally {
    uploadingImages.value = false
  }
  return false
}

// 移除图片
const removeImage = (index: number) => {
  editForm.value.images.splice(index, 1)
}

// 保存编辑
const handleSaveEdit = async () => {
  if (!editForm.value.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!achievement.value) return
  
  try {
    await updateResult(
      achievement.value.id,
      editForm.value.title,
      editForm.value.content,
      JSON.stringify(editForm.value.images)
    )
    ElMessage.success('更新成功')
    showEditDialog.value = false
    await fetchAchievementDetail()
  } catch (error) {
    console.error('更新失败:', error)
    ElMessage.error('更新失败')
  }
}

// 删除成果展示贴
const handleDelete = async () => {
  if (!achievement.value) return
  try {
    await ElMessageBox.confirm(
      '确定要删除这个成果展示贴吗？删除后不可恢复！',
      '确认删除',
      { type: 'error', confirmButtonText: '删除' }
    )
    await deleteResult(achievement.value.id)
    ElMessage.success('删除成功')
    router.push('/achievements')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 删除评论
const handleDeleteComment = async (commentId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '确认删除', { type: 'warning' })
    await deleteComment(commentId)
    ElMessage.success('删除成功')
    await fetchComments()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 封禁/解封用户（管理员功能）
const handleBanUser = async (userId: number, userName: string, isBanned: boolean) => {
  const action = isBanned ? '解封' : '封禁'
  const confirmText = isBanned 
    ? `确定要解封用户 "${userName}" 吗？` 
    : `确定要封禁用户 "${userName}" 吗？封禁后该用户将无法登录系统。`
  
  try {
    await ElMessageBox.confirm(
      confirmText,
      `确认${action}`,
      { type: isBanned ? 'info' : 'warning', confirmButtonText: action, cancelButtonText: '取消' }
    )
    
    if (isBanned) {
      await unbanUser(userId)
    } else {
      await banUser(userId)
    }
    
    ElMessage.success(`${action}成功`)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}
</script>

<template>
  <div v-loading="loading" class="achievement-detail-container">
    <div class="page-header">
      <div class="back-link" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回成果列表</span>
      </div>
    </div>
    
    <!-- 成果展示贴内容 -->
    <el-card v-if="achievement" class="detail-card">
      <template #header>
        <div class="detail-header">
          <h2>{{ achievement.title }}</h2>
          <div class="header-actions">
            <!-- 创建者可以编辑和删除 -->
            <template v-if="isCreator">
              <el-button type="primary" size="small" @click="goToEditPage">编辑</el-button>
              <el-button type="danger" size="small" @click="handleDelete">删除</el-button>
            </template>
            <div class="header-meta">
              <div class="creator-info">
                <el-avatar :size="32" :src="getAvatarUrl(achievement.creator?.avatar)">
                  {{ achievement.creator?.nickname?.charAt(0) || 'U' }}
                </el-avatar>
                <span class="creator-name">{{ achievement.creator?.nickname || '匿名用户' }}</span>
              </div>
              <span class="create-time">{{ formatDate(achievement.createTime) }}</span>
            </div>
          </div>
        </div>
      </template>
      
      <div class="detail-content">
        <!-- 关联活动 -->
        <div v-if="achievement.activity" class="activity-link">
          <el-tag type="warning" effect="light">
            <el-icon><Calendar /></el-icon>
            {{ achievement.activity.title }}
          </el-tag>
        </div>
        
        <!-- 内容 - Markdown 渲染 -->
        <div class="content-section">
          <MdPreview
            :model-value="achievement.content || ''"
            :editor-id="'preview-' + achievement.id"
            language="zh-CN"
            theme="light"
          />
        </div>
        
        <!-- 图片展示 -->
        <div v-if="parseImages(achievement.images).length > 0" class="images-section">
          <el-image
            v-for="(img, index) in parseImages(achievement.images)"
            :key="index"
            :src="img"
            :preview-src-list="parseImages(achievement.images)"
            fit="cover"
            class="achievement-image"
          />
        </div>
        
        <!-- 统计信息 -->
        <div class="stats-section">
          <div class="stat-item">
            <el-icon><View /></el-icon>
            <span>{{ achievement.viewCount }} 浏览</span>
          </div>
          <div class="stat-item">
            <el-icon><ChatDotRound /></el-icon>
            <span>{{ achievement.commentCount }} 评论</span>
          </div>
        </div>
      </div>
    </el-card>
    
    <!-- 编辑对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑成果展示贴"
      width="900px"
      :close-on-click-modal="false"
    >
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="内容">
          <MdEditor
            v-model="editForm.content"
            :editor-id="'edit-' + achievement?.id"
            language="zh-CN"
            theme="light"
            placeholder="支持 Markdown 格式，可插入图片、链接等"
            :toolbars="[
              'bold',
              'underline',
              'italic',
              'strikeThrough',
              'title',
              'sub',
              'sup',
              'quote',
              'unorderedList',
              'orderedList',
              'codeRow',
              'code',
              'link',
              'image',
              'table',
              'preview',
              'fullscreen'
            ]"
            style="height: 400px"
          />
        </el-form-item>
        <el-form-item label="图片">
          <div class="image-upload-section">
            <div v-for="(img, index) in editForm.images" :key="index" class="image-item">
              <el-image :src="img" style="width: 100px; height: 100px;" fit="cover" />
              <el-button type="danger" size="small" @click="removeImage(index)">删除</el-button>
            </div>
            <el-upload
              v-if="editForm.images.length < 9"
              class="image-uploader"
              :show-file-list="false"
              :before-upload="handleImageUpload"
              :disabled="uploadingImages"
              accept="image/*"
            >
              <el-button type="primary" :loading="uploadingImages">
                <el-icon><Plus /></el-icon>
                上传图片
              </el-button>
              <template #tip>
                <div class="upload-tip">最多9张，单张不超过10MB</div>
              </template>
            </el-upload>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 评论区 -->
    <el-card class="comment-card">
      <template #header>
        <h3>评论 ({{ totalCommentCount }})</h3>
      </template>
      
      <div class="comment-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <div class="comment-header">
            <div class="comment-user-info">
              <el-avatar :size="32" :src="getAvatarUrl(comment.user?.avatar)">
                {{ comment.user?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="comment-user">{{ comment.user?.nickname || '匿名用户' }}</span>
              <span class="comment-time">{{ formatDate(comment.createTime) }}</span>
            </div>
            <div class="comment-actions">
              <!-- 管理员封禁按钮 -->
              <el-button 
                v-if="userStore.isAdmin && comment.user?.id"
                link 
                :type="comment.user?.status === 0 ? 'success' : 'danger'" 
                size="small"
                @click="handleBanUser(comment.user!.id, comment.user!.nickname, comment.user!.status === 0)"
              >
                {{ comment.user?.status === 0 ? '解封' : '封禁' }}
              </el-button>
              <!-- 删除评论（自己或管理员） -->
              <el-button 
                v-if="comment.user?.id === userStore.userInfo?.id || userStore.isAdmin"
                link 
                type="danger" 
                size="small"
                @click="handleDeleteComment(comment.id)"
              >
                删除
              </el-button>
              <el-button 
                v-if="replyState.parentId !== comment.id"
                link 
                type="primary" 
                size="small"
                @click="startReply(comment)"
              >
                回复
              </el-button>
            </div>
          </div>
          <p class="comment-content">{{ comment.content }}</p>
          
          <!-- 回复列表 -->
          <div v-if="comment.children && comment.children.length > 0" class="reply-list">
            <div v-for="reply in comment.children" :key="reply.id" class="reply-item">
              <div class="reply-header">
                <el-avatar :size="24" :src="getAvatarUrl(reply.user?.avatar)">
                  {{ reply.user?.nickname?.charAt(0) || 'U' }}
                </el-avatar>
                <span class="reply-user">{{ reply.user?.nickname }}</span>
                <span v-if="reply.replyToUser" class="reply-to"> 回复 {{ reply.replyToUser.nickname }}</span>
                <span class="reply-time">{{ formatDate(reply.createTime) }}</span>
                <div class="reply-actions">
                  <!-- 管理员封禁按钮 -->
                  <el-button 
                    v-if="userStore.isAdmin && reply.user?.id"
                    link 
                    :type="reply.user?.status === 0 ? 'success' : 'danger'" 
                    size="small"
                    @click="handleBanUser(reply.user!.id, reply.user!.nickname, reply.user!.status === 0)"
                  >
                    {{ reply.user?.status === 0 ? '解封' : '封禁' }}
                  </el-button>
                  <!-- 删除回复（自己或管理员） -->
                  <el-button 
                    v-if="reply.user?.id === userStore.userInfo?.id || userStore.isAdmin"
                    link 
                    type="danger" 
                    size="small"
                    @click="handleDeleteComment(reply.id)"
                  >
                    删除
                  </el-button>
                  <el-button 
                    link 
                    type="primary" 
                    size="small"
                    @click="startReply(comment, reply.user || undefined)"
                  >
                    回复
                  </el-button>
                </div>
              </div>
              <span class="reply-content">{{ reply.content }}</span>
            </div>
          </div>
          
          <!-- 回复输入框 -->
          <div v-if="replyState.parentId === comment.id" class="reply-input-area">
            <el-input
              v-model="replyState.replyContent"
              type="textarea"
              :rows="2"
              :placeholder="replyState.replyToUserName ? `回复 ${replyState.replyToUserName}:` : '写下你的回复...'"
            />
            <div class="reply-actions">
              <el-button size="small" @click="cancelReply">取消</el-button>
              <el-button type="primary" size="small" @click="submitReply">发送回复</el-button>
            </div>
          </div>
        </div>
        
        <el-empty v-if="comments.length === 0" description="暂无评论" />
      </div>
      
      <div class="comment-input">
        <el-input
          v-model="newMessage"
          type="textarea"
          :rows="3"
          placeholder="发表你的评论..."
        />
        <el-button type="primary" @click="submitMessage" style="margin-top: 10px">
          发表评论
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.achievement-detail-container {
  padding: 0;
  width: 100%;
  max-width: 100%;
}

.page-header {
  margin-bottom: 24px;
  padding: 0 4px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 15px;
  transition: all 0.2s;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
}

.back-link:hover {
  color: var(--color-primary);
  background-color: var(--primary-bg);
}

.detail-card,
.comment-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
  background: var(--bg-primary);
  margin-top: 20px;
}

.detail-card {
  margin-top: 0;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 24px;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.creator-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.creator-name {
  color: var(--text-regular);
  font-weight: 500;
}

.create-time {
  color: var(--text-muted);
  font-size: 14px;
}

.detail-content {
  padding: 20px 0;
}

.activity-link {
  margin-bottom: 20px;
}

.activity-link :deep(.el-tag) {
  display: inline-flex !important;
  align-items: center !important;
  vertical-align: middle !important;
  height: auto !important;
  padding: 6px 12px !important;
}

.activity-link :deep(.el-tag__content) {
  display: inline-flex !important;
  align-items: center !important;
  gap: 6px !important;
}

.activity-link :deep(.el-icon) {
  display: inline-flex !important;
  align-items: center !important;
  vertical-align: middle !important;
  margin-right: 0 !important;
}

.content-section {
  margin-bottom: 24px;
}

.content-text {
  color: var(--text-regular);
  line-height: 1.8;
  font-size: 16px;
  white-space: pre-wrap;
}

.images-section {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.achievement-image {
  width: 100%;
  height: 200px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  cursor: pointer;
}

.stats-section {
  display: flex;
  gap: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 14px;
}

.comment-card {
  margin-top: 20px;
}

.comment-list {
  margin-bottom: 20px;
}

.comment-item {
  padding: 16px;
  border-bottom: 1px solid var(--border-color-light);
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.comment-user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.comment-user {
  font-weight: 600;
  color: var(--color-primary);
}

.comment-time {
  color: var(--text-muted);
  font-size: 12px;
}

.comment-content {
  color: var(--text-regular);
  margin: 0 0 12px 42px;
  line-height: 1.6;
}

.reply-list {
  margin: 12px 0 12px 42px;
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
}

.reply-item {
  margin: 8px 0;
  font-size: 14px;
  padding: 10px;
  background: var(--bg-primary);
  border-radius: var(--radius-sm);
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.reply-user {
  color: var(--color-primary);
  font-weight: 600;
  font-size: 13px;
}

.reply-to {
  color: var(--text-secondary);
  font-size: 12px;
}

.reply-time {
  color: var(--text-muted);
  font-size: 12px;
  margin-left: auto;
}

.reply-content {
  color: var(--text-regular);
  padding-left: 32px;
  line-height: 1.5;
}

.reply-input-area {
  margin: 12px 0 12px 42px;
  padding: 12px;
  background: var(--primary-bg);
  border-radius: var(--radius-sm);
  border: 1px solid var(--primary-bg-hover);
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.comment-input {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
}

.comment-actions,
.reply-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 删除按钮红色文字 */
.comment-actions .el-button--danger.is-link,
.reply-actions .el-button--danger.is-link {
  color: var(--color-danger) !important;
}

.comment-actions .el-button--danger.is-link:hover,
.reply-actions .el-button--danger.is-link:hover {
  color: var(--el-color-danger-light-3) !important;
}

.image-upload-section {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.image-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.image-uploader {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  border: 2px dashed var(--border-color);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: border-color 0.3s;
}

.image-uploader:hover {
  border-color: var(--color-primary);
}

.upload-tip {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 8px;
}

/* Markdown 编辑器样式 */
.content-section {
  margin: 20px 0;
}

/* 移除 MdPreview 的外框和背景 */
.content-section :deep(.md-editor) {
  border: none !important;
  background: transparent !important;
  box-shadow: none !important;
}

.content-section :deep(.md-editor-preview) {
  background: transparent !important;
  border: none !important;
}

.content-section :deep(.md-editor-preview-wrapper) {
  padding: 0 !important;
  background: transparent !important;
}

/* 移除可能的外层容器边框 */
.content-section :deep(.md-editor-content) {
  background: transparent !important;
  border: none !important;
}

/* 编辑对话框中的编辑器 */
:deep(.md-editor) {
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
}

:deep(.md-editor-toolbar) {
  border-bottom: 1px solid var(--border-color-light);
}
</style>
