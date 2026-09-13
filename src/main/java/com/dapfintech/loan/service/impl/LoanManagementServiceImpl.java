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
    public LoanManagementDashboardResponse getDashboard() {
        long pending = 0, approved = 0, rejected = 0, active = 0, closed = 0, total = 0;
        java.util.List<Object[]> rows = loanRepository.countGroupedByLoanStatus();
        for (Object[] row : rows) {
            LoanStatus status = (LoanStatus) row[0];
            long cnt = ((Number) row[1]).longValue();
            total += cnt;
            if (status == LoanStatus.PENDING_APPROVAL) pending = cnt;
            else if (status == LoanStatus.APPROVED) approved = cnt;
            else if (status == LoanStatus.REJECTED) rejected = cnt;
            else if (status == LoanStatus.ACTIVE) active = cnt;
            else if (status == LoanStatus.CLOSED) closed = cnt;
        }

        return LoanManagementDashboardResponse.builder()
                .totalLoans(total)
                .pendingApprovalLoans(pending)
                .approvedLoans(approved)
                .rejectedLoans(rejected)
                .activeLoans(active)
                .closedLoans(closed)
                .build();
    }

}