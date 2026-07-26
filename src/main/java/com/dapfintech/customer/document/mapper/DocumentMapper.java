package com.dapfintech.customer.document.mapper;

import org.springframework.stereotype.Component;

import com.dapfintech.customer.document.dto.response.DocumentResponse;
import com.dapfintech.customer.entity.CustomerDocument;


@Component
public class DocumentMapper {

    public DocumentResponse toResponse(
            CustomerDocument document
    ) {

        return DocumentResponse.builder()

                .id(document.getId())

                .customerId(
                        document.getCustomer().getId()
                )

                .documentType(
                        document.getDocumentType()
                )

                .mandatory(
                        document.getDocumentType()
                                .isMandatory()
                )

                .fileName(
                        document.getFileName()
                )

                .filePath(
                        document.getFilePath()
                )

                .verificationStatus(
                        document.getVerificationStatus()
                )

                .verificationRemark(
                        document.getVerificationRemark()
                )

                .verifiedBy(
                        document.getVerifiedBy()
                )

                .build();
    }
}