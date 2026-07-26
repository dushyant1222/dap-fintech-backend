package com.dapfintech.customer.guarantor.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dapfintech.common.response.ApiResponse;
import com.dapfintech.customer.guarantor.dto.request.CreateGuarantorRequest;
import com.dapfintech.customer.guarantor.dto.request.UpdateGuarantorRequest;
import com.dapfintech.customer.guarantor.dto.response.GuarantorResponse;
import com.dapfintech.customer.guarantor.service.GuarantorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/customer-guarantors")
@RequiredArgsConstructor
public class GuarantorController {

    private final GuarantorService guarantorService;


    // =========================================================
    // CREATE GUARANTOR
    // =========================================================

    @PostMapping
    public ResponseEntity<ApiResponse<GuarantorResponse>>
    createGuarantor(
            @RequestBody CreateGuarantorRequest request
    ) {

        GuarantorResponse response =
                guarantorService.createGuarantor(
                        request
                );

        return ResponseEntity.ok(
                ApiResponse
                        .<GuarantorResponse>builder()
                        .success(true)
                        .message(
                                "Guarantor created successfully"
                        )
                        .data(response)
                        .build()
        );
    }


    // =========================================================
    // UPDATE GUARANTOR
    // =========================================================

    @PutMapping("/{guarantorId}")
    public ResponseEntity<ApiResponse<GuarantorResponse>>
    updateGuarantor(
            @PathVariable UUID guarantorId,
            @RequestBody UpdateGuarantorRequest request
    ) {

        GuarantorResponse response =
                guarantorService.updateGuarantor(
                        guarantorId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse
                        .<GuarantorResponse>builder()
                        .success(true)
                        .message(
                                "Guarantor updated successfully"
                        )
                        .data(response)
                        .build()
        );
    }


    // =========================================================
    // GET GUARANTOR BY ID
    // =========================================================

    @GetMapping("/{guarantorId}")
    public ResponseEntity<ApiResponse<GuarantorResponse>>
    getById(
            @PathVariable UUID guarantorId
    ) {

        GuarantorResponse response =
                guarantorService.getGuarantorById(
                        guarantorId
                );

        return ResponseEntity.ok(
                ApiResponse
                        .<GuarantorResponse>builder()
                        .success(true)
                        .message(
                                "Guarantor fetched successfully"
                        )
                        .data(response)
                        .build()
        );
    }


    // =========================================================
    // GET GUARANTORS BY CUSTOMER
    // =========================================================

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<GuarantorResponse>>>
    getCustomerGuarantors(
            @PathVariable UUID customerId
    ) {

        List<GuarantorResponse> response =
                guarantorService
                        .getCustomerGuarantors(
                                customerId
                        );

        return ResponseEntity.ok(
                ApiResponse
                        .<List<GuarantorResponse>>builder()
                        .success(true)
                        .message(
                                "Customer guarantors fetched successfully"
                        )
                        .data(response)
                        .build()
        );
    }


    // =========================================================
    // DELETE GUARANTOR
    // =========================================================

    @DeleteMapping("/{guarantorId}")
    public ResponseEntity<ApiResponse<Void>>
    deleteGuarantor(
            @PathVariable UUID guarantorId
    ) {

        guarantorService.deleteGuarantor(
                guarantorId
        );

        return ResponseEntity.ok(
                ApiResponse
                        .<Void>builder()
                        .success(true)
                        .message(
                                "Guarantor deleted successfully"
                        )
                        .data(null)
                        .build()
        );
    }
}