package com.dapfintech.capital.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CapitalSummaryResponse {
    private BigDecimal totalCapitalInjected;
    private BigDecimal totalDisbursedPrincipal;
    private BigDecimal balanceInMarket;
    private BigDecimal totalCollections;
    private BigDecimal totalSettledCash;
    private BigDecimal balanceOnEmployees;
    private BigDecimal totalExpenses;
    private BigDecimal vaultAvailableCash;
    private BigDecimal expectedTotalReturn;
    private BigDecimal totalLoanAmount;
    private BigDecimal totalInterestExpected;
    private BigDecimal totalInterestCollected;
    private BigDecimal totalChargesAndPenalties;
    private BigDecimal capitalGrowth;
    private BigDecimal realizedGrowth;
}
