package com.aven.avenclipboard.controller;

import com.aven.avenclipboard.model.NewMessage;
import com.aven.avenclipboard.model.Picture;
import com.aven.avenclipboard.model.WebResponse;
import com.aven.avenclipboard.repository.PictureRepository;
import com.aven.avenclipboard.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
public class MainController {

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private PictureRepository pictureRepository;

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
        byte[] content = picture.getBytes();
        Picture p = new Picture(1L, content);
        pictureRepository.save(p);
    }

    @GetMapping("/api/picture")
    public ResponseEntity<Resource> getPicture() {
        Optional<Picture> optionalPicture = pictureRepository.findById(1L);
        byte[] pictureBytes = optionalPicture.map(Picture::getContent).orElse(new byte[1]);

        ByteArrayResource resource = new ByteArrayResource(pictureBytes);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=picture.jpg");
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(pictureBytes.length)
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }
}
