package com.dapfintech.loan.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dapfintech.loan.entity.LoanClosure;

public interface LoanClosureRepository
        extends JpaRepository<LoanClosure, UUID> {

    Optional<LoanClosure> findByLoanId(
            UUID loanId
    );

    boolean existsByLoanId(
            UUID loanId
    );
}