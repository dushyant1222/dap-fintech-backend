package com.dapfintech.dashboard.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardResponse {

    // ===========================
    // Employee Summary
    // ===========================
    private Long activeEmployees;

    // ===========================
    // Customer Summary
    // ===========================
    private Long totalCustomers;

    // ===========================
    // Loan Summary
    // ===========================
    private Long totalLoans;
    private Long activeLoans;
    private Long approvedLoans;
    private Long pendingLoans;
    private Long rejectedLoans;
    private Long closedLoans;

    // ===========================
    // EMI Summary
    // ===========================
    private Long pendingEmi;
    private Long overdueEmi;

    // ===========================
    // Collection Summary
    // ===========================
    private BigDecimal todayCollection;
    private BigDecimal monthCollection;

    // ===========================
    // Portfolio Summary
    // ===========================
    private BigDecimal totalLoanPortfolio;

    // ===========================
    // Overdue Summary
    // ===========================
    private Long overdueCustomers;
    private Long overdueLoans;
    private BigDecimal overdueAmount;

    // ===========================
    // Top Performer
    // ===========================
    private String topCollectorName;
    private BigDecimal topCollectorAmount;
}