package com.dapfintech.employee.dto;

import com.dapfintech.employee.enums.DayBookStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDayBookResponse {
    private UUID id;
    private UUID marketId;
    private String marketName;
    private LocalDate date;
    private BigDecimal openingBalance;
    private BigDecimal collections;
    private BigDecimal incomingTransfers;
    private BigDecimal cashIncomingTransfers;
    private BigDecimal spends;
    private BigDecimal loansDisbursed;
    private BigDecimal outgoingTransfers;
    private BigDecimal cashOutgoingTransfers;
    private BigDecimal officeRemittance;
    private BigDecimal transferToOthers;
    private BigDecimal closingBalance;
    private DayBookStatus status;
    private boolean previousDayClosed;
    private LocalDate unclosedDate;
    private List<EmployeeDayBookSummary> employeeSummaries;
    private String closedByEmployeeName;
    private List<String> marketEmployeeNames;
}
