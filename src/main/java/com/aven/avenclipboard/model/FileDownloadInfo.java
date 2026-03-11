package com.aven.avenclipboard.model;

import org.springframework.core.io.Resource;

public class FileDownloadInfo {
    private final Resource resource;
    private final String originalName;
    private final String contentType;
    private final long sizeBytes;

    public FileDownloadInfo(Resource resource, String originalName, String contentType, long sizeBytes) {
        this.resource = resource;
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public Resource getResource() {
        return resource;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }
}
