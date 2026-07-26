package com.dapfintech.loan.dto.request;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CloseSpecialLoanRequest {
    private BigDecimal waivedPenaltyPercent;
    private BigDecimal settlementAmountPaid;
    private String specialRemarks;
}
