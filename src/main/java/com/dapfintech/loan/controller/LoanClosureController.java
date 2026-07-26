package com.dapfintech.loan.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dapfintech.common.response.ApiResponse;
import com.dapfintech.loan.dto.request.CloseLoanRequest;
import com.dapfintech.loan.dto.response.LoanClosureResponse;
import com.dapfintech.loan.service.LoanClosureService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/loan-closures")
@RequiredArgsConstructor
public class LoanClosureController {

    private final LoanClosureService
            loanClosureService;


    @PostMapping("/{loanId}")
    public ResponseEntity<
            ApiResponse<LoanClosureResponse>>
    closeLoan(
            @PathVariable UUID loanId,
            @RequestBody(
                    required = false
            )
            CloseLoanRequest request
    ) {

        LoanClosureResponse response =
                loanClosureService
                        .closeLoan(
                                loanId,
                                request
                        );

        return ResponseEntity.ok(

                ApiResponse
                        .<LoanClosureResponse>builder()

                        .success(true)

                        .message(
                                "Loan closed successfully"
                        )

                        .data(response)

                        .build()

        );
    }


    @GetMapping("/{loanId}")
    public ResponseEntity<
            ApiResponse<LoanClosureResponse>>
    getLoanClosure(
            @PathVariable UUID loanId
    ) {

        LoanClosureResponse response =
                loanClosureService
                        .getLoanClosure(
                                loanId
                        );

        return ResponseEntity.ok(

                ApiResponse
                        .<LoanClosureResponse>builder()

                        .success(true)

                        .message(
                                "Loan closure fetched successfully"
                        )

                        .data(response)

                        .build()

        );
    }
}