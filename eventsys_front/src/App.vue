<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SideLayout from './components/SideLayout.vue'

const route = useRoute()

// 判断是否为公开页面（未登录可访问）
const publicPages = ['/', '/login', '/register']
const isPublicPage = computed(() => publicPages.includes(route.path))
</script>

<template>
  <div class="app-container">
    <!-- 公开页面（未登录首页、登录、注册）全屏显示，无导航 -->
    <template v-if="isPublicPage">
      <main class="public-page">
        <router-view />
      </main>
    </template>
    
    <!-- 登录后的页面使用侧边栏布局 -->
    <template v-else>
      <SideLayout>
        <router-view />
      </SideLayout>
    </template>
  </div>
</template>

<style>
/* 主题变量已在 main.ts 中于 Element Plus 样式之后统一引入（styles/theme.css） */

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  width: 100%;
}

.app-container {
  min-height: 100vh;
  width: 100%;
  font-family: var(--font-family);
  color: var(--text-primary);
}

/* 公开页面全屏 */
.public-page {
  min-height: 100vh;
  width: 100%;
  background: var(--bg-secondary);
}

/* 全局滚动条样式（跟随主题色） */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: var(--bg-secondary);
}

::-webkit-scrollbar-thumb {
  background: var(--color-primary-light);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--color-primary);
}
</style>
