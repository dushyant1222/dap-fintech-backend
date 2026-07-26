package com.dapfintech.capital.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PivotRowResponse {
    private String groupLabel;
    private BigDecimal capitalIn;
    private BigDecimal disbursedAmount;
    private BigDecimal marketBalance;
    private BigDecimal collectedAmount;
    private BigDecimal settledAmount;
    private BigDecimal pendingEmployeeBalance;
    private BigDecimal expenses;
    private BigDecimal netEarnings;
}
