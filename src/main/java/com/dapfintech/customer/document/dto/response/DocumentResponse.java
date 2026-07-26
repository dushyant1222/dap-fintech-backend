package com.dapfintech.customer.document.dto.response;

import java.util.UUID;

import com.dapfintech.customer.enums.DocumentType;
import com.dapfintech.customer.enums.VerificationStatus;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class DocumentResponse {

    private UUID id;

    private UUID customerId;

    private DocumentType documentType;

    private boolean mandatory;

    private String fileName;

    private String filePath;

    private VerificationStatus verificationStatus;

    private String verificationRemark;

    private UUID verifiedBy;
}