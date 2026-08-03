package com.dapfintech.loan.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dapfintech.loan.entity.LoanCollection;
import com.dapfintech.loan.entity.LoanRepaymentSchedule;
import com.dapfintech.loan.enums.RepaymentStatus;
import com.dapfintech.loan.projection.CollectionHistoryProjection;
import com.dapfintech.loan.projection.PendingCollectionProjection;
import com.dapfintech.loan.projection.TodayScheduleProjection;
import com.dapfintech.report.projection.EmployeeCollectionProjection;
import com.dapfintech.report.projection.MarketCollectionProjection;
import com.dapfintech.report.projection.MarketPerformanceProjection;
import com.dapfintech.report.projection.MonthlyCollectionProjection;
import com.dapfintech.report.projection.OverdueCustomerProjection;
import com.dapfintech.report.projection.TopCollectorProjection;

@Repository
public interface LoanCollectionRepository extends JpaRepository<LoanCollection, UUID> {

    List<LoanCollection> findByLoanId(UUID loanId);
    
    List<LoanCollection> findAllByOrderByCollectionDateDesc();
    
    Long countByLoanCustomerMarketId(
            UUID marketId
    );
    
    @Query("""

    		SELECT COALESCE(
    		SUM(c.collectedAmount),
    		0)

    		FROM LoanCollection c

    		WHERE c.loan.customer.market.id=:marketId

    		""")
    		BigDecimal getTotalCollectionAmount(
    		        UUID marketId
    		);
    
    @Query("""
    		SELECT COUNT(c)
    		FROM LoanCollection c
    		WHERE c.collectedBy.id = :employeeId
    		AND c.collectionStatus = com.dapfintech.loan.enums.CollectionStatus.SUCCESS
    		AND FUNCTION('DATE', c.collectionDate) = CURRENT_DATE
    		""")
    		Long countTodayCollectionsByEmployee(
    		        @Param("employeeId") UUID employeeId
    		);
    
    
    @Query(value = """
    		SELECT
    		    m.id AS marketId,
    		    m.market_name AS marketName,

    		    COUNT(DISTINCT c.id) AS totalCustomers,

    		    COUNT(DISTINCT l.id) AS totalLoans,

    		    COALESCE(
    		        SUM(lc.collected_amount),
    		        0
    		    ) AS totalCollection

    		FROM markets m

    		LEFT JOIN customers c
    		ON c.market_id = m.id

    		LEFT JOIN loans l
    		ON l.customer_id = c.id

    		LEFT JOIN loan_collections lc
    		ON lc.loan_id = l.id

    		GROUP BY
    		    m.id,
    		    m.market_name

    		ORDER BY totalCollection DESC
    		""", nativeQuery = true)
    		List<MarketPerformanceProjection>
    		getMarketPerformanceReport();
    @Query(value = """
            SELECT COUNT(*)
            FROM loan_repayment_schedules s
            WHERE s.repayment_status != 'PAID'
            AND s.due_date <= CURRENT_DATE
            """, nativeQuery = true)
    Long getPendingCollectionCount();
    
    @Query(value = """
    		SELECT

    		l.id AS loanId,

    		CAST(MIN(CAST(s.id AS varchar)) AS uuid) AS scheduleId,

    		c.id AS customerId,

    		CONCAT(
    		c.first_name,
    		' ',
    		c.last_name
    		) AS customerName,

    		c.mobile_number AS mobileNumber,

    		MIN(s.installment_number) AS installmentNumber,

    		MIN(s.due_date) AS dueDate,

    		SUM(s.installment_amount) AS installmentAmount,

    		SUM(s.outstanding_amount) AS outstandingAmount

    		FROM loan_repayment_schedules s

    		JOIN loans l
    		ON s.loan_id=l.id

    		JOIN customers c
    		ON l.customer_id=c.id

    		WHERE s.repayment_status != 'PAID'

    		AND s.due_date <= CURRENT_DATE

    		AND l.id IN (
    		    SELECT s2.loan_id 
    		    FROM loan_repayment_schedules s2 
    		    WHERE s2.due_date = CURRENT_DATE 
    		    AND s2.repayment_status != 'PAID'
    		)

            GROUP BY l.id, c.id, c.first_name, c.last_name, c.mobile_number

    		ORDER BY c.first_name
    		""", nativeQuery = true)
    		List<TodayScheduleProjection> getTodaySchedule();
    
    @Query(value = """
    		SELECT

    		l.id AS loanId,

    		CAST(MIN(CAST(s.id AS varchar)) AS uuid) AS scheduleId,

    		c.id AS customerId,

    		CONCAT(
    		c.first_name,
    		' ',
    		c.last_name
    		) AS customerName,

    		c.mobile_number AS mobileNumber,

    		MIN(s.installment_number) AS installmentNumber,

    		MIN(s.due_date) AS dueDate,

    		SUM(s.installment_amount) AS installmentAmount,

    		SUM(s.outstanding_amount) AS outstandingAmount,

    		MAX(GREATEST(
    		CURRENT_DATE - s.due_date,
    		0
    		)) AS overdueDays

    		FROM loan_repayment_schedules s

    		JOIN loans l
    		ON s.loan_id = l.id

    		JOIN customers c
    		ON l.customer_id = c.id

    		WHERE s.repayment_status != 'PAID'
    		AND s.due_date <= CURRENT_DATE

            GROUP BY l.id, c.id, c.first_name, c.last_name, c.mobile_number

    		ORDER BY
    		MIN(s.due_date) ASC
    		""", nativeQuery = true)
    		List<PendingCollectionProjection> getPendingCollections();
    
    @Query(value = """
    		SELECT

    		lc.id AS collectionId,

    		l.id AS loanId,

    		lc.receipt_number AS receiptNumber,

    		CONCAT(
    		c.first_name,
    		' ',
    		c.last_name
    		) AS customerName,

    		c.mobile_number AS mobileNumber,

    		lc.collected_amount AS collectedAmount,

    		lc.collection_mode AS collectionMode,

    		lc.collection_status AS collectionStatus,

    		lc.collection_date AS collectionDate,

    		u.full_name AS collectedBy

    		FROM loan_collections lc

    		JOIN loans l
    		ON lc.loan_id = l.id

    		JOIN customers c
    		ON l.customer_id = c.id

    		JOIN users u
    		ON lc.collected_by = u.id

    		ORDER BY lc.collection_date DESC
    		""", nativeQuery = true)
    		List<CollectionHistoryProjection>
    		getCollectionHistory();
    
    @Query(value = """
    		SELECT

    		lc.id AS collectionId,

    		l.id AS loanId,

    		lc.receipt_number AS receiptNumber,

    		CONCAT(
    		c.first_name,
    		' ',
    		c.last_name
    		) AS customerName,

    		c.mobile_number AS mobileNumber,

    		lc.collected_amount AS collectedAmount,

    		lc.collection_mode AS collectionMode,

    		lc.collection_status AS collectionStatus,

    		lc.collection_date AS collectionDate,

    		u.full_name AS collectedBy

    		FROM loan_collections lc

    		JOIN loans l
    		ON lc.loan_id = l.id

    		JOIN customers c
    		ON l.customer_id = c.id

    		JOIN users u
    		ON lc.collected_by = u.id

    		WHERE c.market_id = :marketId

    		ORDER BY lc.collection_date DESC
    		""", nativeQuery = true)
    		List<CollectionHistoryProjection>
    		getCollectionHistoryByMarket(
    		        UUID marketId
    		);
    
    @Query(value = """
    		SELECT

    		l.id AS loanId,

    		CAST(MIN(CAST(s.id AS varchar)) AS uuid) AS scheduleId,

    		c.id AS customerId,

    		CONCAT(
    		c.first_name,
    		' ',
    		c.last_name
    		) AS customerName,

    		c.mobile_number AS mobileNumber,

    		MIN(s.installment_number) AS installmentNumber,

    		MIN(s.due_date) AS dueDate,

    		SUM(s.installment_amount) AS installmentAmount,

    		SUM(s.outstanding_amount) AS outstandingAmount

    		FROM loan_repayment_schedules s

    		JOIN loans l
    		ON s.loan_id=l.id

    		JOIN customers c
    		ON l.customer_id=c.id

    		WHERE c.market_id = :marketId

    		AND s.repayment_status != 'PAID'

    		AND s.due_date <= CURRENT_DATE

    		AND l.id IN (
    		    SELECT s2.loan_id 
    		    FROM loan_repayment_schedules s2 
    		    WHERE s2.due_date = CURRENT_DATE 
    		    AND s2.repayment_status != 'PAID'
    		)

            GROUP BY l.id, c.id, c.first_name, c.last_name, c.mobile_number

    		ORDER BY c.first_name

    		""", nativeQuery = true)
    		List<TodayScheduleProjection>
    		getTodayScheduleByMarket(
    		        UUID marketId
    		);
    
    @Query(value = """
    		SELECT

    		l.id AS loanId,

    		CAST(MIN(CAST(s.id AS varchar)) AS uuid) AS scheduleId,

    		c.id AS customerId,

    		CONCAT(
    		c.first_name,
    		' ',
    		c.last_name
    		) AS customerName,

    		c.mobile_number AS mobileNumber,

    		MIN(s.installment_number) AS installmentNumber,

    		MIN(s.due_date) AS dueDate,

    		SUM(s.installment_amount) AS installmentAmount,

    		SUM(s.outstanding_amount) AS outstandingAmount,

    		MAX(GREATEST(
    		CURRENT_DATE-s.due_date,
    		0
    		)) AS overdueDays

    		FROM loan_repayment_schedules s

    		JOIN loans l
    		ON s.loan_id=l.id

    		JOIN customers c
    		ON l.customer_id=c.id

    		WHERE c.market_id=:marketId
    		AND s.repayment_status!='PAID'
    		AND s.due_date <= CURRENT_DATE
    		
            GROUP BY l.id, c.id, c.first_name, c.last_name, c.mobile_number

    		ORDER BY MIN(s.due_date)

    		""", nativeQuery = true)
    		List<PendingCollectionProjection>
    		getPendingCollectionsByMarket(
    		        UUID marketId
    		);
    
    @Query(value = """
    		SELECT COUNT(DISTINCT l.id)

    		FROM loan_repayment_schedules s

    		JOIN loans l
    		ON s.loan_id=l.id

    		JOIN customers c
    		ON l.customer_id=c.id

    		WHERE c.market_id=:marketId

    		AND s.due_date=CURRENT_DATE

    		AND s.repayment_status!='PAID'
    		""", nativeQuery = true)
    		Long getTodayScheduleCountByMarket(
    		        UUID marketId
    		);
    @Query(value = """
    		SELECT COUNT(*)

    		FROM loan_repayment_schedules s

    		JOIN loans l
    		ON s.loan_id=l.id

    		JOIN customers c
    		ON l.customer_id=c.id

    		WHERE c.market_id=:marketId

    		AND s.repayment_status!='PAID'
    		AND s.due_date <= CURRENT_DATE
    		""", nativeQuery = true)
    		Long getPendingCollectionCountByMarket(
    		        UUID marketId
    		);
    
    
    @Query(value = """
            SELECT COUNT(DISTINCT loan_id)
            FROM loan_repayment_schedules
            WHERE due_date = CURRENT_DATE
            AND repayment_status != 'PAID'
            """, nativeQuery = true)
    Long getTodayScheduleCount();
    
    @Query(value = """
    		SELECT
    		    u.id AS employeeId,
    		    u.full_name AS employeeName,
    		    SUM(c.collected_amount) AS totalCollection
    		FROM loan_collections c
    		JOIN users u
    		ON c.collected_by = u.id
    		GROUP BY
    		    u.id,
    		    u.full_name
    		ORDER BY totalCollection DESC
    		LIMIT 1
    		""", nativeQuery = true)
    		TopCollectorProjection
    		getTopCollector();
    
    @Query(value = """
    		SELECT COALESCE(
    		       SUM(collected_amount),
    		       0
    		)
    		FROM loan_collections
    		WHERE DATE(collection_date)
    		BETWEEN :fromDate
    		AND :toDate
    		""", nativeQuery = true)
    		BigDecimal getCollectionBetweenDates(
    		        LocalDate fromDate,
    		        LocalDate toDate
    		);
    
    @Query(value = """
    		SELECT COUNT(*)
    		FROM loan_collections
    		WHERE DATE(collection_date)
    		BETWEEN :fromDate
    		AND :toDate
    		""", nativeQuery = true)
    		Long getCollectionCountBetweenDates(
    		        LocalDate fromDate,
    		        LocalDate toDate
    		);
    
    @Query(value = """
    		SELECT
    		    m.id AS marketId,
    		    m.market_name AS marketName,
    		    COALESCE(
    		        SUM(lc.collected_amount),
    		        0
    		    ) AS totalCollection,
    		    COUNT(*) AS totalTransactions
    		FROM loan_collections lc
    		JOIN loans l
    		    ON lc.loan_id = l.id
    		JOIN customers c
    		    ON l.customer_id = c.id
    		JOIN markets m
    		    ON c.market_id = m.id
    		GROUP BY
    		    m.id,
    		    m.market_name
    		ORDER BY totalCollection DESC
    		""", nativeQuery = true)
    		List<MarketCollectionProjection>
    		getMarketCollectionReport();
    
    
    @Query(value = """
    		SELECT
    		    u.id AS employeeId,
    		    u.full_name AS employeeName,
    		    COALESCE(
    		        SUM(c.collected_amount),
    		        0
    		    ) AS totalCollection,
    		    COUNT(*) AS totalTransactions
    		FROM loan_collections c
    		JOIN users u
    		ON c.collected_by = u.id
    		GROUP BY
    		    u.id,
    		    u.full_name
    		ORDER BY totalCollection DESC
    		""", nativeQuery = true)
    		List<EmployeeCollectionProjection>
    		getEmployeeCollectionReport();
    
    
    @Query(value = """
    		SELECT COALESCE(
    		       SUM(collected_amount),
    		       0
    		)
    		FROM loan_collections
    		WHERE DATE(collection_date)
    		      = CURRENT_DATE
    		""", nativeQuery = true)
    		BigDecimal getTodayCollection();
    
    @Query(value = """
    		SELECT COUNT(*)
    		FROM loan_collections
    		WHERE DATE(collection_date)
    		      = CURRENT_DATE
    		""", nativeQuery = true)
    		Long getTodayCollectionCount();
    
    @Query(value = """
    		SELECT COALESCE(
    		       SUM(collected_amount),
    		       0
    		)
    		FROM loan_collections
    		WHERE EXTRACT(YEAR FROM collection_date)
    		      = EXTRACT(YEAR FROM CURRENT_DATE)
    		AND EXTRACT(MONTH FROM collection_date)
    		      = EXTRACT(MONTH FROM CURRENT_DATE)
    		""", nativeQuery = true)
    		BigDecimal getMonthCollection();
    
    @Query(value = """
    		SELECT COUNT(*)
    		FROM loan_collections
    		WHERE EXTRACT(YEAR FROM collection_date)
    		      = EXTRACT(YEAR FROM CURRENT_DATE)
    		AND EXTRACT(MONTH FROM collection_date)
    		      = EXTRACT(MONTH FROM CURRENT_DATE)
    		""", nativeQuery = true)
    		Long getMonthCollectionCount();
    
    @Query(value = """
    		SELECT COALESCE(
    		       SUM(collected_amount),
    		       0
    		)
    		FROM loan_collections
    		WHERE collected_by = :employeeId
    		AND DATE(collection_date) = CURRENT_DATE
    		""", nativeQuery = true)
    		BigDecimal getTodayCollectionByEmployee(
    		        UUID employeeId
    		);
    
    @Query(value = """
    		SELECT COALESCE(
    		       SUM(collected_amount),
    		       0
    		)
    		FROM loan_collections
    		WHERE collected_by = :employeeId
    		AND EXTRACT(YEAR FROM collection_date)
    		    = EXTRACT(YEAR FROM CURRENT_DATE)
    		AND EXTRACT(MONTH FROM collection_date)
    		    = EXTRACT(MONTH FROM CURRENT_DATE)
    		""", nativeQuery = true)
    		BigDecimal getMonthCollectionByEmployee(
    		        UUID employeeId
    		);
    
    @Query(value = """
            SELECT

                EXTRACT(MONTH FROM collection_date) AS monthNumber,

                TO_CHAR(collection_date,'Mon') AS monthName,

                COALESCE(
                    SUM(collected_amount),
                    0
                ) AS amount

            FROM loan_collections

            WHERE EXTRACT(YEAR FROM collection_date)
                  =
                  EXTRACT(YEAR FROM CURRENT_DATE)

            GROUP BY

                EXTRACT(MONTH FROM collection_date),

                TO_CHAR(collection_date,'Mon')

            ORDER BY monthNumber

            """, nativeQuery = true)
    List<MonthlyCollectionProjection>
    getMonthlyCollection();
    
    @Query(value = """
            SELECT

                lc.id AS collectionId,

                l.id AS loanId,

                l.loan_code AS loanCode,

                lc.receipt_number AS receiptNumber,

                CONCAT(
                    c.first_name,
                    ' ',
                    c.last_name
                ) AS customerName,

                c.mobile_number AS mobileNumber,

                lc.collected_amount AS collectedAmount,

                lc.collection_mode AS collectionMode,

                lc.collection_status AS collectionStatus,

                lc.collection_date AS collectionDate,

                u.full_name AS collectedBy

            FROM loan_collections lc

            JOIN loans l
                ON lc.loan_id = l.id

            JOIN customers c
                ON l.customer_id = c.id

            JOIN users u
                ON lc.collected_by = u.id

            WHERE lc.collected_by = :employeeId

            ORDER BY lc.collection_date DESC
            """,
            nativeQuery = true)
    List<CollectionHistoryProjection>
    getCollectionHistoryByEmployee(
            @Param("employeeId") UUID employeeId
    );


    @Query(value = """
            SELECT COALESCE(
                SUM(collected_amount),
                0
            )

            FROM loan_collections

            WHERE collected_by = :employeeId

            AND collection_status = 'SUCCESS'

            AND DATE(collection_date) = CURRENT_DATE
            """,
            nativeQuery = true)
    BigDecimal getSuccessfulTodayCollectionByEmployee(
            @Param("employeeId") UUID employeeId
    );


    @Query(value = """
            SELECT COUNT(*)

            FROM loan_collections

            WHERE collected_by = :employeeId

            AND collection_status = 'SUCCESS'

            AND DATE(collection_date) = CURRENT_DATE
            """,
            nativeQuery = true)
    Long getSuccessfulTodayCollectionCountByEmployee(
            @Param("employeeId") UUID employeeId
    );
    
    
    
}