<template>
  <el-card
    shadow="never"
    class="glass-card top-card image-card"
    :body-style="{ padding: '24px', display: 'flex', flexDirection: 'column', boxSizing: 'border-box' }"
  >
    <template #header>
      <div class="card-header">
        <span class="header-title">
          <span class="icon-wrap">🖼️</span> 图片区
        </span>
      </div>
    </template>

    <div
      class="image-wrapper glass-panel"
      :class="{ 'empty-upload-target': !hasImage && !imageRemoving }"
      :tabindex="!hasImage && !imageRemoving ? 0 : -1"
      @click="triggerImageUpload"
      @keydown.enter.prevent="triggerImageUpload"
      @keydown.space.prevent="triggerImageUpload"
    >
      <el-image v-if="hasImage" :key="imgUrl" :src="imgUrl" fit="scale-down" class="uploaded-image">
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
          ref="imageUploadRef"
          class="upload-demo action-btn upload-btn"
          action="/api/picture"
          accept="image/png, image/jpeg"
          :disabled="imageRemoving"
          :on-error="handleUploadFailed"
          :on-success="handleUploadSucceed"
          :show-file-list="false"
        >
          <el-button class="w-100 glass-btn primary-btn">⬆️ 上传图片</el-button>
        </el-upload>
        <el-button
          class="glass-btn success-btn action-btn download-btn"
          :disabled="!hasImage || imageRemoving"
          @click="emitDownload"
        >
          ⬇️ 下载
        </el-button>
        <el-button
          class="glass-btn danger-btn action-btn remove-btn"
          :disabled="!hasImage || imageRemoving"
          :loading="imageRemoving"
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
import type { UploadInstance } from 'element-plus'

export default defineComponent({
  emits: ['download', 'remove', 'upload-failed', 'upload-succeed'],
  props: {
    hasImage: {
      type: Boolean,
      required: true
    },
    imageRemoving: {
      type: Boolean,
      required: true
    },
    imgUrl: {
      type: String,
      required: true
    }
  },
  setup(props, { emit }) {
    const imageUploadRef = ref<UploadInstance | null>(null)

    const triggerImageUpload = () => {
      if (props.hasImage || props.imageRemoving) {
        return
      }

      imageUploadRef.value?.$el?.querySelector('input[type="file"]')?.click()
    }

    const handleUploadSucceed = () => {
      emit('upload-succeed')
    }

    const handleUploadFailed = () => {
      emit('upload-failed')
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
      handleUploadFailed,
      handleUploadSucceed,
      imageUploadRef,
      triggerImageUpload
    }
  }
})
</script>
