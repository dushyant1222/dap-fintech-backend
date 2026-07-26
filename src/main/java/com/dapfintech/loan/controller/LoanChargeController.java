package com.dapfintech.loan.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dapfintech.loan.dto.request.CreateLoanChargeRequest;
import com.dapfintech.loan.dto.request.UpdateLoanChargeRequest;
import com.dapfintech.loan.dto.response.LoanChargeResponse;
import com.dapfintech.loan.service.LoanChargeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/loan-charges")
@RequiredArgsConstructor
public class LoanChargeController {

    private final LoanChargeService loanChargeService;

    @PostMapping
    public ResponseEntity<LoanChargeResponse>
    createCharge(
            @RequestBody CreateLoanChargeRequest request
    ) {

        return ResponseEntity.ok(
                loanChargeService.createCharge(request)
        );
    }

    @PutMapping("/{chargeId}")
    public ResponseEntity<LoanChargeResponse>
    updateCharge(
            @PathVariable UUID chargeId,
            @RequestBody UpdateLoanChargeRequest request
    ) {

        return ResponseEntity.ok(
                loanChargeService.updateCharge(
                        chargeId,
                        request
                )
        );
    }

    @GetMapping("/{chargeId}")
    public ResponseEntity<LoanChargeResponse>
    getChargeById(
            @PathVariable UUID chargeId
    ) {

        return ResponseEntity.ok(
                loanChargeService.getChargeById(
                        chargeId
                )
        );
    }

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<LoanChargeResponse>>
    getLoanCharges(
            @PathVariable UUID loanId
    ) {

        return ResponseEntity.ok(
                loanChargeService.getLoanCharges(
                        loanId
                )
        );
    }

    @DeleteMapping("/{chargeId}")
    public ResponseEntity<String>
    deleteCharge(
            @PathVariable UUID chargeId
    ) {

        loanChargeService.deleteCharge(chargeId);

        return ResponseEntity.ok(
                "Charge deleted successfully"
        );
    }
}