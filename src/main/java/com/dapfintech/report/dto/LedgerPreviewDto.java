package com.dapfintech.report.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class LedgerPreviewDto {
    private String loanCode;
    private String loanNumber;
    private String customerName;
    private String marketName;
    private BigDecimal totalLoanAmount;
    private String interestRate;
    private BigDecimal disbursedAmount;
    private String loanType;
    private String tenure;
    private BigDecimal amountCollectedToday;
    private String status;

    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalInterest;
    private BigDecimal outstandingBalance;
    private BigDecimal closingBalance;

    private List<LedgerPreviewScheduleDto> schedules;
    private List<LedgerEntryDto> ledgerEntries;
}

