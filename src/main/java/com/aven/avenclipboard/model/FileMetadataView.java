package com.aven.avenclipboard.model;

import java.time.LocalDateTime;

public class FileMetadataView {
    private String originalName;
    private String contentType;
    private Long sizeBytes;
    private LocalDateTime uploadedAt;

    public FileMetadataView() {
    }

    public FileMetadataView(String originalName, String contentType, Long sizeBytes, LocalDateTime uploadedAt) {
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedAt = uploadedAt;
    }

    public static FileMetadataView from(TransferFile transferFile) {
        return new FileMetadataView(
                transferFile.getOriginalName(),
                transferFile.getContentType(),
                transferFile.getSizeBytes(),
                transferFile.getUploadedAt()
        );
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
