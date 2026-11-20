<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus, Delete } from '@element-plus/icons-vue'
import { getActivityDetail } from '../api/activity'
import { createResult, updateResult, getResultByActivityId, uploadResultImages } from '../api/result'
import MarkdownIt from 'markdown-it'

const route = useRoute()
const router = useRouter()

const activityId = computed(() => Number(route.query.activityId))
const resultId = computed(() => route.query.resultId ? Number(route.query.resultId) : null)
const isEdit = computed(() => !!resultId.value)

// 页面状态
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const activityTitle = ref('')

// 表单数据
const form = ref({
  title: '',
  content: '',
  images: [] as string[]
})

// Markdown 预览
const md = new MarkdownIt()
const previewContent = computed(() => {
  return md.render(form.value.content || '')
})

// 获取活动详情
const fetchActivityDetail = async () => {
  if (!activityId.value) return
  try {
    const res: any = await getActivityDetail(activityId.value)
    activityTitle.value = res.title || ''
  } catch (error) {
    console.error('获取活动详情失败:', error)
  }
}

// 获取成果展示贴详情（编辑模式）
const fetchResultDetail = async () => {
  if (!isEdit.value || !activityId.value) return
  loading.value = true
  try {
    const res: any = await getResultByActivityId(activityId.value)
    if (res) {
      form.value = {
        title: res.title || '',
        content: res.content || '',
        images: parseImages(res.images)
      }
    }
  } catch (error) {
    console.error('获取成果展示贴详情失败:', error)
    ElMessage.error('获取成果展示贴详情失败')
  } finally {
    loading.value = false
  }
}

// 解析图片URL列表
const parseImages = (images: string | undefined): string[] => {
  if (!images) return []
  try {
    const parsed = JSON.parse(images)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

// 处理图片上传
const handleImageUpload = async (file: File) => {
  if (!activityId.value) return false
  
  // 检查图片数量限制
  if (form.value.images.length >= 9) {
    ElMessage.warning('最多只能上传9张图片')
    return false
  }
  
  // 检查文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    return false
  }
  
  // 检查文件大小（10MB）
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('单张图片大小不能超过10MB')
    return false
  }
  
  uploading.value = true
  try {
    const res: any = await uploadResultImages(activityId.value, [file])
    if (res && Array.isArray(res)) {
      form.value.images.push(...res)
      ElMessage.success('图片上传成功')
    }
  } catch (error) {
    console.error('图片上传失败:', error)
    ElMessage.error('图片上传失败')
  } finally {
    uploading.value = false
  }
  return false
}

// 移除图片
const removeImage = (index: number) => {
  form.value.images.splice(index, 1)
}

// 获取完整图片URL
const getImageUrl = (url: string): string => {
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  if (url.startsWith('/api/')) {
    return url
  }
  return '/api' + url
}

// 保存成果展示贴
const handleSave = async () => {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  
  if (!activityId.value) {
    ElMessage.error('活动ID不存在')
    return
  }
  
  saving.value = true
  try {
    const imagesJson = JSON.stringify(form.value.images)
    
    if (isEdit.value && resultId.value) {
      // 编辑模式
      await updateResult(
        resultId.value,
        form.value.title,
        form.value.content,
        imagesJson
      )
      ElMessage.success('更新成功')
    } else {
      // 创建模式
      await createResult(
        activityId.value,
        form.value.title,
        form.value.content,
        imagesJson
      )
      ElMessage.success('创建成功')
    }
    
    // 跳转到成果展示贴详情页
    router.push(`/achievement/${activityId.value}`)
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 取消编辑
const handleCancel = () => {
  ElMessageBox.confirm('确定要放弃编辑吗？未保存的内容将丢失。', '确认取消', {
    confirmButtonText: '确定',
    cancelButtonText: '继续编辑',
    type: 'warning'
  }).then(() => {
    goBack()
  }).catch(() => {})
}

onMounted(() => {
  if (!activityId.value) {
    ElMessage.error('活动ID不存在')
    router.push('/events')
    return
  }
  
  fetchActivityDetail()
  if (isEdit.value) {
    fetchResultDetail()
  }
})
</script>

<template>
  <div v-loading="loading" class="edit-achievement-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="back-link" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </div>
      <h2 class="page-title">{{ isEdit ? '编辑成果展示贴' : '创建成果展示贴' }}</h2>
      <div class="activity-info" v-if="activityTitle">
        关联活动：{{ activityTitle }}
      </div>
    </div>
    
    <!-- 编辑表单 -->
    <el-card class="edit-card" shadow="never">
      <el-form label-width="80px" class="achievement-form">
        <!-- 标题 -->
        <el-form-item label="标题" required>
          <el-input 
            v-model="form.title" 
            placeholder="请输入成果展示贴标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        
        <!-- 内容编辑 -->
        <el-form-item label="内容">
          <div class="content-editor">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="12"
              placeholder="请输入内容，支持 Markdown 格式"
            />
            <div class="markdown-tips">
              支持 Markdown 格式：# 标题、**粗体**、*斜体*、- 列表、```代码块``` 等
            </div>
          </div>
        </el-form-item>
        
        <!-- 实时预览 -->
        <el-form-item label="预览">
          <div class="preview-box" v-html="previewContent"></div>
        </el-form-item>
        
        <!-- 图片上传 -->
        <el-form-item label="图片">
          <div class="image-upload-section">
            <div class="image-list">
              <div v-for="(img, index) in form.images" :key="index" class="image-item">
                <el-image 
                  :src="getImageUrl(img)" 
                  fit="cover"
                  class="uploaded-image"
                />
                <div class="image-actions">
                  <el-button 
                    type="danger" 
                    size="small" 
                    circle
                    @click="removeImage(index)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
              
              <!-- 上传按钮 -->
              <el-upload
                v-if="form.images.length < 9"
                class="image-uploader"
                :show-file-list="false"
                :before-upload="handleImageUpload"
                accept="image/*"
              >
                <div class="upload-trigger" v-loading="uploading">
                  <el-icon><Plus /></el-icon>
                  <span>上传图片</span>
                  <div class="upload-limit">{{ form.images.length }}/9</div>
                </div>
              </el-upload>
            </div>
            <div class="upload-hint">最多上传9张图片，单张不超过10MB</div>
          </div>
        </el-form-item>
        
        <!-- 操作按钮 -->
        <el-form-item>
          <div class="form-actions">
            <el-button @click="handleCancel">取消</el-button>
            <el-button type="primary" :loading="saving" @click="handleSave">
              {{ isEdit ? '保存修改' : '创建成果展示贴' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.edit-achievement-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  cursor: pointer;
  margin-bottom: 12px;
  transition: color 0.3s;
}

.back-link:hover {
  color: var(--color-primary);
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.activity-info {
  color: var(--text-secondary);
  font-size: 14px;
}

.edit-card {
  border-radius: var(--radius-lg);
}

.achievement-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--text-regular);
}

.content-editor {
  width: 100%;
}

.markdown-tips {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-muted);
}

.preview-box {
  min-height: 200px;
  max-height: 400px;
  overflow-y: auto;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
}

.preview-box :deep(h1) {
  font-size: 24px;
  font-weight: 600;
  margin: 16px 0 12px;
  color: var(--text-primary);
}

.preview-box :deep(h2) {
  font-size: 20px;
  font-weight: 600;
  margin: 14px 0 10px;
  color: var(--text-primary);
}

.preview-box :deep(h3) {
  font-size: 18px;
  font-weight: 600;
  margin: 12px 0 8px;
  color: var(--text-primary);
}

.preview-box :deep(p) {
  margin: 8px 0;
  line-height: 1.6;
  color: var(--text-regular);
}

.preview-box :deep(ul), .preview-box :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}

.preview-box :deep(li) {
  margin: 4px 0;
  color: var(--text-regular);
}

.preview-box :deep(code) {
  background: var(--bg-hover);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  font-family: monospace;
  font-size: 14px;
}

.preview-box :deep(pre) {
  background: var(--bg-hover);
  padding: 16px;
  border-radius: var(--radius-sm);
  overflow-x: auto;
}

.preview-box :deep(pre code) {
  background: none;
  padding: 0;
}

.preview-box :deep(blockquote) {
  border-left: 4px solid var(--color-primary);
  padding-left: 16px;
  margin: 8px 0;
  color: var(--text-secondary);
}

.image-upload-section {
  width: 100%;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.image-item {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.uploaded-image {
  width: 100%;
  height: 100%;
}

.image-actions {
  position: absolute;
  top: 4px;
  right: 4px;
  opacity: 0;
  transition: opacity 0.3s;
}

.image-item:hover .image-actions {
  opacity: 1;
}

.image-uploader {
  width: 120px;
  height: 120px;
}

.upload-trigger {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2px dashed var(--border-color);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.3s;
  color: var(--text-muted);
}

.upload-trigger:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.upload-trigger .el-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.upload-trigger span {
  font-size: 12px;
}

.upload-limit {
  font-size: 11px;
  margin-top: 4px;
}

.upload-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-muted);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}
</style>
