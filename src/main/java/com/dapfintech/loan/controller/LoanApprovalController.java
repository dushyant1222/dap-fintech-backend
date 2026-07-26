package com.dapfintech.loan.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dapfintech.common.response.ApiResponse;
import com.dapfintech.loan.dto.request.ApproveLoanRequest;
import com.dapfintech.loan.dto.request.RejectLoanRequest;
import com.dapfintech.loan.dto.request.SubmitLoanRequest;
import com.dapfintech.loan.dto.response.LoanApprovalResponse;
import com.dapfintech.loan.service.LoanApprovalService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/loan-approvals")
@RequiredArgsConstructor
public class LoanApprovalController {

    private final LoanApprovalService loanApprovalService;

    @PostMapping("/submit/{loanId}")
    public ResponseEntity<ApiResponse<Void>>
    submitLoan(
            @PathVariable UUID loanId,
            @RequestBody SubmitLoanRequest request
    ) {

        loanApprovalService.submitLoan(
                loanId,
                request
        );

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message(
                                "Loan submitted successfully"
                        )
                        .data(null)
                        .build()
        );
    }

    @PostMapping("/approve/{loanId}")
    public ResponseEntity<ApiResponse<Void>>
    approveLoan(
            @PathVariable UUID loanId,
            @RequestBody ApproveLoanRequest request
    ) {

        loanApprovalService.approveLoan(
                loanId,
                request
        );

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message(
                                "Loan approved successfully"
                        )
                        .data(null)
                        .build()
        );
    }

    

    @PostMapping("/resubmit/{loanId}")
    public ResponseEntity<String>
    resubmitLoan(
            @PathVariable UUID loanId,
            @RequestBody SubmitLoanRequest request
    ) {

        loanApprovalService.resubmitLoan(
                loanId,
                request
        );

        return ResponseEntity.ok(
                "Loan resubmitted successfully"
        );
    }

    @GetMapping("/history/{loanId}")
    public ResponseEntity<List<LoanApprovalResponse>>
    getApprovalHistory(
            @PathVariable UUID loanId
    ) {

        return ResponseEntity.ok(
                loanApprovalService.getApprovalHistory(
                        loanId
                )
        );
    }
}