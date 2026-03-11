package com.aven.avenclipboard.controller;

import com.aven.avenclipboard.model.NewMessage;
import com.aven.avenclipboard.model.FileDownloadInfo;
import com.aven.avenclipboard.model.Picture;
import com.aven.avenclipboard.model.WebResponse;
import com.aven.avenclipboard.repository.PictureRepository;
import com.aven.avenclipboard.service.FileStorageService;
import com.aven.avenclipboard.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@RestController
public class MainController {

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private PictureRepository pictureRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${clipboard.picture.max-size:10MB}")
    private String configuredMaxPictureSize;

    @PostMapping("/api/post")
    public WebResponse postContent(@RequestBody NewMessage newMessage) {
        try {
            if (newMessage.getMessage() == null)
                newMessage.setMessage("");
            List<String> afterAddedResult = memoryService.putMessage(newMessage.getFromId(), newMessage.getMessage());
            return new WebResponse(HttpStatus.OK.value(), afterAddedResult);
        } catch (Exception e) {
            e.printStackTrace();
            return new WebResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }
    }

    @GetMapping("/api/getAll")
    public WebResponse getContent() {
        try {
            return new WebResponse(HttpStatus.OK.value(), memoryService.getAllMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return new WebResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }
    }

    @PostMapping("/api/picture")
    public void uploadPicture(@RequestParam("file") MultipartFile picture) throws IOException {
        if (picture == null || picture.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "上传图片不能为空");
        }

        long maxPictureSizeBytes = DataSize.parse(configuredMaxPictureSize).toBytes();
        if (picture.getSize() > maxPictureSizeBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "图片超过大小限制");
        }

        byte[] content = picture.getBytes();
        Picture p = new Picture(1L, content);
        pictureRepository.save(p);
    }

    @GetMapping("/api/picture")
    public ResponseEntity<Resource> getPicture() {
        Optional<Picture> optionalPicture = pictureRepository.findById(1L);
        if (optionalPicture.isEmpty() || optionalPicture.get().getContent() == null
                || optionalPicture.get().getContent().length <= 1) {
            return ResponseEntity.notFound().build();
        }
        byte[] pictureBytes = optionalPicture.get().getContent();

        ByteArrayResource resource = new ByteArrayResource(pictureBytes);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=picture.jpg");
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(pictureBytes.length)
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    @DeleteMapping("/api/picture")
    public WebResponse removePicture() {
        try {
            Optional<Picture> optionalPicture = pictureRepository.findById(1L);
            optionalPicture.ifPresent(picture -> pictureRepository.deleteById(picture.getId()));
            return new WebResponse(HttpStatus.OK.value(), optionalPicture.isPresent() ? "图片已移除" : "暂无图片");
        } catch (Exception ex) {
            ex.printStackTrace();
            return new WebResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "图片移除失败");
        }
    }

    @PostMapping("/api/file")
    public WebResponse uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            return new WebResponse(HttpStatus.OK.value(), fileStorageService.store(file));
        } catch (ResponseStatusException ex) {
            return new WebResponse(ex.getStatus().value(), ex.getReason());
        } catch (Exception ex) {
            ex.printStackTrace();
            return new WebResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "文件上传失败");
        }
    }

    @GetMapping("/api/file/meta")
    public WebResponse getFileMetadata() {
        try {
            return fileStorageService.getMetadata()
                    .<WebResponse>map(fileMetadataView -> new WebResponse(HttpStatus.OK.value(), fileMetadataView))
                    .orElseGet(() -> new WebResponse(HttpStatus.NOT_FOUND.value(), "暂无文件"));
        } catch (Exception ex) {
            ex.printStackTrace();
            return new WebResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "文件信息获取失败");
        }
    }

    @GetMapping("/api/file")
    public ResponseEntity<Resource> downloadFile() {
        FileDownloadInfo fileDownloadInfo = fileStorageService.prepareDownload();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileDownloadInfo.getOriginalName(), StandardCharsets.UTF_8)
                .build());

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(fileDownloadInfo.getContentType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(fileDownloadInfo.getSizeBytes())
                .contentType(mediaType)
                .body(fileDownloadInfo.getResource());
    }

    @DeleteMapping("/api/file")
    public WebResponse removeFile() {
        try {
            boolean removed = fileStorageService.remove();
            return new WebResponse(HttpStatus.OK.value(), removed ? "文件已移除" : "暂无文件");
        } catch (ResponseStatusException ex) {
            return new WebResponse(ex.getStatus().value(), ex.getReason());
        } catch (Exception ex) {
            ex.printStackTrace();
            return new WebResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "文件移除失败");
        }
    }
}
