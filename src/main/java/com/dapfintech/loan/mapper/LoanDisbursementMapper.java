package com.dapfintech.loan.mapper;

import org.springframework.stereotype.Component;

import com.dapfintech.loan.dto.response.DisbursementResponse;
import com.dapfintech.loan.entity.LoanDisbursement;

@Component
public class LoanDisbursementMapper {

    public DisbursementResponse toResponse(
            LoanDisbursement disbursement
    ) {

        return DisbursementResponse.builder()
                .id(disbursement.getId())
                .loanId(disbursement.getLoan().getId())
                .disbursementMode(
                        disbursement.getDisbursementMode()
                )
                .approvedAmount(
                        disbursement.getApprovedAmount()
                )
                .totalCharges(
                        disbursement.getTotalCharges()
                )
                .netDisbursedAmount(
                        disbursement.getNetDisbursedAmount()
                )
                .transactionReference(
                        disbursement.getTransactionReference()
                )
                .disbursementDate(
                        disbursement.getDisbursementDate()
                )
                .build();
    }
}