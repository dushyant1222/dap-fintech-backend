package com.dapfintech.market.service;

import java.util.List;
import java.util.UUID;

import com.dapfintech.market.dto.request.AssignMarketRequest;
import com.dapfintech.market.dto.response.AssignmentResponse;

public interface EmployeeMarketAssignmentService {

    AssignmentResponse assignMarket(
            AssignMarketRequest request
    );

    AssignmentResponse transferMarket(
            AssignMarketRequest request
    );

    List<AssignmentResponse> getEmployeeMarkets(
            UUID employeeId
    );

    List<AssignmentResponse> getMarketEmployees(
            UUID marketId
    );

    List<AssignmentResponse> getMyMarkets();

    void unassignEmployeeFromMarket(
            UUID marketId,
            UUID employeeId
    );
}