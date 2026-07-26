package com.dapfintech.file.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.dapfintech.file.dto.response.FileUploadResponse;
import com.dapfintech.file.service.FileStorageService;

@Service
public class FileStorageServiceImpl
        implements FileStorageService {

    @Value("${upload.path}")
    private String uploadPath;

    @Override
    public FileUploadResponse uploadFile(
            MultipartFile file
    ) {

        try {

            Path uploadDirectory =
                    Paths.get(uploadPath);

            if (!Files.exists(uploadDirectory)) {

                Files.createDirectories(
                        uploadDirectory
                );

            }

            String originalFileName =
                    file.getOriginalFilename();

            String extension = "";

            if (originalFileName != null
                    && originalFileName.contains(".")) {

                extension =
                        originalFileName.substring(
                                originalFileName.lastIndexOf(".")
                        );

            }

            String fileName =
                    UUID.randomUUID()
                            + extension;

            Path destination =
                    uploadDirectory.resolve(
                            fileName
                    );

            Files.copy(

                    file.getInputStream(),

                    destination,

                    StandardCopyOption.REPLACE_EXISTING

            );

            return FileUploadResponse
                    .builder()
                    .fileName(fileName)
                    .filePath(destination.toString())
                    .build();

        }

        catch (IOException e) {

            throw new RuntimeException(
                    "Unable to upload file"
            );

        }

    }

}