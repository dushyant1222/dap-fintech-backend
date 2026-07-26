package com.dapfintech.customer.history.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dapfintech.common.response.ApiResponse;
import com.dapfintech.customer.history.dto.response.CustomerHistoryResponse;
import com.dapfintech.customer.history.service.CustomerHistoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/customer-history")
@RequiredArgsConstructor
public class CustomerHistoryController {

    private final CustomerHistoryService customerHistoryService;


    @GetMapping("/customer/{customerId}")
    public ResponseEntity<
            ApiResponse<List<CustomerHistoryResponse>>
            > getCustomerHistory(
            @PathVariable UUID customerId
    ) {

        List<CustomerHistoryResponse> history =
                customerHistoryService
                        .getCustomerHistory(customerId);

        return ResponseEntity.ok(

                ApiResponse
                        .<List<CustomerHistoryResponse>>builder()
                        .success(true)
                        .message(
                                "Customer history fetched successfully"
                        )
                        .data(history)
                        .build()

        );
    }
}