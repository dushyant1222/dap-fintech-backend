package com.dapfintech.loan.service;

import java.util.UUID;

import com.dapfintech.loan.dto.response.LoanDetailsResponse;

public interface LoanDetailsService {

    LoanDetailsResponse getLoanDetails(
            UUID loanId
    );

    
}