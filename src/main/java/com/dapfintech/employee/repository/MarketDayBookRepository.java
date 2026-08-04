package com.dapfintech.employee.repository;

import com.dapfintech.employee.entity.MarketDayBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketDayBookRepository extends JpaRepository<MarketDayBook, UUID> {
    Optional<MarketDayBook> findByMarketIdAndDate(UUID marketId, LocalDate date);
}
