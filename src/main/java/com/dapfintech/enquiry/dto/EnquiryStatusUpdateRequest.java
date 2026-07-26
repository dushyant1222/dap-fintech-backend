package com.dapfintech.enquiry.dto;

import com.dapfintech.enquiry.enums.EnquiryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnquiryStatusUpdateRequest {
    @NotNull(message = "Target status is required")
    private EnquiryStatus status;

    @NotBlank(message = "Remarks are required for status change")
    private String remarks;
}