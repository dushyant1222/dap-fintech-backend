package com.dapfintech.loan.service;

import java.util.List;
import java.util.UUID;

import com.dapfintech.loan.dto.request.CreateLoanChargeRequest;
import com.dapfintech.loan.dto.request.UpdateLoanChargeRequest;
import com.dapfintech.loan.dto.response.LoanChargeResponse;

public interface LoanChargeService {

    LoanChargeResponse createCharge(
            CreateLoanChargeRequest request
    );

    LoanChargeResponse updateCharge(
            UUID chargeId,
            UpdateLoanChargeRequest request
    );

    LoanChargeResponse getChargeById(
            UUID chargeId
    );
    

    List<LoanChargeResponse> getLoanCharges(
            UUID loanId
    );

    void deleteCharge(
            UUID chargeId
    );
}