package com.dapfintech.loan.dto.request;

import java.math.BigDecimal;

import com.dapfintech.loan.enums.ChargeType;

import lombok.Data;

@Data
public class UpdateLoanChargeRequest {

    private ChargeType chargeType;

    private BigDecimal chargeAmount;

    private Boolean isMandatory;
}