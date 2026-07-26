package com.dapfintech.loan.service;

import java.util.List;
import java.util.UUID;

import com.dapfintech.loan.dto.response.RepaymentScheduleResponse;

public interface LoanRepaymentScheduleService {

    void generateSchedule(
            UUID loanId
    );

    List<RepaymentScheduleResponse>
    getLoanSchedule(
            UUID loanId
    );
    //List<RepaymentScheduleResponse> getLoanRepaymentSchedule(UUID loanId);
}