<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createAnnouncement,
  updateAnnouncement,
  getAdminAnnouncement,
  type AnnouncementPayload
} from '@/api/notification'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const editingId = computed(() => {
  const id = route.query.id
  if (!id) return null
  const n = Number(id)
  return Number.isFinite(n) && n > 0 ? n : null
})

const isEdit = computed(() => editingId.value != null)

const form = ref({
  title: '',
  type: '系统公告',
  isTop: false,
  scope: '全部用户',
  publishType: 'immediate' as 'immediate' | 'scheduled',
  publishTime: null as string | null,
  content: '',
  remark: '',
  loadedStatus: null as number | null
})

const announcementTypeOptions = ['系统公告', '活动通知', '紧急通知'] as const
const scopeOptions = ['全部用户', '仅普通用户', '仅管理员'] as const

const scopeToApi: Record<string, string> = {
  全部用户: 'ALL',
  仅普通用户: 'USER',
  仅管理员: 'ADMIN'
}
const categoryToApi: Record<string, string> = {
  系统公告: 'SYSTEM',
  活动通知: 'ACTIVITY',
  紧急通知: 'URGENT'
}
const scopeFromApi = (code: string | undefined) => {
  if (code === 'USER') return '仅普通用户'
  if (code === 'ADMIN') return '仅管理员'
  return '全部用户'
}
const categoryFromApi = (code: string | undefined) => {
  if (code === 'ACTIVITY') return '活动通知'
  if (code === 'URGENT') return '紧急通知'
  return '系统公告'
}

const submitting = ref(false)
const loadError = ref(false)

const goBack = () => {
  router.push('/notifications')
}

const buildPayload = (status: number): AnnouncementPayload => {
  let publishTime: string | undefined
  if (status === 1) {
    if (form.value.publishType === 'scheduled') {
      publishTime = form.value.publishTime || undefined
    }
  }
  return {
    title: form.value.title.trim(),
    content: form.value.content.trim(),
    publishScope: scopeToApi[form.value.scope] || 'ALL',
    isPinned: form.value.isTop,
    publishTime,
    status,
    announceCategory: categoryToApi[form.value.type] || 'SYSTEM'
  }
}

const validateBase = () => {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入公告标题')
    return false
  }
  if (!form.value.content.trim()) {
    ElMessage.warning('请输入公告内容')
    return false
  }
  return true
}

const handleSaveDraft = async () => {
  if (!validateBase()) return
  submitting.value = true
  try {
    const payload = buildPayload(0)
    if (isEdit.value && editingId.value != null) {
      await updateAnnouncement(editingId.value, payload)
      ElMessage.success('草稿已保存')
    } else {
      await createAnnouncement(payload)
      ElMessage.success('草稿已保存')
    }
    router.push('/notifications/manage-announcements')
  } catch (error) {
    console.error('保存草稿失败:', error)
    ElMessage.error('保存草稿失败')
  } finally {
    submitting.value = false
  }
}

const handlePublish = async () => {
  if (!validateBase()) return
  if (form.value.publishType === 'scheduled' && !form.value.publishTime) {
    ElMessage.warning('请选择定时发布时间')
    return
  }
  submitting.value = true
  try {
    const payload = buildPayload(1)
    if (isEdit.value && editingId.value != null) {
      await updateAnnouncement(editingId.value, payload)
      ElMessage.success('公告已发布')
    } else {
      await createAnnouncement(payload)
      ElMessage.success('公告已发布')
    }
    router.push('/notifications')
  } catch (error) {
    console.error('发布公告失败:', error)
    ElMessage.error('发布公告失败')
  } finally {
    submitting.value = false
  }
}

const loadDetail = async () => {
  if (!editingId.value) return
  loadError.value = false
  try {
    const res: any = await getAdminAnnouncement(editingId.value)
    if (!res) {
      loadError.value = true
      return
    }
    form.value.title = res.title || ''
    form.value.content = res.content || ''
    form.value.type = categoryFromApi(res.announceCategory)
    form.value.scope = scopeFromApi(res.publishScope)
    form.value.isTop = res.isPinned === 1
    form.value.loadedStatus = res.status != null ? res.status : null
    const timeStr = (t: string) => String(t).replace('T', ' ').slice(0, 19)
    if (res.status === 1 && res.publishTime) {
      const pt = new Date(res.publishTime)
      if (pt.getTime() > Date.now()) {
        form.value.publishType = 'scheduled'
        form.value.publishTime = timeStr(res.publishTime)
      } else {
        form.value.publishType = 'immediate'
        form.value.publishTime = null
      }
    } else if (res.status === 0) {
      form.value.publishType = res.publishTime ? 'scheduled' : 'immediate'
      form.value.publishTime = res.publishTime ? timeStr(res.publishTime) : null
    } else {
      form.value.publishType = 'immediate'
      form.value.publishTime = null
    }
  } catch (e) {
    console.error(e)
    loadError.value = true
  }
}

onMounted(() => {
  if (editingId.value) {
    loadDetail()
  }
})

watch(editingId, (id) => {
  if (id) loadDetail()
})
</script>

<template>
  <div class="send-announcement-page">
    <div class="page-header">
      <div class="back-link" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回消息通知</span>
      </div>
      <div class="title-row">
        <el-icon :size="28" color="var(--color-primary)"><Bell /></el-icon>
        <h2>{{ isEdit ? '编辑系统公告' : '发送系统公告' }}</h2>
      </div>
    </div>

    <el-alert
      v-if="loadError"
      type="error"
      title="加载公告失败"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />
    <el-alert
      v-else-if="form.loadedStatus === 2"
      type="warning"
      title="该公告已撤回，用户端不可见。可修改后重新发布。"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />

    <el-card class="form-card" shadow="never">
      <el-form :model="form" label-width="100px" class="announcement-form">
        <div class="form-section">
          <h3 class="section-title">基础信息</h3>
          <el-form-item label="标题" required>
            <el-input
              v-model="form.title"
              placeholder="请输入公告标题"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="公告类型">
            <el-select v-model="form.type" placeholder="请选择公告类型" style="width: 100%">
              <el-option
                v-for="opt in announcementTypeOptions"
                :key="opt"
                :label="opt"
                :value="opt"
              />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-section">
          <h3 class="section-title">发布设置</h3>
          <el-form-item label="发布范围">
            <el-radio-group v-model="form.scope">
              <el-radio v-for="s in scopeOptions" :key="s" :label="s">
                {{ s }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="是否置顶">
            <el-switch v-model="form.isTop" />
          </el-form-item>
          <el-form-item label="发布时间">
            <el-radio-group v-model="form.publishType">
              <el-radio label="immediate">立即发布</el-radio>
              <el-radio label="scheduled">定时发布</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="form.publishType === 'scheduled'" label="定时时间">
            <el-date-picker
              v-model="form.publishTime"
              type="datetime"
              placeholder="选择发布时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
        </div>

        <div class="form-section">
          <h3 class="section-title">公告内容</h3>
          <el-form-item label="正文" required>
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="9"
              placeholder="请输入公告内容"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="备注">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="3"
              placeholder="选填"
            />
          </el-form-item>
        </div>

        <el-form-item class="actions-form-item">
          <div class="form-actions">
            <el-button @click="goBack">取消</el-button>
            <el-button :loading="submitting" @click="handleSaveDraft">保存草稿</el-button>
            <el-button type="danger" :loading="submitting" @click="handlePublish">
              发布
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.send-announcement-page {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
  padding: 48px 16px 56px;
  box-sizing: border-box;
}

.page-header {
  margin-bottom: 24px;
  padding: 0 4px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 15px;
  transition: all 0.2s;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  width: fit-content;
  margin-bottom: 12px;
}

.back-link:hover {
  color: var(--color-primary);
  background-color: var(--primary-bg);
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-row h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.form-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
  background: var(--bg-primary);
}

.form-card :deep(.el-card__body) {
  padding: 26px 28px 24px;
}

.announcement-form {
  padding: 0;
}

.form-section {
  margin-bottom: 8px;
}

.form-section + .form-section {
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid var(--bg-hover);
}

.section-title {
  margin: 0 0 18px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-primary);
  letter-spacing: 0.02em;
}

.actions-form-item {
  margin-bottom: 0;
  margin-top: 8px;
}

.actions-form-item :deep(.el-form-item__content) {
  display: block;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding-top: 8px;
  flex-wrap: wrap;
}
</style>
