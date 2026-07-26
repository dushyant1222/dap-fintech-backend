package com.dapfintech.loan.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.dapfintech.loan.enums.LoanStatus;
import com.dapfintech.loan.enums.LoanType;

import lombok.Data;

@Data
public class LoanFilterRequest {

    private String keyword;

    private LoanStatus status;

    private LoanType loanType;

    private UUID employeeId;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private LocalDate fromDate;

    private LocalDate toDate;

}