package com.dapfintech.report.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MarketCollectionReportResponse {

    private UUID marketId;

    private String marketName;

    private BigDecimal totalCollection;

    private Long totalTransactions;
}