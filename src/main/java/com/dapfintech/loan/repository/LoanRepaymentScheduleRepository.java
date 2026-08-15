package com.dapfintech.loan.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dapfintech.loan.entity.LoanRepaymentSchedule;
import com.dapfintech.loan.enums.RepaymentStatus;
import com.dapfintech.report.projection.BucketWiseOverdueProjection;
import com.dapfintech.report.projection.OverdueCustomerProjection;

public interface LoanRepaymentScheduleRepository
        extends JpaRepository<
                LoanRepaymentSchedule,
                UUID> {

    List<LoanRepaymentSchedule>
    findByLoanIdOrderByInstallmentNumberAsc(
            UUID loanId
    );
    List<LoanRepaymentSchedule> findByRepaymentStatus(RepaymentStatus repaymentStatus);
    Long countByRepaymentStatus(
            RepaymentStatus repaymentStatus);
    
    
    @Query(value = """
    		SELECT COUNT(*)
    		FROM loan_repayment_schedules s
    		JOIN loans l
    		ON s.loan_id = l.id
    		JOIN customers c
    		ON l.customer_id = c.id
    		JOIN employee_market_assignments ema
    		ON ema.market_id = c.market_id
    		WHERE ema.employee_id = :employeeId
    		AND ema.is_active = true
    		AND s.repayment_status='PENDING'
    		AND s.due_date=CURRENT_DATE
    		""", nativeQuery = true)
    		Long countTodayPendingCollections(
    		        UUID employeeId
    		);
    
    
    @Query(value = """
    		SELECT COUNT(DISTINCT c.id)
    		FROM loan_repayment_schedules s
    		JOIN loans l
    		ON s.loan_id = l.id
    		JOIN customers c
    		ON l.customer_id = c.id
    		JOIN employee_market_assignments ema
    		ON ema.market_id = c.market_id
    		WHERE ema.employee_id = :employeeId
    		AND ema.is_active = true
    		AND s.repayment_status='PENDING'
    		AND s.due_date<CURRENT_DATE
    		""", nativeQuery = true)
    		Long countOverdueCustomers(
    		        UUID employeeId
    		);
    
    

    @Query(value = """
    		SELECT

    		c.id AS customerId,

    		CONCAT(
    		c.first_name,
    		' ',
    		c.last_name
    		) AS customerName,

    		c.mobile_number AS mobileNumber,

    		m.market_name AS marketName,

    		l.id AS loanId,

    		SUM(s.outstanding_amount) AS overdueAmount,

    		MAX(CAST(
    		CURRENT_DATE - s.due_date
    		AS INTEGER
    		)) AS overdueDays

    		FROM loan_repayment_schedules s

    		JOIN loans l
    		ON s.loan_id=l.id

    		JOIN customers c
    		ON l.customer_id=c.id

    		LEFT JOIN markets m
    		ON c.market_id=m.id

    		WHERE c.market_id=:marketId

    		AND s.repayment_status!='PAID'

    		AND s.due_date<CURRENT_DATE

            GROUP BY c.id, c.first_name, c.last_name, c.mobile_number, m.market_name, l.id

    		ORDER BY overdueDays DESC

    		""", nativeQuery = true)
    		List<OverdueCustomerProjection>
    		getOverdueCustomersByMarket(
    		        UUID marketId
    		);
    
    @Query(value = """
    		SELECT COUNT(DISTINCT c.id)

    		FROM loan_repayment_schedules s

    		JOIN loans l
    		ON s.loan_id=l.id

    		JOIN customers c
    		ON l.customer_id=c.id

    		WHERE c.market_id=:marketId

    		AND s.repayment_status!='PAID'

    		AND s.due_date<CURRENT_DATE
    		""", nativeQuery = true)
    		Long getTotalOverdueCustomersByMarket(
    		        UUID marketId
    		);
    
    @Query("""
    	       SELECT COUNT(s)
    	       FROM LoanRepaymentSchedule s
    	       WHERE s.repaymentStatus <> com.dapfintech.loan.enums.RepaymentStatus.PAID
    	       AND s.dueDate < CURRENT_DATE
    	       """)
    	Long countOverdueEmi();
    
    @Query(value = """
    		SELECT
    		CASE

    		WHEN CURRENT_DATE - due_date
    		BETWEEN 1 AND 30
    		THEN '0-30 Days'

    		WHEN CURRENT_DATE - due_date
    		BETWEEN 31 AND 60
    		THEN '31-60 Days'

    		WHEN CURRENT_DATE - due_date
    		BETWEEN 61 AND 90
    		THEN '61-90 Days'

    		ELSE '90+ Days'

    		END AS bucket,

    		SUM(outstanding_amount)
    		AS overdueAmount

    		FROM loan_repayment_schedules

    		WHERE repayment_status != 'PAID'
    		AND due_date < CURRENT_DATE

    		GROUP BY bucket

    		ORDER BY bucket
    		""", nativeQuery = true)
    		List<BucketWiseOverdueProjection>
    		getBucketWiseOverdueReport();
    
    @Query(value = """
    		SELECT COUNT(DISTINCT l.customer_id)
    		FROM loan_repayment_schedules s
    		JOIN loans l
    		ON s.loan_id = l.id
    		WHERE s.repayment_status != 'PAID'
    		AND s.due_date < CURRENT_DATE
    		""", nativeQuery = true)
    		Long getTotalOverdueCustomers();
    
    @Query(value = """
    		SELECT COUNT(DISTINCT s.loan_id)
    		FROM loan_repayment_schedules s
    		WHERE s.repayment_status != 'PAID'
    		AND s.due_date < CURRENT_DATE
    		""", nativeQuery = true)
    		Long getTotalOverdueLoans();
    
    @Query(value = """
    		SELECT COALESCE(
    		       SUM(s.outstanding_amount),
    		       0
    		)
    		FROM loan_repayment_schedules s
    		WHERE s.repayment_status != 'PAID'
    		AND s.due_date < CURRENT_DATE
    		""", nativeQuery = true)
    		BigDecimal getTotalOverdueAmount();
    
    @Query(value = """
    		SELECT
    		    c.id AS customerId,

    		    CONCAT(
    		        c.first_name,
    		        ' ',
    		        c.last_name
    		    ) AS customerName,

    		    c.mobile_number AS mobileNumber,

    		    m.market_name AS marketName,

    		    l.id AS loanId,

    		    SUM(s.outstanding_amount) AS overdueAmount,

    		    MAX(CAST(
    		        CURRENT_DATE - s.due_date
    		        AS INTEGER
    		    )) AS overdueDays

    		FROM loan_repayment_schedules s

    		JOIN loans l
    		ON s.loan_id = l.id

    		JOIN customers c
    		ON l.customer_id = c.id

    		LEFT JOIN markets m
    		ON c.market_id = m.id

    		WHERE s.repayment_status != 'PAID'
    		AND s.due_date < CURRENT_DATE

            GROUP BY c.id, c.first_name, c.last_name, c.mobile_number, m.market_name, l.id

    		ORDER BY overdueDays DESC
    		""", nativeQuery = true)
    		List<OverdueCustomerProjection>
    		getOverdueCustomers();

    // ── Bulk UPDATE (replaces load-all + save-in-loop) ─────────────────────
    @Modifying
    @Query(value = """
            UPDATE loan_repayment_schedules
            SET repayment_status = 'OVERDUE'
            WHERE repayment_status = 'PENDING'
            AND due_date < CURRENT_DATE
            """, nativeQuery = true)
    void bulkMarkOverdue();

    @Modifying
    @Query(value = """
            UPDATE loan_repayment_schedules
            SET repayment_status = 'PAID',
                paid_amount = installment_amount,
                outstanding_amount = 0
            WHERE loan_id = :loanId
            AND (outstanding_amount IS NULL OR outstanding_amount > 0)
            """, nativeQuery = true)
    void bulkMarkLoanSchedulesPaid(@Param("loanId") UUID loanId);

    // ── Aggregate SUM queries (replaces stream().reduce()) ─────────────────
    @Query(value = """
            SELECT COALESCE(SUM(outstanding_amount), 0)
            FROM loan_repayment_schedules
            WHERE loan_id = :loanId
            """, nativeQuery = true)
    BigDecimal getSumOutstandingByLoan(@Param("loanId") UUID loanId);

    @Query(value = """
            SELECT COALESCE(installment_amount, 0)
            FROM loan_repayment_schedules
            WHERE loan_id = :loanId
            ORDER BY installment_number ASC
            LIMIT 1
            """, nativeQuery = true)
    BigDecimal getFirstInstallmentAmountByLoan(@Param("loanId") UUID loanId);
}
