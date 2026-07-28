package com.dapfintech.capital.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.capital.dto.request.InternalTransferRequest;
import com.dapfintech.capital.dto.response.InternalTransferResponse;
import com.dapfintech.capital.service.InternalTransferService;
import com.dapfintech.common.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class InternalTransferController {

    private final InternalTransferService internalTransferService;

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<InternalTransferResponse>> initiateTransfer(
            @Valid @RequestBody InternalTransferRequest request) {
        return ResponseEntity.ok(ApiResponse.<InternalTransferResponse>builder()
                .success(true)
                .message("Transfer initiated successfully")
                .data(internalTransferService.initiateTransfer(request))
                .build());
    }

    @PutMapping("/{transferId}/accept")
    public ResponseEntity<ApiResponse<InternalTransferResponse>> acceptTransfer(
            @PathVariable UUID transferId) {
        return ResponseEntity.ok(ApiResponse.<InternalTransferResponse>builder()
                .success(true)
                .message("Transfer accepted successfully")
                .data(internalTransferService.acceptTransfer(transferId))
                .build());
    }

    @PutMapping("/{transferId}/reject")
    public ResponseEntity<ApiResponse<InternalTransferResponse>> rejectTransfer(
            @PathVariable UUID transferId) {
        return ResponseEntity.ok(ApiResponse.<InternalTransferResponse>builder()
                .success(true)
                .message("Transfer rejected")
                .data(internalTransferService.rejectTransfer(transferId))
                .build());
    }

    @GetMapping("/incoming/pending")
    public ResponseEntity<ApiResponse<List<InternalTransferResponse>>> getPendingIncomingTransfers() {
        return ResponseEntity.ok(ApiResponse.<List<InternalTransferResponse>>builder()
                .success(true)
                .message("Fetched pending transfers")
                .data(internalTransferService.getPendingIncomingTransfers())
                .build());
    }

    @GetMapping("/incoming")
    public ResponseEntity<ApiResponse<List<InternalTransferResponse>>> getMyIncomingTransfers() {
        return ResponseEntity.ok(ApiResponse.<List<InternalTransferResponse>>builder()
                .success(true)
                .message("Fetched incoming transfers")
                .data(internalTransferService.getMyIncomingTransfers())
                .build());
    }

    @GetMapping("/outgoing")
    public ResponseEntity<ApiResponse<List<InternalTransferResponse>>> getMyOutgoingTransfers() {
        return ResponseEntity.ok(ApiResponse.<List<InternalTransferResponse>>builder()
                .success(true)
                .message("Fetched outgoing transfers")
                .data(internalTransferService.getMyOutgoingTransfers())
                .build());
    }
}
