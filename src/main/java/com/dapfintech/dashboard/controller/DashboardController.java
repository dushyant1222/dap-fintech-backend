package com.dapfintech.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.common.response.ApiResponse;
import com.dapfintech.dashboard.dto.response.EmployeeDashboardResponse;
import com.dapfintech.dashboard.service.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<EmployeeDashboardResponse>>
    getMyDashboard() {

        EmployeeDashboardResponse response =
                dashboardService.getMyDashboard();

        return ResponseEntity.ok(

                ApiResponse.<EmployeeDashboardResponse>builder()
                        .success(true)
                        .message("Dashboard loaded successfully")
                        .data(response)
                        .build()

        );
    }

}