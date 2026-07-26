package com.dapfintech.customer.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.dapfintech.common.response.ApiResponse;
import com.dapfintech.customer.dto.request.CreateCustomerRequest;
import com.dapfintech.customer.dto.request.CustomerFilterRequest;
import com.dapfintech.customer.dto.request.UpdateCustomerRequest;
import com.dapfintech.customer.dto.request.UpdateCustomerStatusRequest;
import com.dapfintech.customer.dto.response.CustomerDetailsResponse;
import com.dapfintech.customer.dto.response.CustomerResponse;
import com.dapfintech.customer.dto.response.DeleteCustomerResponse;
import com.dapfintech.customer.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    
    
    @PatchMapping("/{customerId}/status")
    public ResponseEntity<ApiResponse<CustomerResponse>>
    updateCustomerStatus(

            @PathVariable UUID customerId,

            @Valid
            @RequestBody
            UpdateCustomerStatusRequest request

    ) {

        CustomerResponse response =
                customerService.updateCustomerStatus(
                        customerId,
                        request
                );

        return ResponseEntity.ok(

                ApiResponse
                        .<CustomerResponse>builder()

                        .success(true)

                        .message(
                                "Customer status updated successfully"
                        )

                        .data(response)

                        .build()
        );
    }
    
    @PostMapping("/filter")
    public ResponseEntity<
            ApiResponse<Page<CustomerResponse>>>
    filterCustomers(

            @RequestBody(required = false)
            CustomerFilterRequest filter,

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
                        .<Page<CustomerResponse>>builder()

                        .success(true)

                        .message(
                                "Customers filtered successfully"
                        )

                        .data(
                                customerService
                                        .filterCustomers(
                                                filter,
                                                page,
                                                size
                                        )
                        )

                        .build()

        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request
    ) {

        CustomerResponse response =
                customerService.createCustomer(request);

        return ResponseEntity.ok(

                ApiResponse.<CustomerResponse>builder()
                        .success(true)
                        .message("Customer created successfully")
                        .data(response)
                        .build()

        );
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable UUID customerId,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {

        CustomerResponse response =
                customerService.updateCustomer(
                        customerId,
                        request
                );

        return ResponseEntity.ok(

                ApiResponse.<CustomerResponse>builder()
                        .success(true)
                        .message("Customer updated successfully")
                        .data(response)
                        .build()

        );
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @PathVariable UUID customerId
    ) {

        return ResponseEntity.ok(
                customerService.getCustomerById(
                        customerId
                )
        );
    }
    @GetMapping("/{customerId}/details")
    public ResponseEntity<CustomerDetailsResponse> getCustomerDetails(
            @PathVariable UUID customerId
    ) {

        return ResponseEntity.ok(

                customerService.getCustomerDetails(
                        customerId
                )

        );
    }

    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        return ResponseEntity.ok(
                customerService.getAllCustomers(
                        page,
                        size
                )
        );
    }

    @GetMapping("/search")
    public ResponseEntity<Page<CustomerResponse>> searchCustomers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        return ResponseEntity.ok(
                customerService.searchCustomers(
                        keyword,
                        page,
                        size
                )
        );
    }
    
    @DeleteMapping("/{customerId}")
    public ResponseEntity<ApiResponse<DeleteCustomerResponse>> deleteCustomer(

            @PathVariable UUID customerId,

            Authentication authentication

    ) {

        UUID adminId = UUID.fromString(
                authentication.getName()
        );

        DeleteCustomerResponse response =
                customerService.deleteCustomer(
                        customerId,
                        adminId
                );

        return ResponseEntity.ok(

                ApiResponse.<DeleteCustomerResponse>builder()

                        .success(true)

                        .message(
                                "Customer deleted successfully"
                        )

                        .data(response)

                        .build()

        );
    }    

}