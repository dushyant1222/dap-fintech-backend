package com.dapfintech.loan.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dapfintech.loan.entity.LoanApproval;

@Repository
public interface LoanApprovalRepository
        extends JpaRepository<LoanApproval, UUID> {

    List<LoanApproval> findByLoanIdOrderByApprovalDateAsc(
            UUID loanId
    );
}