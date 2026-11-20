<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores'
import { User, Avatar, Postcard, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  // 身份绑定：0-学生，1-教师（模拟校验规则，可替换为学籍接口对接）
  userType: 0,
  studentId: '',
  teacherId: ''
})

const registerFormRef = ref()

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== registerForm.value.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 学生学号：10 位数字，前 4 位为入学年份（2020~2026）；与后端校验规则一致
const STUDENT_ID_PATTERN = /^202[0-6]\d{6}$/
// 教师工号：6 位数字；与后端校验规则一致
const TEACHER_ID_PATTERN = /^\d{6}$/

const validateStudentId = (_rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('请输入学号'))
  } else if (!STUDENT_ID_PATTERN.test(value)) {
    callback(new Error('学号须为10位数字，且前4位为入学年份（2020~2026）'))
  } else {
    callback()
  }
}

const validateTeacherId = (_rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('请输入工号'))
  } else if (!TEACHER_ID_PATTERN.test(value)) {
    callback(new Error('工号须为6位数字'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  userType: [{ required: true, message: '请选择身份', trigger: 'change' }],
  studentId: [{ required: true, validator: validateStudentId, trigger: 'blur' }],
  teacherId: [{ required: true, validator: validateTeacherId, trigger: 'blur' }]
}

// 切换身份时清空另一身份的编号并重置校验提示
watch(() => registerForm.value.userType, () => {
  registerForm.value.studentId = ''
  registerForm.value.teacherId = ''
  registerFormRef.value?.clearValidate(['studentId', 'teacherId'])
})

const handleRegister = async () => {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return

  const success = await userStore.registerAction({
    username: registerForm.value.username,
    password: registerForm.value.password,
    nickname: registerForm.value.nickname,
    userType: registerForm.value.userType,
    // 学生只提交学号，教师只提交工号，避免携带另一身份的脏数据
    studentId: registerForm.value.userType === 0 ? registerForm.value.studentId : undefined,
    teacherId: registerForm.value.userType === 1 ? registerForm.value.teacherId : undefined
  })

  if (success) {
    router.push('/login')
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<template>
  <div class="register-page">
    <!-- 左右分栏卡片式布局：左侧品牌渐变区 + 右侧表单区 -->
    <div class="register-card">
      <!-- 左侧品牌区：系统名 + 渐变背景 -->
      <div class="brand-panel">
        <div class="brand-logo">
          <el-icon :size="56" color="#ffffff"><UserFilled /></el-icon>
        </div>
        <h1 class="brand-title">重庆师范大学校园活动平台</h1>
        <p class="brand-desc">与志同道合的伙伴一起，探索校园精彩活动</p>
        <div class="features-list">
          <div class="feature-item">
            <el-icon :size="20" color="#ffffff"><Check /></el-icon>
            <span>加入我们，发现更多精彩活动</span>
          </div>
          <div class="feature-item">
            <el-icon :size="20" color="#ffffff"><Check /></el-icon>
            <span>报名、评论、评价一站参与</span>
          </div>
          <div class="feature-item">
            <el-icon :size="20" color="#ffffff"><Check /></el-icon>
            <span>成果展示，记录活动精彩瞬间</span>
          </div>
        </div>
        <p class="brand-footer">© 重庆师范大学 · 校园活动综合管理系统</p>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-panel">
        <div class="form-header">
          <div class="back-btn" @click="goToLogin">
            <el-icon><ArrowLeft /></el-icon>
          </div>
          <h2 class="register-title">创建账号</h2>
          <p class="register-subtitle">加入我们，开启校园活动之旅</p>
        </div>

        <el-form ref="registerFormRef" :model="registerForm" :rules="rules" label-position="top">
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="registerForm.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item label="昵称" prop="nickname">
            <el-input
              v-model="registerForm.nickname"
              placeholder="请输入昵称"
              size="large"
              :prefix-icon="Avatar"
            />
          </el-form-item>

          <!-- 身份绑定：真实用户须选择学生/教师身份并绑定对应学号/工号（模拟校验规则，可替换为学籍接口对接） -->
          <el-form-item label="身份" prop="userType">
            <el-radio-group v-model="registerForm.userType" size="large" class="user-type-group">
              <el-radio-button :label="0">学生</el-radio-button>
              <el-radio-button :label="1">教师</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="registerForm.userType === 0" label="学号" prop="studentId">
            <el-input
              v-model="registerForm.studentId"
              placeholder="请输入10位数字学号（前4位为入学年份2020-2026）"
              size="large"
              maxlength="10"
              :prefix-icon="Postcard"
            />
            <div class="field-hint">学号在系统内唯一，绑定后不可自行修改</div>
          </el-form-item>

          <el-form-item v-else-if="registerForm.userType === 1" label="工号" prop="teacherId">
            <el-input
              v-model="registerForm.teacherId"
              placeholder="请输入6位数字工号"
              size="large"
              maxlength="6"
              :prefix-icon="Postcard"
            />
            <div class="field-hint">工号在系统内唯一，绑定后不可自行修改</div>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码（至少6位）"
              size="large"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              size="large"
              show-password
              :prefix-icon="Lock"
              @keyup.enter="handleRegister"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="submit-btn"
              @click="handleRegister"
            >
              创建账号
            </el-button>
          </el-form-item>

          <div class="login-link">
            已有账号？<el-link type="primary" @click="goToLogin">立即登录</el-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 页面背景：浅色中性底 + 品牌色光晕装饰 */
.register-page {
  min-height: 100vh;
  width: 100%;
  background:
    radial-gradient(ellipse 600px 400px at 12% 8%, var(--primary-bg) 0%, transparent 60%),
    radial-gradient(ellipse 500px 360px at 88% 92%, var(--primary-bg) 0%, transparent 60%),
    var(--bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
}

/* 整体卡片：左侧品牌 + 右侧表单 */
.register-card {
  display: flex;
  width: 100%;
  max-width: 960px;
  min-height: 640px;
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
  line-height: 1.6;
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
  padding: 48px 56px;
  background: var(--bg-primary);
}

.form-header {
  margin-bottom: 28px;
  position: relative;
}

.back-btn {
  position: absolute;
  left: 0;
  top: 4px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s;
  color: var(--text-secondary);
  background: var(--bg-secondary);
}

.back-btn:hover {
  background-color: var(--primary-bg);
  color: var(--primary-color);
}

.register-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.register-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

/* 身份选择按钮组：占满一行 */
.user-type-group {
  width: 100%;
  display: flex;
}

.user-type-group :deep(.el-radio-button) {
  flex: 1;
}

.user-type-group :deep(.el-radio-button__inner) {
  width: 100%;
}

/* 学号/工号输入框下方的格式说明 */
.field-hint {
  width: 100%;
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-top: 4px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}

.login-link {
  text-align: center;
  margin-top: 20px;
  color: var(--text-secondary);
  font-size: 14px;
}

@media (max-width: 768px) {
  .register-page {
    padding: 20px 16px;
  }

  .register-card {
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
