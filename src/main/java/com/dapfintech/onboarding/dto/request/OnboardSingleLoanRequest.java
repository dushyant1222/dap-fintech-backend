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

    @NotNull(message = "Interest rate / Flat amount is required")
    private BigDecimal interestRate;

    @Builder.Default
    private InterestType interestType = InterestType.FLAT;

    private Integer tenure;

    private RepaymentFrequency repaymentFrequency;

    @NotNull(message = "Disbursement date is required")
    private LocalDate disbursementDate;

    @Builder.Default
    private BigDecimal totalCollectedSoFar = BigDecimal.ZERO;

    private LocalDate lastPaymentDate;

    public void setLoanType(Object value) {
        if (value == null) {
            this.loanType = LoanType.REGULAR;
            return;
        }
        if (value instanceof LoanType) {
            this.loanType = (LoanType) value;
            return;
        }
        String str = value.toString().trim().toUpperCase();
        if ("ELN".equals(str) || "EMERGENCY".equals(str)) {
            this.loanType = LoanType.EMERGENCY;
        } else {
            this.loanType = LoanType.REGULAR;
        }
    }

    public void setInterestType(Object value) {
        if (value == null) {
            this.interestType = InterestType.FLAT;
            return;
        }
        if (value instanceof InterestType) {
            this.interestType = (InterestType) value;
            return;
        }
        String str = value.toString().trim().toUpperCase();
        if (str.contains("DIRECT") || str.contains("FLAT_DIRECT")) {
            this.interestType = InterestType.FLAT_DIRECT;
        } else if (str.contains("MONTH")) {
            this.interestType = InterestType.FLAT_PER_MONTH;
        } else {
            this.interestType = InterestType.FLAT;
        }
    }

    public void setRepaymentFrequency(Object value) {
        if (value == null) {
            this.repaymentFrequency = RepaymentFrequency.EDI;
            return;
        }
        if (value instanceof RepaymentFrequency) {
            this.repaymentFrequency = (RepaymentFrequency) value;
            return;
        }
        String str = value.toString().trim().toUpperCase();
        if (str.contains("WEEK") || "EWI".equals(str)) {
            this.repaymentFrequency = RepaymentFrequency.EWI;
        } else if (str.contains("MONTH") || "EMI".equals(str)) {
            this.repaymentFrequency = RepaymentFrequency.EMI;
        } else {
            this.repaymentFrequency = RepaymentFrequency.EDI;
        }
    }
}
