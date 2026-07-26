package com.dapfintech.collection.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.dapfintech.collection.entity.CustomerVisit;
import com.dapfintech.report.projection.EmployeePerformanceProjection;
import com.dapfintech.report.projection.RecoveryEfficiencyProjection;

public interface CustomerVisitRepository
        extends JpaRepository<
                CustomerVisit,
                UUID> {

    List<CustomerVisit>
    findByCustomerId(
            UUID customerId
    );

    List<CustomerVisit>
    findByEmployeeId(
            UUID employeeId
    );
    
    @Query(value = """
    		SELECT
    		    u.id AS employeeId,
    		    u.full_name AS employeeName,

    		    COALESCE(
    		        (
    		            SELECT SUM(lc.collected_amount)
    		            FROM loan_collections lc
    		            WHERE lc.collected_by = u.id
    		        ),
    		        0
    		    ) AS totalCollection,

    		    COUNT(cv.id) AS totalVisits

    		FROM users u

    		LEFT JOIN customer_visits cv
    		ON cv.employee_id = u.id

    		WHERE u.role_id IN (
    		    SELECT id
    		    FROM roles
    		    WHERE role_name = 'EMPLOYEE'
    		)

    		GROUP BY
    		    u.id,
    		    u.full_name
    		""", nativeQuery = true)
    		List<RecoveryEfficiencyProjection>
    		getRecoveryEfficiencyReport();
    
    @Query(value = """
    		SELECT
    		    u.id AS employeeId,
    		    u.full_name AS employeeName,

    		    COALESCE(
    		        (
    		            SELECT SUM(lc.collected_amount)
    		            FROM loan_collections lc
    		            WHERE lc.collected_by = u.id
    		        ),
    		        0
    		    ) AS totalCollection,

    		    COUNT(cv.id) AS totalVisits,

    		    SUM(
    		        CASE
    		            WHEN cv.visit_status = 'PROMISED_TO_PAY'
    		            THEN 1
    		            ELSE 0
    		        END
    		    ) AS totalPromiseToPay

    		FROM users u

    		LEFT JOIN customer_visits cv
    		ON cv.employee_id = u.id

    		WHERE u.role_id IN (
    		    SELECT id
    		    FROM roles
    		    WHERE role_name = 'EMPLOYEE'
    		)

    		GROUP BY
    		    u.id,
    		    u.full_name

    		ORDER BY totalCollection DESC
    		""", nativeQuery = true)
    		List<EmployeePerformanceProjection>
    		getEmployeePerformanceReport();
    
    @Query(value = """
    		SELECT COUNT(*)
    		FROM customer_visits
    		WHERE employee_id = :employeeId
    		AND DATE(visit_date) = CURRENT_DATE
    		""", nativeQuery = true)
    		Long countTodayVisits(UUID employeeId);
    
    @Query(value = """
    		SELECT COUNT(*)
    		FROM customer_visits
    		WHERE employee_id = :employeeId
    		AND EXTRACT(YEAR FROM visit_date)
    		    = EXTRACT(YEAR FROM CURRENT_DATE)
    		AND EXTRACT(MONTH FROM visit_date)
    		    = EXTRACT(MONTH FROM CURRENT_DATE)
    		""", nativeQuery = true)
    		Long countMonthVisits(UUID employeeId);
    
    @Query("""
    		SELECT COUNT(v)
    		FROM CustomerVisit v
    		WHERE v.employee.id = :employeeId
    		AND v.visitStatus =
    		com.dapfintech.collection.enums.VisitStatus.PROMISED_TO_PAY
    		""")
    		Long countPromiseToPay(
    		        UUID employeeId
    		);
}