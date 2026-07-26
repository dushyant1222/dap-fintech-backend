package com.dapfintech.customer.document.dto.request;


import com.dapfintech.customer.enums.VerificationStatus;

import jakarta.validation.constraints.NotNull;

import lombok.Data;


@Data
public class UpdateDocumentVerificationRequest {

    @NotNull(message = "Verification status is required")
    private VerificationStatus verificationStatus;


    private String remark;
}