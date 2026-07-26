package com.dapfintech.customer.document.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dapfintech.common.response.ApiResponse;
import com.dapfintech.customer.document.dto.request.CreateDocumentRequest;
import com.dapfintech.customer.document.dto.request.ReplaceDocumentRequest;
import com.dapfintech.customer.document.dto.request.UpdateDocumentVerificationRequest;
import com.dapfintech.customer.document.dto.response.DocumentResponse;
import com.dapfintech.customer.document.service.DocumentService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/customer-documents")
@RequiredArgsConstructor
public class DocumentController {


    private final DocumentService documentService;


    // ==========================================
    // CREATE DOCUMENT
    // ==========================================

    @PostMapping
    public ResponseEntity<
            ApiResponse<DocumentResponse>
            > createDocument(

            @Valid
            @RequestBody
            CreateDocumentRequest request

    ) {

        DocumentResponse response =
                documentService.createDocument(request);


        return ResponseEntity.ok(

                ApiResponse
                        .<DocumentResponse>builder()

                        .success(true)

                        .message(
                                "Document uploaded successfully"
                        )

                        .data(response)

                        .build()
        );
    }


    // ==========================================
    // GET DOCUMENT BY ID
    // ==========================================

    @GetMapping("/{documentId}")
    public ResponseEntity<
            ApiResponse<DocumentResponse>
            > getDocumentById(

            @PathVariable UUID documentId

    ) {

        DocumentResponse response =
                documentService.getDocumentById(
                        documentId
                );


        return ResponseEntity.ok(

                ApiResponse
                        .<DocumentResponse>builder()

                        .success(true)

                        .message(
                                "Document fetched successfully"
                        )

                        .data(response)

                        .build()
        );
    }


    // ==========================================
    // GET ALL CUSTOMER DOCUMENTS
    // ==========================================

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<
            ApiResponse<List<DocumentResponse>>
            > getCustomerDocuments(

            @PathVariable UUID customerId

    ) {

        List<DocumentResponse> response =
                documentService
                        .getCustomerDocuments(
                                customerId
                        );


        return ResponseEntity.ok(

                ApiResponse
                        .<List<DocumentResponse>>builder()

                        .success(true)

                        .message(
                                "Customer documents fetched successfully"
                        )

                        .data(response)

                        .build()
        );
    }


    // ==========================================
    // VERIFY OR REJECT DOCUMENT
    // ==========================================

    @PatchMapping(
            "/{documentId}/verification"
    )
    public ResponseEntity<
            ApiResponse<DocumentResponse>
            > updateVerificationStatus(

            @PathVariable UUID documentId,

            @Valid
            @RequestBody
            UpdateDocumentVerificationRequest request

    ) {

        DocumentResponse response =
                documentService
                        .updateVerificationStatus(
                                documentId,
                                request
                        );


        String message =
                switch (
                        response.getVerificationStatus()
                ) {

                    case VERIFIED ->
                            "Document verified successfully";

                    case REJECTED ->
                            "Document rejected successfully";

                    default ->
                            "Document verification status updated successfully";
                };


        return ResponseEntity.ok(

                ApiResponse
                        .<DocumentResponse>builder()

                        .success(true)

                        .message(message)

                        .data(response)

                        .build()
        );
    }


    // ==========================================
    // REPLACE DOCUMENT
    // ==========================================

    @PutMapping("/{documentId}/replace")
    public ResponseEntity<
            ApiResponse<DocumentResponse>
            > replaceDocument(

            @PathVariable UUID documentId,

            @Valid
            @RequestBody
            ReplaceDocumentRequest request

    ) {

        DocumentResponse response =
                documentService.replaceDocument(
                        documentId,
                        request
                );


        return ResponseEntity.ok(

                ApiResponse
                        .<DocumentResponse>builder()

                        .success(true)

                        .message(
                                "Document replaced successfully. Verification status reset to pending."
                        )

                        .data(response)

                        .build()
        );
    }


    // ==========================================
    // DELETE DOCUMENT
    // ==========================================

    @DeleteMapping("/{documentId}")
    public ResponseEntity<
            ApiResponse<Void>
            > deleteDocument(

            @PathVariable UUID documentId

    ) {

        documentService.deleteDocument(
                documentId
        );


        return ResponseEntity.ok(

                ApiResponse
                        .<Void>builder()

                        .success(true)

                        .message(
                                "Document deleted successfully"
                        )

                        .data(null)

                        .build()
        );
    }
}