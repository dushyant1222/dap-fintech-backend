package com.dapfintech.report.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MarketPerformanceResponse {

    private UUID marketId;

    private String marketName;

    private Long totalCustomers;

    private Long totalLoans;

    private BigDecimal totalCollection;
}