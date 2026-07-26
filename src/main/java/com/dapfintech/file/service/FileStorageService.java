package com.dapfintech.file.service;

import org.springframework.web.multipart.MultipartFile;

import com.dapfintech.file.dto.response.FileUploadResponse;

public interface FileStorageService {

    FileUploadResponse uploadFile(
            MultipartFile file
    );

}