export interface FileMetadata {
  originalName: string
  contentType: string
  sizeBytes: number
  uploadedAt: string
}

export interface TextBoardState {
  currentText: string
  history: string[]
  loading: boolean
}
