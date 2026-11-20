import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, register, getUserInfo, type LoginRequest, type User } from '@/api'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<User | null>(null)
  const loading = ref(false)

  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 1)
  const userId = computed(() => userInfo.value?.id)

  // Actions
  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUserInfo = (user: User | null) => {
    userInfo.value = user
    if (user) {
      localStorage.setItem('userInfo', JSON.stringify(user))
    } else {
      localStorage.removeItem('userInfo')
    }
  }

  const clearToken = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  // 登录
  // options.expectAdmin 为 true 时（管理员入口）：登录成功后校验返回角色 role===1，
  // 非管理员给出明确提示且不写入 token/用户信息，不进入管理后台
  const loginAction = async (data: LoginRequest, options?: { expectAdmin?: boolean }) => {
    loading.value = true
    try {
      const res: any = await login(data)
      // 响应拦截器已处理 code === 200 的情况，直接返回 data: { user, token }
      if (res && res.token) {
        // 检查用户是否被封禁
        if (res.user && res.user.status === 0) {
          ElMessage.error('您的账号已被封禁，请联系管理员')
          return false
        }
        // 管理员入口：校验角色，非管理员拒绝建立会话
        if (options?.expectAdmin && res.user?.role !== 1) {
          ElMessage.error('该账号不是管理员，无法登录管理后台')
          return false
        }
        setToken(res.token)
        setUserInfo(res.user)
        ElMessage.success('登录成功')
        return true
      }
    } catch (error: any) {
      console.error('登录失败:', error)
      // 错误已在响应拦截器中处理并弹出提示
    } finally {
      loading.value = false
    }
    return false
  }

  // 注册
  const registerAction = async (data: Partial<User>) => {
    loading.value = true
    try {
      await register(data)
      ElMessage.success('注册成功')
      return true
    } catch (error) {
      console.error('注册失败:', error)
    } finally {
      loading.value = false
    }
    return false
  }

  // 获取用户信息
  const fetchUserInfo = async () => {
    if (!token.value) return
    try {
      const res: any = await getUserInfo()
      // 检查用户是否被封禁
      if (res && res.status === 0) {
        ElMessage.error('您的账号已被封禁，请联系管理员')
        clearToken()
        return
      }
      setUserInfo(res)
    } catch (error) {
      console.error('获取用户信息失败:', error)
      clearToken()
    }
  }

  // 退出登录
  const logout = () => {
    clearToken()
    ElMessage.success('已退出登录')
  }

  // 更新本地用户信息
  const updateUserInfo = (data: Partial<User>) => {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...data }
    }
  }

  return {
    token,
    userInfo,
    loading,
    isLoggedIn,
    isAdmin,
    userId,
    setToken,
    clearToken,
    loginAction,
    registerAction,
    fetchUserInfo,
    logout,
    updateUserInfo
  }
})
