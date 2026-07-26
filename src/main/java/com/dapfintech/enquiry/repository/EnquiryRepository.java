package com.dapfintech.enquiry.repository;

import com.dapfintech.enquiry.entity.Enquiry;
import com.dapfintech.enquiry.enums.EnquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EnquiryRepository extends JpaRepository<Enquiry, UUID>, JpaSpecificationExecutor<Enquiry> {
    Page<Enquiry> findByEmployeeId(UUID employeeId, Pageable pageable);
    Page<Enquiry> findByStatus(EnquiryStatus status, Pageable pageable);
    boolean existsByMobileNumber(String mobileNumber);
}