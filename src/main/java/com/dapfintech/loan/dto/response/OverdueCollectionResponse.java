package com.dapfintech.loan.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OverdueCollectionResponse {

    private UUID loanId;

    private UUID customerId;

    private String customerName;

    private String mobileNumber;

    private String marketName;

    private BigDecimal overdueAmount;

    private Integer overdueDays;

}