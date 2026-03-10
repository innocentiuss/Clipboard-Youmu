<template>
  <el-container class="dashboard-container">
    <el-header class="dashboard-header">
      <h2>YoumuClipboard - 云端剪切板</h2>
    </el-header>
    <el-main class="dashboard-main">
      <el-row :gutter="24">
        <!-- 图片剪切板 (占 1/4 到 1/3) -->
        <el-col :xs="24" :sm="24" :md="8" :lg="6" class="mb-24">
          <el-card shadow="hover" class="image-card" :body-style="{ padding: '20px', display: 'flex', flexDirection: 'column', height: '100%' }">
            <template #header>
              <div class="card-header">
                <span><el-icon><Picture /></el-icon> 简易图片剪切板</span>
              </div>
            </template>
            <div class="image-wrapper">
              <el-image v-if="imgUrl" :src="imgUrl" :key="imgUrl" fit="scale-down" class="uploaded-image">
                <template #error>
                  <div class="image-slot">
                    暂无图片或加载失败
                  </div>
                </template>
              </el-image>
            </div>
            <div class="upload-actions">
              <el-button-group class="w-100 d-flex">
                <el-upload
                    class="upload-demo flex-grow-1"
                    :action="'http://' + host + ':' + port + '/api/picture'"
                    :on-success="uploadSucceed"
                    :on-error="uploadFailed"
                    accept="image/png, image/jpeg"
                    :show-file-list="false"
                >
                  <el-button type="primary" class="w-100">上传图片</el-button>
                </el-upload>
                <el-button type="success" @click="downloadImage" :disabled="!imgUrl" style="margin-left: -1px;">下载图片</el-button>
              </el-button-group>
            </div>
          </el-card>
        </el-col>

        <!-- 文字剪切板 -->
        <el-col :xs="24" :sm="24" :md="16" :lg="18">
          <el-row :gutter="24">
            <el-col v-for="(board, index) in textboards" :key="index" :xs="24" :sm="12" :md="12" :lg="8" class="mb-24">
              <el-card shadow="hover" class="text-card" :body-style="{ padding: '20px', display: 'flex', flexDirection: 'column', height: '100%' }">
                <template #header>
                  <div class="card-header">
                    <span>文字剪切板 {{ index + 1 }}号</span>
                    <el-button type="primary" link @click="openHistory(index)">
                      历史记录
                    </el-button>
                  </div>
                </template>
                <el-input
                    v-model="board.currentText"
                    :rows="6"
                    type="textarea"
                    placeholder="请输入需要保存的文字..."
                    class="text-input"
                    clearable
                />
                <div class="card-actions">
                  <el-button type="success" @click="postText(index, board.currentText)" class="submit-btn" :loading="board.loading">同步</el-button>
                  <el-button @click="copyText(board.currentText)">复制</el-button>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </el-col>
      </el-row>
    </el-main>

    <!-- 历史记录抽屉 -->
    <el-drawer
      v-model="historyDrawerVisible"
      :title="`文字剪切板 ${currentHistoryIndex + 1}号 - 历史快照`"
      direction="rtl"
      size="35%"
      class="history-drawer"
    >
      <el-timeline v-if="textboards[currentHistoryIndex]?.history?.length > 0">
        <el-timeline-item
          v-for="(item, hIndex) in textboards[currentHistoryIndex].history"
          :key="hIndex"
          :type="hIndex === 0 ? 'primary' : 'info'"
          :hollow="hIndex !== 0"
          :size="hIndex === 0 ? 'large' : 'normal'"
        >
          <div class="history-item">
            <div class="history-badge" v-if="hIndex === 0">当前值</div>
            <div class="history-badge-old" v-else>历史快照 {{ hIndex }}</div>
            <div class="history-text">{{ item }}</div>
            <div class="history-actions">
              <el-button size="small" type="primary" plain @click="copyText(item)">复制</el-button>
              <el-button size="small" type="warning" plain @click="restoreText(item)" v-if="hIndex !== 0">恢复至输入框</el-button>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无历史记录" />
    </el-drawer>
  </el-container>
</template>

<script lang="ts">
import { defineComponent, ref, reactive } from 'vue'
import { ElMessage } from "element-plus"
import axios from "axios"

export default defineComponent({
  setup() {
    const port = import.meta.env.DEV ? '8080' : window.location.port
    const host = window.location.hostname
    const imgUrl = ref('http://' + host + ':' + port + '/api/picture')

    // 管理5个文字剪切板的状态
    const textboards = reactive([
      { currentText: '', history: [] as string[], loading: false },
      { currentText: '', history: [] as string[], loading: false },
      { currentText: '', history: [] as string[], loading: false },
      { currentText: '', history: [] as string[], loading: false },
      { currentText: '', history: [] as string[], loading: false }
    ])

    const historyDrawerVisible = ref(false)
    const currentHistoryIndex = ref(0)

    const getText = () => {
      axios.get('http://' + host + ':' + port + '/api/getAll').then(res => {
        const code = res.data.code
        if (code == 200) {
          const data_arr = res.data.msg
          for (let i = 0; i < 5; i++) {
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
      axios.post('http://' + host + ':' + port + '/api/post', {
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
      imgUrl.value = 'http://' + host + ':' + port + '/api/picture' + '?' + timestamp
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

    // 初始化获取数据
    getText()

    return {
      textboards, host, port, imgUrl,
      uploadFailed, uploadSucceed, getText, postText,
      historyDrawerVisible, currentHistoryIndex, openHistory, restoreText, copyText, downloadImage
    }
  }
});
</script>

<style scoped>
/* 全局布局 */
.dashboard-container {
  min-height: 100vh;
  background-color: #f2f5f8;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
}

.dashboard-header {
  background-color: #ffffff;
  color: #2c3e50;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
  display: flex;
  align-items: center;
  padding: 0 30px;
  height: 64px !important;
}

.dashboard-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.dashboard-main {
  padding: 30px;
  max-width: 1600px;
  margin: 0 auto;
  width: 100%;
}

.mb-24 {
  margin-bottom: 24px;
}

/* 卡片样式 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: 16px;
  color: #303133;
}

.image-card, .text-card {
  height: 100%;
  border-radius: 8px;
  border: none;
  transition: all 0.3s cubic-bezier(.25,.8,.25,1);
}

.image-card:hover, .text-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 28px rgba(0,0,0,0.08), 0 10px 10px rgba(0,0,0,0.04) !important;
}

/* 图片区域 */
.image-wrapper {
  background: #f4f4f5;
  border-radius: 6px;
  height: 220px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20px;
  overflow: hidden;
  flex-grow: 1;
}

.uploaded-image {
  width: 100%;
  height: 100%;
  transition: transform 0.3s ease;
}

.uploaded-image:hover {
  transform: scale(1.02);
}

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  color: #909399;
  font-size: 14px;
}

.upload-actions {
  margin-top: auto;
}

.d-flex {
  display: flex !important;
}

.flex-grow-1 {
  flex-grow: 1;
}

.upload-demo :deep(.el-upload) {
  width: 100%;
}

.w-100 {
  width: 100%;
}

/* 文本区域 */
.text-input {
  flex-grow: 1;
  font-size: 14px;
}

.text-input:deep(.el-textarea__inner) {
  border-radius: 6px;
  resize: none;
  font-family: inherit;
  padding: 12px;
}

.text-input:deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px var(--el-color-primary) inset;
}

.card-actions {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.submit-btn {
  flex-grow: 1;
}

/* 历史记录时间轴 */
.history-drawer :deep(.el-drawer__header) {
  margin-bottom: 20px;
  padding: 20px 24px 0;
  font-weight: bold;
  color: #303133;
}

.history-item {
  background-color: #f8f9fa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 16px;
  transition: background-color 0.2s;
}

.history-item:hover {
  background-color: #f2f6fc;
}

.history-badge {
  display: inline-block;
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  margin-bottom: 8px;
  font-weight: 500;
}

.history-badge-old {
  display: inline-block;
  background-color: var(--el-color-info-light-9);
  color: var(--el-color-info);
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  margin-bottom: 8px;
}

.history-text {
  font-size: 14px;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.6;
  margin-bottom: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.history-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  border-top: 1px dashed #e4e7ed;
  padding-top: 12px;
}

/* 自定义滚动条 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-thumb {
  border-radius: 3px;
  background: #c0c4cc;
}
::-webkit-scrollbar-track {
  border-radius: 3px;
  background: #f4f4f5;
}
</style>