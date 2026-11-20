<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useActivityStore } from '@/stores'
import { approveActivity, rejectActivity, getAdminActivityDetail } from '@/api/admin'

const route = useRoute()
const router = useRouter()
const activityStore = useActivityStore()

const id = computed(() => Number(route.params.id))
const loading = ref(false)
const detail = ref<any>(null)

const form = ref({
  opinion: ''
})

const statusText = computed(() => activityStore.getStatusText(detail.value?.status))
const statusType = computed(() => activityStore.getStatusType(detail.value?.status))

const organizerName = computed(() => {
  const org = detail.value?.organizer
  return org?.nickname || org?.realName || org?.username || '-'
})

const getImageUrl = (url?: string): string => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('/api/')) return url
  return '/api' + url
}

const formatDate = (date?: string) => {
  if (!date) return '-'
  const utcString = date.endsWith('Z') ? date : date + 'Z'
  const d = new Date(utcString)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${year}/${month}/${day} ${hours}:${minutes}:${seconds}`
}

const fetchDetail = async () => {
  loading.value = true
  try {
    const res: any = await getAdminActivityDetail(id.value)
    detail.value = res
  } catch (e) {
    ElMessage.error('获取活动详情失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/admin/activities')
}

const handleApprove = async () => {
  if (!detail.value) return
  loading.value = true
  try {
    await approveActivity(detail.value.id)
    ElMessage.success('审核通过成功')
    goBack()
  } catch (e) {
    ElMessage.error('审核通过失败')
  } finally {
    loading.value = false
  }
}

const handleReject = async () => {
  const reason = form.value.opinion?.trim()
  if (!reason) {
    ElMessage.warning('审核拒绝时必须填写拒绝原因')
    return
  }
  if (!detail.value) return
  loading.value = true
  try {
    await rejectActivity(detail.value.id, reason)
    ElMessage.success('审核拒绝成功')
    goBack()
  } catch (e) {
    ElMessage.error('审核拒绝失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <div class="activity-review" v-loading="loading">
    <div class="page-header">
      <div class="header-title">
        <h2>活动审核</h2>
        <el-tag v-if="detail" :type="statusType" size="large">
          {{ statusText }}
        </el-tag>
      </div>
      <div class="header-actions">
        <el-button @click="goBack">返回列表</el-button>
      </div>
    </div>

    <el-card v-if="detail" class="detail-card" shadow="never">
      <template #header>
        <div class="detail-header">
          <div class="title-area">
            <div class="activity-title">{{ detail.title }}</div>
            <div class="sub-info">
              <span>组织者：{{ organizerName }}</span>
              <span class="dot">·</span>
              <span>创建时间：{{ formatDate(detail.createTime) }}</span>
            </div>
          </div>
        </div>
      </template>

      <div v-if="detail.coverImage" class="cover">
        <el-image :src="getImageUrl(detail.coverImage)" fit="cover" class="cover-image" />
      </div>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="活动时间">
          {{ formatDate(detail.startTime) }} - {{ formatDate(detail.endTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="报名时间">
          {{ formatDate(detail.registrationStartTime) }} - {{ formatDate(detail.registrationEndTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="地点">{{ detail.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分类ID">{{ detail.categoryId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="报名人数">
          {{ detail.currentParticipants ?? 0 }} / {{ detail.maxParticipants ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="标签">{{ detail.tags || '-' }}</el-descriptions-item>
        <el-descriptions-item label="浏览/收藏/评论">
          {{ detail.viewCount ?? 0 }} / {{ detail.favoriteCount ?? 0 }} / {{ detail.commentCount ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="是否需要报名审核">
          {{ detail.needReview ? '是' : '否' }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="desc-section">
        <div class="section-title">活动简介/内容</div>
        <div class="desc-content">{{ detail.description || '-' }}</div>
      </div>
    </el-card>

    <el-card class="action-card" shadow="never">
      <template #header>
        <div class="section-title">审核操作</div>
      </template>

      <el-form label-width="90px">
        <el-form-item label="审核意见">
          <el-input
            v-model="form.opinion"
            type="textarea"
            :rows="6"
            placeholder="审核通过可选填；审核拒绝必须填写拒绝原因"
          />
        </el-form-item>
        <div class="action-row">
          <el-button type="success" :disabled="detail?.status !== 1" @click="handleApprove">
            审核通过
          </el-button>
          <el-button type="danger" :disabled="detail?.status !== 1" @click="handleReject">
            审核拒绝
          </el-button>
          <el-button @click="goBack">返回列表</el-button>
        </div>
        <div v-if="detail?.status !== 1" class="hint">
          当前活动不处于“待审核”状态，已禁止重复审核操作。
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.activity-review {
  padding: 0;
}

.page-header {
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.detail-card,
.action-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
  background: var(--bg-primary);
}

.action-card {
  margin-top: 16px;
}

.activity-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.4;
}

.sub-info {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.dot {
  margin: 0 8px;
  color: var(--border-color);
}

.cover {
  width: 100%;
  height: 260px;
  margin-bottom: 18px;
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.cover-image {
  width: 100%;
  height: 100%;
}

.desc-section {
  margin-top: 18px;
  padding: 14px;
  background: var(--primary-bg);
  border: 1px solid var(--primary-bg-hover);
  border-radius: var(--radius-lg);
}

.section-title {
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 10px;
}

.desc-content {
  white-space: pre-wrap;
  color: var(--text-regular);
  line-height: 1.7;
}

.action-row {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.hint {
  margin-top: 10px;
  color: var(--text-secondary);
  font-size: 12px;
}
</style>

