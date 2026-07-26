package com.dapfintech.capital.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dapfintech.capital.entity.CashSettlement;

public interface CashSettlementRepository extends JpaRepository<CashSettlement, UUID> {

    List<CashSettlement> findAllByOrderBySettlementDateDesc();

    List<CashSettlement> findByEmployeeIdOrderBySettlementDateDesc(UUID employeeId);

    @Query("SELECT COALESCE(SUM(cs.amountSettled), 0) FROM CashSettlement cs")
    BigDecimal getTotalSettledAmount();

    @Query("SELECT COALESCE(SUM(cs.amountSettled), 0) FROM CashSettlement cs WHERE cs.employee.id = :employeeId")
    BigDecimal getTotalSettledAmountByEmployee(@Param("employeeId") UUID employeeId);

    @Query("SELECT COALESCE(SUM(cs.amountSettled), 0) FROM CashSettlement cs WHERE cs.settlementDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalSettledAmountBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
