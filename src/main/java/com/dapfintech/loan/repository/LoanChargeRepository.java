package com.dapfintech.loan.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.dapfintech.loan.entity.LoanCharge;

@Repository
public interface LoanChargeRepository
        extends JpaRepository<LoanCharge, UUID> {

    List<LoanCharge> findByLoanId(
            UUID loanId
    );

    @Query(value = """
            SELECT charge_type, COALESCE(SUM(charge_amount), 0) AS total
            FROM loan_charges
            GROUP BY charge_type
            """, nativeQuery = true)
    List<Object[]> getSumChargesByType();
}