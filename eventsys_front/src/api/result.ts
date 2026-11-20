import request from './request'

export interface ActivityResult {
  id: number
  activityId: number
  title: string
  content: string
  images: string
  creatorId: number
  viewCount: number
  commentCount: number
  createTime: string
  creator?: {
    id: number
    nickname: string
    avatar: string
  }
  activity?: {
    id: number
    title: string
  }
}

// 创建成果展示贴
export const createResult = (activityId: number, title: string, content?: string, images?: string) => {
  return request.post(`/results/create/${activityId}`, null, {
    params: { title, content, images }
  })
}

// 更新成果展示贴
export const updateResult = (id: number, title?: string, content?: string, images?: string) => {
  return request.put(`/results/update/${id}`, null, {
    params: { title, content, images }
  })
}

// 删除成果展示贴
export const deleteResult = (id: number) => {
  return request.delete(`/results/delete/${id}`)
}

// 获取成果展示贴
export const getResultByActivityId = (activityId: number) => {
  return request.get(`/results/activity/${activityId}`)
}

// 检查成果展示贴是否存在
export const checkResultExists = (activityId: number) => {
  return request.get(`/results/exists/${activityId}`)
}

// 上传成果展示贴图片
export const uploadResultImages = (activityId: number, files: File[]) => {
  const formData = new FormData()
  files.forEach(file => {
    formData.append('files', file)
  })
  // 不要手动设置 Content-Type，让浏览器自动设置（包含 boundary）
  return request.post(`/results/upload-images/${activityId}`, formData)
}
