package com.dapfintech.report.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OverdueSummaryResponse {

    private Long totalOverdueCustomers;

    private Long totalOverdueLoans;

    private BigDecimal totalOverdueAmount;
}