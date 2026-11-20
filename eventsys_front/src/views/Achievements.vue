<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getResultByActivityId, checkResultExists, type ActivityResult } from '@/api/result'
import { getActivityList, type Activity } from '@/api'

const router = useRouter()

const achievements = ref<ActivityResult[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const hasMore = ref(false)

// 获取已结束的活动列表，并检查是否有成果展示贴
const fetchAchievements = async (isLoadMore = false) => {
  if (!isLoadMore) {
    currentPage.value = 1
    achievements.value = []
  }
  loading.value = true
  
  try {
    // 1. 获取已结束的活动列表（状态=5）
    const res: any = await getActivityList({
      status: 5, // 已结束
      page: currentPage.value,
      size: pageSize.value
    })
    
    let activities: Activity[] = []
    if (res && Array.isArray(res.records)) {
      activities = res.records
      total.value = res.total || 0
    } else if (Array.isArray(res)) {
      activities = res
    }
    
    // 2. 检查每个活动是否有成果展示贴
    const results: ActivityResult[] = []
    for (const activity of activities) {
      try {
        const exists: any = await checkResultExists(activity.id)
        if (exists === true) {
          const result: any = await getResultByActivityId(activity.id)
          if (result) {
            results.push(result)
          }
        }
      } catch (error) {
        console.error(`检查活动 ${activity.id} 成果展示贴失败:`, error)
      }
    }
    
    if (isLoadMore) {
      achievements.value.push(...results)
    } else {
      achievements.value = results
    }
    
    hasMore.value = achievements.value.length < total.value
  } catch (error) {
    console.error('获取成果列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = async () => {
  if (loading.value || !hasMore.value) return
  currentPage.value++
  await fetchAchievements(true)
}

onMounted(() => {
  fetchAchievements(false)
})

const viewDetail = (activityId: number) => {
  router.push(`/achievement/${activityId}`)
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

/**
 * 获取成果展示贴第一张图片URL
 * 后端返回格式: ["/uploads/images/activity_1/xxx.jpg", ...]
 * 需要添加 /api 前缀
 */
const getFirstImage = (images: string | undefined): string => {
  if (!images) return ''
  try {
    const parsed = JSON.parse(images)
    if (Array.isArray(parsed) && parsed.length > 0) {
      const url = parsed[0]
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
    return ''
  } catch {
    return ''
  }
}

/**
 * 将 Markdown 源文本转为纯文本摘要
 * 成果贴 content 为 Markdown 正文（详情页用 MdPreview 渲染），
 * 列表卡片摘要为纯文本展示，直接截断会把 #、** 等语法符号原样暴露给用户，
 * 这里剥离常见 Markdown 语法后再截断，不引入任何新依赖
 */
const stripMarkdown = (md: string | undefined): string => {
  if (!md) return ''
  return md
    .replace(/!\[([^\]]*)\]\(([^)]*)\)/g, '$1')  // 图片 ![alt](url) → alt
    .replace(/\[([^\]]*)\]\(([^)]*)\)/g, '$1')   // 链接 [text](url) → text
    .replace(/^#{1,6}\s+/gm, '')                 // 标题井号
    .replace(/(\*\*|__)(.*?)\1/g, '$2')          // 粗体
    .replace(/(\*|_)([^*_]*?)\1/g, '$2')         // 斜体
    .replace(/^>\s?/gm, '')                      // 引用符
    .replace(/^\s*([-*+]|\d+\.)\s+/gm, '')       // 列表标记
    .replace(/`{1,3}([^`]*)`{1,3}/g, '$1')       // 行内/围栏代码标记
    .replace(/\s+/g, ' ')                        // 连续空白合并为一格，摘要保持连贯
    .trim()
}
</script>

<template>
  <div class="achievements-container">
    <div class="page-header">
      <div class="header-title">
        <el-icon :size="28" color="var(--primary-color)"><Trophy /></el-icon>
        <h2>活动成果展示</h2>
      </div>
    </div>

    <!-- 首屏加载用骨架屏；加载更多时保留 v-loading 遮罩 -->
    <el-row v-loading="loading && achievements.length > 0" :gutter="20" class="achievements-list">
      <template v-if="loading && achievements.length === 0">
        <el-col
          v-for="i in 6"
          :key="'skeleton-' + i"
          :span="8"
          class="achievement-col"
        >
          <el-card class="achievement-card" shadow="never">
            <el-skeleton animated>
              <template #template>
                <el-skeleton-item variant="h3" style="width: 55%" />
                <el-skeleton-item variant="image" class="skeleton-image" />
                <el-skeleton-item variant="text" style="margin-top: 16px; width: 100%" />
                <el-skeleton-item variant="text" style="margin-top: 8px; width: 60%" />
              </template>
            </el-skeleton>
          </el-card>
        </el-col>
      </template>

      <el-col
        v-for="item in achievements"
        :key="item.id"
        :span="8"
        class="achievement-col"
      >
        <el-card class="achievement-card" shadow="never">
          <template #header>
            <h3 class="achievement-title">{{ item.title }}</h3>
          </template>

          <div class="achievement-images">
            <el-image
              :src="getFirstImage(item.images)"
              fit="cover"
              style="width: 100%; height: 200px"
            />
          </div>

          <p class="achievement-summary">{{ stripMarkdown(item.content).substring(0, 100) }}...</p>

          <div class="achievement-footer">
            <div class="interaction-stats">
              <span>
                <el-icon><View /></el-icon> {{ item.viewCount }}
              </span>
              <span>
                <el-icon><ChatDotRound /></el-icon> {{ item.commentCount }}
              </span>
            </div>
            <span class="create-time">{{ formatDate(item.createTime) }}</span>
          </div>

          <div class="card-actions">
            <el-button type="primary" link @click="viewDetail(item.activityId)">
              查看详情
            </el-button>
          </div>
        </el-card>
      </el-col>

      <el-col v-if="achievements.length === 0 && !loading" :span="24">
        <el-empty description="暂无活动成果" />
      </el-col>
    </el-row>

    <!-- 加载更多 -->
    <div v-if="achievements.length > 0" class="load-more-container">
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
.achievements-container {
  padding: 0;
  width: 100%;
  max-width: 100%;
}

.page-header {
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

.achievements-list {
  margin-top: 20px;
}

.achievement-col {
  margin-bottom: 20px;
}

/* 骨架屏占位块 */
.skeleton-image {
  display: block;
  width: 100%;
  height: 200px;
  margin-top: 16px;
  border-radius: var(--radius-sm);
}

.achievement-card {
  height: 100%;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
  background: var(--bg-primary);
  transition: all 0.3s ease;
}

/* 卡片悬停上浮阴影 */
.achievement-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-primary-light);
}

.achievement-title {
  margin: 0;
  font-size: 18px;
  color: var(--text-primary);
}

.achievement-images {
  margin-bottom: 15px;
}

.achievement-summary {
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 15px;
}

.achievement-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid var(--border-color-light);
}

.interaction-stats {
  display: flex;
  gap: 20px;
}

.interaction-stats span {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--text-muted);
  cursor: pointer;
}

.interaction-stats span:hover {
  color: var(--primary-color);
}

.create-time {
  color: var(--text-muted);
  font-size: 12px;
}

.card-actions {
  text-align: center;
  padding-top: 10px;
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
