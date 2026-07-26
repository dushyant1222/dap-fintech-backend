package com.dapfintech.report.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OverdueCustomerResponse {

    private UUID customerId;

    private String customerName;

    private String mobileNumber;

    private String marketName;

    private UUID loanId;

    private BigDecimal overdueAmount;

    private Integer overdueDays;
}