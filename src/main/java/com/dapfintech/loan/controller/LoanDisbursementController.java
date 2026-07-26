package com.dapfintech.loan.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dapfintech.loan.dto.request.CreateDisbursementRequest;
import com.dapfintech.loan.dto.response.DisbursementResponse;
import com.dapfintech.loan.service.LoanDisbursementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/disbursements")
@RequiredArgsConstructor
public class LoanDisbursementController {

    private final LoanDisbursementService service;

    @PostMapping("/{loanId}")
    public ResponseEntity<DisbursementResponse>
    disburseLoan(
            @PathVariable UUID loanId,
            @RequestBody CreateDisbursementRequest request
    ) {

        return ResponseEntity.ok(
                service.disburseLoan(
                        loanId,
                        request
                )
        );
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<DisbursementResponse>
    getDisbursement(
            @PathVariable UUID loanId
    ) {

        return ResponseEntity.ok(
                service.getDisbursement(
                        loanId
                )
        );
    }
}