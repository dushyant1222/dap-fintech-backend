package com.dapfintech.loan.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.dapfintech.loan.enums.ChargeType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoanChargeResponse {

    private UUID id;

    private UUID loanId;

    private ChargeType chargeType;

    private BigDecimal chargeAmount;

    private Boolean isMandatory;
}