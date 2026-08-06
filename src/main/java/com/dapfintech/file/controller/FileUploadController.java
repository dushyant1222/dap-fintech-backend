package com.dapfintech.file.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.net.URI;

import com.dapfintech.common.response.ApiResponse;
import com.dapfintech.file.dto.response.FileUploadResponse;
import com.dapfintech.file.service.FileStorageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;
    
    @GetMapping("/view/{fileName}")
    public ResponseEntity<Void> viewFile(
            @PathVariable String fileName
    ) {
        String presignedUrl = fileStorageService.getPresignedUrl(fileName);
        
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(presignedUrl))
                .build();
    }

    @GetMapping("/local/{fileName}")
    public ResponseEntity<org.springframework.core.io.Resource> getLocalFile(@PathVariable String fileName) {
        try {
            java.nio.file.Path filePath = java.nio.file.Paths.get("uploads").resolve(fileName).normalize();
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());
            
            if (resource.exists() || resource.isReadable()) {
                String contentType = java.nio.file.Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                return ResponseEntity.ok()
                        .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, contentType)
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file
    ) {
        FileUploadResponse response = fileStorageService.uploadFile(file);

        return ResponseEntity.ok(
                ApiResponse.<FileUploadResponse>builder()
                        .success(true)
                        .message("File uploaded successfully")
                        .data(response)
                        .build()
        );
    }
}