package com.dapfintech.report.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LedgerEntryDto {
    private String date;
    private BigDecimal openingBalance;
    private BigDecimal todayEdi;
    private BigDecimal credit;
    private BigDecimal debit;
    private BigDecimal remainingBalance;
}
