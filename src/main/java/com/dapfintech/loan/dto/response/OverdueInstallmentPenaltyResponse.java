package com.dapfintech.loan.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverdueInstallmentPenaltyResponse {
    private Integer installmentNumber;
    private LocalDate dueDate;
    private Long daysOverdue;
    private BigDecimal installmentAmount;
    private BigDecimal paidAmount;
    private BigDecimal shortfallAmount;
    private Boolean withinGracePeriod;
    private BigDecimal compoundPenalty;
}
