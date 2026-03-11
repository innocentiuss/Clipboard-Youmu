<template>
  <el-container class="dashboard-container">
    <el-header class="dashboard-header glass-header">
      <div class="header-content">
        <div class="logo-myon" title="半灵 (Myon)"></div>
        <h2>Youmu Clipboard <span class="subtitle">楼观剑影 · 云端同步领域 (Phantom Edge)</span></h2>
      </div>
    </el-header>
    <el-main class="dashboard-main">
      <el-row :gutter="24" class="top-zone">
        <el-col :xs="24" :sm="12" :md="12" :lg="12" class="mb-24">
          <el-card shadow="never" class="glass-card top-card image-card" :body-style="{ padding: '24px', display: 'flex', flexDirection: 'column', boxSizing: 'border-box' }">
            <template #header>
              <div class="card-header">
                <span class="header-title">
                  <span class="icon-wrap">🖼️</span> 图片区
                </span>
              </div>
            </template>
            <div class="image-wrapper glass-panel">
              <el-image v-if="hasImage" :src="imgUrl" :key="imgUrl" fit="scale-down" class="uploaded-image">
                <template #error>
                  <div class="image-slot err-slot">
                    <span class="empty-icon text-2xl">⚠️</span>
                    <span>图片加载失败</span>
                  </div>
                </template>
              </el-image>
              <div v-else class="image-slot empty-slot">
                <span class="empty-icon text-3xl">📸</span>
                <span>暂无图片</span>
              </div>
            </div>
            <div class="upload-actions">
              <div class="image-btn-row">
                <el-upload
                    class="upload-demo action-btn upload-btn"
                    action="/api/picture"
                    :on-success="uploadSucceed"
                    :on-error="uploadFailed"
                    accept="image/png, image/jpeg"
                    :show-file-list="false"
                    :disabled="imageRemoving"
                >
                  <el-button class="w-100 glass-btn primary-btn">⬆️ 上传图片</el-button>
                </el-upload>
                <el-button @click="downloadImage" :disabled="!hasImage || imageRemoving" class="glass-btn success-btn action-btn download-btn">⬇️ 下载</el-button>
                <el-button
                  @click="removeImage"
                  :disabled="!hasImage || imageRemoving"
                  :loading="imageRemoving"
                  class="glass-btn danger-btn action-btn remove-btn"
                >
                  🗑️ 移除
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="12" :lg="12" class="mb-24">
          <el-card shadow="never" class="glass-card top-card file-card" :body-style="{ padding: '24px', display: 'flex', flexDirection: 'column', boxSizing: 'border-box' }">
            <template #header>
              <div class="card-header">
                <span class="header-title">
                  <span class="icon-wrap">📦</span> 文件区
                </span>
              </div>
            </template>
            <div class="file-wrapper glass-panel">
              <div class="file-slot" :class="{ 'empty-slot': !hasFile }">
                <span class="empty-icon text-3xl">{{ hasFile ? '📄' : '🗂️' }}</span>
                <span>{{ selectedFileName || '暂无文件' }}</span>
                <br>
                <span v-if="hasFile && fileUploadedAtText()" class="file-helper-text">{{ fileUploadedAtText() }}</span>
              </div>
            </div>
            <div class="upload-actions">
              <div class="image-btn-row">
                <input ref="fileInputRef" type="file" class="hidden-file-input" @change="handleFileSelected" />
                <el-button @click="selectFile" class="w-100 glass-btn primary-btn action-btn upload-btn" :loading="fileUploading" :disabled="fileRemoving">⬆️ 上传文件</el-button>
                <el-button @click="downloadFile" class="glass-btn success-btn action-btn download-btn" :disabled="!hasFile || fileUploading || fileRemoving">⬇️ 下载</el-button>
                <el-button
                  @click="removeFile"
                  class="glass-btn danger-btn action-btn remove-btn"
                  :disabled="!hasFile || fileUploading || fileRemoving"
                  :loading="fileRemoving"
                >
                  🗑️ 移除
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="24" class="text-zone">
        <el-col v-for="(board, index) in textboards" :key="index" :xs="24" :sm="12" :md="12" :lg="12" class="mb-24">
          <el-card shadow="never" class="glass-card text-card" :body-style="{ padding: '24px', display: 'flex', flexDirection: 'column', height: '100%' }">
            <template #header>
              <div class="card-header">
                <div class="header-title-with-badge">
                  <span class="header-title">文本区</span>
                  <span class="index-badge">NO.{{ index + 1 }}</span>
                </div>
                <el-button class="glass-btn-link" type="primary" link @click="openHistory(index)">
                  ⏱️ 历史
                </el-button>
              </div>
            </template>
            <div class="input-wrapper">
              <el-input
                  v-model="board.currentText"
                  :rows="6"
                  type="textarea"
                  placeholder="在这里输入需要云端同步的文字，支持设备互传..."
                  class="glass-input"
                  clearable
              />
            </div>
            <div class="card-actions">
              <el-button @click="postText(index, board.currentText)" class="glass-btn submit-btn" :loading="board.loading">
                ☁️ 同步
              </el-button>
              <el-button @click="copyText(board.currentText)" class="glass-btn copy-btn">
                📋 复制
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-main>

    <!-- 历史记录抽屉 -->
    <el-drawer
      v-model="historyDrawerVisible"
      :title="`文本区 NO.${currentHistoryIndex + 1} - 历史快照`"
      direction="rtl"
      size="35%"
      class="glass-drawer"
    >
      <div class="drawer-content-scroll">
        <el-timeline v-if="textboards[currentHistoryIndex]?.history?.length > 0" class="custom-timeline">
          <el-timeline-item
            v-for="(item, hIndex) in textboards[currentHistoryIndex].history"
            :key="hIndex"
            :type="hIndex === 0 ? 'primary' : 'info'"
            :hollow="hIndex !== 0"
            :size="hIndex === 0 ? 'large' : 'normal'"
            :color="hIndex === 0 ? '#10b981' : '#94a3b8'"
          >
            <div class="history-item glass-history-item">
              <div class="history-header">
                <div class="history-badge" v-if="hIndex === 0">✨ 当前生效值</div>
                <div class="history-badge-old" v-else>🕰️ 快照 #{{ hIndex }}</div>
              </div>
              <div class="history-text glass-panel">{{ item }}</div>
              <div class="history-actions">
                <el-button size="small" class="glass-btn copy-btn-small" @click="copyText(item)">📋 复制</el-button>
                <el-button size="small" class="glass-btn warning-btn-small" @click="restoreText(item)" v-if="hIndex !== 0">↩️ 恢复至面板</el-button>
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无历史记录" class="glass-empty" />
      </div>
    </el-drawer>
  </el-container>
</template>

<script lang="ts">
import { defineComponent, ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from "element-plus"
import axios from "axios"

interface FileMetadata {
  originalName: string
  contentType: string
  sizeBytes: number
  uploadedAt: string
}

export default defineComponent({
  setup() {
    const imgUrl = ref('/api/picture')
    const hasImage = ref(false)
    const selectedFileName = ref('')
    const hasFile = ref(false)
    const fileUploading = ref(false)
    const fileRemoving = ref(false)
    const imageRemoving = ref(false)
    const fileInputRef = ref<HTMLInputElement | null>(null)
    const fileMeta = ref<FileMetadata | null>(null)

    const resetImageState = () => {
      imgUrl.value = '/api/picture'
      hasImage.value = false
    }

    const resetFileState = () => {
      hasFile.value = false
      fileMeta.value = null
      selectedFileName.value = ''
    }

    // 检查服务器上是否有图片
    axios.head('/api/picture').then(() => {
      hasImage.value = true
    }).catch(() => {
      resetImageState()
    })

    // 管理6个文字剪切板的状态
    const textboards = reactive([
      { currentText: '', history: [] as string[], loading: false },
      { currentText: '', history: [] as string[], loading: false },
      { currentText: '', history: [] as string[], loading: false },
      { currentText: '', history: [] as string[], loading: false },
      { currentText: '', history: [] as string[], loading: false },
      { currentText: '', history: [] as string[], loading: false }
    ])

    const historyDrawerVisible = ref(false)
    const currentHistoryIndex = ref(0)

    const getText = () => {
      axios.get('/api/getAll').then(res => {
        const code = res.data.code
        if (code == 200) {
          const data_arr = res.data.msg
          for (let i = 0; i < 6; i++) {
            if (data_arr[i] && data_arr[i].length > 0) {
              textboards[i].history = data_arr[i]
              textboards[i].currentText = data_arr[i][0]
            }
          }
        }
        else {
          ElMessage({
            message: '获取数据失败',
            type: 'warning',
          })
        }
      }).catch(() => {
        ElMessage.error('网络错误，获取剪切板失败')
      })
    }

    const postText = (fromId: number, content: string) => {
      textboards[fromId].loading = true
      axios.post('/api/post', {
        fromId: fromId,
        message: content
      }).then(res => {
        textboards[fromId].loading = false
        const code = res.data.code
        if (code == 200) {
          const data_arr = res.data.msg
          textboards[fromId].history = data_arr
          textboards[fromId].currentText = data_arr[0]
          ElMessage({
            message: `剪切板 ${fromId + 1}号 同步成功 :)`,
            type: 'success',
          })
        }
        else {
          ElMessage({
            message: '文字同步失败 :(',
            type: 'warning',
          })
        }
      }).catch(() => {
        textboards[fromId].loading = false
        ElMessage.error('网络错误，同步失败')
      })
    }

    function uploadSucceed() {
      const timestamp = new Date().getTime()
      imgUrl.value = '/api/picture?' + timestamp
      hasImage.value = true
      ElMessage({
        message: '图片上传成功 :)',
        type: 'success',
      })
    }

    function uploadFailed() {
      ElMessage({
        message: '图片上传失败 :(',
        type: 'warning',
      })
    }

    // 打开历史记录面板
    const openHistory = (index: number) => {
      currentHistoryIndex.value = index
      historyDrawerVisible.value = true
    }

    // 将历史记录恢复到输入框（不直接提交，给用户确认机会）
    const restoreText = (text: string) => {
      textboards[currentHistoryIndex.value].currentText = text
      ElMessage.success('已恢复到输入框，请点击"同步"按钮以保存')
      historyDrawerVisible.value = false
    }

    // 复制文本到系统剪切板
    const copyText = (text: string) => {
      if (!text) {
        ElMessage.warning('内容为空')
        return
      }
      navigator.clipboard.writeText(text).then(() => {
        ElMessage.success('已复制到系统剪切板')
      }).catch(() => {
        ElMessage.error('复制失败，请手动选择复制')
      })
    }

    // 下载当前图片
    const downloadImage = () => {
      if (!imgUrl.value) return;
      // Fetch an image and download it to avoid opening a new tab incorrectly
      axios.get(imgUrl.value, { responseType: 'blob' }).then(res => {
        const url = window.URL.createObjectURL(new Blob([res.data]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', 'clipboard-image.png');
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
      }).catch(() => {
        ElMessage.warning('图片下载失败或跨域被拦截')
        // Fallback: open in new tab
        window.open(imgUrl.value, '_blank')
      })
    }

    const removeImage = async () => {
      if (!hasImage.value || imageRemoving.value) {
        return
      }

      try {
        await ElMessageBox.confirm('移除后将无法继续下载当前图片，是否继续？', '移除图片', {
          confirmButtonText: '移除',
          cancelButtonText: '取消',
          type: 'warning'
        })
      } catch {
        return
      }

      imageRemoving.value = true
      try {
        const res = await axios.delete('/api/picture')
        if (res.data.code === 200) {
          resetImageState()
          ElMessage.success(res.data.msg || '图片已移除')
          return
        }

        ElMessage.warning(res.data.msg || '图片移除失败')
      } catch {
        ElMessage.error('图片移除失败')
      } finally {
        imageRemoving.value = false
      }
    }

    const loadFileMeta = (silent = true) => {
      axios.get('/api/file/meta').then(res => {
        if (res.data.code === 200) {
          fileMeta.value = res.data.msg as FileMetadata
          hasFile.value = true
          selectedFileName.value = fileMeta.value.originalName
          return
        }

        resetFileState()
        if (!silent && res.data.msg) {
          ElMessage.warning(res.data.msg)
        }
      }).catch(() => {
        resetFileState()
        if (!silent) {
          ElMessage.error('文件信息获取失败')
        }
      })
    }

    const selectFile = () => {
      if (fileUploading.value) return
      fileInputRef.value?.click()
    }

    const extractFilename = (contentDisposition: string | undefined) => {
      if (!contentDisposition) return ''

      const utfMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
      if (utfMatch?.[1]) {
        return decodeURIComponent(utfMatch[1])
      }

      const basicMatch = contentDisposition.match(/filename="?([^"]+)"?/i)
      return basicMatch?.[1] || ''
    }

    const extractBlobMessage = async (payload: unknown) => {
      if (!(payload instanceof Blob)) return ''

      try {
        const text = await payload.text()
        if (!text) return ''
        const parsed = JSON.parse(text)
        return parsed.message || parsed.msg || ''
      } catch {
        return ''
      }
    }

    const removeFile = async () => {
      if (!hasFile.value || fileUploading.value || fileRemoving.value) {
        return
      }

      try {
        await ElMessageBox.confirm('移除后将无法继续下载当前文件，是否继续？', '移除文件', {
          confirmButtonText: '移除',
          cancelButtonText: '取消',
          type: 'warning'
        })
      } catch {
        return
      }

      fileRemoving.value = true
      try {
        const res = await axios.delete('/api/file')
        if (res.data.code === 200) {
          resetFileState()
          ElMessage.success(res.data.msg || '文件已移除')
          return
        }

        ElMessage.warning(res.data.msg || '文件移除失败')
      } catch (error: any) {
        const blobMessage = await extractBlobMessage(error?.response?.data)
        const responseMessage = error?.response?.data?.msg || error?.response?.data?.message
        ElMessage.error(blobMessage || responseMessage || '文件移除失败')
      } finally {
        fileRemoving.value = false
      }
    }

    const uploadFile = (file: File) => {
      const formData = new FormData()
      formData.append('file', file)

      fileUploading.value = true
      axios.post('/api/file', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      }).then(res => {
        if (res.data.code === 200) {
          fileMeta.value = res.data.msg as FileMetadata
          hasFile.value = true
          selectedFileName.value = fileMeta.value.originalName
          ElMessage.success('文件上传成功')
          return
        }

        ElMessage.warning(res.data.msg || '文件上传失败')
      }).catch(async (error) => {
        const blobMessage = await extractBlobMessage(error?.response?.data)
        const responseMessage = error?.response?.data?.msg || error?.response?.data?.message
        ElMessage.error(blobMessage || responseMessage || '文件上传失败')
      }).finally(() => {
        fileUploading.value = false
      })
    }

    const handleFileSelected = (event: Event) => {
      const input = event.target as HTMLInputElement
      const file = input.files?.[0]
      if (!file) return

      uploadFile(file)
      input.value = ''
    }

    const downloadFile = async () => {
      if (!hasFile.value) {
        ElMessage.warning('暂无可下载文件')
        return
      }

      try {
        const res = await axios.get('/api/file', { responseType: 'blob' })
        const downloadName = extractFilename(res.headers['content-disposition']) || selectedFileName.value || 'clipboard-file.dat'
        const url = window.URL.createObjectURL(res.data)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', downloadName)
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
      } catch (error: any) {
        const blobMessage = await extractBlobMessage(error?.response?.data)
        ElMessage.error(blobMessage || '文件下载失败')
      }
    }

    const formatFileSize = (sizeBytes: number | undefined) => {
      if (!sizeBytes || sizeBytes <= 0) return ''

      const units = ['B', 'KB', 'MB', 'GB']
      let size = sizeBytes
      let unitIndex = 0

      while (size >= 1024 && unitIndex < units.length - 1) {
        size /= 1024
        unitIndex++
      }

      return `${size.toFixed(size >= 10 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`
    }

    const formatTimestamp = (timestamp: string | undefined) => {
      if (!timestamp) return ''

      const date = new Date(timestamp)
      if (Number.isNaN(date.getTime())) return ''

      return new Intl.DateTimeFormat('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      }).format(date)
    }

    const fileUploadedAtText = () => {
      if (!hasFile.value || !fileMeta.value) {
        return ''
      }

      const formatted = formatTimestamp(fileMeta.value.uploadedAt)
      return formatted ? `上传时间：${formatted}` : ''
    }

    // 初始化获取数据
    getText()
    loadFileMeta()

    return {
      textboards, imgUrl, hasImage, selectedFileName, hasFile, fileUploading, fileRemoving, imageRemoving, fileInputRef,
      uploadFailed, uploadSucceed, getText, postText,
      historyDrawerVisible, currentHistoryIndex, openHistory, restoreText, copyText, downloadImage,
      removeImage, selectFile, downloadFile, removeFile, handleFileSelected, fileUploadedAtText
    }
  }
});
</script>

<style scoped>
/* 全局动态背景 */
.dashboard-container {
  min-height: 100vh;
  /* Emerald Glass Mesh Gradient */
  background-color: #e0f2f1;
  background-image: 
    radial-gradient(at 0% 0%, hsla(160, 50%, 85%, 1) 0, transparent 50%), 
    radial-gradient(at 50% 0%, hsla(140, 40%, 90%, 1) 0, transparent 50%), 
    radial-gradient(at 100% 0%, hsla(180, 50%, 85%, 1) 0, transparent 50%),
    radial-gradient(at 0% 100%, hsla(170, 40%, 85%, 1) 0, transparent 50%),
    radial-gradient(at 100% 100%, hsla(150, 40%, 85%, 1) 0, transparent 50%);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  color: #1e293b;
}

/* 隐藏原有生硬滚动条，换用平滑模式 */
::selection { background: rgba(16, 185, 129, 0.3); color: #064e3b; }

/* 头部透明悬浮态 */
.dashboard-header.glass-header {
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.03);
  display: flex;
  align-items: center;
  padding: 0 40px;
  height: 70px !important;
  z-index: 10;
  position: sticky;
  top: 0;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo-myon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, #ffffff 10%, #a7f3d0 50%, #10b981 100%);
  box-shadow: 0 0 16px rgba(167, 243, 208, 0.8), 0 0 24px rgba(16, 185, 129, 0.4);
  position: relative;
  animation: float-myon 4s ease-in-out infinite;
}

.logo-myon::after {
  content: '';
  position: absolute;
  bottom: -2px;
  right: -6px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, #ffffff 10%, #a7f3d0 50%, #10b981 100%);
  box-shadow: 0 0 10px rgba(167, 243, 208, 0.8);
  animation: float-tail 4s ease-in-out infinite reverse;
}

@keyframes float-myon {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-4px) scale(1.02); }
}

@keyframes float-tail {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(2px, -2px); }
}

.dashboard-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.5px;
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.subtitle {
  font-size: 14px;
  font-weight: 600;
  background: linear-gradient(135deg, #059669, #34d399);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 4px 12px rgba(16, 185, 129, 0.2);
  letter-spacing: 0.5px;
}

.dashboard-main {
  padding: 40px;
  max-width: 1800px;
  margin: 0 auto;
  width: 100%;
}

.mb-24 {
  margin-bottom: 24px;
}

.top-zone,
.text-zone {
  margin-bottom: 0;
}

/* =========================================
   毛玻璃卡片核心样式 Glassmorphism
========================================= */
.glass-card {
  background: rgba(255, 255, 255, 0.45) !important;
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.8) !important;
  border-radius: 20px !important;
  box-shadow: 0 10px 40px -10px rgba(0,0,0,0.08) !important;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.glass-card:hover {
  transform: translateY(-6px);
  background: rgba(255, 255, 255, 0.55) !important;
  box-shadow: 0 20px 40px -10px rgba(16, 185, 129, 0.15), 0 0 20px rgba(255,255,255,0.8) inset !important;
  border: 1px solid rgba(255, 255, 255, 1) !important;
}

.top-card {
  height: 100%;
}

.top-card :deep(.el-card__body) {
  flex: 1;
}

/* 消除 el-card 本身的边框和背景 */
.image-card :deep(.el-card__body),
.file-card :deep(.el-card__body),
.text-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title-with-badge {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title {
  font-weight: 700;
  font-size: 17px;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 6px;
}

.index-badge {
  background: linear-gradient(135deg, #10b981, #059669);
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

/* =========================================
   内嵌玻璃面板 (图片/输入框区)
========================================= */
.glass-panel {
  background: rgba(255, 255, 255, 0.4);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: inset 0 2px 10px rgba(0,0,0,0.02);
}

.image-wrapper {
  height: 220px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20px;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
  transition: all 0.3s ease;
}

.file-wrapper {
  height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  padding: 24px;
  box-sizing: border-box;
  overflow: hidden;
  transition: all 0.3s ease;
}

.image-wrapper:hover {
  background: rgba(255, 255, 255, 0.6);
}

.file-wrapper:hover {
  background: rgba(255, 255, 255, 0.6);
}

.uploaded-image {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  transition: transform 0.4s ease;
}

.uploaded-image:hover {
  transform: scale(1.03);
}

.empty-slot, .err-slot {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 12px;
  color: #94a3b8;
  font-size: 15px;
  font-weight: 500;
}

.empty-icon {
  font-size: 48px;
  color: #cbd5e1;
  transition: all 0.3s ease;
}

.file-slot {
  text-align: center;
}

.file-helper-text {
  font-size: 13px;
  color: #94a3b8;
  line-height: 1.5;
}

.hidden-file-input {
  display: none;
}

.text-2xl { font-size: 32px; }
.text-3xl { font-size: 48px; }

.image-wrapper:hover .empty-icon {
  transform: scale(1.1);
  color: #94a3b8;
}

.upload-actions {
  margin-top: auto;
  flex-shrink: 0;
}

.image-btn-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.action-btn {
  min-width: 0;
}

.upload-btn {
  flex: 1.8 1 0;
}

.download-btn {
  flex: 1.1 1 0;
}

.remove-btn {
  flex: 0.8 1 0;
}

.upload-demo :deep(.el-upload) {
  width: 100%;
  display: block;
}

.image-btn-row :deep(.el-button + .el-button) {
  margin-left: 0;
}

.w-100 {
  width: 100%;
}

/* =========================================
   按钮定制 (Glass Buttons)
========================================= */
.glass-btn {
  border-radius: 12px !important;
  font-weight: 600 !important;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1) !important;
  border: 1px solid rgba(255,255,255,0.8) !important;
  backdrop-filter: blur(10px);
  height: 40px !important;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.glass-btn:hover:not(.is-disabled) {
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.08);
}

.glass-btn:active:not(.is-disabled) {
  transform: translateY(0) scale(0.98);
}

.primary-btn, .submit-btn {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%) !important;
  color: white !important;
  border: none !important;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.25) !important;
}

.primary-btn:hover, .submit-btn:hover {
  box-shadow: 0 8px 20px rgba(16, 185, 129, 0.4) !important;
}

.success-btn, .copy-btn {
  background: rgba(255, 255, 255, 0.7) !important;
  color: #0f172a !important;
}

.success-btn:hover, .copy-btn:hover {
  background: rgba(255, 255, 255, 1) !important;
}

.danger-btn {
  background: rgba(239, 68, 68, 0.12) !important;
  color: #b91c1c !important;
  border-color: rgba(248, 113, 113, 0.28) !important;
}

.danger-btn:hover {
  background: rgba(239, 68, 68, 0.2) !important;
  box-shadow: 0 8px 20px rgba(239, 68, 68, 0.12) !important;
}

.glass-btn-link {
  font-weight: 600 !important;
  color: #10b981 !important;
  transition: all 0.2s;
  font-size: 14px;
}

.glass-btn-link:hover {
  color: #059669 !important;
  transform: translateY(-1px);
}

/* =========================================
   输入框唯美光晕
========================================= */
.input-wrapper {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.glass-input :deep(.el-textarea__inner) {
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.6);
  resize: none;
  font-family: inherit;
  padding: 16px;
  font-size: 15px;
  color: #1e293b;
  line-height: 1.6;
  box-shadow: inset 0 2px 6px rgba(0,0,0,0.02);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  height: 100% !important;
}

.glass-input :deep(.el-textarea__inner:hover) {
  background: rgba(255, 255, 255, 0.8);
}

.glass-input :deep(.el-textarea__inner:focus) {
  background: rgba(255, 255, 255, 1);
  border-color: #34d399;
  box-shadow: 0 0 0 4px rgba(52, 211, 153, 0.2), inset 0 2px 6px rgba(0,0,0,0.01);
  outline: none;
}

.glass-input :deep(.el-textarea__inner::placeholder) {
  color: #94a3b8;
}

.card-actions {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.submit-btn {
  flex-grow: 1;
}

/* =========================================
   历史记录抽屉 (Glass Drawer)
========================================= */
.glass-drawer {
  background: rgba(248, 250, 252, 0.6) !important;
  backdrop-filter: blur(40px) !important;
  -webkit-backdrop-filter: blur(40px) !important;
}

/* 去除默认抽屉纯白背景 */
.glass-drawer :deep(.el-drawer) {
  background: rgba(255, 255, 255, 0.4) !important;
  box-shadow: -10px 0 40px rgba(0,0,0,0.1);
}

.glass-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 24px 30px;
  font-weight: 700;
  color: #0f172a;
  border-bottom: 1px solid rgba(255,255,255,0.5);
}

.drawer-content-scroll {
  padding: 24px 30px;
  height: calc(100% - 70px);
  overflow-y: auto;
}

.custom-timeline {
  padding-left: 4px;
}

.glass-history-item {
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  padding: 20px;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(0,0,0,0.02);
}

.glass-history-item:hover {
  background: rgba(255, 255, 255, 0.95);
  transform: translateX(6px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.06);
  border-color: rgba(255, 255, 255, 1);
}

.history-header {
  margin-bottom: 12px;
}

.history-badge {
  display: inline-block;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(5, 150, 105, 0.1));
  color: #059669;
  border: 1px solid rgba(16, 185, 129, 0.2);
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: 600;
}

.history-badge-old {
  display: inline-block;
  background: rgba(148, 163, 184, 0.1);
  color: #64748b;
  border: 1px solid rgba(148, 163, 184, 0.2);
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: 500;
}

.history-text.glass-panel {
  padding: 16px;
  font-size: 14px;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.6;
  margin-bottom: 16px;
  max-height: 250px;
  overflow-y: auto;
  border-radius: 12px;
}

.history-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.copy-btn-small, .warning-btn-small {
  height: 32px !important;
  border-radius: 8px !important;
  font-size: 13px !important;
}

.warning-btn-small {
  background: rgba(245, 158, 11, 0.1) !important;
  color: #d97706 !important;
  border-color: rgba(245, 158, 11, 0.2) !important;
}

.warning-btn-small:hover {
  background: rgba(245, 158, 11, 0.2) !important;
}

/* 自定义滚动条美化 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}
::-webkit-scrollbar-thumb {
  border-radius: 4px;
  background: rgba(148, 163, 184, 0.4);
}
::-webkit-scrollbar-thumb:hover {
  background: rgba(148, 163, 184, 0.6);
}
::-webkit-scrollbar-track {
  background: transparent;
}

/* Drawer内部空状态 */
.glass-empty :deep(.el-empty__description) p {
  color: #94a3b8;
  font-weight: 500;
}

@media (max-width: 768px) {
  .dashboard-header.glass-header {
    padding: 0 20px;
  }

  .dashboard-main {
    padding: 24px 16px;
  }

  .header-content {
    gap: 12px;
  }

  .dashboard-header h2 {
    font-size: 18px;
    flex-direction: column;
    align-items: flex-start;
    gap: 2px;
  }

  .image-btn-row {
    flex-direction: column;
  }

  .action-btn,
  .upload-btn,
  .download-btn,
  .remove-btn {
    width: 100%;
  }
}
</style>
