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

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file
    ) {
        FileUploadResponse response = fileStorageService.uploadFile(file);

        return ResponseEntity.ok(
                ApiResponse.<FileUploadResponse>builder()
                        .success(true)
                        .message("File uploaded securely to Cloudflare R2")
                        .data(response)
                        .build()
        );
    }
}