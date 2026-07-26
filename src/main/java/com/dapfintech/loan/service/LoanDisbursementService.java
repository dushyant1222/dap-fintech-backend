package com.dapfintech.loan.service;

import java.util.UUID;

import com.dapfintech.loan.dto.request.CreateDisbursementRequest;
import com.dapfintech.loan.dto.response.DisbursementResponse;

public interface LoanDisbursementService {

    DisbursementResponse disburseLoan(
            UUID loanId,
            CreateDisbursementRequest request
    );

    DisbursementResponse getDisbursement(
            UUID loanId
    );
}