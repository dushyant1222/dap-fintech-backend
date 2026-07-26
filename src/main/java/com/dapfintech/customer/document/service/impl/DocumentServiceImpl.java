package com.dapfintech.customer.document.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dapfintech.customer.history.enums.CustomerHistoryAction;
import com.dapfintech.customer.history.service.CustomerHistoryService;
import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.customer.document.dto.request.CreateDocumentRequest;
import com.dapfintech.customer.document.dto.request.ReplaceDocumentRequest;
import com.dapfintech.customer.document.dto.request.UpdateDocumentVerificationRequest;
import com.dapfintech.customer.document.dto.response.DocumentResponse;
import com.dapfintech.customer.document.mapper.DocumentMapper;
import com.dapfintech.customer.document.service.DocumentService;
import com.dapfintech.customer.entity.Customer;
import com.dapfintech.customer.entity.CustomerDocument;
import com.dapfintech.customer.enums.VerificationStatus;
import com.dapfintech.customer.repository.CustomerDocumentRepository;
import com.dapfintech.customer.repository.CustomerRepository;
import com.dapfintech.security.service.AccessControlService;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
@Transactional
public class DocumentServiceImpl
        implements DocumentService {


    private final CustomerRepository customerRepository;

    private final CustomerDocumentRepository documentRepository;

    private final DocumentMapper documentMapper;

    private final AccessControlService accessControlService;

    private final UserRepository userRepository;
    private final CustomerHistoryService customerHistoryService;


    @Override
    public DocumentResponse createDocument(
            CreateDocumentRequest request
    ) {

        accessControlService.validateCustomerAccess(
                request.getCustomerId()
        );


        Customer customer =
                customerRepository.findById(
                        request.getCustomerId()
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "Customer not found"
                        )
                );


        boolean alreadyExists =
                documentRepository
                        .existsByCustomerIdAndDocumentType(
                                request.getCustomerId(),
                                request.getDocumentType()
                        );


        if (alreadyExists) {

            throw new RuntimeException(
                    "A document of type "
                            + request.getDocumentType()
                            + " already exists for this customer. "
                            + "Please replace the existing document instead."
            );

        }
        

        CustomerDocument document =
                CustomerDocument.builder()

                        .customer(customer)

                        .documentType(
                                request.getDocumentType()
                        )

                        .fileName(
                                request.getFileName().trim()
                        )

                        .filePath(
                                request.getFilePath().trim()
                        )

                        .verificationStatus(
                                VerificationStatus.PENDING
                        )

                        .verificationRemark(null)

                        .verifiedBy(null)

                        .build();
        VerificationStatus oldStatus =
                document.getVerificationStatus();

        CustomerDocument savedDocument =
                documentRepository.save(document);
        CustomerHistoryAction historyAction;

        switch (savedDocument.getVerificationStatus()) {

            case VERIFIED ->
                    historyAction =
                            CustomerHistoryAction.DOCUMENT_VERIFIED;

            case REJECTED ->
                    historyAction =
                            CustomerHistoryAction.DOCUMENT_REJECTED;

            default ->
                    historyAction =
                            CustomerHistoryAction.DOCUMENT_STATUS_CHANGED;
        }
        customerHistoryService.recordHistory(

                savedDocument.getCustomer(),

                historyAction,

                "Document Status Changed",

                savedDocument.getDocumentType() +
                        " status changed from " +
                        oldStatus +
                        " to " +
                        savedDocument.getVerificationStatus() +
                        ".",

                oldStatus.name(),

                savedDocument
                        .getVerificationStatus()
                        .name()
        );
        customerHistoryService.recordHistory(

                customer,

                CustomerHistoryAction.DOCUMENT_UPLOADED,

                "Document Uploaded",

                savedDocument.getDocumentType() +
                        " was uploaded for the customer.",

                null,

                savedDocument.getDocumentType().name()
        );

        return documentMapper.toResponse(
                savedDocument
        );
       
    }


    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(
            UUID documentId
    ) {

        CustomerDocument document =
                getDocumentEntity(documentId);


        accessControlService.validateCustomerAccess(
                document.getCustomer().getId()
        );


        return documentMapper.toResponse(document);
    }


    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getCustomerDocuments(
            UUID customerId
    ) {

        accessControlService.validateCustomerAccess(
                customerId
        );


        return documentRepository
                .findByCustomerId(customerId)
                .stream()
                .map(documentMapper::toResponse)
                .toList();
    }


    @Override
    public DocumentResponse updateVerificationStatus(
            UUID documentId,
            UpdateDocumentVerificationRequest request
    ) {

        CustomerDocument document =
                getDocumentEntity(documentId);


        accessControlService.validateCustomerAccess(
                document.getCustomer().getId()
        );


        VerificationStatus requestedStatus =
                request.getVerificationStatus();


        if (requestedStatus ==
                VerificationStatus.PENDING) {

            throw new RuntimeException(
                    "Verification status cannot be manually changed to PENDING"
            );

        }


        String remark =
                request.getRemark() == null
                        ? null
                        : request.getRemark().trim();


        if (requestedStatus ==
                VerificationStatus.REJECTED
                &&
                (remark == null || remark.isEmpty())) {

            throw new RuntimeException(
                    "Rejection remark is required when rejecting a document"
            );

        }


        document.setVerificationStatus(
                requestedStatus
        );


        document.setVerificationRemark(
                remark == null || remark.isEmpty()
                        ? null
                        : remark
        );


        document.setVerifiedBy(
                getCurrentUserId()
        );


        return documentMapper.toResponse(
                documentRepository.save(document)
        );
    }


    @Override
    public DocumentResponse replaceDocument(
            UUID documentId,
            ReplaceDocumentRequest request
    ) {

        CustomerDocument document =
                getDocumentEntity(documentId);


        accessControlService.validateCustomerAccess(
                document.getCustomer().getId()
        );


        document.setFileName(
                request.getFileName().trim()
        );


        document.setFilePath(
                request.getFilePath().trim()
        );

        

        // Every replaced document requires
        // fresh verification.
        document.setVerificationStatus(
                VerificationStatus.PENDING
        );


        document.setVerificationRemark(null);

        document.setVerifiedBy(null);


        return documentMapper.toResponse(
                documentRepository.save(document)
        );
    }


    @Override
    @Transactional
    public void deleteDocument(
            UUID documentId
    ) {

        CustomerDocument document =
                documentRepository.findById(
                        documentId
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "Document not found"
                        )
                );

        accessControlService.validateCustomerAccess(
                document.getCustomer().getId()
        );

        customerHistoryService.recordHistory(

                document.getCustomer(),

                CustomerHistoryAction.DOCUMENT_DELETED,

                "Document Deleted",

                document.getDocumentType() +
                        " was deleted from the customer profile.",

                document.getDocumentType().name(),

                null
        );

        documentRepository.delete(document);
    }


    private CustomerDocument getDocumentEntity(
            UUID documentId
    ) {

        return documentRepository.findById(documentId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Document not found"
                        )
                );
    }


    private UUID getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (authentication == null
                ||
                !authentication.isAuthenticated()) {

            throw new RuntimeException(
                    "Authenticated user not found"
            );

        }


        String mobileNumber =
                authentication.getName();


        User user =
                userRepository
                        .findByMobileNumber(mobileNumber)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Authenticated user not found"
                                )
                        );


        return user.getId();
    }
}