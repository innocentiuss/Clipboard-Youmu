<template>
  <el-card
    shadow="never"
    class="glass-card text-card"
    :body-style="{ padding: '24px', display: 'flex', flexDirection: 'column', height: '100%' }"
  >
    <template #header>
      <div class="card-header">
        <div class="header-title-with-badge">
          <span class="header-title">文本区</span>
          <span class="index-badge">NO.{{ index + 1 }}</span>
        </div>
        <el-button class="glass-btn-link" type="primary" link @click="emitOpenHistory">
          ⏱️ 历史
        </el-button>
      </div>
    </template>

    <div class="input-wrapper">
      <el-input
        v-model="textValue"
        :rows="6"
        type="textarea"
        placeholder="在这里输入需要云端同步的文字，支持设备互传..."
        class="glass-input"
        clearable
      />
    </div>

    <div class="card-actions">
      <el-button class="glass-btn submit-btn" :loading="loading" @click="emitSync">
        ☁️ 同步
      </el-button>
      <el-button class="glass-btn copy-btn" @click="emitCopy">
        📋 复制
      </el-button>
    </div>
  </el-card>
</template>

<script lang="ts">
import { computed, defineComponent } from 'vue'

export default defineComponent({
  emits: ['copy', 'open-history', 'sync', 'update:text'],
  props: {
    index: {
      type: Number,
      required: true
    },
    loading: {
      type: Boolean,
      required: true
    },
    text: {
      type: String,
      required: true
    }
  },
  setup(props, { emit }) {
    const textValue = computed({
      get: () => props.text,
      set: (value: string) => {
        emit('update:text', value)
      }
    })

    const emitOpenHistory = () => {
      emit('open-history', props.index)
    }

    const emitSync = () => {
      emit('sync', {
        index: props.index,
        text: props.text
      })
    }

    const emitCopy = () => {
      emit('copy', props.text)
    }

    return {
      emitCopy,
      emitOpenHistory,
      emitSync,
      textValue
    }
  }
})
</script>
