package com.dapfintech.loan.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.dapfintech.loan.enums.RepaymentStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RepaymentScheduleResponse {

    private UUID id;

    private Integer installmentNumber;

    private LocalDate dueDate;

    private BigDecimal principalAmount;

    private BigDecimal interestAmount;
    private String displayStatus;

    private BigDecimal installmentAmount;

    private BigDecimal dueAmount;

    private BigDecimal paidAmount;

    private BigDecimal outstandingAmount;

    private RepaymentStatus repaymentStatus;
}