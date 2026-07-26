package com.dapfintech.dashboard.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessGrowthResponse {

    // ===========================
    // Capital & Disbursement Summary
    // ===========================
    private BigDecimal totalCapitalInjected;
    private BigDecimal totalDisbursedPrincipal;
    private BigDecimal balanceInMarket;
    private BigDecimal balanceOnEmployees;
    private BigDecimal vaultAvailableCash;
    private BigDecimal totalCollections;
    private BigDecimal totalSettledCash;

    // ===========================
    // Automated Profit & Revenue Summary
    // ===========================
    private BigDecimal totalInterestExpected;
    private BigDecimal totalInterestCollected;
    private BigDecimal totalPenaltyAccrued;
    private BigDecimal totalPenaltyCollected;
    private BigDecimal processingFeesCollected;
    private BigDecimal fileChargesCollected;
    private BigDecimal miscChargesCollected;
    private BigDecimal totalExpenses;

    // Fully automated net profit metrics
    private BigDecimal realizedNetProfit;
    private BigDecimal projectedNetProfit;
    private String profitTrendText;

    // ===========================
    // Time-Series Chart Data
    // ===========================
    private List<String> chartLabels;
    private List<BigDecimal> chartValues;

    // ===========================
    // Loan Type Analytics
    // ===========================
    private Long regularActiveCount;
    private Long regularEmiCount;
    private Long regularEdiCount;
    private Long regularEwiCount;
    private Long emergencyActiveCount;
}
