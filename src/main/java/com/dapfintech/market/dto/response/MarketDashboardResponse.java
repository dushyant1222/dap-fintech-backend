package com.dapfintech.market.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MarketDashboardResponse {

    private UUID marketId;

    private String marketCode;

    private String marketName;

    private String city;

    private String state;

    private String description;

    private String status;

    // Statistics

    private Long totalCustomers;

    private Long activeLoans;

    private Long totalCollections;

    private BigDecimal totalCollectionAmount;

    // Employees

    private List<AssignmentResponse> employees;
}