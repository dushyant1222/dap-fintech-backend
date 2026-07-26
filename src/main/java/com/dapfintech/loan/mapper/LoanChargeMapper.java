package com.dapfintech.loan.mapper;

import org.springframework.stereotype.Component;

import com.dapfintech.loan.dto.response.LoanChargeResponse;
import com.dapfintech.loan.entity.LoanCharge;

@Component
public class LoanChargeMapper {

    public LoanChargeResponse toResponse(
            LoanCharge charge
    ) {

        return LoanChargeResponse.builder()
                .id(charge.getId())
                .loanId(
                        charge.getLoan().getId()
                )
                .chargeType(
                        charge.getChargeType()
                )
                .chargeAmount(
                        charge.getChargeAmount()
                )
                .isMandatory(
                        charge.getIsMandatory()
                )
                .build();
    }
}