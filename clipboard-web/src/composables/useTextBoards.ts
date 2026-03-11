import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

import type { TextBoardState } from '../types/clipboard'

const BOARD_COUNT = 6

function createTextBoard(): TextBoardState {
  return {
    currentText: '',
    history: [],
    loading: false
  }
}

export function useTextBoards() {
  const textboards = reactive(
    Array.from({ length: BOARD_COUNT }, () => createTextBoard())
  ) as TextBoardState[]

  const historyDrawerVisible = ref(false)
  const currentHistoryIndex = ref(0)

  const getText = async () => {
    try {
      const res = await axios.get('/api/getAll')
      const code = res.data.code

      if (code === 200) {
        const dataArr = res.data.msg
        for (let i = 0; i < BOARD_COUNT; i++) {
          if (dataArr[i] && dataArr[i].length > 0) {
            textboards[i].history = dataArr[i]
            textboards[i].currentText = dataArr[i][0]
          }
        }
        return
      }

      ElMessage({
        message: '获取数据失败',
        type: 'warning'
      })
    } catch {
      ElMessage.error('网络错误，获取剪切板失败')
    }
  }

  const postText = async (fromId: number, content: string) => {
    textboards[fromId].loading = true

    try {
      const res = await axios.post('/api/post', {
        fromId,
        message: content
      })

      const code = res.data.code
      if (code === 200) {
        const dataArr = res.data.msg
        textboards[fromId].history = dataArr
        textboards[fromId].currentText = dataArr[0]
        ElMessage({
          message: `剪切板 ${fromId + 1}号 同步成功 :)`,
          type: 'success'
        })
        return
      }

      ElMessage({
        message: '文字同步失败 :(',
        type: 'warning'
      })
    } catch {
      ElMessage.error('网络错误，同步失败')
    } finally {
      textboards[fromId].loading = false
    }
  }

  const openHistory = (index: number) => {
    currentHistoryIndex.value = index
    historyDrawerVisible.value = true
  }

  const restoreText = (text: string) => {
    textboards[currentHistoryIndex.value].currentText = text
    ElMessage.success('已恢复到输入框，请点击"同步"按钮以保存')
    historyDrawerVisible.value = false
  }

  return {
    currentHistoryIndex,
    getText,
    historyDrawerVisible,
    openHistory,
    postText,
    restoreText,
    textboards
  }
}
