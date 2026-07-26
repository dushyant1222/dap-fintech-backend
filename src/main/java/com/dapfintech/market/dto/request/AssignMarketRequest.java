package com.dapfintech.market.dto.request;

import java.util.UUID;

import lombok.Data;

@Data
public class AssignMarketRequest {

    private UUID marketId;

    private UUID employeeId;
}