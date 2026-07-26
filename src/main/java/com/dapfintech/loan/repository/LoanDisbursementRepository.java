package com.dapfintech.loan.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dapfintech.loan.entity.LoanDisbursement;

@Repository
public interface LoanDisbursementRepository
        extends JpaRepository<LoanDisbursement, UUID> {

    Optional<LoanDisbursement> findByLoanId(
            UUID loanId
    );
}