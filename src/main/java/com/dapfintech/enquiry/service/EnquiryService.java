package com.dapfintech.enquiry.service;

import com.dapfintech.customer.dto.response.CustomerResponse;
import com.dapfintech.enquiry.dto.EnquiryRequest;
import com.dapfintech.enquiry.dto.EnquiryResponse;
import com.dapfintech.enquiry.dto.EnquiryStatusUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EnquiryService {
    EnquiryResponse createEnquiry(EnquiryRequest request, UUID employeeId);
    EnquiryResponse getEnquiryById(UUID id);
    Page<EnquiryResponse> getAllEnquiries(Pageable pageable);
    Page<EnquiryResponse> getEnquiriesByEmployee(UUID employeeId, Pageable pageable);
    EnquiryResponse updateEnquiryStatus(UUID id, EnquiryStatusUpdateRequest request, UUID actionById);
    CustomerResponse convertEnquiryToCustomer(UUID enquiryId, UUID actionById);
}