package com.dapfintech.loan.service;

import java.util.UUID;
import com.dapfintech.loan.dto.request.CloseSpecialLoanRequest;
import com.dapfintech.loan.dto.request.UpdatePenaltySettingsRequest;
import com.dapfintech.loan.dto.response.LoanClosureResponse;
import com.dapfintech.loan.dto.response.LoanPenaltySummaryResponse;

public interface LoanPenaltyService {
    LoanPenaltySummaryResponse calculatePenalty(UUID loanId);
    LoanPenaltySummaryResponse updatePenaltySettings(UUID loanId, UpdatePenaltySettingsRequest request);
    LoanClosureResponse closeOnSpecialCondition(UUID loanId, CloseSpecialLoanRequest request);
}
