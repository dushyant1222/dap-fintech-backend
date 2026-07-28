package com.dapfintech.wallet.controller;

import com.dapfintech.common.response.ApiResponse;
import com.dapfintech.wallet.dto.LedgerSummaryResponse;
import com.dapfintech.wallet.dto.PersonalLedgerRequest;
import com.dapfintech.wallet.dto.PersonalLedgerResponse;
import com.dapfintech.wallet.service.PersonalLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/personal-ledger")
@RequiredArgsConstructor
public class PersonalLedgerController {

    private final PersonalLedgerService personalLedgerService;

    @PostMapping
    public ResponseEntity<ApiResponse<PersonalLedgerResponse>> addTransaction(@RequestBody PersonalLedgerRequest request) {
        return ResponseEntity.ok(ApiResponse.<PersonalLedgerResponse>builder()
                .success(true)
                .message("Transaction added successfully")
                .data(personalLedgerService.addTransaction(request))
                .build());
    }

    @GetMapping("/admin/{adminId}")
    public ResponseEntity<ApiResponse<List<PersonalLedgerResponse>>> getMyLedger(@PathVariable UUID adminId) {
        return ResponseEntity.ok(ApiResponse.<List<PersonalLedgerResponse>>builder()
                .success(true)
                .message("Fetched personal ledger")
                .data(personalLedgerService.getMyLedger(adminId))
                .build());
    }

    @GetMapping("/admin/{adminId}/summary")
    public ResponseEntity<ApiResponse<LedgerSummaryResponse>> getSummary(@PathVariable UUID adminId) {
        return ResponseEntity.ok(ApiResponse.<LedgerSummaryResponse>builder()
                .success(true)
                .message("Fetched ledger summary")
                .data(personalLedgerService.getSummary(adminId))
                .build());
    }
}
