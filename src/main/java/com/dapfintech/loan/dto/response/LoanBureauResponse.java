package com.dapfintech.loan.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoanBureauResponse {
    private UUID loanId;
    private String loanCode;
    private String customerName;
    private String repaymentFrequency;
    private Integer tenure;
    private BigDecimal installmentAmount;
    private BigDecimal totalAmount;
    private BigDecimal receivedTillDate;
    private BigDecimal totalBalance;
    private BigDecimal pendingBalance;
}
