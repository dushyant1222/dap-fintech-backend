package com.dapfintech.employee.repository;

import com.dapfintech.employee.entity.MarketDayBook;
import com.dapfintech.employee.enums.DayBookStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketDayBookRepository extends JpaRepository<MarketDayBook, UUID> {
    Optional<MarketDayBook> findByMarketIdAndDate(UUID marketId, LocalDate date);
    List<MarketDayBook> findByMarketIdOrderByDateDesc(UUID marketId);
    List<MarketDayBook> findByMarketIdOrderByDateAsc(UUID marketId);
    List<MarketDayBook> findByMarketIdAndStatusOrderByDateDesc(UUID marketId, DayBookStatus status);
    List<MarketDayBook> findByDateAndStatusNot(LocalDate date, DayBookStatus status);
}
