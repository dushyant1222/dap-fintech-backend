package com.dapfintech.loan.service;

import java.util.UUID;

import com.dapfintech.loan.dto.request.CloseLoanRequest;
import com.dapfintech.loan.dto.response.LoanClosureResponse;

public interface LoanClosureService {

    LoanClosureResponse closeLoan(
            UUID loanId,
            CloseLoanRequest request
    );

    LoanClosureResponse getLoanClosure(
            UUID loanId
    );
}