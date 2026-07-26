package com.dapfintech.loan.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dapfintech.common.response.ApiResponse;
import com.dapfintech.loan.dto.response.LoanDetailsResponse;
import com.dapfintech.loan.service.LoanDetailsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class LoanDetailsController {

    private final LoanDetailsService
            loanDetailsService;

    @GetMapping("/loan-details/{loanId}")
    public ResponseEntity<
            ApiResponse<LoanDetailsResponse>>
    getLoanDetails(

            @PathVariable UUID loanId

    ) {

        return ResponseEntity.ok(

                ApiResponse
                        .<LoanDetailsResponse>builder()
                        .success(true)
                        .message(
                                "Loan details fetched successfully"
                        )
                        .data(

                                loanDetailsService
                                        .getLoanDetails(
                                                loanId
                                        )

                        )
                        .build()

        );

    }

}