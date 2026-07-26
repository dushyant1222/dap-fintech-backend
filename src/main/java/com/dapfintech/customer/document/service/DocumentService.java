package com.dapfintech.customer.document.service;

import java.util.List;
import java.util.UUID;

import com.dapfintech.customer.document.dto.request.CreateDocumentRequest;
import com.dapfintech.customer.document.dto.request.ReplaceDocumentRequest;
import com.dapfintech.customer.document.dto.request.UpdateDocumentVerificationRequest;
import com.dapfintech.customer.document.dto.response.DocumentResponse;


public interface DocumentService {

    DocumentResponse createDocument(
            CreateDocumentRequest request
    );


    DocumentResponse getDocumentById(
            UUID documentId
    );


    List<DocumentResponse> getCustomerDocuments(
            UUID customerId
    );


    DocumentResponse updateVerificationStatus(
            UUID documentId,
            UpdateDocumentVerificationRequest request
    );


    DocumentResponse replaceDocument(
            UUID documentId,
            ReplaceDocumentRequest request
    );


    void deleteDocument(
            UUID documentId
    );
}