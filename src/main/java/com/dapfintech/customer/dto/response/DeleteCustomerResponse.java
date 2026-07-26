package com.dapfintech.customer.dto.response;


import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class DeleteCustomerResponse {

    private UUID customerId;

    private String customerCode;

    private String customerName;

    private LocalDateTime deletedAt;

}