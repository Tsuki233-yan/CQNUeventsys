<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo || { nickname: '游客', avatar: '' })

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

const navItems = [
  { path: '/', label: '首页' },
  { path: '/events', label: '活动列表' },
  { path: '/achievements', label: '成果展示' }
]

const isActive = (path: string) => {
  return route.path === path
}

const navigate = (path: string) => {
  router.push(path)
}

const handleCommand = (command: string) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'my-events') {
    router.push('/profile')
  } else if (command === 'logout') {
    userStore.logout()
    router.push('/')
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<template>
  <el-header class="navbar">
    <div class="nav-container">
      <div class="logo" @click="navigate('/')">
        <span>校园活动平台</span>
      </div>
      
      <nav class="nav-menu">
        <span
          v-for="item in navItems"
          :key="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          @click="navigate(item.path)"
        >
          {{ item.label }}
        </span>
      </nav>
      
      <div class="user-section">
        <template v-if="isLoggedIn">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="getAvatarUrl(userInfo.avatar)">
                {{ userInfo.nickname.charAt(0) }}
              </el-avatar>
              <span class="username">{{ userInfo.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="my-events">我的活动</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" @click="goToLogin">登录</el-button>
        </template>
      </div>
    </div>
  </el-header>
</template>

<style scoped>
/* 顶部导航：吸顶 + 毛玻璃效果 */
.navbar {
  background-color: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-color-light);
  box-shadow: var(--shadow-sm);
  padding: 0;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  height: 60px;
}

.nav-container {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  color: var(--primary-color);
  cursor: pointer;
}

.nav-menu {
  display: flex;
  gap: 16px;
}

/* 导航项激活高亮：与侧边栏菜单统一的主题色样式 */
.nav-item {
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 16px;
  padding: 6px 14px;
  border-radius: var(--radius-sm);
  transition: all 0.3s;
}

.nav-item:hover {
  color: var(--primary-color);
  background-color: var(--primary-bg);
}

.nav-item.active {
  color: var(--primary-color);
  background-color: var(--primary-bg);
  font-weight: 600;
}

.user-section {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: var(--radius-sm);
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: var(--bg-hover);
}

.username {
  font-size: 14px;
  color: var(--text-secondary);
}
</style>
