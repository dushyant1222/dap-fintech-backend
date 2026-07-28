package com.dapfintech.employee.dto;

import com.dapfintech.employee.enums.DayBookStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class DayBookResponse {
    private UUID id;
    private UUID employeeId;
    private LocalDate date;
    private BigDecimal openingBalance;
    private BigDecimal collections;
    private BigDecimal incomingTransfers;
    private BigDecimal spends;
    private BigDecimal loansDisbursed;
    private BigDecimal outgoingTransfers;
    private BigDecimal officeRemittance;
    private BigDecimal closingBalance;
    private DayBookStatus status;
}
