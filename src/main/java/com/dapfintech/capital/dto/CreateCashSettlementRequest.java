package com.dapfintech.capital.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateCashSettlementRequest {
    private UUID employeeId;
    private BigDecimal amountSettled;
    private String remarks;
}
