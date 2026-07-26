package com.dapfintech.loan.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PendingCollectionResponse {

    private UUID loanId;

    private UUID scheduleId;

    private UUID customerId;

    private String customerName;

    private String mobileNumber;

    private Integer installmentNumber;

    private LocalDate dueDate;

    private BigDecimal installmentAmount;

    private BigDecimal outstandingAmount;

    private Long overdueDays;

}