package com.dapfintech.enquiry.controller;

import com.dapfintech.common.response.ApiResponse;
import com.dapfintech.customer.dto.response.CustomerResponse;
import com.dapfintech.enquiry.dto.EnquiryRequest;
import com.dapfintech.enquiry.dto.EnquiryResponse;
import com.dapfintech.enquiry.dto.EnquiryStatusUpdateRequest;
import com.dapfintech.enquiry.service.EnquiryService;
import com.dapfintech.security.utils.SecurityUtils; // Assuming standard contextual security utility
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/enquiries")
@RequiredArgsConstructor
@Tag(name = "Enquiry Management", description = "APIs for pre-customer lead verification")
public class EnquiryController {

    private final EnquiryService enquiryService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('EMPLOYEE', 'ADMIN')")
    @Operation(summary = "Create a new Enquiry (Field Visit)")
    public ResponseEntity<ApiResponse<EnquiryResponse>> createEnquiry(@Valid @RequestBody EnquiryRequest request) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        EnquiryResponse response = enquiryService.createEnquiry(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Enquiry created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('EMPLOYEE', 'ADMIN')")
    @Operation(summary = "Get Enquiry details by ID")
    public ResponseEntity<ApiResponse<EnquiryResponse>> getEnquiry(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(enquiryService.getEnquiryById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @Operation(summary = "Get all Enquiries (Admin)")
    public ResponseEntity<ApiResponse<Page<EnquiryResponse>>> getAllEnquiries(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(enquiryService.getAllEnquiries(pageable)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('EMPLOYEE')")
    @Operation(summary = "Get all Enquiries created by logged-in Employee")
    public ResponseEntity<ApiResponse<Page<EnquiryResponse>>> getMyEnquiries(Pageable pageable) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(enquiryService.getEnquiriesByEmployee(currentUserId, pageable)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @Operation(summary = "Approve or Reject an Enquiry")
    public ResponseEntity<ApiResponse<EnquiryResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody EnquiryStatusUpdateRequest request) {
        UUID adminId = securityUtils.getCurrentUserId();
        EnquiryResponse response = enquiryService.updateEnquiryStatus(id, request, adminId);
        return ResponseEntity.ok(ApiResponse.success("Enquiry status updated successfully", response));
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Convert an approved enquiry into a Customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> convertToCustomer(@PathVariable UUID id) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        CustomerResponse response = enquiryService.convertEnquiryToCustomer(id, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Enquiry converted to customer successfully", response));
    }
}