package com.dapfintech.market.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dapfintech.common.response.ApiResponse;
import com.dapfintech.market.dto.request.AssignMarketRequest;
import com.dapfintech.market.dto.response.AssignmentResponse;
import com.dapfintech.market.service.EmployeeMarketAssignmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/market-assignments")
@RequiredArgsConstructor
public class EmployeeMarketAssignmentController {

    private final EmployeeMarketAssignmentService service;

    // =====================================================
    // GET MY MARKETS
    // =====================================================

    @GetMapping("/me")
    public ResponseEntity<
            ApiResponse<List<AssignmentResponse>>>
    getMyMarkets() {

        return ResponseEntity.ok(

                ApiResponse
                        .<List<AssignmentResponse>>builder()
                        .success(true)
                        .message(
                                "Assigned markets fetched successfully"
                        )
                        .data(
                                service.getMyMarkets()
                        )
                        .build()
        );
    }

    // =====================================================
    // ASSIGN EMPLOYEE TO MARKET
    // =====================================================

    @PostMapping
    public ResponseEntity<
            ApiResponse<AssignmentResponse>>
    assignMarket(
            @RequestBody
            AssignMarketRequest request
    ) {

        return ResponseEntity.ok(

                ApiResponse
                        .<AssignmentResponse>builder()
                        .success(true)
                        .message(
                                "Employee assigned to market successfully"
                        )
                        .data(
                                service.assignMarket(
                                        request
                                )
                        )
                        .build()
        );
    }

    // =====================================================
    // GET EMPLOYEE MARKETS
    // =====================================================

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<
            ApiResponse<List<AssignmentResponse>>>
    getEmployeeMarkets(
            @PathVariable UUID employeeId
    ) {

        return ResponseEntity.ok(

                ApiResponse
                        .<List<AssignmentResponse>>builder()
                        .success(true)
                        .message(
                                "Employee markets fetched successfully"
                        )
                        .data(
                                service.getEmployeeMarkets(
                                        employeeId
                                )
                        )
                        .build()
        );
    }

    // =====================================================
    // GET MARKET EMPLOYEES
    // =====================================================

    @GetMapping("/market/{marketId}")
    public ResponseEntity<
            ApiResponse<List<AssignmentResponse>>>
    getMarketEmployees(
            @PathVariable UUID marketId
    ) {

        return ResponseEntity.ok(

                ApiResponse
                        .<List<AssignmentResponse>>builder()
                        .success(true)
                        .message(
                                "Market employees fetched successfully"
                        )
                        .data(
                                service.getMarketEmployees(
                                        marketId
                                )
                        )
                        .build()
        );
    }

    // =====================================================
    // UNASSIGN EMPLOYEE FROM MARKET
    // =====================================================

    @DeleteMapping(
            "/market/{marketId}/employee/{employeeId}"
    )
    public ResponseEntity<
            ApiResponse<String>>
    unassignEmployeeFromMarket(
            @PathVariable UUID marketId,
            @PathVariable UUID employeeId
    ) {

        service.unassignEmployeeFromMarket(
                marketId,
                employeeId
        );

        return ResponseEntity.ok(

                ApiResponse
                        .<String>builder()
                        .success(true)
                        .message(
                                "Employee unassigned from market successfully"
                        )
                        .data("SUCCESS")
                        .build()
        );
    }
}