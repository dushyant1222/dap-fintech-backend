package com.dapfintech.onboarding.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.dapfintech.loan.enums.InterestType;
import com.dapfintech.loan.enums.LoanType;
import com.dapfintech.loan.enums.RepaymentFrequency;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardSingleLoanRequest {

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;

    private String address;

    private String marketName;

    private String collectorMobile;

    @NotNull(message = "Loan type is required")
    private LoanType loanType;

    @NotNull(message = "Principal amount is required")
    private BigDecimal principalAmount;

    @NotNull(message = "Interest rate is required")
    private BigDecimal interestRate;

    @Builder.Default
    private InterestType interestType = InterestType.FLAT;

    @NotNull(message = "Tenure is required")
    private Integer tenure;

    @NotNull(message = "Repayment frequency is required")
    private RepaymentFrequency repaymentFrequency;

    @NotNull(message = "Disbursement date is required")
    private LocalDate disbursementDate;

    @Builder.Default
    private BigDecimal totalCollectedSoFar = BigDecimal.ZERO;

    private LocalDate lastPaymentDate;
}
