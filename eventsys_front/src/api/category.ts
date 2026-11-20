import request from './request'

export interface Category {
  id: number
  name: string
  description: string
  icon: string
  sortOrder: number
  status: number
  activityCount: number
  createTime: string
  updateTime: string
}

// 获取分类列表（公开接口，只返回启用的）
export const getCategoryList = () => {
  return request.get('/categories/list')
}

// 获取所有分类（管理员接口，包括禁用的）
export const getAdminCategoryList = () => {
  return request.get('/admin/categories/list')
}

// 获取分类详情
export const getCategoryDetail = (id: number) => {
  return request.get(`/categories/detail/${id}`)
}

// 创建分类（管理员）
export const createCategory = (data: Partial<Category>) => {
  return request.post('/admin/categories/create', data)
}

// 更新分类（管理员）
export const updateCategory = (id: number, data: Partial<Category>) => {
  return request.put(`/admin/categories/update/${id}`, data)
}

// 删除分类（管理员）
export const deleteCategory = (id: number) => {
  return request.delete(`/admin/categories/delete/${id}`)
}
