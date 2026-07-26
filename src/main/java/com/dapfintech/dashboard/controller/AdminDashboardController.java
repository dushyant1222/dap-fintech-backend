package com.dapfintech.dashboard.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.common.response.ApiResponse;
import org.springframework.web.bind.annotation.RequestParam;
import com.dapfintech.dashboard.dto.response.AdminDashboardResponse;
import com.dapfintech.dashboard.dto.response.BusinessGrowthResponse;
import com.dapfintech.dashboard.dto.response.MonthlyCollectionResponse;
import com.dapfintech.dashboard.service.AdminDashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService
            dashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {

        return ResponseEntity.ok(

                ApiResponse.<AdminDashboardResponse>builder()
                        .success(true)
                        .message("Admin dashboard fetched successfully")
                        .data(
                                dashboardService.getDashboard()
                        )
                        .build()

        );
    }
    @GetMapping("/monthly-collection")
    public ResponseEntity<
            ApiResponse<List<MonthlyCollectionResponse>>>
    getMonthlyCollection() {

        return ResponseEntity.ok(

                ApiResponse
                        .<List<MonthlyCollectionResponse>>builder()

                        .success(true)

                        .message("Monthly Collection Loaded")

                        .data(

                                dashboardService
                                        .getMonthlyCollection()

                        )

                        .build()

        );

    }

    @GetMapping("/growth-analytics")
    public ResponseEntity<ApiResponse<BusinessGrowthResponse>> getBusinessGrowthAnalytics(
            @RequestParam(required = false, defaultValue = "Year") String timeframe
    ) {
        return ResponseEntity.ok(
                ApiResponse.<BusinessGrowthResponse>builder()
                        .success(true)
                        .message("Business growth analytics retrieved successfully")
                        .data(dashboardService.getBusinessGrowthAnalytics(timeframe))
                        .build()
        );
    }
}