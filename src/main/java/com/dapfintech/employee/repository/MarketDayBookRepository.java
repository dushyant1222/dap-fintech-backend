package com.dapfintech.employee.repository;

import com.dapfintech.employee.entity.MarketDayBook;
import com.dapfintech.employee.enums.DayBookStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketDayBookRepository extends JpaRepository<MarketDayBook, UUID> {
    @Query("SELECT m FROM MarketDayBook m WHERE m.marketId = :marketId AND m.date = :date ORDER BY m.updatedAt DESC NULLS LAST, m.createdAt DESC NULLS LAST, m.id DESC")
    List<MarketDayBook> findAllByMarketIdAndDate(@Param("marketId") UUID marketId, @Param("date") LocalDate date);

    default Optional<MarketDayBook> findByMarketIdAndDate(UUID marketId, LocalDate date) {
        List<MarketDayBook> list = findAllByMarketIdAndDate(marketId, date);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    List<MarketDayBook> findByMarketIdOrderByDateDesc(UUID marketId);
    List<MarketDayBook> findByMarketIdOrderByDateAsc(UUID marketId);
    List<MarketDayBook> findByMarketIdAndStatusOrderByDateDesc(UUID marketId, DayBookStatus status);
    List<MarketDayBook> findByDateAndStatusNot(LocalDate date, DayBookStatus status);
}
