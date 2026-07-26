package com.dapfintech.enquiry.repository;

import com.dapfintech.enquiry.entity.EnquiryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EnquiryHistoryRepository extends JpaRepository<EnquiryHistory, UUID> {
    List<EnquiryHistory> findByEnquiryIdOrderByCreatedAtDesc(UUID enquiryId);
}