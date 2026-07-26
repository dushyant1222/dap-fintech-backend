package com.dapfintech.loan.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollectionDashboardResponse {

    private BigDecimal todayCollection;

    private Long todaySchedule;

    private Long pendingCollections;

    private Long overdueCustomers;

}