package com.dapfintech.customer.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.dapfintech.customer.entity.Customer;

import org.springframework.data.repository.query.Param;
import java.util.Collection;
import com.dapfintech.loan.enums.LoanStatus;

public interface CustomerRepository
        extends JpaRepository<Customer, UUID>,
        JpaSpecificationExecutor<Customer> {

    boolean existsByMobileNumber(
            String mobileNumber
    );

    boolean existsByFirstNameIgnoreCaseAndLastNameIgnoreCase(
            String firstName,
            String lastName
    );
    Long countByMarketId(UUID marketId);

    Long countBy();

    Page<Customer>
    findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrMobileNumberContaining(
            String firstName,
            String lastName,
            String mobileNumber,
            Pageable pageable
    );
    Optional<Customer> findByIdAndDeletedFalse(
            UUID customerId
    );


    Page<Customer> findAllByDeletedFalse(
            Pageable pageable
    );

    Page<Customer>
    findByMarketIdAndFirstNameContainingIgnoreCaseOrMarketIdAndLastNameContainingIgnoreCaseOrMarketIdAndMobileNumberContaining(
            UUID marketId1,
            String firstName,

            UUID marketId2,
            String lastName,

            UUID marketId3,
            String mobileNumber,

            Pageable pageable
    );

    Page<Customer> findByMarketId(
            UUID marketId,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(c)
            FROM Customer c
            WHERE c.market.id IN (
                SELECT a.market.id
                FROM EmployeeMarketAssignment a
                WHERE a.employee.id = :employeeId
                AND a.isActive = true
            )
            """)
    Long countAssignedCustomers(
            @Param("employeeId") UUID employeeId
    );

    @Query("""
            SELECT c FROM Customer c
            WHERE c.deleted = false
            AND c.id NOT IN (
                SELECT l.customer.id FROM Loan l
                WHERE l.loanStatus IN :blockingStatuses
            )
            AND (
                :keyword IS NULL OR :keyword = '' OR
                LOWER(c.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(c.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                c.mobileNumber LIKE CONCAT('%', :keyword, '%')
            )
            ORDER BY c.createdAt DESC
            """)
    Page<Customer> findCustomersWithoutActiveLoans(
            @Param("keyword") String keyword,
            @Param("blockingStatuses") Collection<LoanStatus> blockingStatuses,
            Pageable pageable
    );

    @Query("""
            SELECT c FROM Customer c
            WHERE c.deleted = false
            AND c.market.id = :marketId
            AND c.id NOT IN (
                SELECT l.customer.id FROM Loan l
                WHERE l.loanStatus IN :blockingStatuses
            )
            AND (
                :keyword IS NULL OR :keyword = '' OR
                LOWER(c.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(c.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                c.mobileNumber LIKE CONCAT('%', :keyword, '%')
            )
            ORDER BY c.createdAt DESC
            """)
    Page<Customer> findMarketCustomersWithoutActiveLoans(
            @Param("marketId") UUID marketId,
            @Param("keyword") String keyword,
            @Param("blockingStatuses") Collection<LoanStatus> blockingStatuses,
            Pageable pageable
    );
}