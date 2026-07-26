package com.dapfintech.file.controller;

import org.springframework.http.ResponseEntity;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<Resource> viewFile(
            @PathVariable String fileName
    ) throws MalformedURLException {

        Path path =
                Paths.get("uploads")
                        .resolve(fileName);

        Resource resource =
                new UrlResource(path.toUri());

        if (!resource.exists()) {

            throw new RuntimeException(
                    "File not found"
            );

        }

        return ResponseEntity.ok()

                .contentType(MediaType.APPLICATION_OCTET_STREAM)

                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + fileName + "\""
                )

                .body(resource);

    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>>
    uploadFile(

            @RequestParam("file")
            MultipartFile file

    ) {

        FileUploadResponse response =
                fileStorageService
                        .uploadFile(file);

        return ResponseEntity.ok(

                ApiResponse
                        .<FileUploadResponse>builder()
                        .success(true)
                        .message(
                                "File uploaded successfully"
                        )
                        .data(response)
                        .build()

        );

    }

}