<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useActivityStore } from '@/stores'
import { useUserStore } from '@/stores'
import { getCategoryList, type Category } from '@/api'
import type { Activity } from '@/api'

const router = useRouter()
const route = useRoute()
const activityStore = useActivityStore()
const userStore = useUserStore()

const searchKeyword = ref('')
// 支持从其它页面带 categoryId 查询参数进入（如 /events?categoryId=1 直达分类过滤），初始化分类筛选；
// 在 setup 顶层同步赋初值，避免 mounted 后再赋值触发 watch 造成重复请求
const initialCategoryId = Number(route.query.categoryId)
const selectedTag = ref(!isNaN(initialCategoryId) && initialCategoryId > 0 ? String(initialCategoryId) : '')
const loading = ref(false)
const categories = ref<Category[]>([])

const events = ref<Activity[]>([])
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const hasMore = computed(() => events.value.length < total.value)

// 获取分类列表
const fetchCategories = async () => {
  try {
    const res: any = await getCategoryList()
    if (res && Array.isArray(res)) {
      categories.value = [{ id: 0, name: '全部' } as Category, ...res.filter((c: Category) => c.status === 1)]
    }
  } catch (error) {
    console.error('获取分类列表失败:', error)
  }
}

// 获取活动列表
const fetchEvents = async (isLoadMore = false) => {
  if (!isLoadMore) {
    currentPage.value = 1
    events.value = []
  }
  loading.value = true
  const res: any = await activityStore.fetchActivityList({
    keyword: searchKeyword.value || undefined,
    categoryId: selectedTag.value ? parseInt(selectedTag.value) : undefined,
    page: currentPage.value,
    size: pageSize.value
  })
  if (res) {
    if (isLoadMore) {
      events.value.push(...res)
    } else {
      events.value = res
    }
    total.value = activityStore.total
  }
  loading.value = false
}

// 加载更多
const loadMore = async () => {
  if (loading.value || !hasMore.value) return
  currentPage.value++
  await fetchEvents(true)
}

// 监听搜索条件变化（手动防抖）
let searchTimer: ReturnType<typeof setTimeout> | null = null
watch([searchKeyword, selectedTag], () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    fetchEvents(false)
    // 搜索条件变化时重启轮询
    activityStore.stopActivityListPolling()
    activityStore.startActivityListPolling({
      keyword: searchKeyword.value || undefined,
      categoryId: selectedTag.value ? parseInt(selectedTag.value) : undefined,
      page: currentPage.value,
      size: pageSize.value
    })
  }, 300)
})

onMounted(() => {
  fetchCategories()
  // 首次加载活动列表
  fetchEvents(false)
  // 启动定时刷新活动列表
  activityStore.startActivityListPolling({
    keyword: searchKeyword.value || undefined,
    categoryId: selectedTag.value ? parseInt(selectedTag.value) : undefined,
    page: currentPage.value,
    size: pageSize.value
  })
})

onUnmounted(() => {
  // 停止定时刷新
  activityStore.stopActivityListPolling()
})

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

/**
 * 获取活动封面图片URL
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

const viewEventDetail = (id: number) => {
  router.push(`/event/${id}`)
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

const createEvent = () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  router.push('/create-event')
}
</script>

<template>
  <div class="events-container">
    <div class="page-header">
      <div class="header-title">
        <el-icon :size="28" color="var(--primary-color)"><Calendar /></el-icon>
        <h2>活动列表</h2>
      </div>
      <el-button type="primary" size="large" @click="createEvent">
        <el-icon><Plus /></el-icon>
        创建活动
      </el-button>
    </div>

    <el-card class="filter-card" shadow="never">
      <div class="filter-section">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索活动"
          style="width: 280px"
          clearable
          size="large"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-radio-group v-model="selectedTag" size="large">
          <el-radio-button
            v-for="category in categories"
            :key="category.id"
            :label="category.id === 0 ? '' : String(category.id)"
          >
            {{ category.name }}
          </el-radio-button>
        </el-radio-group>
      </div>
    </el-card>

    <!-- 首屏加载用骨架屏；加载更多时保留 v-loading 遮罩 -->
    <el-row v-loading="loading && events.length > 0" :gutter="24" class="events-list">
      <template v-if="loading && events.length === 0">
        <el-col
          v-for="i in 6"
          :key="'skeleton-' + i"
          :xs="24"
          :sm="12"
          :lg="8"
          class="event-col"
        >
          <el-card class="event-card" shadow="never">
            <el-skeleton animated>
              <template #template>
                <el-skeleton-item variant="image" class="skeleton-cover" />
                <div class="skeleton-body">
                  <el-skeleton-item variant="h3" style="width: 60%" />
                  <div class="skeleton-line">
                    <el-skeleton-item variant="text" style="margin-right: 16px" />
                    <el-skeleton-item variant="text" style="width: 30%" />
                  </div>
                  <el-skeleton-item variant="text" style="width: 100%" />
                  <el-skeleton-item variant="text" style="width: 45%; margin-top: 8px" />
                </div>
              </template>
            </el-skeleton>
          </el-card>
        </el-col>
      </template>

      <el-col
        v-for="event in events"
        :key="event.id"
        :xs="24"
        :sm="12"
        :lg="8"
        class="event-col"
      >
        <el-card class="event-card" shadow="hover" @click="viewEventDetail(event.id)">
          <!-- 封面图片 -->
          <div class="event-cover" v-if="event.coverImage">
            <el-image
              :src="getImageUrl(event.coverImage)"
              fit="cover"
              class="cover-image"
            />
          </div>
          <div class="event-cover placeholder" v-else>
            <el-icon :size="48" color="var(--text-muted)"><Calendar /></el-icon>
          </div>

          <template #header>
            <div class="card-header">
              <span class="event-title">{{ event.title }}</span>
              <el-tag
                v-for="tag in parseTags(event.tags)"
                :key="tag"
                :type="getTagType(tag)"
                effect="light"
                round
                style="margin-right: 6px;"
              >
                {{ getTagLabel(tag) }}
              </el-tag>
            </div>
          </template>

          <p class="event-description">{{ event.description }}</p>

          <div class="event-info">
            <div class="info-item">
              <el-icon color="var(--primary-color)"><Clock /></el-icon>
              <span>{{ formatDate(event.startTime) }}</span>
            </div>
            <div class="info-item">
              <el-icon color="var(--primary-color)"><Location /></el-icon>
              <span>{{ event.location }}</span>
            </div>
          </div>

          <div class="event-stats">
            <div class="stat-item">
              <el-icon><View /></el-icon>
              <span>{{ event.viewCount || 0 }}</span>
            </div>
            <div class="stat-item">
              <el-icon><Star /></el-icon>
              <span>{{ event.favoriteCount || 0 }}</span>
            </div>
            <div class="stat-item">
              <el-icon><ChatDotRound /></el-icon>
              <span>{{ event.commentCount || 0 }}</span>
            </div>
            <div class="stat-item participants">
              <el-icon color="var(--primary-color)"><User /></el-icon>
              <span>{{ event.currentParticipants }}/{{ event.maxParticipants }}</span>
            </div>
          </div>

          <div class="event-status">
            <el-tag :type="activityStore.getStatusType(event.status)" effect="dark" round>
              {{ activityStore.getStatusText(event.status) }}
            </el-tag>
          </div>
        </el-card>
      </el-col>

      <el-col v-if="events.length === 0 && !loading" :span="24">
        <el-empty description="暂无活动" />
      </el-col>
    </el-row>

    <!-- 加载更多 -->
    <div v-if="events.length > 0" class="load-more-container">
      <el-button 
        v-if="hasMore" 
        :loading="loading" 
        @click="loadMore"
        size="large"
        type="primary"
        plain
      >
        加载更多
      </el-button>
      <span v-else class="no-more">没有更多了</span>
    </div>
  </div>
</template>

<style scoped>
.events-container {
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

.filter-card {
  margin-bottom: 24px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
  background: var(--bg-primary);
}

.filter-section {
  display: flex;
  gap: 20px;
  align-items: center;
  flex-wrap: wrap;
}

.events-list {
  margin-top: 8px;
}

.event-col {
  margin-bottom: 24px;
}

.event-card {
  cursor: pointer;
  height: 100%;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
  transition: all 0.3s ease;
  background: var(--bg-primary);
  overflow: hidden;
}

/* 卡片悬停上浮阴影 */
.event-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-primary-light);
}

/* 骨架屏占位块 */
.skeleton-cover {
  width: 100%;
  height: 160px;
  border-radius: var(--radius-sm);
}

.skeleton-body {
  padding: 16px 4px 8px;
}

.skeleton-line {
  display: flex;
  align-items: center;
  margin-top: 16px;
}

.event-cover {
  width: 100%;
  height: 160px;
  overflow: hidden;
  margin: -20px -20px 16px -20px;
  width: calc(100% + 40px);
}

.event-cover.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  /* 空间不足时标签换到下一行，保证标题有足够横向空间 */
  flex-wrap: wrap;
  gap: 6px;
}

.event-title {
  /* 占据剩余宽度，且保证最低横向空间，避免长标题被右侧标签挤压成逐字竖排 */
  flex: 1;
  min-width: 10em;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.event-description {
  color: var(--text-secondary);
  margin: 16px 0;
  min-height: 44px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.event-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 16px 0;
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-regular);
  font-size: 14px;
}

.event-status {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.event-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 16px 0;
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
}

.stat-item .el-icon {
  color: var(--primary-color);
}

.participants {
  margin-left: auto;
  padding-left: 12px;
  border-left: 1px solid var(--border-color);
}

.event-status {
  margin-top: 12px;
  display: flex;
  justify-content: flex-start;
}

.load-more-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 0;
}

.no-more {
  color: var(--text-muted);
  font-size: 14px;
}
</style>
