package com.dapfintech.loan.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanPenaltySummaryResponse {
    private UUID loanId;
    private BigDecimal loanAmount;
    private BigDecimal totalOutstandingPrincipal;
    private BigDecimal penaltyRate;
    private BigDecimal penaltyWaivedPercent;
    private Integer gracePeriodDays;
    private Integer totalOverdueInstallments;
    private List<OverdueInstallmentPenaltyResponse> overdueInstallments;
    private BigDecimal grossCompoundPenalty;
    private BigDecimal waivedPenaltyAmount;
    private BigDecimal netPayablePenalty;
    private BigDecimal totalPayableWithPenalty;
}
