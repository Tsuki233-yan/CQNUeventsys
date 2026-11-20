<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores'
import { getUnreadCount } from '@/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo)
const isAdmin = computed(() => userStore.isAdmin)
const unreadCount = ref(0)

/**
 * 获取头像URL
 * 静态资源受 context-path 影响，需要加 /api 前缀
 */
const getAvatarUrl = (url: string | undefined): string => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('/api/')) return url
  return '/api' + url
}
let refreshTimer: number | null = null

// 获取未读通知数量
const fetchUnreadCount = async () => {
  if (!isLoggedIn.value) return
  try {
    const res: any = await getUnreadCount()
    // 响应拦截器已解包，直接返回 { count: 5 }
    unreadCount.value = res?.count || 0
  } catch (error) {
    console.error('获取未读数量失败:', error)
  }
}

// 启动定时刷新
const startRefreshTimer = () => {
  if (refreshTimer) return
  refreshTimer = window.setInterval(() => {
    fetchUnreadCount()
  }, 30000) // 每30秒刷新一次
}

// 停止定时刷新
const stopRefreshTimer = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  if (isLoggedIn.value) {
    fetchUnreadCount()
    startRefreshTimer()
  }
  // 监听刷新角标事件
  window.addEventListener('refresh-unread-count', fetchUnreadCount)
})

onUnmounted(() => {
  stopRefreshTimer()
  // 移除事件监听
  window.removeEventListener('refresh-unread-count', fetchUnreadCount)
})

// 监听登录状态变化
watch(isLoggedIn, (newVal) => {
  if (newVal) {
    fetchUnreadCount()
    startRefreshTimer()
  } else {
    unreadCount.value = 0
    stopRefreshTimer()
  }
})

// 普通用户菜单
const userMenuItems = [
  { path: '/events', label: '活动列表', icon: 'Calendar' },
  { path: '/create-event', label: '创建活动', icon: 'Plus' },
  { path: '/achievements', label: '成果展示', icon: 'Trophy' },
  { path: '/notifications', label: '消息通知', icon: 'Bell' },
  { path: '/profile', label: '个人中心', icon: 'UserFilled' },
  { path: '/organizer/reviews', label: '评价管理', icon: 'ChatDotRound' }
]

// 管理员菜单
const adminMenuItems = [
  { path: '/events', label: '活动列表', icon: 'Calendar' },
  { path: '/create-event', label: '创建活动', icon: 'Plus' },
  { path: '/achievements', label: '成果展示', icon: 'Trophy' },
  { path: '/notifications', label: '消息通知', icon: 'Bell' },
  { path: '/profile', label: '个人中心', icon: 'UserFilled' },
  { path: '/admin/activities', label: '活动管理', icon: 'DocumentChecked' },
  { path: '/admin/reviews', label: '评价管理', icon: 'ChatDotRound' },
  { path: '/admin/users', label: '用户管理', icon: 'User' },
  { path: '/admin/categories', label: '分类管理', icon: 'Folder' }
]

const menuItems = computed(() => isAdmin.value ? adminMenuItems : userMenuItems)

const isActive = (path: string) => {
  return route.path === path || route.path.startsWith(path + '/')
}

const navigate = (path: string) => {
  router.push(path)
}

const goToHome = () => {
  // 已登录用户点击Logo跳转到活动列表，未登录跳转到首页
  if (isLoggedIn.value) {
    router.push('/events')
  } else {
    router.push('/')
  }
}

const handleCommand = (command: string) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}
</script>

<template>
  <div class="side-layout">
    <!-- 左侧内容区 -->
    <main class="main-content">
      <slot />
    </main>

    <!-- 右侧侧边栏：吸顶固定 + 毛玻璃效果 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo" @click="goToHome">
          <el-icon :size="32" color="var(--primary-color)"><Calendar /></el-icon>
          <span class="logo-text">校园活动</span>
        </div>
      </div>

      <nav class="sidebar-menu">
        <div
          v-for="item in menuItems"
          :key="item.path"
          class="menu-item"
          :class="{ active: isActive(item.path) }"
          @click="navigate(item.path)"
        >
          <el-icon :size="20">
            <component :is="item.icon" />
          </el-icon>
          <span class="menu-label">{{ item.label }}</span>
          <el-badge
            v-if="item.path === '/notifications' && unreadCount > 0"
            :value="unreadCount > 99 ? '99+' : unreadCount"
            :max="99"
            class="menu-badge"
          />
        </div>
      </nav>

      <div class="sidebar-footer">
        <template v-if="isLoggedIn">
          <el-dropdown @command="handleCommand" placement="top">
            <div class="user-info">
              <el-avatar :size="40" :src="getAvatarUrl(userInfo?.avatar)">
                {{ userInfo?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="user-meta">
                <span class="user-name">{{ userInfo?.nickname }}</span>
                <span class="user-role">{{ userInfo?.role === 1 ? '管理员' : '用户' }}</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" size="large" style="width: 100%" @click="navigate('/login')">
            登录 / 注册
          </el-button>
        </template>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.side-layout {
  display: flex;
  height: 100vh;
  width: 100%;
  background: var(--bg-secondary);
  overflow: hidden;
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  height: 100vh;
}

/* 侧边栏：视口高度内固定（吸顶吸底）+ 毛玻璃 */
.sidebar {
  width: 240px;
  min-width: 240px;
  max-width: 240px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-left: 1px solid var(--border-color-light);
  display: flex;
  flex-direction: column;
  z-index: 100;
  box-shadow: var(--shadow-sm);
  height: 100vh;
}

.sidebar-header {
  padding: 24px 20px;
  border-bottom: 1px solid var(--border-color-light);
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.2s;
}

.logo:hover {
  transform: scale(1.02);
}

.logo-text {
  font-size: 22px;
  font-weight: 700;
  color: var(--primary-color);
  letter-spacing: -0.5px;
}

.sidebar-menu {
  flex: 1;
  padding: 16px 12px;
  overflow-y: auto;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  margin-bottom: 6px;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all 0.25s ease;
  color: var(--text-secondary);
}

.menu-item:hover {
  background-color: var(--primary-bg);
  color: var(--primary-color);
  transform: translateX(-4px);
}

/* 激活菜单高亮：与顶部导航统一（主题色实底白字 + 主题色投影） */
.menu-item.active {
  background: var(--primary-color);
  color: #ffffff;
  box-shadow: var(--shadow-primary);
}

.menu-item.active:hover {
  background: var(--primary-dark);
  color: #ffffff;
}

.menu-label {
  font-size: 15px;
  font-weight: 500;
}

.menu-badge {
  margin-left: auto;
}

.sidebar-footer {
  padding: 20px;
  border-top: 1px solid var(--border-color-light);
  background: transparent;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-primary);
  border: 1px solid var(--border-color-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: var(--shadow-sm);
}

.user-info:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--color-primary-light);
}

.user-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.user-role {
  font-size: 12px;
  color: var(--primary-color);
}

@media (max-width: 768px) {
  .sidebar {
    width: 200px;
  }

  .main-content {
    width: calc(100% - 200px);
    padding: 16px;
  }
}
</style>
