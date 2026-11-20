import request from './request'

export interface LoginRequest {
  username: string
  password: string
}

export interface User {
  id: number
  username: string
  /** 密码：仅注册请求携带，接口返回的用户信息不含此字段 */
  password?: string
  nickname: string
  avatar: string
  email: string
  phone: string
  realName: string
  /** 用户身份：0-学生，1-教师（null/undefined 视为未绑定身份的老账号） */
  userType?: number | null
  /** 学号：学生为10位数字（前4位为入学年份2020~2026） */
  studentId: string
  /** 工号：教师为6位数字 */
  teacherId?: string | null
  college: string
  major: string
  grade: string
  gender: number
  bio: string
  role: number
  status: number
  organizedCount: number
  participatedCount: number
  createTime: string
}

export interface UpdateUserInfoRequest {
  nickname?: string
  email?: string
  phone?: string
  realName?: string
  college?: string
  major?: string
  grade?: string
  gender?: number
  bio?: string
}

// 身份绑定请求参数：学生填 studentId，教师填 teacherId
export interface BindIdentityRequest {
  userType: number
  studentId?: string
  teacherId?: string
}

// 用户登录
export const login = (data: LoginRequest) => {
  return request.post('/auth/login', data)
}

// 用户注册
export const register = (data: Partial<User>) => {
  return request.post('/auth/register', data)
}

// 获取当前用户信息
export const getUserInfo = () => {
  return request.get('/user/info')
}

// 上传头像
export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('avatar', file)
  return request.post('/user/upload-avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 更新用户信息
export const updateUserInfo = (data: UpdateUserInfoRequest) => {
  const params = new URLSearchParams()
  Object.entries(data).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      params.append(key, String(value))
    }
  })
  return request.put(`/user/update-info?${params.toString()}`)
}

// 补填身份绑定（历史用户"完善身份认证"）：学生绑学号、教师绑工号
// 后端校验格式与唯一性，校验不通过返回 400 中文错误（模拟校验规则，可替换为学籍接口对接）
export const bindIdentity = (data: BindIdentityRequest) => {
  const params = new URLSearchParams()
  params.append('userType', String(data.userType))
  if (data.studentId) params.append('studentId', data.studentId)
  if (data.teacherId) params.append('teacherId', data.teacherId)
  return request.post(`/user/bind-identity?${params.toString()}`)
}
