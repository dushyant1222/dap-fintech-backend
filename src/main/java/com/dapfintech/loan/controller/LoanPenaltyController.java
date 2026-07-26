package com.dapfintech.loan.controller;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dapfintech.loan.dto.request.CloseSpecialLoanRequest;
import com.dapfintech.loan.dto.request.UpdatePenaltySettingsRequest;
import com.dapfintech.loan.dto.response.LoanClosureResponse;
import com.dapfintech.loan.dto.response.LoanPenaltySummaryResponse;
import com.dapfintech.loan.service.LoanPenaltyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanPenaltyController {

    private final LoanPenaltyService loanPenaltyService;

    @GetMapping("/{loanId}/penalty")
    public ResponseEntity<LoanPenaltySummaryResponse> getPenalty(@PathVariable UUID loanId) {
        return ResponseEntity.ok(loanPenaltyService.calculatePenalty(loanId));
    }

    @PutMapping("/{loanId}/penalty-settings")
    public ResponseEntity<LoanPenaltySummaryResponse> updatePenaltySettings(
            @PathVariable UUID loanId,
            @RequestBody UpdatePenaltySettingsRequest request
    ) {
        return ResponseEntity.ok(loanPenaltyService.updatePenaltySettings(loanId, request));
    }

    @PostMapping("/{loanId}/close-special")
    public ResponseEntity<LoanClosureResponse> closeOnSpecialCondition(
            @PathVariable UUID loanId,
            @RequestBody CloseSpecialLoanRequest request
    ) {
        return ResponseEntity.ok(loanPenaltyService.closeOnSpecialCondition(loanId, request));
    }
}
