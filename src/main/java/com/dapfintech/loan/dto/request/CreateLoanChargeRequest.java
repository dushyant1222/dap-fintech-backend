package com.dapfintech.loan.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import com.dapfintech.loan.enums.ChargeType;

import lombok.Data;

@Data
public class CreateLoanChargeRequest {

    private UUID loanId;

    private ChargeType chargeType;

    private BigDecimal chargeAmount;

    private Boolean isMandatory;
}