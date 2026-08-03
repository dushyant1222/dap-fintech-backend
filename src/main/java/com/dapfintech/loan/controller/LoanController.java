package com.dapfintech.loan.controller;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dapfintech.common.response.ApiResponse;
import com.dapfintech.loan.dto.request.CalculateEmiRequest;
import com.dapfintech.loan.dto.request.CreateLoanRequest;
import com.dapfintech.loan.dto.request.LoanFilterRequest;
import com.dapfintech.loan.dto.request.UpdateLoanRequest;
import com.dapfintech.loan.dto.response.CalculateEmiResponse;
import com.dapfintech.loan.dto.response.LoanResponse;
import com.dapfintech.loan.dto.response.LoanStatisticsResponse;
import com.dapfintech.loan.dto.response.LoanSummaryResponse;
import com.dapfintech.loan.enums.LoanStatus;
import com.dapfintech.loan.service.LoanService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    
    @GetMapping("/bureau")
    public ResponseEntity<ApiResponse<Page<com.dapfintech.loan.dto.response.LoanBureauResponse>>> getLoanBureau(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Loan bureau fetched successfully",
                        loanService.getLoanBureau(page, size)
                )
        );
    }
    
    
    
    
    @PostMapping("/filter")
    public ResponseEntity<
            ApiResponse<Page<LoanResponse>>>
    filterLoans(

            @RequestBody
            LoanFilterRequest filter,

            @RequestParam(
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    defaultValue = "20"
            )
            int size

    ) {

        return ResponseEntity.ok(

                ApiResponse
                        .<Page<LoanResponse>>builder()

                        .success(true)

                        .message(
                                "Loans filtered successfully"
                        )

                        .data(

                                loanService.filterLoans(

                                        filter,

                                        page,

                                        size

                                )

                        )

                        .build()

        );

    }
    
    @PostMapping("/calculate")
    public ResponseEntity<
            ApiResponse<CalculateEmiResponse>>
    calculateEmi(
            @RequestBody
            CalculateEmiRequest request
    ) {

        return ResponseEntity.ok(

                ApiResponse
                        .<CalculateEmiResponse>builder()
                        .success(true)
                        .message(
                                "EMI calculated successfully"
                        )
                        .data(
                                loanService.calculateEmi(
                                        request
                                )
                        )
                        .build()
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LoanResponse>>
    createLoan(
            @RequestBody CreateLoanRequest request
    ) {

        LoanResponse response =
                loanService.createLoan(request);

        return ResponseEntity.ok(

                ApiResponse
                        .<LoanResponse>builder()
                        .success(true)
                        .message(
                                "Loan created successfully"
                        )
                        .data(response)
                        .build()

        );

    }
    @GetMapping("/{loanId}/summary")
    public ResponseEntity<
            LoanSummaryResponse>
    getLoanSummary(
            @PathVariable UUID loanId
    ) {

        return ResponseEntity.ok(
                loanService.getLoanSummary(
                        loanId
                )
        );
    }
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<LoanStatisticsResponse>>
    getLoanStatistics() {

        return ResponseEntity.ok(

                ApiResponse
                        .<LoanStatisticsResponse>builder()
                        .success(true)
                        .message(
                                "Loan statistics fetched successfully"
                        )
                        .data(
                                loanService.getLoanStatistics()
                        )
                        .build()

        );

    }

    @PutMapping("/{loanId}")
    public ResponseEntity<ApiResponse<LoanResponse>>
    updateLoan(
            @PathVariable UUID loanId,
            @RequestBody UpdateLoanRequest request
    ) {

        LoanResponse response =
                loanService.updateLoan(
                        loanId,
                        request
                );

        return ResponseEntity.ok(

                ApiResponse
                        .<LoanResponse>builder()

                        .success(true)

                        .message(
                                "Loan updated successfully"
                        )

                        .data(response)

                        .build()

        );
    }
    @GetMapping
    public ResponseEntity<ApiResponse<Page<LoanResponse>>> getAllLoans(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(required = false)
            LoanStatus status

    ) {
    	

        return ResponseEntity.ok(

                ApiResponse
                        .<Page<LoanResponse>>builder()
                        .success(true)
                        .message("Loans fetched successfully")
                        .data(
                        		loanService.getAllLoans(

                        		        page,

                        		        size,

                        		        status

                        		)
                        )
                        .build()

        );
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<LoanResponse>>> searchLoans(

            @RequestParam String keyword,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size

    ) {

        return ResponseEntity.ok(

                ApiResponse
                        .<Page<LoanResponse>>builder()
                        .success(true)
                        .message("Loans fetched successfully")
                        .data(

                                loanService.searchLoans(

                                        keyword,

                                        page,

                                        size

                                )

                        )
                        .build()

        );

    }
    @GetMapping("/{loanId}")
    public ResponseEntity<LoanResponse>
    getLoanById(
            @PathVariable UUID loanId
    ) {

        return ResponseEntity.ok(
                loanService.getLoanById(
                        loanId
                )
        );
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<LoanResponse>>
    getCustomerLoans(
            @PathVariable UUID customerId
    ) {

        return ResponseEntity.ok(
                loanService.getCustomerLoans(
                        customerId
                )
        );
    }

    @DeleteMapping("/{loanId}")
    public ResponseEntity<String>
    deleteLoan(
            @PathVariable UUID loanId
    ) {

        loanService.deleteLoan(loanId);

        return ResponseEntity.ok(
                "Loan deleted successfully"
        );
    }
}