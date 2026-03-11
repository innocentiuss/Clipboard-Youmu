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
          <ImagePanel
            :has-image="hasImage"
            :image-removing="imageRemoving"
            :img-url="imgUrl"
            @download="downloadImage"
            @remove="removeImage"
            @upload-failed="uploadFailed"
            @upload-succeed="uploadSucceed"
          />
        </el-col>

        <el-col :xs="24" :sm="12" :md="12" :lg="12" class="mb-24">
          <FilePanel
            :file-removing="fileRemoving"
            :file-uploading="fileUploading"
            :has-file="hasFile"
            :selected-file-name="selectedFileName"
            :uploaded-at-text="fileUploadedAtText()"
            @download="downloadFile"
            @file-selected="uploadFile"
            @remove="removeFile"
          />
        </el-col>
      </el-row>

      <el-row :gutter="24" class="text-zone">
        <el-col
          v-for="(board, index) in textboards"
          :key="index"
          :xs="24"
          :sm="12"
          :md="12"
          :lg="12"
          class="mb-24"
        >
          <TextBoardCard
            :index="index"
            :loading="board.loading"
            :text="board.currentText"
            @copy="copyText"
            @open-history="openHistory"
            @sync="handleTextSync"
            @update:text="board.currentText = $event"
          />
        </el-col>
      </el-row>
    </el-main>

    <HistoryDrawer
      v-model:visible="historyDrawerVisible"
      :current-history-index="currentHistoryIndex"
      :history="textboards[currentHistoryIndex]?.history || []"
      @copy="copyText"
      @restore="restoreText"
    />
  </el-container>
</template>

<script lang="ts">
import { defineComponent, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import HistoryDrawer from './clipboard/HistoryDrawer.vue'
import FilePanel from './clipboard/FilePanel.vue'
import ImagePanel from './clipboard/ImagePanel.vue'
import TextBoardCard from './clipboard/TextBoardCard.vue'
import { useFileClipboard, useImageClipboard } from '../composables/useMediaClipboard'
import { useTextBoards } from '../composables/useTextBoards'

export default defineComponent({
  components: {
    FilePanel,
    HistoryDrawer,
    ImagePanel,
    TextBoardCard
  },
  setup() {
    const confirmRemoval = (title: string, message: string) => {
      return ElMessageBox.confirm(message, title, {
        confirmButtonText: '移除',
        cancelButtonText: '取消',
        type: 'warning',
        showClose: false,
        closeOnClickModal: false,
        closeOnPressEscape: true,
        customClass: 'glass-message-box',
        confirmButtonClass: 'glass-message-confirm',
        cancelButtonClass: 'glass-message-cancel'
      })
    }

    const {
      currentHistoryIndex,
      getText,
      historyDrawerVisible,
      openHistory,
      postText,
      restoreText,
      textboards
    } = useTextBoards()

    const {
      downloadImage,
      hasImage,
      imageRemoving,
      imgUrl,
      loadImageState,
      removeImage,
      uploadFailed,
      uploadSucceed
    } = useImageClipboard(confirmRemoval)

    const {
      downloadFile,
      fileRemoving,
      fileUploadedAtText,
      fileUploading,
      hasFile,
      loadFileMeta,
      removeFile,
      selectedFileName,
      uploadFile
    } = useFileClipboard(confirmRemoval)

    const copyText = async (text: string) => {
      if (!text) {
        ElMessage.warning('内容为空')
        return
      }

      try {
        await navigator.clipboard.writeText(text)
        ElMessage.success('已复制到系统剪切板')
      } catch {
        ElMessage.error('复制失败，请手动选择复制')
      }
    }

    const handleTextSync = ({ index, text }: { index: number; text: string }) => {
      void postText(index, text)
    }

    onMounted(() => {
      void loadImageState()
      void getText()
      void loadFileMeta()
    })

    return {
      copyText,
      currentHistoryIndex,
      downloadFile,
      downloadImage,
      fileRemoving,
      fileUploadedAtText,
      fileUploading,
      handleTextSync,
      hasFile,
      hasImage,
      historyDrawerVisible,
      imageRemoving,
      imgUrl,
      openHistory,
      removeFile,
      removeImage,
      restoreText,
      selectedFileName,
      textboards,
      uploadFailed,
      uploadFile,
      uploadSucceed
    }
  }
})
</script>

<style src="../styles/clipboard-view.css"></style>
