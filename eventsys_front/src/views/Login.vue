<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

// 登录方式：user-普通登录，admin-管理员登录（两者复用同一 /auth/login 接口，管理员入口额外校验角色）
const activeTab = ref<'user' | 'admin'>('user')

const loginForm = ref({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const loginFormRef = ref()

// 副标题随登录方式切换，管理员入口给出明确说明
const loginSubtitle = computed(() =>
  activeTab.value === 'admin' ? '管理员专用入口，请使用管理员账号登录' : '登录您的账号，开启校园活动之旅'
)

const handleLogin = async () => {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return

  const credentials = {
    username: loginForm.value.username,
    password: loginForm.value.password
  }

  // 管理员登录：登录成功后校验返回角色 role===1，非管理员明确提示且不进入后台
  if (activeTab.value === 'admin') {
    const success = await userStore.loginAction(credentials, { expectAdmin: true })
    if (success) {
      router.push('/admin/activities')
    }
    return
  }

  // 普通登录：登录成功后进入活动列表
  const success = await userStore.loginAction(credentials)
  if (success) {
    router.push('/events')
  }
}

const goToRegister = () => {
  router.push('/register')
}
</script>

<template>
  <div class="login-page">
    <!-- 左右分栏卡片式布局：左侧品牌渐变区 + 右侧表单区 -->
    <div class="login-card">
      <!-- 左侧品牌区：系统名 + 渐变背景 -->
      <div class="brand-panel">
        <div class="brand-logo">
          <el-icon :size="56" color="#ffffff"><Calendar /></el-icon>
        </div>
        <h1 class="brand-title">重庆师范大学校园活动平台</h1>
        <p class="brand-desc">发现精彩活动，记录美好时光</p>
        <div class="features-list">
          <div class="feature-item">
            <el-icon :size="20" color="#ffffff"><Check /></el-icon>
            <span>轻松创建和管理活动</span>
          </div>
          <div class="feature-item">
            <el-icon :size="20" color="#ffffff"><Check /></el-icon>
            <span>一键报名，快速参与</span>
          </div>
          <div class="feature-item">
            <el-icon :size="20" color="#ffffff"><Check /></el-icon>
            <span>记录精彩活动瞬间</span>
          </div>
        </div>
        <p class="brand-footer">© 重庆师范大学 · 校园活动综合管理系统</p>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-panel">
        <div class="form-header">
          <h2 class="login-title">欢迎回来</h2>
          <p class="login-subtitle">{{ loginSubtitle }}</p>
        </div>

        <!-- 登录方式切换：普通登录 / 管理员登录 -->
        <el-tabs v-model="activeTab" class="login-tabs" stretch>
          <el-tab-pane label="普通登录" name="user" />
          <el-tab-pane label="管理员登录" name="admin" />
        </el-tabs>

        <el-form ref="loginFormRef" :model="loginForm" :rules="rules" label-position="top">
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
              :prefix-icon="Lock"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="submit-btn"
              @click="handleLogin"
            >
              {{ activeTab === 'admin' ? '管理员登录' : '登录' }}
            </el-button>
          </el-form-item>

          <!-- 管理员入口不提供注册引导 -->
          <template v-if="activeTab === 'user'">
            <div class="divider">
              <span>还没有账号？</span>
            </div>

            <el-button
              size="large"
              class="register-btn"
              @click="goToRegister"
            >
              立即注册
            </el-button>
          </template>
        </el-form>
      </div>
    </div>

    <!-- 默认账号提示：初始数据自动创建，便于验收登录，正式使用请及时修改密码 -->
    <p class="login-hint">管理员账号：admin / 初始密码：test</p>
  </div>
</template>

<style scoped>
/* 页面背景：浅色中性底 + 品牌色光晕装饰（纵向堆叠卡片与底部提示） */
.login-page {
  min-height: 100vh;
  width: 100%;
  background:
    radial-gradient(ellipse 600px 400px at 12% 8%, var(--primary-bg) 0%, transparent 60%),
    radial-gradient(ellipse 500px 360px at 88% 92%, var(--primary-bg) 0%, transparent 60%),
    var(--bg-secondary);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
}

/* 整体卡片：左侧品牌 + 右侧表单 */
.login-card {
  display: flex;
  width: 100%;
  max-width: 960px;
  min-height: 560px;
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-lg);
}

/* 左侧品牌区：红色渐变背景 */
.brand-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 56px 48px;
  background: var(--primary-gradient);
  color: #ffffff;
  position: relative;
  overflow: hidden;
}

/* 品牌区装饰光斑 */
.brand-panel::before,
.brand-panel::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.brand-panel::before {
  width: 260px;
  height: 260px;
  top: -80px;
  right: -80px;
}

.brand-panel::after {
  width: 180px;
  height: 180px;
  bottom: -60px;
  left: -50px;
}

.brand-logo {
  width: 88px;
  height: 88px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.16);
  margin-bottom: 28px;
  position: relative;
  z-index: 1;
}

.brand-title {
  font-size: 32px;
  font-weight: 800;
  line-height: 1.35;
  margin: 0 0 12px;
  position: relative;
  z-index: 1;
}

.brand-desc {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.82);
  margin-bottom: 40px;
  position: relative;
  z-index: 1;
}

.features-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: relative;
  z-index: 1;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  color: rgba(255, 255, 255, 0.92);
}

.brand-footer {
  margin-top: 48px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  position: relative;
  z-index: 1;
}

/* 右侧表单区 */
.form-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 56px 56px;
  background: var(--bg-primary);
}

.form-header {
  margin-bottom: 24px;
}

/* 登录方式切换标签页：普通登录 / 管理员登录 */
.login-tabs {
  margin-bottom: 8px;
}

.login-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.login-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}

.divider {
  text-align: center;
  margin: 20px 0;
  position: relative;
}

.divider::before,
.divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 80px;
  height: 1px;
  background: var(--border-color);
}

.divider::before {
  left: 0;
}

.divider::after {
  right: 0;
}

.divider span {
  color: var(--text-muted);
  font-size: 14px;
}

/* 注册按钮：主色描边样式，圆角随主题 */
.register-btn {
  width: 100%;
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.register-btn:hover {
  background-color: var(--primary-bg);
  border-color: var(--primary-dark);
  color: var(--primary-dark);
}

/* 登录页底部默认账号提示 */
.login-hint {
  margin-top: 16px;
  font-size: 13px;
  color: var(--text-muted);
  text-align: center;
  letter-spacing: 0.2px;
}

@media (max-width: 768px) {
  .login-page {
    padding: 20px 16px;
  }

  .login-card {
    flex-direction: column;
    min-height: auto;
  }

  .brand-panel {
    padding: 36px 28px;
  }

  .brand-title {
    font-size: 24px;
  }

  .brand-footer {
    margin-top: 24px;
  }

  .form-panel {
    padding: 32px 28px;
  }
}
</style>
