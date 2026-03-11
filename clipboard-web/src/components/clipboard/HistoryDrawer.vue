<template>
  <el-drawer
    class="glass-drawer"
    direction="rtl"
    size="35%"
    :model-value="visible"
    :title="`文本区 NO.${currentHistoryIndex + 1} - 历史快照`"
    @update:model-value="updateVisible"
  >
    <div class="drawer-content-scroll">
      <el-timeline v-if="history.length > 0" class="custom-timeline">
        <el-timeline-item
          v-for="(item, hIndex) in history"
          :key="hIndex"
          :color="hIndex === 0 ? '#10b981' : '#94a3b8'"
          :hollow="hIndex !== 0"
          :size="hIndex === 0 ? 'large' : 'normal'"
          :type="hIndex === 0 ? 'primary' : 'info'"
        >
          <div class="history-item glass-history-item">
            <div class="history-header">
              <div v-if="hIndex === 0" class="history-badge">✨ 当前生效值</div>
              <div v-else class="history-badge-old">🕰️ 快照 #{{ hIndex }}</div>
            </div>
            <div class="history-text glass-panel">{{ item }}</div>
            <div class="history-actions">
              <el-button size="small" class="glass-btn copy-btn-small" @click="emitCopy(item)">
                📋 复制
              </el-button>
              <el-button
                v-if="hIndex !== 0"
                size="small"
                class="glass-btn warning-btn-small"
                @click="emitRestore(item)"
              >
                ↩️ 恢复至面板
              </el-button>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无历史记录" class="glass-empty" />
    </div>
  </el-drawer>
</template>

<script lang="ts">
import { defineComponent } from 'vue'

export default defineComponent({
  emits: ['copy', 'restore', 'update:visible'],
  props: {
    currentHistoryIndex: {
      type: Number,
      required: true
    },
    history: {
      type: Array as () => string[],
      required: true
    },
    visible: {
      type: Boolean,
      required: true
    }
  },
  setup(_, { emit }) {
    const updateVisible = (value: boolean) => {
      emit('update:visible', value)
    }

    const emitCopy = (text: string) => {
      emit('copy', text)
    }

    const emitRestore = (text: string) => {
      emit('restore', text)
    }

    return {
      emitCopy,
      emitRestore,
      updateVisible
    }
  }
})
</script>
