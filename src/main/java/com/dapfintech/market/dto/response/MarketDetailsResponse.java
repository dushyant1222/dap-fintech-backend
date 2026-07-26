package com.dapfintech.market.dto.response;

import java.util.List;
import java.util.UUID;

import com.dapfintech.market.dto.response.MarketDetailsResponse;
import com.dapfintech.market.enums.MarketStatus;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDetailsResponse {

    private UUID id;

    private String marketCode;

    private String marketName;

    private String city;

    private String state;

    private String description;

    private MarketStatus status;

    private int employeeCount;

    private List<AssignmentResponse> assignedEmployees;

}