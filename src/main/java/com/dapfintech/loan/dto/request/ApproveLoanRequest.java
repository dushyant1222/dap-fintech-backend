package com.dapfintech.loan.dto.request;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ApproveLoanRequest {

    private BigDecimal approvedAmount;

    private String remarks;
}