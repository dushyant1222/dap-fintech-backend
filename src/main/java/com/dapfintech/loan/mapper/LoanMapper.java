package com.dapfintech.loan.mapper;

import org.springframework.stereotype.Component;

import com.dapfintech.loan.dto.response.LoanResponse;
import com.dapfintech.loan.entity.Loan;

@Component
public class LoanMapper {

    public LoanResponse toResponse(
            Loan loan
    ) {

        return LoanResponse.builder()

                .id(
                        loan.getId()
                )

                .customerId(
                        loan.getCustomer().getId()
                )

                .customerName(
                        loan.getCustomer().getFirstName()
                                + " "
                                + loan.getCustomer().getLastName()
                )

                .customerCode(
                        loan.getCustomer().getCustomerCode()
                )
                .employeeId(
                        loan.getCreatedBy() != null
                                ? loan.getCreatedBy().getId()
                                : null
                )

                .employeeName(
                        loan.getCreatedBy() != null
                                ? loan.getCreatedBy().getFullName()
                                : null
                )
                .approvalDate(
                	    loan.getApprovalDate()
                	)

                	.disbursementDate(
                	    loan.getDisbursementDate()
                	)
                .mobileNumber(
                	    loan.getCustomer().getMobileNumber()
                	)

                .loanType(
                        loan.getLoanType()
                )

                .loanAmount(
                        loan.getLoanAmount()
                )

                .approvedAmount(
                        loan.getApprovedAmount()
                )

                .disbursedAmount(
                        loan.getDisbursedAmount()
                )

                .interestType(
                        loan.getInterestType()
                )

                .interestRate(
                        loan.getInterestRate()
                )

                .tenure(
                        loan.getTenure()
                )

                .repaymentFrequency(
                        loan.getRepaymentFrequency()
                )

                .loanStatus(
                        loan.getLoanStatus()
                )

                .applicationDate(
                        loan.getApplicationDate()
                )

                .build();
    }
}