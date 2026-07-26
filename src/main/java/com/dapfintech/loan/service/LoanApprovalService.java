package com.dapfintech.loan.service;

import java.util.List;
import java.util.UUID;

import com.dapfintech.loan.dto.request.ApproveLoanRequest;
import com.dapfintech.loan.dto.request.RejectLoanRequest;
import com.dapfintech.loan.dto.request.SubmitLoanRequest;
import com.dapfintech.loan.dto.response.LoanApprovalResponse;

public interface LoanApprovalService {

    void submitLoan(
            UUID loanId,
            SubmitLoanRequest request
    );

    void approveLoan(
            UUID loanId,
            ApproveLoanRequest request
    );
    

    void rejectLoan(
            UUID loanId,
            RejectLoanRequest request
    );

    void resubmitLoan(
            UUID loanId,
            SubmitLoanRequest request
    );

    List<LoanApprovalResponse> getApprovalHistory(
            UUID loanId
    );
}