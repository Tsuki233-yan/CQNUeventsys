import request from './request'

export interface Comment {
  id: number
  targetId: number
  targetType: number
  userId: number
  content: string
  parentId: number
  replyToUserId: number
  createTime: string
  user?: {
    id: number
    nickname: string
    avatar: string
    /** 用户状态：0-禁用（封禁），1-正常；仅管理员封禁/解封操作使用 */
    status?: number
  }
  replyToUser?: {
    id: number
    nickname: string
  }
  children?: Comment[]
}

// 获取评论列表
export const getComments = (targetId: number, targetType: number) => {
  return request.get('/comments/list', {
    params: { targetId, targetType }
  })
}

// 发表评论
export const postComment = (targetId: number, targetType: number, content: string) => {
  return request.post('/comments/post', null, {
    params: { targetId, targetType, content }
  })
}

// 回复评论
export const replyComment = (parentId: number, content: string, replyToUserId?: number) => {
  return request.post(`/comments/reply/${parentId}`, null, {
    params: { content, replyToUserId }
  })
}

// 删除评论
export const deleteComment = (id: number) => {
  return request.delete(`/comments/delete/${id}`)
}

// 获取评论数量
export const getCommentCount = (targetId: number, targetType: number) => {
  return request.get('/comments/count', {
    params: { targetId, targetType }
  })
}
