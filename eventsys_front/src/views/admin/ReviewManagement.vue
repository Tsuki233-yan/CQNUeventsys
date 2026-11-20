<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getAllActivities } from '@/api/admin'
import { getActivityReviews, getReviewStatistics, deleteReview, type Review, type ReviewStatistics } from '@/api/review'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Activity } from '@/api/activity'

const loading = ref(false)
const activityLoading = ref(false)
const reviews = ref<Review[]>([])
const reviewStats = ref<ReviewStatistics | null>(null)
const activityList = ref<Activity[]>([])
const selectedActivityId = ref<number | null>(null)
const selectedActivityTitle = ref('')
const searchKeyword = ref('')

// 搜索过滤后的活动列表
const filteredActivityList = computed(() => {
  if (!searchKeyword.value.trim()) return activityList.value
  const keyword = searchKeyword.value.trim().toLowerCase()
  return activityList.value.filter(a =>
    a.title?.toLowerCase().includes(keyword) ||
    a.location?.toLowerCase().includes(keyword)
  )
})

// 获取头像URL
const getAvatarUrl = (url: string | undefined): string => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('/api/')) return url
  return '/api' + url
}

// 统一格式化为 yyyy/mm/dd hh:mm:ss（处理UTC时间）
const formatDate = (date: string) => {
  if (!date) return ''
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

// 获取活动列表（仅显示已结束的活动，只有已结束活动才能有评价）
const fetchActivityList = async () => {
  activityLoading.value = true
  try {
    const res: any = await getAllActivities({ page: 1, size: 1000, status: 5 })
    if (res && Array.isArray(res.records)) {
      activityList.value = res.records
    } else if (Array.isArray(res)) {
      activityList.value = res
    } else {
      activityList.value = res?.list || []
    }
  } catch (error) {
    console.error('获取活动列表失败:', error)
    activityList.value = []
  } finally {
    activityLoading.value = false
  }
}

// 选择活动
const selectActivity = (activity: Activity) => {
  selectedActivityId.value = activity.id
  selectedActivityTitle.value = activity.title
  fetchReviews()
}

// 返回活动列表
const backToList = () => {
  selectedActivityId.value = null
  selectedActivityTitle.value = ''
  reviews.value = []
  reviewStats.value = null
}

// 获取活动评价
const fetchReviews = async () => {
  if (!selectedActivityId.value) {
    reviews.value = []
    reviewStats.value = null
    return
  }
  loading.value = true
  try {
    // 响应拦截器已解包 response.data，这里以 any 接收实际业务数据
    const [reviewsRes, statsRes] = await Promise.all([
      getActivityReviews(selectedActivityId.value),
      getReviewStatistics(selectedActivityId.value)
    ]) as [any, any]
    if (reviewsRes && Array.isArray(reviewsRes)) {
      reviews.value = reviewsRes
    } else if (reviewsRes && Array.isArray(reviewsRes.records)) {
      reviews.value = reviewsRes.records
    } else {
      reviews.value = []
    }
    if (statsRes) {
      reviewStats.value = statsRes
    }
  } catch (error) {
    console.error('获取评价列表失败:', error)
    reviews.value = []
    reviewStats.value = null
  } finally {
    loading.value = false
  }
}

// 删除评价
const handleDeleteReview = async (review: Review) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除这条评价吗？此操作不可恢复。`,
      '确认删除',
      { type: 'warning', confirmButtonText: '删除' }
    )
    await deleteReview(review.id)
    ElMessage.success('删除成功')
    await fetchReviews()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除评价失败:', error)
    }
  }
}

onMounted(() => {
  fetchActivityList()
})
</script>

<template>
  <div class="review-management">
    <div class="page-header">
      <div class="header-title">
        <el-icon :size="28" color="var(--color-primary)"><ChatDotRound /></el-icon>
        <h2>评价管理</h2>
      </div>
    </div>

    <!-- 活动列表视图 -->
    <template v-if="!selectedActivityId">
      <el-card shadow="never" class="filter-card">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索活动名称或地点"
          clearable
          size="large"
          style="width: 100%"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </el-card>

      <el-card v-loading="activityLoading" shadow="never" class="activity-list-card">
        <div v-if="filteredActivityList.length > 0" class="activity-list">
          <div
            v-for="activity in filteredActivityList"
            :key="activity.id"
            class="activity-item"
            @click="selectActivity(activity)"
          >
            <div class="activity-main">
              <h4 class="activity-title">{{ activity.title }}</h4>
              <div class="activity-info-row">
                <span class="activity-info-item">
                  <el-icon><Location /></el-icon>
                  {{ activity.location || '未设置' }}
                </span>
                <span class="activity-info-item">
                  <el-icon><Clock /></el-icon>
                  {{ formatDate(activity.endTime) }} 结束
                </span>
                <span class="activity-info-item">
                  <el-icon><User /></el-icon>
                  {{ activity.currentParticipants || 0 }} 人参与
                </span>
              </div>
            </div>
            <el-icon class="activity-arrow" :size="18" color="var(--text-muted)"><ArrowRight /></el-icon>
          </div>
        </div>
        <el-empty v-else description="暂无已结束的活动" />
      </el-card>
    </template>

    <!-- 评价详情视图 -->
    <template v-else>
      <el-card shadow="never" class="filter-card">
        <div class="review-detail-header">
          <el-button text @click="backToList">
            <el-icon><ArrowLeft /></el-icon>
            返回活动列表
          </el-button>
          <span class="current-activity-title">{{ selectedActivityTitle }}</span>
        </div>
      </el-card>

      <el-card v-loading="loading" shadow="never" class="reviews-card">
        <!-- 评分统计 -->
        <div v-if="reviewStats && reviewStats.totalCount > 0" class="review-stats">
          <div class="avg-rating">
            <span class="avg-score">{{ reviewStats.avgRating?.toFixed(1) || '0.0' }}</span>
            <el-rate :model-value="Math.round(reviewStats.avgRating || 0)" disabled />
            <span class="total-count">共 {{ reviewStats.totalCount }} 条评价</span>
          </div>
          <div v-if="reviewStats.ratingDistribution" class="rating-distribution">
            <div
              v-for="(count, rating) in reviewStats.ratingDistribution"
              :key="rating"
              class="distribution-item"
            >
              <span class="distribution-label">{{ rating }} 星</span>
              <el-progress
                :percentage="reviewStats.totalCount > 0 ? Math.round((count / reviewStats.totalCount) * 100) : 0"
                :stroke-width="16"
                :color="'var(--color-primary)'"
                :show-text="false"
                style="width: 200px"
              />
              <span class="distribution-count">{{ count }}</span>
            </div>
          </div>
        </div>

        <el-divider v-if="reviewStats && reviewStats.totalCount > 0" />

        <div v-if="reviews.length > 0" class="review-list">
          <div
            v-for="review in reviews"
            :key="review.id"
            class="review-item"
          >
            <div class="review-header-row">
              <div class="review-user-info">
                <el-avatar :size="40" :src="getAvatarUrl(review.user?.avatar)">
                  {{ review.user?.nickname?.charAt(0) || 'U' }}
                </el-avatar>
                <div class="review-meta">
                  <span class="review-user">{{ review.user?.nickname || '匿名用户' }}</span>
                  <el-rate :model-value="review.rating" disabled size="small" />
                </div>
              </div>
              <div class="review-info">
                <span class="review-time">{{ formatDate(review.createTime) }}</span>
                <el-button
                  type="danger"
                  size="small"
                  @click="handleDeleteReview(review)"
                >
                  删除
                </el-button>
              </div>
            </div>

            <p class="review-content">{{ review.content }}</p>

            <!-- 组织者回复 -->
            <div v-if="review.reply" class="review-reply">
              <span class="reply-label">组织者回复：</span>
              <span class="reply-content">{{ review.reply }}</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="该活动暂无评价" />
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.review-management {
  padding: 0;
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
}

.header-title h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.filter-card {
  margin-bottom: 20px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
}

.activity-list-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.activity-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.activity-item:hover {
  background: var(--primary-bg);
  border-color: var(--primary-bg-hover);
}

.activity-main {
  flex: 1;
  min-width: 0;
}

.activity-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-info-row {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.activity-info-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);
}

.activity-arrow {
  flex-shrink: 0;
  margin-left: 12px;
}

.review-detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.current-activity-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.reviews-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
}

.review-stats {
  padding: 20px;
  background: var(--primary-bg);
  border-radius: var(--radius-lg);
  margin-bottom: 20px;
}

.avg-rating {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.avg-score {
  font-size: 48px;
  font-weight: 700;
  color: var(--color-primary);
}

.total-count {
  color: var(--text-secondary);
  font-size: 14px;
}

.rating-distribution {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.distribution-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.distribution-label {
  width: 40px;
  text-align: right;
  color: var(--text-secondary);
  font-size: 13px;
}

.distribution-count {
  color: var(--text-muted);
  font-size: 13px;
  width: 30px;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  padding: 20px;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  transition: all 0.2s;
}

.review-item:hover {
  border-color: var(--primary-bg-hover);
  box-shadow: var(--shadow-md);
}

.review-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.review-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
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

.review-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.review-time {
  color: var(--text-muted);
  font-size: 13px;
}

.review-content {
  color: var(--text-regular);
  margin: 0 0 12px 0;
  line-height: 1.6;
  padding-left: 52px;
}

.review-reply {
  margin: 12px 0 0 52px;
  padding: 12px 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--color-primary);
}

.reply-label {
  color: var(--color-primary);
  font-weight: 600;
  font-size: 13px;
}

.reply-content {
  color: var(--text-regular);
  font-size: 13px;
}
</style>
