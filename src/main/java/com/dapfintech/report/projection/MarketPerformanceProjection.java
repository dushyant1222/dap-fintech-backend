package com.dapfintech.report.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface MarketPerformanceProjection {

    UUID getMarketId();

    String getMarketName();

    Long getTotalCustomers();

    Long getTotalLoans();

    BigDecimal getTotalCollection();
}