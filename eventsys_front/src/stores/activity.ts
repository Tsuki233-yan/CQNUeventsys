import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getActivityList,
  getActivityDetail,
  createActivity,
  updateActivity,
  deleteActivity,
  joinActivity,
  cancelJoin,
  getMyActivities,
  getActivityRole,
  checkIn,
  checkInStatus,
  startActivity,
  type Activity,
  type ActivityQuery
} from '@/api'
import { ElMessage } from 'element-plus'
import { useUserStore } from './user'

export const useActivityStore = defineStore('activity', () => {
  // State
  const activityList = ref<Activity[]>([])
  const currentActivity = ref<Activity | null>(null)
  const myActivities = ref<any[]>([])
  const activityRole = ref<any>(null)

  const loading = ref(false)
  const total = ref(0)
  
  // 定时器相关
  let activityListTimer: ReturnType<typeof setInterval> | null = null
  let activityDetailTimer: ReturnType<typeof setInterval> | null = null
  const POLLING_INTERVAL = 30000 // 30秒，与后端保持一致

  // Getters
  // activityRole.role: 1-组织者, 2-参与者, 3-管理者
  // 注意：系统管理员（user.role === 1）不等于活动组织者，需要区分系统角色和活动角色
  const isOrganizer = computed(() => {
    const userStore = useUserStore()
    // 方式1：通过 activityRole.role 字段判断（1表示组织者）
    if (activityRole.value?.role === 1) return true
    // 方式2：通过比较 organizerId 判断（后备方案）
    if (currentActivity.value?.organizerId && userStore.userInfo?.id) {
      return currentActivity.value.organizerId === userStore.userInfo.id
    }
    return false
  })
  // 活动管理者（被组织者添加的管理者，不是系统管理员）
  const isManager = computed(() => activityRole.value?.role === 3)
  // 活动参与者（已报名）
  const isParticipant = computed(() => activityRole.value?.role === 2)
  // 组织者或管理者（有管理权限）
  const isOrganizerOrManager = computed(() => isOrganizer.value || isManager.value)
  const canJoin = computed(() => {
    if (!currentActivity.value) return false
    return currentActivity.value.status === 2 && // 报名中
           currentActivity.value.currentParticipants < currentActivity.value.maxParticipants
  })

  // 活动状态映射
  const getStatusText = (status: number) => {
    const statusMap: Record<number, string> = {
      [-2]: '已下架',
      [-1]: '已拒绝',
      0: '草稿',
      1: '待审核',
      2: '报名中',
      3: '即将开始',
      4: '进行中',
      5: '已结束'
    }
    return statusMap[status] || '未知状态'
  }

  const getStatusType = (status: number) => {
    const typeMap: Record<number, string> = {
      [-2]: 'danger',
      [-1]: 'danger',
      0: 'info',
      1: 'warning',
      2: 'success',
      3: 'warning',
      4: 'primary',
      5: 'info'
    }
    return typeMap[status] || 'info'
  }

  // Actions
  // 获取活动列表
  const fetchActivityList = async (params: ActivityQuery = {}) => {
    loading.value = true
    try {
      const res: any = await getActivityList(params)
      // 后端返回分页格式 { records: [], total: number, ... }
      if (res && Array.isArray(res.records)) {
        activityList.value = res.records
        total.value = res.total || 0
        return res.records
      } else if (Array.isArray(res)) {
        // 兼容直接返回数组的情况
        activityList.value = res
        total.value = res.length
        return res
      } else {
        activityList.value = res || []
        total.value = res?.length || 0
        return res
      }
    } catch (error) {
      console.error('获取活动列表失败:', error)
    } finally {
      loading.value = false
    }
  }
  
  // 启动活动列表定时刷新
  const startActivityListPolling = (params: ActivityQuery = {}) => {
    // 先清除已有定时器
    stopActivityListPolling()
    // 立即执行一次
    fetchActivityList(params)
    // 启动定时器
    activityListTimer = setInterval(() => {
      fetchActivityList(params)
    }, POLLING_INTERVAL)
  }
  
  // 停止活动列表定时刷新
  const stopActivityListPolling = () => {
    if (activityListTimer) {
      clearInterval(activityListTimer)
      activityListTimer = null
    }
  }

  // 获取活动详情
  const fetchActivityDetail = async (id: number) => {
    loading.value = true
    try {
      const res: any = await getActivityDetail(id)
      currentActivity.value = res
      return res
    } catch (error) {
      console.error('获取活动详情失败:', error)
    } finally {
      loading.value = false
    }
  }
  
  // 启动活动详情定时刷新
  const startActivityDetailPolling = (id: number, userStore?: any) => {
    // 先清除已有定时器
    stopActivityDetailPolling()
    // 立即执行一次
    fetchActivityDetail(id)
    // 如果已登录，同时刷新角色信息
    if (userStore?.isLoggedIn) {
      fetchActivityRole(id)
    }
    // 启动定时器
    activityDetailTimer = setInterval(() => {
      fetchActivityDetail(id)
      // 定时刷新角色信息（用于更新按钮状态）
      if (userStore?.isLoggedIn) {
        fetchActivityRole(id)
      }
    }, POLLING_INTERVAL)
  }
  
  // 停止活动详情定时刷新
  const stopActivityDetailPolling = () => {
    if (activityDetailTimer) {
      clearInterval(activityDetailTimer)
      activityDetailTimer = null
    }
  }

  // 创建活动
  const createNewActivity = async (data: Partial<Activity>) => {
    loading.value = true
    try {
      const res: any = await createActivity(data)
      ElMessage.success('活动创建成功，等待审核')
      // 返回创建的活动ID（后端返回ResultLong，data为活动ID）
      // 可能是数字或包含id的对象
      const activityId = typeof res === 'number' ? res : (res?.id || res)
      return activityId || null
    } catch (error) {
      console.error('创建活动失败:', error)
    } finally {
      loading.value = false
    }
    return null
  }

  // 更新活动
  const updateActivityInfo = async (id: number, data: Partial<Activity>) => {
    loading.value = true
    try {
      await updateActivity(id, data)
      ElMessage.success('活动更新成功')
      return true
    } catch (error) {
      console.error('更新活动失败:', error)
    } finally {
      loading.value = false
    }
    return false
  }

  // 删除活动
  const removeActivity = async (id: number) => {
    loading.value = true
    try {
      await deleteActivity(id)
      ElMessage.success('活动删除成功')
      return true
    } catch (error) {
      console.error('删除活动失败:', error)
    } finally {
      loading.value = false
    }
    return false
  }

  // 报名活动
  const join = async (activityId: number, remark?: string, images?: File[]) => {
    try {
      await joinActivity(activityId, remark, images)
      ElMessage.success('报名成功')
      return true
    } catch (error) {
      console.error('报名失败:', error)
      return false
    }
  }

  // 取消报名
  const cancel = async (activityId: number) => {
    try {
      await cancelJoin(activityId)
      ElMessage.success('取消报名成功')
      return true
    } catch (error) {
      console.error('取消报名失败:', error)
      return false
    }
  }

  // 获取我的活动
  const fetchMyActivities = async () => {
    loading.value = true
    try {
      const res: any = await getMyActivities()
      myActivities.value = res || []
      return res
    } catch (error) {
      console.error('获取我的活动失败:', error)
    } finally {
      loading.value = false
    }
  }

  // 获取活动角色
  const fetchActivityRole = async (activityId: number) => {
    try {
      const res: any = await getActivityRole(activityId)
      activityRole.value = res
      return res
    } catch (error) {
      console.error('获取活动角色失败:', error)
      activityRole.value = null
    }
  }

  // 活动签到
  const doCheckIn = async (activityId: number) => {
    try {
      await checkIn(activityId)
      ElMessage.success('签到成功')
      return true
    } catch (error) {
      console.error('签到失败:', error)
      return false
    }
  }

  // 检查签到状态
  const fetchCheckInStatus = async (activityId: number) => {
    try {
      const res: any = await checkInStatus(activityId)
      return res
    } catch (error) {
      console.error('获取签到状态失败:', error)
      return false
    }
  }

  // 开始活动
  const start = async (activityId: number) => {
    try {
      await startActivity(activityId)
      ElMessage.success('活动已开始')
      return true
    } catch (error) {
      console.error('开始活动失败:', error)
      return false
    }
  }

  return {
    activityList,
    currentActivity,
    myActivities,
    activityRole,
    loading,
    total,
    isOrganizer,
    isManager,
    isParticipant,
    isOrganizerOrManager,
    canJoin,
    getStatusText,
    getStatusType,
    fetchActivityList,
    fetchActivityDetail,
    createNewActivity,
    updateActivityInfo,
    removeActivity,
    join,
    cancel,
    fetchMyActivities,
    fetchActivityRole,
    doCheckIn,
    fetchCheckInStatus,
    start,
    startActivityListPolling,
    stopActivityListPolling,
    startActivityDetailPolling,
    stopActivityDetailPolling
  }
})
