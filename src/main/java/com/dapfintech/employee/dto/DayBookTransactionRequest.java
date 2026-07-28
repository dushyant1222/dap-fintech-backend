package com.dapfintech.employee.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DayBookTransactionRequest {
    private String type; // SPENDS, COLLECTIONS, LOANS_DISBURSED, OFFICE_REMITTANCE, INCOMING_TRANSFER, OUTGOING_TRANSFER
    private BigDecimal amount;
    private String remarks;
}
