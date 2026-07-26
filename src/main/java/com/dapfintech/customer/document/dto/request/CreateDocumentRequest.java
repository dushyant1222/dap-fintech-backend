package com.dapfintech.customer.document.dto.request;

import java.util.UUID;

import com.dapfintech.customer.enums.DocumentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;


@Data
public class CreateDocumentRequest {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;


    @NotNull(message = "Document type is required")
    private DocumentType documentType;


    @NotBlank(message = "File name is required")
    private String fileName;


    @NotBlank(message = "File path is required")
    private String filePath;
}