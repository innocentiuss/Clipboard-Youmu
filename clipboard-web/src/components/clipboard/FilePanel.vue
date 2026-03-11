<template>
  <el-card
    shadow="never"
    class="glass-card top-card file-card"
    :body-style="{ padding: '24px', display: 'flex', flexDirection: 'column', boxSizing: 'border-box' }"
  >
    <template #header>
      <div class="card-header">
        <span class="header-title">
          <span class="icon-wrap">📦</span> 文件区
        </span>
      </div>
    </template>

    <div
      class="file-wrapper glass-panel"
      :class="{ 'empty-upload-target': !hasFile && !fileUploading && !fileRemoving }"
      :tabindex="!hasFile && !fileUploading && !fileRemoving ? 0 : -1"
      @click="triggerFileUpload"
      @keydown.enter.prevent="triggerFileUpload"
      @keydown.space.prevent="triggerFileUpload"
    >
      <div class="file-slot" :class="{ 'empty-slot': !hasFile }">
        <span class="empty-icon text-3xl">{{ hasFile ? '📄' : '🗂️' }}</span>
        <span>{{ selectedFileName || '暂无文件' }}</span>
        <span v-if="hasFile && uploadedAtText" class="file-helper-text">{{ uploadedAtText }}</span>
      </div>
    </div>

    <div class="upload-actions">
      <div class="image-btn-row">
        <input ref="fileInputRef" type="file" class="hidden-file-input" @change="handleFileSelected" />
        <el-button
          class="w-100 glass-btn primary-btn action-btn upload-btn"
          :disabled="fileRemoving"
          :loading="fileUploading"
          @click="selectFile"
        >
          ⬆️ 上传文件
        </el-button>
        <el-button
          class="glass-btn success-btn action-btn download-btn"
          :disabled="!hasFile || fileUploading || fileRemoving"
          @click="emitDownload"
        >
          ⬇️ 下载
        </el-button>
        <el-button
          class="glass-btn danger-btn action-btn remove-btn"
          :disabled="!hasFile || fileUploading || fileRemoving"
          :loading="fileRemoving"
          @click="emitRemove"
        >
          🗑️ 移除
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<script lang="ts">
import { defineComponent, ref } from 'vue'

export default defineComponent({
  emits: ['download', 'file-selected', 'remove'],
  props: {
    fileRemoving: {
      type: Boolean,
      required: true
    },
    fileUploading: {
      type: Boolean,
      required: true
    },
    hasFile: {
      type: Boolean,
      required: true
    },
    selectedFileName: {
      type: String,
      required: true
    },
    uploadedAtText: {
      type: String,
      required: true
    }
  },
  setup(props, { emit }) {
    const fileInputRef = ref<HTMLInputElement | null>(null)

    const selectFile = () => {
      if (props.fileUploading) {
        return
      }

      fileInputRef.value?.click()
    }

    const triggerFileUpload = () => {
      if (props.hasFile || props.fileUploading || props.fileRemoving) {
        return
      }

      selectFile()
    }

    const handleFileSelected = (event: Event) => {
      const input = event.target as HTMLInputElement
      const file = input.files?.[0]

      if (!file) {
        return
      }

      emit('file-selected', file)
      input.value = ''
    }

    const emitDownload = () => {
      emit('download')
    }

    const emitRemove = () => {
      emit('remove')
    }

    return {
      emitDownload,
      emitRemove,
      fileInputRef,
      handleFileSelected,
      selectFile,
      triggerFileUpload
    }
  }
})
</script>
