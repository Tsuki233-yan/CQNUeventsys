import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'

// 获取用户登录状态（从 localStorage 读取，避免在路由守卫外初始化 Pinia）
const getIsLoggedIn = () => {
  return !!localStorage.getItem('token')
}

// 获取用户角色（从 localStorage 读取）
const getIsAdmin = () => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    try {
      const user = JSON.parse(userInfo)
      return user.role === 1
    } catch {
      return false
    }
  }
  return false
}

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
    meta: { public: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true, guestOnly: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { public: true, guestOnly: true }
  },
  {
    path: '/events',
    name: 'Events',
    component: () => import('../views/Events.vue')
  },
  {
    path: '/event/:id',
    name: 'EventDetail',
    component: () => import('../views/EventDetail.vue')
  },
  {
    path: '/create-event',
    name: 'CreateEvent',
    component: () => import('../views/CreateEvent.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/achievements',
    name: 'Achievements',
    component: () => import('../views/Achievements.vue')
  },
  {
    path: '/achievement/:id',
    name: 'AchievementDetail',
    component: () => import('../views/AchievementDetail.vue')
  },
  {
    path: '/edit-achievement',
    name: 'EditAchievement',
    component: () => import('../views/EditAchievement.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('../views/Notifications.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/notifications/send-announcement',
    name: 'SendAnnouncement',
    component: () => import('../views/SendAnnouncement.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/notifications/manage-announcements',
    name: 'ManageAnnouncements',
    component: () => import('../views/ManageAnnouncements.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    // 管理后台根路径，重定向到活动管理首页（权限校验由目标路由的 meta 完成）
    path: '/admin',
    redirect: '/admin/activities'
  },
  {
    path: '/admin/users',
    name: 'UserManagement',
    component: () => import('../views/admin/UserManagement.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/activities',
    name: 'ActivityManagement',
    component: () => import('../views/admin/ActivityManagement.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/activities/review/:id',
    name: 'ActivityReview',
    component: () => import('../views/admin/ActivityReview.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/categories',
    name: 'CategoryManagement',
    component: () => import('../views/admin/CategoryManagement.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/reviews',
    name: 'ReviewManagement',
    component: () => import('../views/admin/ReviewManagement.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/organizer/reviews',
    name: 'OrganizerReviews',
    component: () => import('../views/OrganizerReviews.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局路由守卫 - 登录检查
router.beforeEach((to, _from, next) => {
  const isLoggedIn = getIsLoggedIn()
  const isAdmin = getIsAdmin()

  // 1. 需要登录的页面，未登录则跳转到登录页
  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/login')
    return
  }

  // 2. 仅限游客访问的页面（登录页、注册页），已登录则跳转到活动列表
  if (to.meta.guestOnly && isLoggedIn) {
    next('/events')
    return
  }

  // 3. 需要管理员权限的页面，非管理员跳转到活动列表
  if (to.meta.requiresAdmin && !isAdmin) {
    next('/events')
    return
  }

  // 4. 其他情况正常放行
  next()
})

export default router
