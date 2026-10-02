package com.dapfintech.employee.dto;

import com.dapfintech.employee.enums.DayBookStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDayBookSummary {
    private UUID employeeId;
    private String employeeName;
    private String employeeCode;
    private UUID dayBookId;
    private BigDecimal openingBalance;
    private BigDecimal collections;
    private BigDecimal incomingTransfers;
    private BigDecimal cashIncomingTransfers;
    private BigDecimal spends;
    private BigDecimal loansDisbursed;
    private BigDecimal outgoingTransfers;
    private BigDecimal cashOutgoingTransfers;
    private BigDecimal officeRemittance;
    private BigDecimal closingBalance;
    private DayBookStatus status;
}
