package com.dapfintech.loan.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoanSummaryResponse {

    private UUID loanId;

    private BigDecimal approvedAmount;

    private BigDecimal disbursedAmount;

    private BigDecimal totalCollected;

    private BigDecimal outstandingAmount;

    private String loanStatus;
}