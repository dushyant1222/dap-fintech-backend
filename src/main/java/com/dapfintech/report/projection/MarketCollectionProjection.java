package com.dapfintech.report.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface MarketCollectionProjection {

    UUID getMarketId();

    String getMarketName();

    BigDecimal getTotalCollection();

    Long getTotalTransactions();
}