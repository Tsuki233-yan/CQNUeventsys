import request from './request'
import type { User } from './auth'
import type { Activity } from './activity'

// 用户管理
export interface UserListParams {
  page?: number
  size?: number
  keyword?: string
  role?: number
  status?: number
  /** 身份筛选：0-学生，1-教师，-1-未绑定身份 */
  userType?: number
}

export interface UserListResult {
  list: User[]
  total: number
}

// 获取用户列表
export const getUserList = (params: UserListParams) => {
  return request.get('/admin/users/list', { params })
}

// 获取用户详情
export const getUserDetail = (id: number) => {
  return request.get(`/admin/users/detail/${id}`)
}

// 设置管理员
export const setAdmin = (id: number) => {
  return request.post(`/admin/users/set-admin/${id}`)
}

// 取消管理员
export const cancelAdmin = (id: number) => {
  return request.post(`/admin/users/cancel-admin/${id}`)
}

// 重置密码
export const resetPassword = (id: number, newPassword: string) => {
  return request.post(`/admin/users/reset-password/${id}?newPassword=${encodeURIComponent(newPassword)}`)
}

// 更新用户信息
export const updateUser = (id: number, data: Partial<User>) => {
  const params = new URLSearchParams()
  Object.entries(data).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      params.append(key, String(value))
    }
  })
  return request.put(`/admin/users/update/${id}?${params.toString()}`)
}

// 删除用户
export const deleteUser = (id: number) => {
  return request.delete(`/admin/users/delete/${id}`)
}

// 封禁用户
export const banUser = (id: number) => {
  return request.post(`/admin/users/ban/${id}`)
}

// 解封用户
export const unbanUser = (id: number) => {
  return request.post(`/admin/users/unban/${id}`)
}

// 活动管理
export interface ActivityListParams {
  page?: number
  size?: number
  status?: number
  keyword?: string
}

export interface ActivityListResult {
  list: Activity[]
  total: number
}

// 获取所有活动列表
export const getAllActivities = (params: ActivityListParams) => {
  return request.get('/admin/activities/list', { params })
}

// 获取活动详情（管理员）
export const getAdminActivityDetail = (id: number) => {
  return request.get(`/admin/activities/detail/${id}`)
}

// 获取待审核活动
export const getPendingActivities = (params: { page?: number; size?: number }) => {
  return request.get('/admin/activities/pending', { params })
}

// 审核通过
export const approveActivity = (id: number) => {
  return request.post(`/admin/activities/approve/${id}`)
}

// 审核拒绝
export const rejectActivity = (id: number, reason?: string) => {
  const url = reason 
    ? `/admin/activities/reject/${id}?reason=${encodeURIComponent(reason)}`
    : `/admin/activities/reject/${id}`
  return request.post(url)
}

// 强制下架
export const takeDownActivity = (id: number, reason?: string) => {
  const url = reason
    ? `/admin/activities/take-down/${id}?reason=${encodeURIComponent(reason)}`
    : `/admin/activities/take-down/${id}`
  return request.post(url)
}

// 删除活动
export const deleteActivity = (id: number) => {
  return request.delete(`/admin/activities/delete/${id}`)
}

// 批量刷新活动状态
export const refreshActivityStatus = () => {
  return request.post('/admin/activities/refresh-status')
}
