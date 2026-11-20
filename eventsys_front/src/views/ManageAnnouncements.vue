<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listAdminAnnouncements, recallAnnouncement, type Notification } from '@/api/notification'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const records = ref<Notification[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const scopeLabel = (s?: string) => {
  if (s === 'USER') return '仅普通用户'
  if (s === 'ADMIN') return '仅管理员'
  return '全部用户'
}

const statusLabel = (s?: number) => {
  if (s === 0) return '草稿'
  if (s === 2) return '已撤回'
  return '已发布'
}

const formatTime = (time: string | null | undefined) => {
  if (!time) return '-'
  const date = new Date(time)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  const sec = String(date.getSeconds()).padStart(2, '0')
  return `${y}/${m}/${d} ${h}:${min}:${sec}`
}

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await listAdminAnnouncements({ page: page.value, size: size.value })
    if (res && Array.isArray(res.records)) {
      records.value = res.records
      total.value = res.total || 0
    } else {
      records.value = []
      total.value = 0
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('加载公告列表失败')
  } finally {
    loading.value = false
  }
}

const goCreate = () => router.push('/notifications/send-announcement')

const goEdit = (id: number) => {
  router.push(`/notifications/send-announcement?id=${id}`)
}

const handleRecall = async (row: Notification) => {
  try {
    await ElMessageBox.confirm('确定撤回？撤回后普通用户将不可见。', '确认撤回', { type: 'warning' })
    await recallAnnouncement(row.id)
    ElMessage.success('已撤回')
    fetchData()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error('撤回失败')
    }
  }
}

const handlePageChange = (p: number) => {
  page.value = p
  fetchData()
}

onMounted(fetchData)
</script>

<template>
  <div class="manage-page">
    <div class="page-header">
      <div class="back-link" @click="router.push('/notifications')">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回消息通知</span>
      </div>
      <div class="title-row">
        <h2>公告管理</h2>
        <el-button type="danger" @click="goCreate">新建公告</el-button>
      </div>
    </div>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="records" style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            {{ statusLabel(row.status) }}
          </template>
        </el-table-column>
        <el-table-column label="范围" width="120">
          <template #default="{ row }">
            {{ scopeLabel(row.publishScope) }}
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="70">
          <template #default="{ row }">
            {{ row.isPinned === 1 ? '是' : '否' }}
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.publishTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="goEdit(row.id)">编辑</el-button>
            <el-button
              v-if="row.status === 1"
              type="danger"
              link
              size="small"
              @click="handleRecall(row)"
            >
              撤回
            </el-button>
          </template>
        </el-table-column>
        <!-- 空数据状态 -->
        <template #empty>
          <el-empty description="暂无公告数据" :image-size="80" />
        </template>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.manage-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 16px 48px;
}

.page-header {
  margin-bottom: 20px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 15px;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  margin-bottom: 12px;
}

.back-link:hover {
  color: var(--color-primary);
  background-color: var(--primary-bg);
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.title-row h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.table-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
}

.pager {
  display: flex;
  justify-content: center;
  padding: 16px 0 0;
}
</style>
