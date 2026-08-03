package com.dapfintech.loan.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.dapfintech.customer.entity.Customer;
import com.dapfintech.loan.enums.InterestType;
import com.dapfintech.loan.enums.LoanStatus;
import com.dapfintech.loan.enums.LoanType;
import com.dapfintech.loan.enums.RepaymentFrequency;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoanResponse {

    private UUID id;
    
    private String loanCode;
    
    private String customerName;

    private String customerCode;
    private String mobileNumber;
    private UUID employeeId;

    private String employeeName;

    private UUID customerId;

    private LoanType loanType;

    private BigDecimal loanAmount;

    private BigDecimal approvedAmount;

    private BigDecimal disbursedAmount;

    private InterestType interestType;

    private BigDecimal interestRate;

    private LocalDateTime approvalDate;

    private LocalDateTime disbursementDate;
    private Integer tenure;

    private RepaymentFrequency repaymentFrequency;

    private LoanStatus loanStatus;

    private LocalDateTime applicationDate;

}