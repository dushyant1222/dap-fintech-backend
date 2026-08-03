package com.dapfintech.loan.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dapfintech.loan.entity.Loan;
import com.dapfintech.loan.enums.LoanStatus;
import com.dapfintech.loan.enums.LoanType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
@Repository
public interface LoanRepository
        extends JpaRepository<Loan, UUID>, JpaSpecificationExecutor<Loan> {

    List<Loan> findByLoanTypeAndLoanStatus(LoanType loanType, LoanStatus loanStatus);

    List<Loan> findByCustomerId(
            UUID customerId
    );
    
    long countByCustomerId(UUID customerId);

    Page<Loan> findByLoanStatus(
            LoanStatus loanStatus,
            Pageable pageable
    );
    
    boolean existsByCustomerIdAndLoanStatusIn(
            UUID customerId,
            Collection<LoanStatus> statuses
    );

    Page<Loan> findByCustomerMarketIdAndLoanStatus(
            UUID marketId,
            LoanStatus loanStatus,
            Pageable pageable
    );
    Page<Loan> findByCustomerFirstNameContainingIgnoreCaseOrCustomerLastNameContainingIgnoreCaseOrCustomerMobileNumberContaining(

            String firstName,

            String lastName,

            String mobileNumber,

            Pageable pageable

    );
    Page<Loan> findByCustomerMarketIdAndCustomerFirstNameContainingIgnoreCaseOrCustomerMarketIdAndCustomerLastNameContainingIgnoreCaseOrCustomerMarketIdAndCustomerMobileNumberContaining(

            UUID marketId1,

            String firstName,

            UUID marketId2,

            String lastName,

            UUID marketId3,

            String mobile,

            Pageable pageable

    );
    Long countByCustomerMarketId(
            UUID marketId
    );

    Long countByCustomerMarketIdAndLoanStatus(
            UUID marketId,
            LoanStatus loanStatus
    );
    Page<Loan> findByCustomerMarketId(
            UUID marketId,
            Pageable pageable
    );
    
    Long countByLoanStatus(
            LoanStatus loanStatus
    );
    
    
    @Query(value = """
    		SELECT COUNT(*)
    		FROM loans l
    		JOIN customers c
    		ON l.customer_id = c.id
    		JOIN employee_market_assignments ema
    		ON ema.market_id = c.market_id
    		WHERE ema.employee_id = :employeeId
    		AND ema.is_active = true
    		AND l.loan_status = 'ACTIVE'
    		""", nativeQuery = true)
    		Long countActiveLoansByEmployee(
    		        @Param("employeeId") UUID employeeId
    		);
    
    
    @Query("""
    		SELECT COALESCE(
    		       SUM(l.approvedAmount),
    		       0
    		)
    		FROM Loan l
    		WHERE l.loanStatus =
    		      com.dapfintech.loan.enums.LoanStatus.ACTIVE
    		""")
    		BigDecimal getTotalLoanPortfolio();
}