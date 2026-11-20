import request from './request'

export interface OrganizerInfo {
  id: number
  username: string
  nickname: string
  realName: string
}

export interface Activity {
  id: number
  title: string
  description: string
  coverImage: string
  organizerId: number
  organizer?: OrganizerInfo
  location: string
  startTime: string
  endTime: string
  registrationStartTime: string
  registrationEndTime: string
  maxParticipants: number
  currentParticipants: number
  tags: string
  status: number
  categoryId: number
  needReview: boolean
  viewCount: number
  favoriteCount: number
  commentCount: number
  createTime: string
}

export interface ActivityQuery {
  page?: number
  size?: number
  keyword?: string
  status?: number
  categoryId?: number
}

// 获取活动列表
export const getActivityList = (params: ActivityQuery) => {
  return request.get('/activities/list', { params })
}

// 获取活动详情
export const getActivityDetail = (id: number) => {
  return request.get(`/activities/detail/${id}`)
}

// 创建活动
export const createActivity = (data: Partial<Activity>) => {
  return request.post('/activities/manage/create', data)
}

// 更新活动
export const updateActivity = (id: number, data: Partial<Activity>) => {
  return request.put(`/activities/manage/update/${id}`, data)
}

// 删除活动
export const deleteActivity = (id: number) => {
  return request.delete(`/activities/manage/delete/${id}`)
}

// 报名活动
export const joinActivity = (activityId: number, remark?: string, images?: File[]) => {
  const formData = new FormData()
  if (remark) {
    formData.append('remark', remark)
  }
  if (images && images.length > 0) {
    images.forEach(file => {
      formData.append('images', file)
    })
  }
  return request.post(`/activities/participant/join/${activityId}`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 取消报名
export const cancelJoin = (activityId: number) => {
  return request.post(`/activities/participant/cancel/${activityId}`)
}

// 获取我参与的活动
export const getMyActivities = () => {
  return request.get('/activities/participant/my-activities')
}

// 获取我创建的活动
export const getMyCreatedActivities = () => {
  return request.get('/activities/manage/my-created')
}

// 获取活动角色信息
export const getActivityRole = (activityId: number) => {
  return request.get(`/activities/participant/role/${activityId}`)
}

// 活动签到
export const checkIn = (activityId: number) => {
  return request.post(`/activities/checkin/${activityId}`)
}

// 检查签到状态
export const checkInStatus = (activityId: number) => {
  return request.get(`/activities/checkin/status/${activityId}`)
}

// 获取我的签到记录
export const getMyCheckInRecords = () => {
  return request.get('/activities/checkin/my-records')
}

// 获取活动状态信息
export const getActivityStatus = (activityId: number) => {
  return request.get(`/activities/status/info/${activityId}`)
}

// 开始活动
export const startActivity = (activityId: number) => {
  return request.post(`/activities/status/start/${activityId}`)
}

// 获取活动签到列表
export const getActivityCheckInList = (activityId: number) => {
  return request.get(`/activities/checkin/list/${activityId}`)
}

// 获取活动组织者列表
export const getActivityOrganizers = (activityId: number) => {
  return request.get(`/activities/manage/${activityId}/organizers`)
}

// 添加活动组织者
export const addActivityOrganizer = (activityId: number, userId: number) => {
  return request.post(`/activities/manage/${activityId}/organizers`, { userId })
}

// 移除活动组织者
export const removeActivityOrganizer = (activityId: number, targetUserId: number) => {
  return request.post(`/activities/manage/${activityId}/organizers/${targetUserId}/remove`)
}

// 获取活动报名人员列表
export const getActivityParticipants = (activityId: number) => {
  return request.get(`/activities/manage/${activityId}/participants`)
}

// 移除报名人员
export const removeActivityParticipant = (registrationId: number) => {
  return request.post(`/activities/manage/registrations/${registrationId}/remove`)
}

// 上传活动封面图片
export const uploadActivityCover = (activityId: number, file: File) => {
  const formData = new FormData()
  // 使用 'cover' 作为参数名，与后端接口匹配
  formData.append('cover', file)
  // 不要手动设置 Content-Type，让浏览器自动设置（包含 boundary）
  return request.post(`/activities/manage/upload-cover/${activityId}`, formData)
}

// 获取活动报名列表（组织者）
export const getActivityRegistrations = (activityId: number) => {
  return request.get(`/activities/manage/${activityId}/registrations`)
}

// 审核报名（组织者通过/拒绝报名申请）
export const reviewRegistration = (registrationId: number, status: number, reason?: string) => {
  const url = reason
    ? `/activities/manage/registrations/${registrationId}/review?status=${status}&reason=${encodeURIComponent(reason)}`
    : `/activities/manage/registrations/${registrationId}/review?status=${status}`
  return request.post(url)
}

// 刷新单个活动状态
export const refreshSingleActivityStatus = (activityId: number) => {
  return request.get(`/activities/status/refresh/${activityId}`)
}

// 批量刷新所有活动状态
export const refreshAllActivityStatus = () => {
  return request.post('/activities/status/refresh-all')
}
