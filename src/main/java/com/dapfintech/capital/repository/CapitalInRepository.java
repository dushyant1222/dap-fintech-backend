package com.dapfintech.capital.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dapfintech.capital.entity.CapitalIn;

public interface CapitalInRepository extends JpaRepository<CapitalIn, UUID> {

    List<CapitalIn> findAllByOrderByCapitalDateDesc();

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CapitalIn c")
    BigDecimal getTotalCapitalInjected();

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CapitalIn c WHERE c.capitalDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalCapitalInjectedBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
