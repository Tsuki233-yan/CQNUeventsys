<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goToEvents = () => {
  router.push('/events')
}

// 跳转创建活动（未登录时由路由守卫引导至登录页）
const goToCreateEvent = () => {
  router.push('/create-event')
}

const goToAchievements = () => {
  router.push('/achievements')
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<template>
  <div class="home-container">
    <!-- 顶部导航栏：吸顶 + 毛玻璃 -->
    <header class="home-header">
      <div class="header-logo">
        <el-icon :size="32" color="var(--primary-color)"><Calendar /></el-icon>
        <span class="logo-text">校园活动平台</span>
      </div>
      <nav class="header-nav">
        <span class="nav-item active">首页</span>
        <span class="nav-item" @click="goToEvents">活动列表</span>
        <span class="nav-item" @click="goToAchievements">成果展示</span>
      </nav>
      <el-button type="primary" size="default" @click="goToLogin">登录</el-button>
    </header>

    <!-- 顶部渐变 Hero 横幅：系统名 + 一句话简介 + 快捷入口 -->
    <section class="hero-banner">
      <div class="hero-inner">
        <div class="hero-badge">
          <el-icon :size="18" color="#ffffff"><Calendar /></el-icon>
          <span>校园活动综合管理系统</span>
        </div>
        <h1 class="hero-title">重庆师范大学校园活动平台</h1>
        <p class="hero-subtitle">为校园社团和爱好团体搭建的统一交流平台</p>
        <div class="actions">
          <el-button type="primary" size="large" class="hero-btn-main" @click="goToEvents">
            <el-icon class="btn-icon"><View /></el-icon>
            浏览活动
          </el-button>
          <el-button size="large" class="hero-btn-ghost" @click="goToAchievements">
            <el-icon class="btn-icon"><Trophy /></el-icon>
            成果展示
          </el-button>
          <el-button size="large" class="hero-btn-ghost" @click="goToLogin">
            <el-icon class="btn-icon"><User /></el-icon>
            登录 / 注册
          </el-button>
        </div>
      </div>
    </section>

    <el-row :gutter="24" class="features">
      <el-col :span="8">
        <!-- 功能卡片均可点击跳转到对应页面 -->
        <el-card class="feature-card" shadow="hover" @click="goToEvents">
          <div class="feature-icon">
            <el-icon :size="48" color="var(--primary-color)"><Collection /></el-icon>
          </div>
          <template #header>
            <span class="feature-title">活动展示</span>
          </template>
          <p class="feature-desc">浏览各类校园活动，按标签筛选感兴趣的活动类型</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="feature-card" shadow="hover" @click="goToCreateEvent">
          <div class="feature-icon">
            <el-icon :size="48" color="var(--primary-color)"><EditPen /></el-icon>
          </div>
          <template #header>
            <span class="feature-title">活动举办</span>
          </template>
          <p class="feature-desc">轻松创建和管理活动，支持定时开启和手动控制</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="feature-card" shadow="hover" @click="goToAchievements">
          <div class="feature-icon">
            <el-icon :size="48" color="var(--primary-color)"><Trophy /></el-icon>
          </div>
          <template #header>
            <span class="feature-title">成果展示</span>
          </template>
          <p class="feature-desc">记录活动精彩瞬间，展示活动成果，支持互动交流</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.home-container {
  min-height: 100vh;
  width: 100%;
  background: var(--bg-secondary);
  display: flex;
  flex-direction: column;
}

/* 顶部导航栏：吸顶 + 毛玻璃效果 */
.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 48px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-color-light);
  box-shadow: var(--shadow-sm);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: var(--primary-color);
}

.header-nav {
  display: flex;
  gap: 24px;
}

/* 导航项与侧边栏菜单统一的激活高亮：主题色文字 + 浅色圆角底 */
.nav-item {
  font-size: 15px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  padding: 6px 14px;
  border-radius: var(--radius-sm);
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

/* 顶部渐变 Hero 横幅 */
.hero-banner {
  position: relative;
  padding: 96px 40px 104px;
  background: var(--primary-gradient);
  text-align: center;
  color: #ffffff;
  overflow: hidden;
}

/* 横幅装饰光斑 */
.hero-banner::before,
.hero-banner::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.09);
}

.hero-banner::before {
  width: 420px;
  height: 420px;
  top: -160px;
  right: -120px;
}

.hero-banner::after {
  width: 300px;
  height: 300px;
  bottom: -140px;
  left: -80px;
}

.hero-inner {
  position: relative;
  z-index: 1;
  max-width: 860px;
  margin: 0 auto;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 14px;
  letter-spacing: 2px;
  margin-bottom: 28px;
}

.hero-title {
  font-size: 48px;
  font-weight: 800;
  margin: 0 0 16px;
  letter-spacing: -1px;
  line-height: 1.25;
}

.hero-subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 40px;
  font-weight: 400;
}

/* 快捷入口按钮 */
.actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.btn-icon {
  margin-right: 8px;
}

/* 主入口：白底主题字，在渐变底上更醒目 */
.hero-btn-main {
  background: #ffffff;
  border-color: #ffffff;
  color: var(--primary-color);
  font-weight: 600;
}

.hero-btn-main:hover,
.hero-btn-main:focus {
  background: rgba(255, 255, 255, 0.88);
  border-color: rgba(255, 255, 255, 0.88);
  color: var(--primary-dark);
}

/* 次入口：白色描边幽灵按钮 */
.hero-btn-ghost {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.55);
  color: #ffffff;
}

.hero-btn-ghost:hover,
.hero-btn-ghost:focus {
  background: rgba(255, 255, 255, 0.18);
  border-color: #ffffff;
  color: #ffffff;
}

/* 功能卡片 */
.features {
  margin-top: 56px;
  padding: 0 40px 72px;
  margin-left: auto;
  margin-right: auto;
  width: 100%;
  max-width: 1200px;
}

.feature-card {
  text-align: center;
  height: 100%;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
  background: var(--bg-primary);
  transition: all 0.3s ease;
  /* 卡片可点击跳转，鼠标悬停显示手型 */
  cursor: pointer;
}

/* 卡片悬停上浮阴影 */
.feature-card:hover {
  transform: translateY(-8px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-primary-light);
}

.feature-icon {
  margin-bottom: 16px;
}

.feature-card :deep(.el-card__header) {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  border-bottom: none;
  padding-bottom: 8px;
}

.feature-title {
  color: var(--text-primary);
}

.feature-desc {
  color: var(--text-secondary);
  line-height: 1.6;
  font-size: 15px;
}

@media (max-width: 768px) {
  .home-header {
    padding: 12px 20px;
  }

  .header-nav {
    gap: 8px;
  }

  .hero-banner {
    padding: 64px 24px 72px;
  }

  .hero-title {
    font-size: 32px;
  }

  .features {
    padding: 0 20px 48px;
  }
}
</style>
