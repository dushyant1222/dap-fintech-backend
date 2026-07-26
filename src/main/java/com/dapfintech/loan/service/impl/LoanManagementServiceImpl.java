package com.dapfintech.loan.service.impl;

import org.springframework.stereotype.Service;

import com.dapfintech.loan.dto.response.LoanManagementDashboardResponse;
import com.dapfintech.loan.enums.LoanStatus;
import com.dapfintech.loan.repository.LoanRepository;
import com.dapfintech.loan.service.LoanManagementService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoanManagementServiceImpl
        implements LoanManagementService {

    private final LoanRepository loanRepository;

    @Override
    public LoanManagementDashboardResponse
    getDashboard() {

        return LoanManagementDashboardResponse
                .builder()

                .totalLoans(
                        loanRepository.count()
                )

                .pendingApprovalLoans(
                        loanRepository.countByLoanStatus(
                                LoanStatus.PENDING_APPROVAL
                        )
                )

                .approvedLoans(
                        loanRepository.countByLoanStatus(
                                LoanStatus.APPROVED
                        )
                )

                .rejectedLoans(
                        loanRepository.countByLoanStatus(
                                LoanStatus.REJECTED
                        )
                )

                .activeLoans(
                        loanRepository.countByLoanStatus(
                                LoanStatus.ACTIVE
                        )
                )

                .closedLoans(
                        loanRepository.countByLoanStatus(
                                LoanStatus.CLOSED
                        )
                )

                .build();

    }

}