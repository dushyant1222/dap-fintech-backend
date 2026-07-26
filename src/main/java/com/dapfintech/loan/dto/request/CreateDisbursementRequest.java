package com.dapfintech.loan.dto.request;

import com.dapfintech.loan.enums.DisbursementMode;

import lombok.Data;

@Data
public class CreateDisbursementRequest {

    private DisbursementMode disbursementMode;

    private String transactionReference;

    private String remarks;
}