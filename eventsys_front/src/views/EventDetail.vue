<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useActivityStore } from '@/stores'
import { useUserStore } from '@/stores'
import { 
  deleteActivity, 
  getActivityCheckInList,
  getActivityOrganizers,
  addActivityOrganizer,
  removeActivityOrganizer,
  getActivityParticipants,
  removeActivityParticipant,
  reviewRegistration
} from '@/api/activity'
import { getComments, postComment, replyComment, deleteComment, type Comment } from '@/api/comment'
import { addFavorite, removeFavorite, checkFavoriteStatus } from '@/api/favorite'
import { createResult, checkResultExists, uploadResultImages } from '@/api/result'
import { banUser, unbanUser } from '@/api/admin'
import { getActivityReviews, getReviewStatistics, createReview, replyReview, type Review, type ReviewStatistics } from '@/api/review'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const activityStore = useActivityStore()
const userStore = useUserStore()

const eventId = Number(route.params.id)
const loading = ref(false)
const comments = ref<Comment[]>([])
const newMessage = ref('')

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
const isFavorited = ref(false)
const isJoining = ref(false)
const isCheckingIn = ref(false)
const showResultDialog = ref(false)
const showJoinDialog = ref(false)
const showOrganizersDialog = ref(false)
const showParticipantsDialog = ref(false)
const showCheckInListDialog = ref(false)
const resultForm = ref({
  title: '',
  content: '',
  images: [] as string[]
})
const joinForm = ref({
  remark: '',
  images: [] as File[]
})
const hasResult = ref(false)
const uploadingImages = ref(false)
const organizers = ref<any[]>([])
const participants = ref<any[]>([])
const checkInList = ref<any[]>([])
const newOrganizerId = ref('')

// 评价相关状态
const reviews = ref<Review[]>([])
const reviewStats = ref<ReviewStatistics | null>(null)
const reviewForm = ref({
  rating: 5,
  content: ''
})
const showReviewForm = ref(false)
const isSubmittingReview = ref(false)
const showReplyDialog = ref(false)
const replyContent = ref('')
const currentReviewId = ref<number | null>(null)
const isSubmittingReply = ref(false)

const event = computed(() => activityStore.currentActivity)
const isOrganizer = computed(() => activityStore.isOrganizer)
const isOrganizerOrManager = computed(() => activityStore.isOrganizerOrManager)
const activityRole = computed(() => activityStore.activityRole)

/**
 * 身份绑定状态（真实用户身份绑定：学生绑学号、教师绑工号）
 * 未绑定身份的老账号不允许报名活动，须先到个人中心完善身份认证；
 * 管理员（role=1）为平台运营角色，豁免绑定校验。
 * 校验规则为模拟规则，可替换为学籍接口对接
 */
const isIdentityBound = computed(() => {
  const u = userStore.userInfo
  if (!u) return false
  if (u.role === 1) return true
  if (u.userType === 1) return !!u.teacherId
  if (u.userType === 0) return !!u.studentId
  return false
})

/**
 * 获取图片URL
 * 后端返回格式:
 * - 新上传图片: "/uploads/images/activity_{id}/cover/{filename}"
 * - 测试数据: "/images/activity_{id}/{filename}"
 * 静态资源受 context-path 影响，需要加 /api 前缀
 */
const getImageUrl = (url: string | undefined): string => {
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

// 计算报名进度百分比
const getJoinPercentage = () => {
  if (!event.value || event.value.maxParticipants <= 0) return 0
  return Math.round((event.value.currentParticipants / event.value.maxParticipants) * 100)
}

// 格式化进度条显示文本
const formatProgressText = () => {
  const percentage = getJoinPercentage()
  return percentage >= 100 ? `${percentage}% (已满)` : `${percentage}%`
}

/**
 * 解析报名图片URL列表
 * 后端返回格式: ["/uploads/images/activity_1/xxx.jpg", ...] 或 ["/api/uploads/images/activity_1/xxx.jpg", ...]
 * 静态资源受 context-path 影响，需要加 /api 前缀
 */
const parseJoinImages = (images: string | undefined): string[] => {
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

// 获取活动详情
const fetchEventDetail = async () => {
  loading.value = true
  try {
    await activityStore.fetchActivityDetail(eventId)
  } catch (error) {
    console.error('获取活动详情失败:', error)
    ElMessage.error('获取活动详情失败')
  } finally {
    loading.value = false
  }
}

// 获取评论列表
const fetchComments = async () => {
  const res: any = await getComments(eventId, 1) // 1-活动类型
  if (res) {
    comments.value = res
  }
}

// 检查收藏状态
const checkFavorite = async () => {
  if (!userStore.isLoggedIn) return
  const res: any = await checkFavoriteStatus(eventId)
  // 响应拦截器提取了 response.data，对于 ResultBoolean 类型，返回的就是 boolean 值
  isFavorited.value = res === true
}

// 获取活动角色
const fetchRole = async () => {
  if (!userStore.isLoggedIn) return
  await activityStore.fetchActivityRole(eventId)
}

onMounted(async () => {
  await fetchEventDetail()
  fetchComments()
  // 评价仅在活动结束后加载
  if (event.value?.status === 5) {
    fetchReviews()
    fetchReviewStatistics()
  }
  checkResult()
  // 以下接口需要登录才能调用
  if (userStore.isLoggedIn) {
    // 刷新用户信息，确保身份绑定状态（userType/学号/工号）以后端数据为准，不信任本地缓存
    await userStore.fetchUserInfo()
    checkFavorite()
    await fetchRole()
  }
  // 启动活动详情定时刷新（传入 userStore 用于刷新角色信息）
  activityStore.startActivityDetailPolling(eventId, userStore)
})

onUnmounted(() => {
  // 停止定时刷新
  activityStore.stopActivityDetailPolling()
})

const goBack = () => {
  router.push('/events')
}

// 打开报名对话框
const openJoinDialog = () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  // 未绑定身份（学号/工号）不允许报名，引导前往个人中心补填（后端报名接口同样校验）
  if (!isIdentityBound.value) {
    ElMessage.warning('请先在个人中心完善身份认证（绑定学号/工号）后再报名活动')
    router.push('/profile')
    return
  }
  joinForm.value = {
    remark: '',
    images: []
  }
  showJoinDialog.value = true
}

// 报名
const handleJoin = async () => {
  isJoining.value = true
  
  const success = await activityStore.join(eventId, joinForm.value.remark, joinForm.value.images)
  if (success) {
    showJoinDialog.value = false
    joinForm.value = { remark: '', images: [] }
    // 确保即使 fetchRole 失败，fetchEventDetail 也会执行
    try {
      await fetchRole()
    } catch (e) {
      console.error('获取角色失败:', e)
    }
    await fetchEventDetail()
  }
  isJoining.value = false
}

// 处理报名图片选择
const handleJoinImageSelect = (file: File) => {
  if (!file) return false
  
  // 检查图片数量限制
  if (joinForm.value.images.length >= 3) {
    ElMessage.warning('最多只能上传3张图片')
    return false
  }
  
  // 检查文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    return false
  }
  
  // 检查文件大小（5MB）
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('单张图片大小不能超过5MB')
    return false
  }
  
  joinForm.value.images.push(file)
  return false // 阻止默认上传行为
}

// 移除报名图片
const removeJoinImage = (index: number) => {
  joinForm.value.images.splice(index, 1)
}

// 生成本地图片预览地址（模板表达式无法直接访问全局 URL 对象）
const createObjectUrl = (file: File) => URL.createObjectURL(file)

// 取消报名
const handleCancelJoin = async () => {
  isJoining.value = true
  const success = await activityStore.cancel(eventId)
  if (success) {
    // 确保即使 fetchRole 失败，fetchEventDetail 也会执行
    try {
      await fetchRole()
    } catch (e) {
      console.error('获取角色失败:', e)
    }
    await fetchEventDetail()
  }
  isJoining.value = false
}

// 签到
const handleCheckIn = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  
  isCheckingIn.value = true
  const success = await activityStore.doCheckIn(eventId)
  if (success) {
    await fetchRole()
  }
  isCheckingIn.value = false
}

// 开始活动（组织者）
const handleStart = async () => {
  const success = await activityStore.start(eventId)
  if (success) {
    await fetchEventDetail()
    // 重新获取角色信息，更新签到权限
    await fetchRole()
  }
}

// 删除活动（组织者）
const handleDeleteEvent = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个活动吗？删除后不可恢复！',
      '确认删除',
      { type: 'error', confirmButtonText: '删除' }
    )
    await deleteActivity(eventId)
    ElMessage.success('活动删除成功')
    router.push('/events')
  } catch (error: any) {
    // ElMessageBox 取消不做处理，API错误由全局拦截器处理
    if (error !== 'cancel') {
      console.error('删除活动失败:', error)
    }
  }
}

// 收藏/取消收藏
const toggleFavorite = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  
  if (isFavorited.value) {
    await removeFavorite(eventId)
    isFavorited.value = false
    ElMessage.success('已取消收藏')
  } else {
    await addFavorite(eventId)
    isFavorited.value = true
    ElMessage.success('收藏成功')
  }
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
  
  await postComment(eventId, 1, newMessage.value)
  newMessage.value = ''
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

// 解析标签字符串（逗号分隔）为数组
const parseTags = (tags: string | undefined): string[] => {
  if (!tags) return []
  return tags.split(',').filter(t => t.trim())
}

const getTagType = (tag: string) => {
  const map: Record<string, string> = {
    '1': 'primary',
    '2': 'success',
    '3': 'warning',
    '4': 'danger',
    '5': 'info'
  }
  return map[tag] || 'info'
}

const getTagLabel = (tag: string) => {
  const map: Record<string, string> = {
    '1': '社团',
    '2': '学术',
    '3': '体育',
    '4': '文艺',
    '5': '志愿'
  }
  return map[tag] || tag
}

// 活动状态判断
const isActivityEnded = computed(() => event.value?.status === 5) // 已结束
const isActivityStarted = computed(() => event.value?.status === 3 || event.value?.status === 4) // 即将开始或进行中

// 是否处于报名和活动开始之间的空档期（报名已结束但活动未开始）
const isRegistrationGap = computed(() => {
  if (event.value?.status !== 2) return false // 只有报名中状态才可能有空档期
  if (!event.value?.registrationEndTime) return false
  const now = new Date()
  const registrationEnd = new Date(event.value.registrationEndTime.endsWith('Z') ? event.value.registrationEndTime : event.value.registrationEndTime + 'Z')
  return now > registrationEnd // 当前时间已超过报名结束时间
})

// 是否可以报名（仅在报名中状态且未报名，且不在空档期，且不是组织者/管理者）
const canJoin = computed(() => {
  return event.value?.status === 2 && // 报名中
         !isRegistrationGap.value && // 不在报名-活动开始空档期
         activityRole.value?.role !== 2 && // 未报名
         activityRole.value?.status !== 3 && // 未签到
         !isOrganizerOrManager.value // 不是组织者或管理者
})

// 是否可以取消报名（仅在报名中状态可以取消，活动开始后不能取消，且不在空档期，且不是组织者/管理者）
const canCancelJoin = computed(() => {
  return activityRole.value?.role === 2 && // 已报名
         event.value?.status === 2 && // 仅在报名中状态可以取消
         !isRegistrationGap.value && // 不在报名-活动开始空档期
         activityRole.value?.status !== 3 && // 已签到后不能取消
         !isOrganizerOrManager.value // 不是组织者或管理者
})

// 是否可以签到（活动即将开始或进行中，且已通过报名且未签到，且不是组织者/管理者）
const canCheckIn = computed(() => {
  return activityRole.value?.canCheckIn && 
         (event.value?.status === 3 || event.value?.status === 4) && // 即将开始或进行中
         activityRole.value?.status === 1 && // 已通过报名（status=1）
         !isActivityEnded.value &&
         !isOrganizerOrManager.value // 不是组织者或管理者
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

// 检查是否已有成果展示贴
const checkResult = async () => {
  try {
    const res: any = await checkResultExists(eventId)
    hasResult.value = res === true
  } catch (error) {
    console.error('检查成果展示贴失败:', error)
  }
}

// 查看成果展示贴详情
const viewResultDetail = async () => {
  try {
    // 使用 eventId（活动ID）获取成果展示贴详情
    // 后端会根据 activityId 返回对应的成果展示贴
    router.push(`/achievement/${eventId}`)
  } catch (error) {
    console.error('跳转成果展示贴失败:', error)
    ElMessage.error('跳转失败')
  }
}

// 跳转到创建成果展示贴页面
const goToCreateResult = () => {
  if (hasResult.value) {
    ElMessage.warning('该活动已创建成果展示贴')
    return
  }
  router.push(`/edit-achievement?activityId=${eventId}`)
}

// 创建成果展示贴
const handleCreateResult = async () => {
  if (!resultForm.value.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  try {
    await createResult(
      eventId,
      resultForm.value.title,
      resultForm.value.content,
      JSON.stringify(resultForm.value.images)
    )
    ElMessage.success('成果展示贴创建成功')
    showResultDialog.value = false
    hasResult.value = true
  } catch (error) {
    console.error('创建成果展示贴失败:', error)
  }
}

// 移除图片
const removeImage = (index: number) => {
  resultForm.value.images.splice(index, 1)
}

// 处理图片文件选择
const handleImageSelect = async (file: File) => {
  if (!file) return
  
  // 检查图片数量限制
  if (resultForm.value.images.length >= 9) {
    ElMessage.warning('最多只能上传9张图片')
    return false
  }
  
  // 检查文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    return false
  }
  
  // 检查文件大小（10MB）
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('单张图片大小不能超过10MB')
    return false
  }
  
  uploadingImages.value = true
  try {
    const res: any = await uploadResultImages(eventId, [file])
    if (res && Array.isArray(res)) {
      resultForm.value.images.push(...res)
      ElMessage.success('图片上传成功')
    }
  } catch (error) {
    console.error('图片上传失败:', error)
    ElMessage.error('图片上传失败')
  } finally {
    uploadingImages.value = false
  }
  return false // 阻止默认上传行为
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

// 获取管理者列表
const fetchOrganizers = async () => {
  try {
    const res: any = await getActivityOrganizers(eventId)
    organizers.value = res || []
  } catch (error) {
    console.error('获取管理者列表失败:', error)
    organizers.value = []
  }
}

// 打开添加管理者对话框
const openOrganizersDialog = () => {
  showOrganizersDialog.value = true
  fetchOrganizers()
}

// 添加管理者
const handleAddOrganizer = async () => {
  if (!newOrganizerId.value) {
    ElMessage.warning('请输入用户ID')
    return
  }
  try {
    await addActivityOrganizer(eventId, parseInt(newOrganizerId.value))
    ElMessage.success('添加成功')
    newOrganizerId.value = ''
    await fetchOrganizers()
  } catch (error) {
    // API错误由全局拦截器处理
    console.error('添加管理者失败:', error)
  }
}

// 移除管理者
const handleRemoveOrganizer = async (userId: number, userName: string) => {
  try {
    await ElMessageBox.confirm(
      `确定要移除管理者 "${userName}" 吗？`,
      '确认移除',
      { type: 'warning' }
    )
    await removeActivityOrganizer(eventId, userId)
    ElMessage.success('移除成功')
    await fetchOrganizers()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('移除管理者失败:', error)
    }
  }
}

// 获取报名人员列表
const fetchParticipants = async () => {
  try {
    const res: any = await getActivityParticipants(eventId)
    participants.value = res || []
  } catch (error) {
    console.error('获取报名人员失败:', error)
    participants.value = []
  }
}

// 打开报名人员管理对话框
const openParticipantsDialog = () => {
  showParticipantsDialog.value = true
  fetchParticipants()
}

// 审核报名人员（通过/拒绝）
const handleReviewParticipant = async (registrationId: number, status: number, actionName: string) => {
  try {
    await ElMessageBox.confirm(
      `确定要${actionName}该报名申请吗？`,
      `确认${actionName}`,
      { type: status === 1 ? 'success' : 'warning' }
    )
    await reviewRegistration(registrationId, status)
    ElMessage.success(`${actionName}成功`)
    await fetchParticipants()
    await fetchEventDetail()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error(`${actionName}报名失败:`, error)
    }
  }
}

// 移除报名人员
const handleRemoveParticipant = async (registrationId: number, userName: string) => {
  try {
    await ElMessageBox.confirm(
      `确定要移除报名人员 "${userName}" 吗？`,
      '确认移除',
      { type: 'warning' }
    )
    await removeActivityParticipant(registrationId)
    ElMessage.success('移除成功')
    // 刷新报名人员列表
    await fetchParticipants()
    // 刷新活动详情，更新报名人数
    await fetchEventDetail()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('移除报名人员失败:', error)
    }
  }
}

// 获取签到列表
const fetchCheckInList = async () => {
  try {
    const res: any = await getActivityCheckInList(eventId)
    checkInList.value = res || []
  } catch (error) {
    console.error('获取签到列表失败:', error)
    checkInList.value = []
  }
}

// 打开签到列表对话框
const openCheckInListDialog = () => {
  showCheckInListDialog.value = true
  fetchCheckInList()
}

// 获取活动评价列表
const fetchReviews = async () => {
  try {
    const res: any = await getActivityReviews(eventId)
    if (res && Array.isArray(res)) {
      reviews.value = res
    } else if (res && Array.isArray(res.records)) {
      reviews.value = res.records
    } else {
      reviews.value = []
    }
  } catch (error) {
    console.error('获取评价列表失败:', error)
    reviews.value = []
  }
}

// 获取评分统计
const fetchReviewStatistics = async () => {
  try {
    const res: any = await getReviewStatistics(eventId)
    if (res) {
      reviewStats.value = res
    }
  } catch (error) {
    console.error('获取评分统计失败:', error)
    reviewStats.value = null
  }
}

// 提交评价
const handleSubmitReview = async () => {
  if (!reviewForm.value.content.trim()) {
    ElMessage.warning('请输入评价内容')
    return
  }
  isSubmittingReview.value = true
  try {
    await createReview({
      activityId: eventId,
      rating: reviewForm.value.rating,
      content: reviewForm.value.content.trim()
    })
    ElMessage.success('评价提交成功')
    reviewForm.value = { rating: 5, content: '' }
    showReviewForm.value = false
    await fetchReviews()
    await fetchReviewStatistics()
  } catch (error) {
    // API错误由全局拦截器处理
    console.error('提交评价失败:', error)
  } finally {
    isSubmittingReview.value = false
  }
}

// 打开回复评价对话框
const openReplyDialog = (review: Review) => {
  currentReviewId.value = review.id
  replyContent.value = review.reply || ''
  showReplyDialog.value = true
}

// 提交评价回复
const handleReplyReview = async () => {
  if (!currentReviewId.value) return
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  isSubmittingReply.value = true
  try {
    await replyReview(currentReviewId.value, replyContent.value.trim())
    ElMessage.success('回复成功')
    showReplyDialog.value = false
    replyContent.value = ''
    currentReviewId.value = null
    await fetchReviews()
  } catch (error) {
    // API错误由全局拦截器处理
    console.error('回复评价失败:', error)
  } finally {
    isSubmittingReply.value = false
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
  <div v-loading="loading" class="event-detail-container">
    <div class="page-header">
      <div class="back-link" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回活动列表</span>
      </div>
    </div>
    
    <el-card v-if="event" class="detail-card">
      <template #header>
        <div class="detail-header">
          <h2>{{ event.title }}</h2>
          <div class="header-actions">
            <el-tag 
              v-for="tag in parseTags(event.tags)" 
              :key="tag"
              :type="getTagType(tag)" 
              size="large" 
              style="margin-right: 10px;"
            >
              {{ getTagLabel(tag) }}
            </el-tag>
            <el-button 
              :type="isFavorited ? 'warning' : 'default'" 
              :icon="isFavorited ? 'StarFilled' : 'Star'"
              @click="toggleFavorite"
            >
              {{ isFavorited ? '已收藏' : '收藏' }}
            </el-button>
          </div>
        </div>
      </template>
      
      <!-- 封面图片 -->
      <div v-if="event.coverImage" class="event-cover">
        <el-image :src="getImageUrl(event.coverImage)" fit="cover" class="cover-image" />
      </div>
      
      <div class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="活动时间">{{ formatDate(event.startTime) }} - {{ formatDate(event.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="活动地点">{{ event.location }}</el-descriptions-item>
          <el-descriptions-item label="浏览次数">{{ event.viewCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="收藏次数">{{ event.favoriteCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="评论次数">{{ event.commentCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="活动状态">
            <el-tag :type="activityStore.getStatusType(event.status)">
              {{ activityStore.getStatusText(event.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="报名人数" :span="2">
            <span :class="{ 'over-capacity': event.currentParticipants > event.maxParticipants }">
              {{ event.currentParticipants }} / {{ event.maxParticipants }}
              <el-tag v-if="event.currentParticipants > event.maxParticipants" type="danger" size="small" style="margin-left: 8px;">超额</el-tag>
            </span>
            <el-progress 
              :percentage="Math.min(getJoinPercentage(), 100)"
              :format="formatProgressText"
              :status="event.currentParticipants >= event.maxParticipants ? 'exception' : ''"
              style="margin-top: 10px"
            />
          </el-descriptions-item>
        </el-descriptions>
        
        <div class="description-section">
          <h3>活动详情</h3>
          <p>{{ event.description }}</p>
        </div>
        
        <!-- 报名图片展示（仅报名者和组织者可见） -->
        <div v-if="activityRole?.role === 2 && parseJoinImages(activityRole?.images).length > 0" class="join-images-section">
          <h3>我的报名资料</h3>
          <div class="join-images">
            <el-image
              v-for="(img, index) in parseJoinImages(activityRole?.images)"
              :key="index"
              :src="img"
              :preview-src-list="parseJoinImages(activityRole?.images)"
              fit="cover"
              class="join-image"
            />
          </div>
          <p v-if="activityRole?.remark" class="join-remark">
            <strong>备注：</strong>{{ activityRole.remark }}
          </p>
        </div>
        
        <div class="action-section">
          <!-- 组织者/管理者操作 -->
          <template v-if="isOrganizerOrManager">
            <!-- 编辑活动按钮（所有状态都显示） -->
            <el-button type="warning" size="large" @click="$router.push(`/create-event?id=${eventId}`)">
              编辑活动
            </el-button>
            <!-- 删除活动按钮（仅组织者） -->
            <el-button 
              v-if="isOrganizer" 
              type="danger" 
              size="large" 
              @click="handleDeleteEvent"
            >
              删除活动
            </el-button>
            <!-- 开始活动按钮 -->
            <el-button 
              v-if="event.status === 2 || event.status === 3" 
              type="primary" 
              size="large" 
              @click="handleStart"
            >
              开始活动
            </el-button>
            <!-- 添加管理 -->
            <el-button 
              v-if="isOrganizer" 
              type="info" 
              size="large" 
              @click="openOrganizersDialog"
            >
              添加管理
            </el-button>
            <!-- 管理报名人员 -->
            <el-button 
              v-if="isOrganizerOrManager" 
              type="primary" 
              size="large" 
              @click="openParticipantsDialog"
            >
              管理报名
            </el-button>
            <!-- 查看签到列表 -->
            <el-button 
              v-if="isOrganizerOrManager" 
              type="success" 
              size="large" 
              @click="openCheckInListDialog"
            >
              签到列表
            </el-button>
            <!-- 活动结束后组织者可以创建成果展示贴 -->
            <el-button 
              v-if="event.status === 5 && !hasResult" 
              type="success" 
              size="large" 
              @click="goToCreateResult"
            >
              创建成果展示
            </el-button>
          </template>
          
          <!-- 活动结束后所有人都可以查看成果展示贴 -->
          <el-button 
            v-if="event.status === 5 && hasResult" 
            type="success" 
            size="large" 
            @click="viewResultDetail"
          >
            查看成果展示
          </el-button>
          
          <!-- 普通参与者操作（活动未结束时显示） -->
          <template v-if="!isActivityEnded">
            <!-- 已登录但未绑定身份（学号/工号）时报名按钮禁用，并给出完善身份认证的引导提示 -->
            <el-button
              v-if="canJoin"
              type="primary"
              size="large"
              :disabled="userStore.isLoggedIn && !isIdentityBound"
              @click="openJoinDialog"
            >
              立即报名
            </el-button>
            <el-tag
              v-if="canJoin && userStore.isLoggedIn && !isIdentityBound"
              type="warning"
              size="large"
              class="identity-warn-tag"
            >
              请先完善身份认证
            </el-tag>
            <el-button
              v-if="canCancelJoin"
              type="danger"
              size="large"
              :loading="isJoining"
              @click="handleCancelJoin"
            >
              取消报名
            </el-button>
            <el-button 
              v-if="canCheckIn"
              type="success" 
              size="large" 
              :loading="isCheckingIn"
              @click="handleCheckIn"
            >
              签到
            </el-button>
            <!-- 活动已开始但未签到，显示提示 -->
            <el-tag 
              v-if="isActivityStarted && activityRole?.role === 2 && activityRole?.status === 1"
              type="warning" 
              size="large"
            >
              请尽快签到
            </el-tag>
            <!-- 已签到显示状态 -->
            <el-tag 
              v-if="activityRole?.status === 3"
              type="success" 
              size="large"
            >
              已签到
            </el-tag>
            <!-- 活动已开始，未报名的人显示提示 -->
            <el-tag 
              v-if="isActivityStarted && activityRole?.role !== 2"
              type="info" 
              size="large"
            >
              报名时间到，报名已截止
            </el-tag>
          </template>
        </div>
      </div>
    </el-card>
    
    <!-- 添加管理者对话框 -->
    <el-dialog
      v-model="showOrganizersDialog"
      title="添加管理者"
      width="500px"
    >
      <div class="manager-section">
        <h4>添加管理者</h4>
        <div class="add-manager-row">
          <el-input
            v-model="newOrganizerId"
            placeholder="输入用户ID"
            style="flex: 1"
          />
          <el-button type="primary" @click="handleAddOrganizer">添加</el-button>
        </div>
      </div>
      
      <div class="manager-section">
        <h4>当前管理者</h4>
        <el-table :data="organizers" v-if="organizers.length > 0">
          <el-table-column label="昵称">
            <template #default="{ row }">
              {{ row.realName || row.username || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="username" label="用户名" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button 
                type="danger" 
                size="small" 
                @click="handleRemoveOrganizer(row.userId, row.realName || row.username)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无管理者" />
      </div>
    </el-dialog>

    <!-- 报名人员管理对话框 -->
    <el-dialog
      v-model="showParticipantsDialog"
      title="管理报名人员"
      width="900px"
    >
      <el-table :data="participants" v-if="participants.length > 0" style="width: 100%">
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column prop="realName" label="昵称" width="120">
          <template #default="{ row }">
            {{ row.realName || row.username || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="studentId" label="学号" width="120" />
        <el-table-column prop="remark" label="报名备注" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : row.status === 0 ? 'warning' : row.status === 3 ? 'primary' : 'danger'">
              {{ row.status === 1 ? '已通过' : row.status === 0 ? '待审核' : row.status === 3 ? '已签到' : '已拒绝' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="报名时间">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <!-- 待审核状态显示审核按钮 -->
            <template v-if="row.status === 0">
              <el-button 
                type="success" 
                size="small" 
                @click="handleReviewParticipant(row.id, 1, '通过')"
              >
                通过
              </el-button>
              <el-button 
                type="danger" 
                size="small" 
                @click="handleReviewParticipant(row.id, 2, '拒绝')"
              >
                拒绝
              </el-button>
            </template>
            <el-button 
              v-if="row.status !== 3"
              type="danger" 
              size="small" 
              @click="handleRemoveParticipant(row.id, row.realName || row.username || '未知用户')"
            >
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无报名人员" />
    </el-dialog>

    <!-- 签到列表对话框 -->
    <el-dialog
      v-model="showCheckInListDialog"
      title="活动签到列表"
      width="600px"
    >
      <el-table :data="checkInList" v-if="checkInList.length > 0" style="width: 100%">
        <el-table-column label="昵称" width="120">
          <template #default="{ row }">
            {{ row.realName || row.nickname || row.username || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="checkInTime" label="签到时间">
          <template #default="{ row }">
            {{ formatDate(row.checkInTime) }}
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无签到记录" />
    </el-dialog>

    <!-- 报名对话框 -->
    <el-dialog
      v-model="showJoinDialog"
      title="报名活动"
      width="600px"
    >
      <el-form :model="joinForm" label-width="80px">
        <el-form-item label="报名备注">
          <el-input
            v-model="joinForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入报名备注（可选）"
          />
        </el-form-item>
        <el-form-item label="报名图片">
          <div class="image-upload-section">
            <div v-for="(file, index) in joinForm.images" :key="index" class="image-item">
              <el-image :src="createObjectUrl(file)" style="width: 100px; height: 100px;" fit="cover" />
              <el-button type="danger" size="small" @click="removeJoinImage(index)">删除</el-button>
            </div>
            <el-upload
              v-if="joinForm.images.length < 3"
              class="image-uploader"
              :show-file-list="false"
              :before-upload="handleJoinImageSelect"
              accept="image/*"
            >
              <el-button type="primary">
                <el-icon><Plus /></el-icon>
                添加图片
              </el-button>
              <template #tip>
                <div class="upload-tip">最多3张，单张不超过5MB</div>
              </template>
            </el-upload>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showJoinDialog = false">取消</el-button>
        <el-button type="primary" :loading="isJoining" @click="handleJoin">确认报名</el-button>
      </template>
    </el-dialog>

    <!-- 创建成果展示贴对话框 -->
    <el-dialog
      v-model="showResultDialog"
      title="创建成果展示贴"
      width="700px"
    >
      <el-form :model="resultForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="resultForm.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="resultForm.content"
            type="textarea"
            :rows="8"
            placeholder="支持 Markdown 格式"
          />
        </el-form-item>
        <el-form-item label="图片">
          <div class="image-upload-section">
            <div v-for="(img, index) in resultForm.images" :key="index" class="image-item">
              <el-image :src="img" style="width: 100px; height: 100px;" fit="cover" />
              <el-button type="danger" size="small" @click="removeImage(index)">删除</el-button>
            </div>
            <el-upload
              v-if="resultForm.images.length < 9"
              class="image-uploader"
              :show-file-list="false"
              :before-upload="handleImageSelect"
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
        <el-button @click="showResultDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateResult">创建</el-button>
      </template>
    </el-dialog>

    <!-- 活动评价（仅已结束活动显示） -->
    <el-card v-if="isActivityEnded" class="review-card">
      <template #header>
        <div class="review-header">
          <h3>活动评价 ({{ reviewStats?.totalCount || reviews.length || 0 }})</h3>
          <el-button
            v-if="isActivityEnded && activityRole?.role === 2"
            type="primary"
            size="small"
            @click="showReviewForm = true"
          >
            写评价
          </el-button>
        </div>
      </template>

      <!-- 评分统计 -->
      <div v-if="reviewStats && reviewStats.totalCount > 0" class="review-stats">
        <div class="avg-rating">
          <span class="avg-score">{{ reviewStats.avgRating?.toFixed(1) || '0.0' }}</span>
          <el-rate :model-value="Math.round(reviewStats.avgRating || 0)" disabled />
          <span class="total-count">共 {{ reviewStats.totalCount }} 条评价</span>
        </div>
      </div>

      <!-- 评价列表 -->
      <div class="review-list">
        <div v-for="review in reviews" :key="review.id" class="review-item">
          <div class="review-user-info">
            <el-avatar :size="32" :src="getAvatarUrl(review.user?.avatar)">
              {{ review.user?.nickname?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="review-meta">
              <span class="review-user">{{ review.user?.nickname || '匿名用户' }}</span>
              <el-rate :model-value="review.rating" disabled size="small" />
            </div>
            <span class="review-time">{{ formatDate(review.createTime) }}</span>
          </div>
          <p class="review-content">{{ review.content }}</p>
          <!-- 组织者回复 -->
          <div v-if="review.reply" class="review-reply">
            <span class="reply-label">组织者回复：</span>
            <span class="reply-content">{{ review.reply }}</span>
          </div>
          <!-- 组织者回复按钮 -->
          <div v-if="isOrganizerOrManager && !review.reply" class="review-actions">
            <el-button link type="primary" size="small" @click="openReplyDialog(review)">
              回复评价
            </el-button>
          </div>
          <div v-else-if="isOrganizerOrManager && review.reply" class="review-actions">
            <el-button link type="primary" size="small" @click="openReplyDialog(review)">
              修改回复
            </el-button>
          </div>
        </div>
        <el-empty v-if="reviews.length === 0" description="暂无评价" />
      </div>

      <!-- 提交评价对话框 -->
      <el-dialog v-model="showReviewForm" title="评价活动" width="500px">
        <el-form :model="reviewForm" label-width="80px">
          <el-form-item label="评分">
            <el-rate v-model="reviewForm.rating" show-score />
          </el-form-item>
          <el-form-item label="评价内容">
            <el-input
              v-model="reviewForm.content"
              type="textarea"
              :rows="4"
              placeholder="分享你的活动体验..."
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showReviewForm = false">取消</el-button>
          <el-button type="primary" :loading="isSubmittingReview" @click="handleSubmitReview">提交评价</el-button>
        </template>
      </el-dialog>

      <!-- 回复评价对话框 -->
      <el-dialog v-model="showReplyDialog" title="回复评价" width="500px">
        <el-form label-width="80px">
          <el-form-item label="回复内容">
            <el-input
              v-model="replyContent"
              type="textarea"
              :rows="4"
              placeholder="请输入回复内容..."
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showReplyDialog = false">取消</el-button>
          <el-button type="primary" :loading="isSubmittingReply" @click="handleReplyReview">提交回复</el-button>
        </template>
      </el-dialog>
    </el-card>

    <el-card class="message-card">
      <template #header>
        <h3>活动留言 ({{ totalCommentCount }})</h3>
      </template>
      
      <div class="message-list">
        <div v-for="comment in comments" :key="comment.id" class="message-item">
          <div class="message-header">
            <div class="message-user-info">
              <el-avatar :size="28" :src="getAvatarUrl(comment.user?.avatar)">
                {{ comment.user?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="message-user">{{ comment.user?.nickname || '匿名用户' }}</span>
              <span class="message-time">{{ formatDate(comment.createTime) }}</span>
            </div>
            <div class="message-actions">
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
          <p class="message-content">{{ comment.content }}</p>
          
          <!-- 回复列表 -->
          <div v-if="comment.children && comment.children.length > 0" class="reply-list">
            <div v-for="reply in comment.children" :key="reply.id" class="reply-item">
              <div class="reply-header">
                <el-avatar :size="20" :src="getAvatarUrl(reply.user?.avatar)">
                  {{ reply.user?.nickname?.charAt(0) || 'U' }}
                </el-avatar>
                <span class="reply-user">{{ reply.user?.nickname }}</span>
                <span v-if="reply.replyToUser" class="reply-to"> 回复 {{ reply.replyToUser.nickname }}</span>
                <span class="reply-time">{{ formatDate(reply.createTime) }}</span>
                <div class="reply-actions">
                  <!-- 删除自己的回复 -->
                  <el-button 
                    v-if="reply.user?.id === userStore.userInfo?.id"
                    link 
                    type="danger" 
                    size="small"
                    @click="handleDeleteComment(reply.id)"
                  >
                    删除
                  </el-button>
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
        
        <el-empty v-if="comments.length === 0" description="暂无留言" />
      </div>
      
      <div class="message-input">
        <el-input
          v-model="newMessage"
          type="textarea"
          :rows="3"
          placeholder="发表你的留言..."
        />
        <el-button type="primary" @click="submitMessage" style="margin-top: 10px">
          发送留言
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.event-detail-container {
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
  color: var(--primary-color);
  background-color: var(--primary-bg);
}

.detail-card,
.message-card {
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
}

.detail-header h2 {
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
}

.reply-list {
  margin-top: 12px;
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
}

.reply-item {
  margin: 8px 0;
  font-size: 14px;
  padding: 8px;
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
  color: var(--primary-color);
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
  padding-left: 28px;
  line-height: 1.5;
}

.reply-input-area {
  margin-top: 12px;
  padding: 12px;
  background: var(--primary-bg);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.message-user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.event-cover {
  width: 100%;
  height: 300px;
  margin-bottom: 24px;
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-content {
  padding: 20px 0;
}

.description-section {
  margin-top: 30px;
}

.description-section h3 {
  margin-bottom: 15px;
  color: var(--primary-color);
}

.action-section {
  margin-top: 30px;
  text-align: center;
}

/* 未绑定身份的报名引导提示：与操作按钮垂直居中对齐 */
.identity-warn-tag {
  vertical-align: middle;
  margin-left: 8px;
}

.message-card {
  margin-top: 20px;
}

.review-card {
  margin-top: 20px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
  background: var(--bg-primary);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-header h3 {
  margin: 0;
}

.review-stats {
  padding: 16px;
  background: var(--primary-bg);
  border-radius: var(--radius-lg);
  margin-bottom: 20px;
}

.avg-rating {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avg-score {
  font-size: 36px;
  font-weight: 700;
  color: var(--primary-color);
}

.total-count {
  color: var(--text-secondary);
  font-size: 14px;
}

.review-list {
  margin-bottom: 20px;
}

.review-item {
  padding: 16px;
  border-bottom: 1px solid var(--border-color-light);
}

.review-user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.review-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.review-user {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 14px;
}

.review-time {
  color: var(--text-muted);
  font-size: 12px;
  margin-left: auto;
}

.review-content {
  color: var(--text-regular);
  margin: 0;
  line-height: 1.6;
  padding-left: 42px;
}

.review-reply {
  margin-top: 12px;
  margin-left: 42px;
  padding: 10px 14px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--primary-color);
}

.review-actions {
  margin-top: 8px;
  margin-left: 42px;
}

.reply-label {
  color: var(--primary-color);
  font-weight: 600;
  font-size: 13px;
}

.reply-content {
  color: var(--text-regular);
  font-size: 13px;
}

.message-list {
  margin-bottom: 20px;
}

.message-item {
  padding: 15px;
  border-bottom: 1px solid var(--border-color-light);
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.message-user {
  font-weight: bold;
  color: var(--primary-color);
}

.message-time {
  color: var(--text-muted);
  font-size: 12px;
}

.message-content {
  color: var(--text-secondary);
  margin: 0;
}

.message-input {
  margin-top: 20px;
}

.manager-section {
  margin-bottom: 20px;
}

.manager-section h4 {
  margin: 0 0 12px 0;
  color: var(--text-primary);
  font-size: 14px;
}

.add-manager-row {
  display: flex;
  gap: 10px;
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

.message-actions,
.reply-actions {
  display: flex;
  align-items: center;
  gap: 8px;
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
  border-color: var(--primary-color);
}

.upload-tip {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 8px;
}

.join-images-section {
  margin-top: 30px;
  padding: 20px;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
}

.join-images-section h3 {
  margin: 0 0 16px 0;
  color: var(--text-primary);
  font-size: 16px;
}

.join-images {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.join-image {
  width: 120px;
  height: 120px;
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.join-remark {
  margin-top: 16px;
  padding: 12px;
  background: var(--bg-primary);
  border-radius: var(--radius-sm);
  color: var(--text-regular);
  line-height: 1.6;
}

/* 超额报名样式 */
.over-capacity {
  color: var(--color-danger);
  font-weight: 600;
}

/* 删除按钮红色文字 */
.message-actions .el-button--danger.is-link,
.reply-actions .el-button--danger.is-link {
  color: var(--color-danger) !important;
}

.message-actions .el-button--danger.is-link:hover,
.reply-actions .el-button--danger.is-link:hover {
  color: var(--el-color-danger-light-3) !important;
}
</style>
