import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

import type { FileMetadata } from '../types/clipboard'

type ConfirmRemoval = (title: string, message: string) => Promise<unknown>

function getResponseData(error: unknown) {
  if (typeof error !== 'object' || error === null || !('response' in error)) {
    return undefined
  }

  return (error as { response?: { data?: unknown } }).response?.data
}

function extractResponseMessage(payload: unknown) {
  if (typeof payload !== 'object' || payload === null) {
    return ''
  }

  const maybePayload = payload as { msg?: unknown; message?: unknown }
  if (typeof maybePayload.msg === 'string') {
    return maybePayload.msg
  }

  if (typeof maybePayload.message === 'string') {
    return maybePayload.message
  }

  return ''
}

async function extractBlobMessage(payload: unknown) {
  if (!(payload instanceof Blob)) {
    return ''
  }

  try {
    const text = await payload.text()
    if (!text) {
      return ''
    }

    const parsed = JSON.parse(text)
    return parsed.message || parsed.msg || ''
  } catch {
    return ''
  }
}

function extractFilename(contentDisposition: string | undefined) {
  if (!contentDisposition) {
    return ''
  }

  const utfMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utfMatch?.[1]) {
    return decodeURIComponent(utfMatch[1])
  }

  const basicMatch = contentDisposition.match(/filename="?([^"]+)"?/i)
  return basicMatch?.[1] || ''
}

function formatTimestamp(timestamp: string | undefined) {
  if (!timestamp) {
    return ''
  }

  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) {
    return ''
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(date)
}

export function useImageClipboard(confirmRemoval: ConfirmRemoval) {
  const imgUrl = ref('/api/picture')
  const hasImage = ref(false)
  const imageRemoving = ref(false)

  const resetImageState = () => {
    imgUrl.value = '/api/picture'
    hasImage.value = false
  }

  const loadImageState = async () => {
    try {
      await axios.head('/api/picture')
      hasImage.value = true
    } catch {
      resetImageState()
    }
  }

  const uploadSucceed = () => {
    imgUrl.value = `/api/picture?${Date.now()}`
    hasImage.value = true
    ElMessage({
      message: '图片上传成功 :)',
      type: 'success'
    })
  }

  const uploadFailed = () => {
    ElMessage({
      message: '图片上传失败 :(',
      type: 'warning'
    })
  }

  const downloadImage = async () => {
    if (!imgUrl.value) {
      return
    }

    try {
      const res = await axios.get(imgUrl.value, { responseType: 'blob' })
      const url = window.URL.createObjectURL(new Blob([res.data]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', 'clipboard-image.png')
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    } catch {
      ElMessage.warning('图片下载失败或跨域被拦截')
      window.open(imgUrl.value, '_blank')
    }
  }

  const removeImage = async () => {
    if (!hasImage.value || imageRemoving.value) {
      return
    }

    try {
      await confirmRemoval('移除图片', '移除后将无法继续下载当前图片，是否继续？')
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

  return {
    downloadImage,
    hasImage,
    imageRemoving,
    imgUrl,
    loadImageState,
    removeImage,
    uploadFailed,
    uploadSucceed
  }
}

export function useFileClipboard(confirmRemoval: ConfirmRemoval) {
  const selectedFileName = ref('')
  const hasFile = ref(false)
  const fileUploading = ref(false)
  const fileRemoving = ref(false)
  const fileMeta = ref<FileMetadata | null>(null)

  const resetFileState = () => {
    hasFile.value = false
    fileMeta.value = null
    selectedFileName.value = ''
  }

  const loadFileMeta = async (silent = true) => {
    try {
      const res = await axios.get('/api/file/meta')
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
    } catch {
      resetFileState()
      if (!silent) {
        ElMessage.error('文件信息获取失败')
      }
    }
  }

  const uploadFile = async (file: File) => {
    const formData = new FormData()
    formData.append('file', file)

    fileUploading.value = true
    try {
      const res = await axios.post('/api/file', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })

      if (res.data.code === 200) {
        fileMeta.value = res.data.msg as FileMetadata
        hasFile.value = true
        selectedFileName.value = fileMeta.value.originalName
        ElMessage.success('文件上传成功')
        return
      }

      ElMessage.warning(res.data.msg || '文件上传失败')
    } catch (error: unknown) {
      const responseData = getResponseData(error)
      const blobMessage = await extractBlobMessage(responseData)
      const responseMessage = extractResponseMessage(responseData)
      ElMessage.error(blobMessage || responseMessage || '文件上传失败')
    } finally {
      fileUploading.value = false
    }
  }

  const downloadFile = async () => {
    if (!hasFile.value) {
      ElMessage.warning('暂无可下载文件')
      return
    }

    try {
      const res = await axios.get('/api/file', { responseType: 'blob' })
      const downloadName =
        extractFilename(res.headers['content-disposition']) ||
        selectedFileName.value ||
        'clipboard-file.dat'
      const url = window.URL.createObjectURL(res.data as Blob)
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', downloadName)
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    } catch (error: unknown) {
      const responseData = getResponseData(error)
      const blobMessage = await extractBlobMessage(responseData)
      ElMessage.error(blobMessage || '文件下载失败')
    }
  }

  const removeFile = async () => {
    if (!hasFile.value || fileUploading.value || fileRemoving.value) {
      return
    }

    try {
      await confirmRemoval('移除文件', '移除后将无法继续下载当前文件，是否继续？')
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
    } catch (error: unknown) {
      const responseData = getResponseData(error)
      const blobMessage = await extractBlobMessage(responseData)
      const responseMessage = extractResponseMessage(responseData)
      ElMessage.error(blobMessage || responseMessage || '文件移除失败')
    } finally {
      fileRemoving.value = false
    }
  }

  const fileUploadedAtText = () => {
    if (!hasFile.value || !fileMeta.value) {
      return ''
    }

    const formatted = formatTimestamp(fileMeta.value.uploadedAt)
    return formatted ? `上传时间：${formatted}` : ''
  }

  return {
    downloadFile,
    fileRemoving,
    fileUploadedAtText,
    fileUploading,
    hasFile,
    loadFileMeta,
    removeFile,
    selectedFileName,
    uploadFile
  }
}
