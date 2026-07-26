package com.dapfintech.loan.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.dapfintech.loan.enums.DisbursementMode;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DisbursementResponse {

    private UUID id;

    private UUID loanId;

    private DisbursementMode disbursementMode;

    private BigDecimal approvedAmount;

    private BigDecimal totalCharges;

    private BigDecimal netDisbursedAmount;

    private String transactionReference;

    private LocalDateTime disbursementDate;
}