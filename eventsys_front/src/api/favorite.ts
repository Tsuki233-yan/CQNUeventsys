import request from './request'

// 收藏活动
export const addFavorite = (activityId: number) => {
  return request.post(`/favorites/add/${activityId}`)
}

// 取消收藏
export const removeFavorite = (activityId: number) => {
  return request.post(`/favorites/remove/${activityId}`)
}

// 检查收藏状态
export const checkFavoriteStatus = (activityId: number) => {
  return request.get(`/favorites/status/${activityId}`)
}

// 获取我的收藏
export const getMyFavorites = () => {
  return request.get('/favorites/my-favorites')
}

// 获取收藏数量
export const getFavoriteCount = (activityId: number) => {
  return request.get(`/favorites/count/${activityId}`)
}
