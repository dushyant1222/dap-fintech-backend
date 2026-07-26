package com.dapfintech.loan.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.common.response.ApiResponse;
import com.dapfintech.loan.dto.response.LoanManagementDashboardResponse;
import com.dapfintech.loan.service.LoanManagementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/loan-management")
@RequiredArgsConstructor
public class LoanManagementController {

    private final LoanManagementService
            loanManagementService;

    @GetMapping("/dashboard")
    public ResponseEntity<
            ApiResponse<
                    LoanManagementDashboardResponse
                    >
            > getDashboard() {

        return ResponseEntity.ok(

                ApiResponse
                        .<LoanManagementDashboardResponse>builder()

                        .success(true)

                        .message(
                                "Loan dashboard fetched successfully"
                        )

                        .data(
                                loanManagementService
                                        .getDashboard()
                        )

                        .build()

        );

    }

}