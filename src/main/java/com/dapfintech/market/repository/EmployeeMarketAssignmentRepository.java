package com.dapfintech.market.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dapfintech.market.entity.EmployeeMarketAssignment;

@Repository
public interface EmployeeMarketAssignmentRepository
        extends JpaRepository<EmployeeMarketAssignment, UUID> {

    // =====================================================
    // ACTIVE ASSIGNMENTS OF ONE EMPLOYEE
    // =====================================================

    List<EmployeeMarketAssignment>
    findByEmployeeIdAndIsActiveTrue(
            UUID employeeId
    );
    

    // =====================================================
    // ACTIVE EMPLOYEES OF ONE MARKET
    // =====================================================

    List<EmployeeMarketAssignment>
    findByMarketIdAndIsActiveTrue(
            UUID marketId
    );
    

    // =====================================================
    // EXACT ACTIVE EMPLOYEE-MARKET ASSIGNMENT
    // =====================================================

    Optional<EmployeeMarketAssignment>
    findByMarketIdAndEmployeeIdAndIsActiveTrue(
            UUID marketId,
            UUID employeeId
    );

    // =====================================================
    // CHECK DUPLICATE ACTIVE ASSIGNMENT
    // =====================================================

    boolean existsByMarketIdAndEmployeeIdAndIsActiveTrue(
            UUID marketId,
            UUID employeeId
    );

    // Keep because your existing code may already use
    // employeeId first.

    boolean existsByEmployeeIdAndMarketIdAndIsActiveTrue(
            UUID employeeId,
            UUID marketId
    );

    // =====================================================
    // COUNT ACTIVE MARKETS OF EMPLOYEE
    // =====================================================

    long countByEmployeeIdAndIsActiveTrue(
            UUID employeeId
    );

    // =====================================================
    // FIRST ACTIVE MARKET OF EMPLOYEE
    // =====================================================

    Optional<EmployeeMarketAssignment>
    findFirstByEmployeeIdAndIsActiveTrue(
            UUID employeeId
    );

    // =====================================================
    // ALL ASSIGNMENTS (INCLUDING NULL IS_ACTIVE)
    // =====================================================

    boolean existsByMarketIdAndIsActiveTrue(UUID marketId);

    List<EmployeeMarketAssignment> findByEmployeeId(UUID employeeId);

    List<EmployeeMarketAssignment> findByMarketId(UUID marketId);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM EmployeeMarketAssignment a WHERE a.market.id = :marketId AND (a.isActive = true OR a.isActive IS NULL)")
    List<EmployeeMarketAssignment> findActiveOrNullByMarketId(@org.springframework.data.repository.query.Param("marketId") UUID marketId);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM EmployeeMarketAssignment a WHERE a.employee.id = :employeeId AND (a.isActive = true OR a.isActive IS NULL)")
    List<EmployeeMarketAssignment> findActiveOrNullByEmployeeId(@org.springframework.data.repository.query.Param("employeeId") UUID employeeId);
}