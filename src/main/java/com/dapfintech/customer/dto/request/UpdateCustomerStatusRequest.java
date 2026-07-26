package com.dapfintech.customer.dto.request;

import com.dapfintech.customer.enums.CustomerStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCustomerStatusRequest {

    @NotNull(
        message = "Customer status is required"
    )
    private CustomerStatus status;

    @Size(
        max = 500,
        message = "Reason cannot exceed 500 characters"
    )
    private String reason;
}