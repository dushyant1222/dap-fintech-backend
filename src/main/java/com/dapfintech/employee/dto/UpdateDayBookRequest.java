package com.dapfintech.employee.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateDayBookRequest {
    private BigDecimal collections;
    private BigDecimal incomingTransfers;
    private BigDecimal spends;
    private BigDecimal loansDisbursed;
    private BigDecimal outgoingTransfers;
    private BigDecimal officeRemittance;
}
