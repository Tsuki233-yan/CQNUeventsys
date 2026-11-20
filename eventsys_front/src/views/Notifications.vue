<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores'
import { 
  getNotificationList, 
  getUnreadCount, 
  markAsRead, 
  markAllAsRead, 
  deleteNotification,
  type Notification 
} from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

// 判断是否为管理员
const isAdmin = computed(() => userStore.userInfo?.role === 1)

const loading = ref(false)
const notifications = ref<Notification[]>([])
const unreadCount = ref(0)
const activeTab = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 通知类型映射（非公告）
const typeMap: Record<number, { label: string; type: string }> = {
  1: { label: '系统公告', type: 'danger' },
  2: { label: '活动提醒', type: 'warning' },
  3: { label: '审核结果', type: 'success' },
  4: { label: '状态变更', type: 'info' },
  5: { label: '评论回复', type: 'primary' }
}

const formatTime = (time: string) => {
  if (!time) return ''
  // 确保时间字符串以 Z 结尾表示 UTC
  const utcString = time.endsWith('Z') ? time : time + 'Z'
  const date = new Date(utcString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}/${month}/${day} ${hours}:${minutes}:${seconds}`
}

const getNoticeTag = (n: Notification) => {
  if (n.type === 1) {
    const c = n.announceCategory
    if (c === 'ACTIVITY') return { label: '活动通知', type: 'warning' as const }
    if (c === 'URGENT') return { label: '紧急通知', type: 'danger' as const }
    return { label: '系统公告', type: 'danger' as const }
  }
  return typeMap[n.type] || { label: '通知', type: 'info' as const }
}

const displayTime = (n: Notification) => {
  if (n.type === 1 && n.publishTime) {
    return formatTime(n.publishTime)
  }
  return formatTime(n.createTime)
}

// 过滤后的通知列表
const filteredNotifications = computed(() => {
  if (activeTab.value === 'unread') {
    return notifications.value.filter(n => !n.isRead)
  }
  return notifications.value
})

// 获取通知列表
const fetchNotifications = async () => {
  loading.value = true
  try {
    const res: any = await getNotificationList({
      page: currentPage.value,
      size: pageSize.value
    })
    // 后端返回分页格式 { records: [], total: number, ... }
    if (res && Array.isArray(res.records)) {
      notifications.value = res.records
      total.value = res.total || 0
    } else if (res && Array.isArray(res)) {
      // 兼容直接返回数组的情况
      notifications.value = res
      total.value = res.length
    }
  } catch (error) {
    console.error('获取通知列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 处理页码变化
const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchNotifications()
}

// 处理每页条数变化
const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  fetchNotifications()
}

// 获取未读数量
const fetchUnreadCount = async () => {
  try {
    const res: any = await getUnreadCount()
    // 响应拦截器已解包，直接返回 { count: 5 }
    unreadCount.value = res?.count || 0
  } catch (error) {
    console.error('获取未读数量失败:', error)
  }
}

// 标记单条已读
const handleMarkAsRead = async (notification: Notification) => {
  if (notification.isRead) return
  try {
    await markAsRead(notification.id)
    notification.isRead = true
    // 重新从API获取未读数量，确保角标准确
    await fetchUnreadCount()
    // 通知 SideLayout 刷新角标
    window.dispatchEvent(new CustomEvent('refresh-unread-count'))
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

// 标记全部已读
const handleMarkAllAsRead = async () => {
  try {
    await ElMessageBox.confirm('确定要标记所有通知为已读吗？', '确认', { type: 'info' })
    await markAllAsRead()
    notifications.value.forEach(n => n.isRead = true)
    // 重新从API获取未读数量，确保角标准确
    await fetchUnreadCount()
    // 通知 SideLayout 刷新角标
    window.dispatchEvent(new CustomEvent('refresh-unread-count'))
    ElMessage.success('已全部标记为已读')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('标记全部已读失败:', error)
    }
  }
}

// 删除通知
const handleDelete = async (notification: Notification) => {
  try {
    await ElMessageBox.confirm('确定要删除这条通知吗？', '确认删除', { type: 'warning' })
    await deleteNotification(notification.id)
    notifications.value = notifications.value.filter(n => n.id !== notification.id)
    // 重新从API获取未读数量，确保角标准确
    if (!notification.isRead) {
      await fetchUnreadCount()
      // 通知 SideLayout 刷新角标
      window.dispatchEvent(new CustomEvent('refresh-unread-count'))
    }
    ElMessage.success('删除成功')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除通知失败:', error)
    }
  }
}

// 点击通知
const handleClick = (notification: Notification) => {
  // 标记已读
  handleMarkAsRead(notification)
  
  // 根据相关类型跳转
  if (notification.relatedType === 'activity' && notification.relatedId) {
    router.push(`/event/${notification.relatedId}`)
  } else if (notification.relatedType === 'comment' && notification.relatedId) {
    // 可以跳转到评论所在的活动
  }
}

// 定时器
let refreshTimer: number | null = null
const POLLING_INTERVAL = 30000 // 30秒

// 启动定时刷新
const startRefreshTimer = () => {
  if (refreshTimer) return
  refreshTimer = window.setInterval(() => {
    fetchNotifications()
    fetchUnreadCount()
  }, POLLING_INTERVAL)
}

// 停止定时刷新
const stopRefreshTimer = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  fetchNotifications()
  fetchUnreadCount()
  startRefreshTimer()
})

onUnmounted(() => {
  stopRefreshTimer()
})
</script>

<template>
  <div class="notifications-container">
    <div class="page-header">
      <div class="header-title">
        <el-icon :size="28" color="var(--primary-color)"><Bell /></el-icon>
        <h2>消息通知</h2>
      </div>
      <div class="header-actions">
        <el-button v-if="unreadCount > 0" type="primary" text @click="handleMarkAllAsRead">
          全部标记为已读
        </el-button>
        <el-button v-if="isAdmin" @click="router.push('/notifications/manage-announcements')">
          公告管理
        </el-button>
        <el-button v-if="isAdmin" type="danger" @click="router.push('/notifications/send-announcement')">
          <el-icon><Plus /></el-icon>
          发送公告
        </el-button>
      </div>
    </div>

    <el-card class="notifications-card" shadow="never">
      <el-tabs v-model="activeTab" class="notification-tabs">
        <el-tab-pane label="全部通知" name="all" />
        <el-tab-pane label="未读通知" name="unread" />
      </el-tabs>

      <!-- 首屏加载用骨架屏；翻页加载时保留 v-loading 遮罩 -->
      <div v-loading="loading && notifications.length > 0" class="notification-list">
        <template v-if="loading && notifications.length === 0">
          <div
            v-for="i in 5"
            :key="'skeleton-' + i"
            class="notification-item skeleton-item"
          >
            <el-skeleton animated>
              <template #template>
                <div class="skeleton-row">
                  <el-skeleton-item variant="button" class="skeleton-tag" />
                  <div class="skeleton-content">
                    <el-skeleton-item variant="h3" style="width: 40%" />
                    <el-skeleton-item variant="text" style="width: 100%; margin-top: 10px" />
                  </div>
                  <el-skeleton-item variant="text" class="skeleton-time" />
                </div>
              </template>
            </el-skeleton>
          </div>
        </template>

        <div
          v-for="notification in filteredNotifications"
          :key="notification.id"
          class="notification-item"
          :class="{ unread: !notification.isRead }"
          @click="handleClick(notification)"
        >
          <div class="notification-icon">
            <el-tag :type="getNoticeTag(notification).type" size="small">
              {{ getNoticeTag(notification).label }}
            </el-tag>
          </div>
          
          <div class="notification-content">
            <div class="notification-header">
              <h4 class="notification-title">{{ notification.title }}</h4>
              <span class="notification-time">{{ displayTime(notification) }}</span>
            </div>
            <p class="notification-text">{{ notification.content }}</p>
          </div>

          <div class="notification-actions" @click.stop>
            <el-button 
              v-if="!notification.isRead" 
              type="primary" 
              link 
              size="small"
              @click="handleMarkAsRead(notification)"
            >
              标记已读
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(notification)">
              删除
            </el-button>
          </div>
        </div>

        <el-empty v-if="filteredNotifications.length === 0" description="暂无通知" />
      </div>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.notifications-container {
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

.unread-badge {
  margin-top: -20px;
  margin-left: -8px;
}

.notifications-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
  background: var(--bg-primary);
}

.notification-tabs :deep(.el-tabs__item.is-active) {
  color: var(--primary-color);
}

.notification-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--primary-color);
}

.notification-list {
  min-height: 400px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  border-bottom: 1px solid var(--border-color-light);
  cursor: pointer;
  transition: background-color 0.2s;
}

.notification-item:hover {
  background-color: var(--bg-secondary);
}

.notification-item.unread {
  background-color: var(--primary-bg);
  border-left: 3px solid var(--primary-color);
}

/* 骨架屏占位条目 */
.skeleton-item {
  cursor: default;
}

.skeleton-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  width: 100%;
  padding: 4px 0;
}

.skeleton-tag {
  width: 64px;
  height: 22px;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.skeleton-content {
  flex: 1;
  min-width: 0;
}

.skeleton-time {
  width: 120px;
  flex-shrink: 0;
  margin-top: 2px;
}

.notification-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.notification-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.notification-time {
  font-size: 12px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.notification-text {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notification-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.notification-item:hover .notification-actions {
  opacity: 1;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 20px;
  border-top: 1px solid var(--border-color-light);
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}
</style>
