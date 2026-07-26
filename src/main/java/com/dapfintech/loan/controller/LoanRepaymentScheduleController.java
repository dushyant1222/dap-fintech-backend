package com.dapfintech.loan.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dapfintech.common.response.ApiResponse;
import com.dapfintech.loan.dto.response.RepaymentScheduleResponse;
import com.dapfintech.loan.service.LoanRepaymentScheduleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/repayment-schedules")
@RequiredArgsConstructor
public class LoanRepaymentScheduleController {

    private final LoanRepaymentScheduleService service;

    @GetMapping("/{loanId}")
    public ResponseEntity<ApiResponse<List<RepaymentScheduleResponse>>>
    getSchedule(
            @PathVariable UUID loanId
    ) {

        return ResponseEntity.ok(

                ApiResponse
                        .<List<RepaymentScheduleResponse>>builder()
                        .success(true)
                        .message("Repayment schedule fetched successfully")
                        .data(
                                service.getLoanSchedule(
                                        loanId
                                )
                        )
                        .build()

        );
    }
}