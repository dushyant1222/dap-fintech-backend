package com.dapfintech.customer.document.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;


@Data
public class ReplaceDocumentRequest {

    @NotBlank(message = "File name is required")
    private String fileName;


    @NotBlank(message = "File path is required")
    private String filePath;
}