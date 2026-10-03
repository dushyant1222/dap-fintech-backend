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
    private UUID marketId;
    private String marketName;
    private LocalDate date;
    private BigDecimal openingBalance;
    private BigDecimal collections;
    private BigDecimal incomingTransfers;
    private BigDecimal spends;
    private BigDecimal loansDisbursed;
    private BigDecimal outgoingTransfers;
    private BigDecimal officeRemittance;
    private BigDecimal cashIncomingTransfers;
    private BigDecimal cashOutgoingTransfers;
    private BigDecimal transferToOthers;
    private BigDecimal closingBalance;
    private DayBookStatus status;
    private Boolean previousDayClosed;
    private LocalDate unclosedDate;
}
