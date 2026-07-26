package com.dapfintech.report.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollectionReportResponse {

    private BigDecimal totalCollection;

    private Long totalTransactions;

    private BigDecimal averageCollection;
}