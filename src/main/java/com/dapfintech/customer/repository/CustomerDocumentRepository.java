package com.dapfintech.customer.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dapfintech.customer.entity.CustomerDocument;
import com.dapfintech.customer.enums.DocumentType;
import com.dapfintech.customer.enums.VerificationStatus;


@Repository
public interface CustomerDocumentRepository
        extends JpaRepository<CustomerDocument, UUID> {


    List<CustomerDocument> findByCustomerId(
            UUID customerId
    );


    Optional<CustomerDocument>
    findByCustomerIdAndDocumentType(
            UUID customerId,
            DocumentType documentType
    );


    boolean existsByCustomerIdAndDocumentType(
            UUID customerId,
            DocumentType documentType
    );


    boolean existsByCustomerIdAndDocumentTypeAndVerificationStatus(
            UUID customerId,
            DocumentType documentType,
            VerificationStatus verificationStatus
    );
}