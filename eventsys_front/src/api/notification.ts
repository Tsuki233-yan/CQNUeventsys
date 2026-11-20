import request from './request'

export interface Notification {
  id: number
  userId: number | null
  type: number
  typeName: string
  title: string
  content: string
  relatedId: number | null
  relatedType: string | null
  isRead: boolean
  createTime: string
  /** 公告扩展（type=1） */
  publishScope?: string
  isPinned?: number
  publishTime?: string | null
  status?: number
  announceCategory?: string
  updateTime?: string
}

export interface NotificationQuery {
  page?: number
  size?: number
  type?: number
  isRead?: boolean
}

export interface AnnouncementPayload {
  title: string
  content: string
  publishScope?: string
  isPinned?: boolean
  publishTime?: string | null
  status?: number
  announceCategory?: string
}

// 获取通知列表
export const getNotificationList = (params?: NotificationQuery) => {
  return request.get('/notifications/list', { params })
}

// 获取未读通知数量
export const getUnreadCount = () => {
  return request.get('/notifications/unread-count')
}

// 标记单条通知已读
export const markAsRead = (id: number) => {
  return request.post(`/notifications/read/${id}`)
}

// 标记全部通知已读
export const markAllAsRead = () => {
  return request.post('/notifications/read-all')
}

// 删除通知
export const deleteNotification = (id: number) => {
  return request.delete(`/notifications/delete/${id}`)
}

function toAnnouncementParams(payload: AnnouncementPayload) {
  const params: Record<string, unknown> = {
    title: payload.title,
    content: payload.content,
    publishScope: payload.publishScope ?? 'ALL',
    isPinned: payload.isPinned ?? false,
    status: payload.status ?? 1,
    announceCategory: payload.announceCategory ?? 'SYSTEM'
  }
  if (payload.publishTime) {
    params.publishTime = payload.publishTime
  }
  return params
}

/** 新建公告（管理员） */
export const createAnnouncement = (payload: AnnouncementPayload) => {
  return request.post('/admin/notifications/announcement', null, {
    params: toAnnouncementParams(payload)
  })
}

/** 兼容旧调用：立即发布 + 默认范围 */
export const sendAnnouncement = (data: { title: string; content: string }) => {
  return createAnnouncement({
    title: data.title,
    content: data.content,
    status: 1,
    publishScope: 'ALL',
    isPinned: false,
    announceCategory: 'SYSTEM'
  })
}

/** 更新公告 */
export const updateAnnouncement = (id: number, payload: AnnouncementPayload) => {
  return request.put(`/admin/notifications/announcement/${id}`, null, {
    params: toAnnouncementParams(payload)
  })
}

/** 撤回公告 */
export const recallAnnouncement = (id: number) => {
  return request.post(`/admin/notifications/announcement/${id}/recall`)
}

/** 管理员公告列表 */
export const listAdminAnnouncements = (params?: { page?: number; size?: number }) => {
  return request.get('/admin/notifications/announcements', { params })
}

/** 管理员公告详情 */
export const getAdminAnnouncement = (id: number) => {
  return request.get(`/admin/notifications/announcement/${id}`)
}
