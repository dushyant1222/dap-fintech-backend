package com.dapfintech.loan.service;

import org.springframework.data.domain.Page;
import java.util.List;
import java.util.UUID;

import com.dapfintech.loan.dto.request.CalculateEmiRequest;
import com.dapfintech.loan.dto.request.CreateLoanRequest;
import com.dapfintech.loan.dto.request.LoanFilterRequest;
import com.dapfintech.loan.dto.request.UpdateLoanRequest;
import com.dapfintech.loan.dto.response.CalculateEmiResponse;
import com.dapfintech.loan.dto.response.LoanResponse;
import com.dapfintech.loan.dto.response.LoanStatisticsResponse;
import com.dapfintech.loan.dto.response.LoanSummaryResponse;
import com.dapfintech.loan.dto.response.LoanBureauResponse;
import com.dapfintech.loan.enums.LoanStatus;

public interface LoanService {
	LoanStatisticsResponse getLoanStatistics();

    LoanResponse createLoan(
            CreateLoanRequest request
    );
    Page<LoanResponse> filterLoans(
            LoanFilterRequest filter,
            int page,
            int size
    );
    Page<LoanResponse> searchLoans(

            String keyword,

            int page,

            int size

    );
    LoanResponse updateLoan(
            UUID loanId,
            UpdateLoanRequest request
    );
    CalculateEmiResponse calculateEmi(
            CalculateEmiRequest request
    );

    Page<LoanResponse> getAllLoans(

            int page,

            int size,

            LoanStatus status

    );
    LoanResponse getLoanById(
            UUID loanId
    );

    List<LoanResponse> getCustomerLoans(
            UUID customerId
    );

    void deleteLoan(
            UUID loanId
    );
    LoanSummaryResponse getLoanSummary(
            UUID loanId
    );

    Page<LoanBureauResponse> getLoanBureau(
            int page,
            int size
    );
}