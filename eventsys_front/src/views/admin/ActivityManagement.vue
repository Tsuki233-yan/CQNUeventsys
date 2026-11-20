<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useActivityStore } from '@/stores'
import { 
  getAllActivities, 
  getPendingActivities, 
  takeDownActivity,
  deleteActivity,
  refreshActivityStatus,
  type ActivityListParams 
} from '@/api/admin'
import type { Activity } from '@/api/activity'

const activityStore = useActivityStore()
const router = useRouter()

const loading = ref(false)
const activityList = ref<Activity[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const statusFilter = ref<number | ''>('')
const activeTab = ref('all')

// 使用 activityStore 中的状态映射
const getStatusLabel = activityStore.getStatusText
const getStatusType = activityStore.getStatusType

// 状态筛选选项（用于下拉框）
const statusOptions = computed(() => [
  { value: -2, label: activityStore.getStatusText(-2) },
  { value: -1, label: activityStore.getStatusText(-1) },
  { value: 0, label: activityStore.getStatusText(0) },
  { value: 1, label: activityStore.getStatusText(1) },
  { value: 2, label: activityStore.getStatusText(2) },
  { value: 3, label: activityStore.getStatusText(3) },
  { value: 4, label: activityStore.getStatusText(4) },
  { value: 5, label: activityStore.getStatusText(5) }
])

// 获取活动列表
const fetchActivityList = async () => {
  loading.value = true
  try {
    let res: any
    if (activeTab.value === 'pending') {
      res = await getPendingActivities({
        page: currentPage.value,
        size: pageSize.value
      })
    } else {
      const params: ActivityListParams = {
        page: currentPage.value,
        size: pageSize.value,
        keyword: searchKeyword.value || undefined,
        status: statusFilter.value !== '' ? statusFilter.value : undefined
      }
      res = await getAllActivities(params)
    }
    // 后端返回分页格式 { records: [], total: number, ... }
    if (res && Array.isArray(res.records)) {
      activityList.value = res.records
      total.value = res.total || 0
    } else if (Array.isArray(res)) {
      // 兼容直接返回数组的情况
      activityList.value = res
      total.value = res.length
    } else {
      activityList.value = res?.list || []
      total.value = res?.total || 0
    }
  } catch (error) {
    console.error('获取活动列表失败:', error)
    activityList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchActivityList()
}

// 切换标签
const handleTabChange = () => {
  currentPage.value = 1
  statusFilter.value = ''
  searchKeyword.value = ''
  fetchActivityList()
}

// 分页
const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchActivityList()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  fetchActivityList()
}

// 跳转到页面式审核
const handleReview = (activity: Activity) => {
  router.push(`/admin/activities/review/${activity.id}`)
}

// 强制下架
const handleTakeDown = async (activity: Activity) => {
  try {
    const promptRes: any = await ElMessageBox.prompt(
      `请输入下架活动 "${activity.title}" 的原因：`,
      '强制下架',
      {
        confirmButtonText: '确认下架',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入下架原因（可选）'
      }
    )
    const reason = promptRes?.value as string | undefined
    await takeDownActivity(activity.id, reason)
    ElMessage.success('强制下架成功')
    fetchActivityList()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('强制下架失败')
    }
  }
}

// 查看详情
const viewDetail = (activity: Activity) => {
  window.open(`/event/${activity.id}`, '_blank')
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

// 删除活动
const handleDelete = async (activity: Activity) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除活动 "${activity.title}" 吗？删除后不可恢复！`,
      '确认删除',
      { type: 'error', confirmButtonText: '删除' }
    )
    await deleteActivity(activity.id)
    ElMessage.success('删除成功')
    fetchActivityList()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 批量刷新活动状态
const handleBatchRefresh = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要批量刷新所有活动状态吗？这将根据时间自动更新活动状态。',
      '确认刷新',
      { type: 'warning' }
    )
    await refreshActivityStatus()
    ElMessage.success('刷新成功')
    fetchActivityList()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('刷新失败')
    }
  }
}

// 定时器
let pollingTimer: ReturnType<typeof setInterval> | null = null
const POLLING_INTERVAL = 30000 // 30秒

// 启动定时刷新
const startPolling = () => {
  stopPolling()
  fetchActivityList()
  pollingTimer = setInterval(() => {
    fetchActivityList()
  }, POLLING_INTERVAL)
}

// 停止定时刷新
const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

onMounted(() => {
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<template>
  <div class="activity-management">
    <div class="page-header">
      <div class="header-title">
        <el-icon :size="28" color="var(--color-primary)"><Management /></el-icon>
        <h2>活动管理</h2>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="handleBatchRefresh">
          <el-icon><Refresh /></el-icon>
          批量刷新状态
        </el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="management-tabs">
      <el-tab-pane label="所有活动" name="all" />
      <el-tab-pane label="待审核" name="pending" />
    </el-tabs>

    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" class="search-form">
        <el-form-item label="关键词">
          <el-input
            v-model="searchKeyword"
            placeholder="活动标题"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态" v-if="activeTab === 'all'">
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px">
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
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

    <!-- 活动列表 -->
    <el-card class="list-card" shadow="never">
      <el-table :data="activityList" v-loading="loading" stripe>
        <el-table-column type="index" width="60" label="序号" align="center" />
        <el-table-column label="活动标题" min-width="200">
          <template #default="{ row }">
            <div class="activity-title" @click="viewDetail(row)">
              {{ row.title }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="组织者" width="120" align="center">
          <template #default="{ row }">
            {{ row.organizer?.nickname || row.organizer?.username || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报名时间" width="220">
          <template #default="{ row }">
            <div class="time-info">
              <div>{{ formatDate(row.registrationStartTime) }}</div>
              <div class="time-separator">至</div>
              <div>{{ formatDate(row.registrationEndTime) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="活动时间" width="220">
          <template #default="{ row }">
            <div class="time-info">
              <div>{{ formatDate(row.startTime) }}</div>
              <div class="time-separator">至</div>
              <div>{{ formatDate(row.endTime) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="统计" width="150" align="center">
          <template #default="{ row }">
            <div class="stats-cell">
              <el-tooltip content="浏览次数" placement="top">
                <span class="stat-item"><el-icon><View /></el-icon>{{ row.viewCount || 0 }}</span>
              </el-tooltip>
              <el-tooltip content="收藏次数" placement="top">
                <span class="stat-item"><el-icon><Star /></el-icon>{{ row.favoriteCount || 0 }}</span>
              </el-tooltip>
              <el-tooltip content="评论次数" placement="top">
                <span class="stat-item"><el-icon><ChatDotRound /></el-icon>{{ row.commentCount || 0 }}</span>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <!-- 待审核状态 -->
            <template v-if="row.status === 1">
              <el-button type="primary" size="small" @click="handleReview(row)">
                审核
              </el-button>
            </template>
            <!-- 已上架状态 -->
            <template v-else-if="row.status >= 2 && row.status <= 4">
              <el-button type="warning" size="small" @click="handleTakeDown(row)">
                下架
              </el-button>
            </template>
            <el-button type="primary" size="small" @click="viewDetail(row)">
              查看
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
        <!-- 空数据状态 -->
        <template #empty>
          <el-empty description="暂无活动数据" :image-size="80" />
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

  </div>
</template>

<style scoped>
.activity-management {
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

.header-actions {
  display: flex;
  gap: 12px;
}

.management-tabs {
  margin-bottom: 20px;
}

.management-tabs :deep(.el-tabs__item.is-active) {
  color: var(--color-primary);
}

.management-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--color-primary);
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

.activity-title {
  color: var(--text-primary);
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s;
}

.activity-title:hover {
  color: var(--color-primary);
}

.time-info {
  font-size: 12px;
  color: var(--text-secondary);
}

.time-separator {
  color: var(--border-color);
  font-size: 10px;
}

.stats-cell {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

.stat-item .el-icon {
  color: var(--color-primary);
  font-size: 14px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
