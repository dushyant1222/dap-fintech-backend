package com.dapfintech.loan.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import com.dapfintech.loan.enums.InterestType;
import com.dapfintech.loan.enums.LoanType;
import com.dapfintech.loan.enums.RepaymentFrequency;

import lombok.Data;

@Data
public class CreateLoanRequest {

    private UUID customerId;

    private LoanType loanType;

    private BigDecimal loanAmount;

    private BigDecimal interestRate;

    private InterestType interestType;

    private Integer tenure;

    private RepaymentFrequency repaymentFrequency;
    private BigDecimal processingCharge;

    private BigDecimal fileCharge;

    private BigDecimal miscellaneousCharge;
}