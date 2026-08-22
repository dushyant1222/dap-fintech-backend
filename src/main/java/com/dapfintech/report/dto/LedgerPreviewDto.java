package com.dapfintech.report.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class LedgerPreviewDto {
    private String loanCode;
    private String customerName;
    private String marketName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal disbursedAmount;
    private BigDecimal totalInterest;
    private String loanType;
    private String status;
    private BigDecimal outstandingBalance;
    private BigDecimal closingBalance;

    private List<LedgerPreviewScheduleDto> schedules;
}
