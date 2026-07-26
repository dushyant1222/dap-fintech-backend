package com.dapfintech.market.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dapfintech.market.entity.Market;
import com.dapfintech.market.enums.MarketStatus;

@Repository
public interface MarketRepository
        extends JpaRepository<Market, UUID> {

    boolean existsByMarketCode(
            String marketCode
    );

    boolean existsByMarketNameIgnoreCaseAndCityIgnoreCase(
            String marketName,
            String city
    );

    List<Market>
    findAllByOrderByMarketNameAsc();

    List<Market>
    findByStatusOrderByMarketNameAsc(
            MarketStatus status
    );

    Optional<Market>
    findByIdAndStatus(
            UUID marketId,
            MarketStatus status
    );
    
}