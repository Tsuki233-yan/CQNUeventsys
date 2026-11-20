import request from './request'

export interface Review {
  id: number
  activityId: number
  userId: number
  rating: number
  content: string
  reply: string | null
  createTime: string
  updateTime: string
  user?: {
    id: number
    nickname: string
    avatar: string
  }
}

export interface ReviewStatistics {
  activityId: number
  avgRating: number
  totalCount: number
  ratingDistribution: Record<string, number>
}

export interface CreateReviewRequest {
  activityId: number
  rating: number
  content: string
}

// 查看活动评价列表（公开接口）
export const getActivityReviews = (activityId: number, params?: { page?: number; size?: number }) => {
  return request.get(`/reviews/activity/${activityId}`, { params })
}

// 获取活动评价统计（公开接口）
export const getReviewStatistics = (activityId: number) => {
  return request.get(`/reviews/statistics/${activityId}`)
}

// 提交活动评价
export const createReview = (data: CreateReviewRequest) => {
  return request.post('/reviews/create', data)
}

// 获取我的评价列表
export const getMyReviews = (params?: { page?: number; size?: number }) => {
  return request.get('/reviews/my', { params })
}

// 组织者查看自己活动的评价
export const getOrganizerReviews = (params?: { activityId?: number; page?: number; size?: number }) => {
  return request.get('/reviews/organizer', { params })
}

// 组织者回复评价
export const replyReview = (reviewId: number, reply: string) => {
  return request.post(`/reviews/reply/${reviewId}`, { reply })
}

// 管理员删除评价
export const deleteReview = (reviewId: number) => {
  return request.delete(`/reviews/delete/${reviewId}`)
}
