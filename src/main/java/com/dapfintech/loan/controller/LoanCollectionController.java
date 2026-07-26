package com.dapfintech.loan.controller;

import com.dapfintech.loan.dto.response.EmployeeCollectionOverviewResponse;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.dapfintech.common.response.ApiResponse;
import com.dapfintech.loan.dto.request.CreateCollectionRequest;
import com.dapfintech.loan.dto.response.CollectionDashboardResponse;
import com.dapfintech.loan.dto.response.CollectionHistoryResponse;
import com.dapfintech.loan.dto.response.CollectionResponse;
import com.dapfintech.loan.dto.response.OverdueCollectionResponse;
import com.dapfintech.loan.dto.response.PendingCollectionResponse;
import com.dapfintech.loan.dto.response.TodayScheduleResponse;
import com.dapfintech.loan.service.LoanCollectionService;
import com.dapfintech.security.jwt.JwtService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
public class LoanCollectionController {

    private final LoanCollectionService service;
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/employee/{employeeId}/overview")
    public ResponseEntity<
            ApiResponse<EmployeeCollectionOverviewResponse>>
    getEmployeeCollectionOverview(
            @PathVariable UUID employeeId
    ) {

        return ResponseEntity.ok(

                ApiResponse
                        .<EmployeeCollectionOverviewResponse>
                                builder()

                        .success(true)

                        .message(
                                "Employee collection overview fetched successfully"
                        )

                        .data(
                                service
                                        .getEmployeeCollectionOverview(
                                                employeeId
                                        )
                        )

                        .build()

        );

    }
    
    
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<
            ApiResponse<
                    List<CollectionHistoryResponse>>>
    getCollectionHistoryByEmployee(
            @PathVariable UUID employeeId
    ) {

        return ResponseEntity.ok(

                ApiResponse
                        .<List<CollectionHistoryResponse>>
                                builder()

                        .success(true)

                        .message(
                                "Employee collection history fetched successfully"
                        )

                        .data(
                                service.getCollectionHistoryByEmployee(
                                        employeeId
                                )
                        )

                        .build()

        );
    }
    
    @GetMapping("/dashboard")
    public ResponseEntity<
            ApiResponse<CollectionDashboardResponse>>
    getDashboard() {

        return ResponseEntity.ok(

                ApiResponse
                        .<CollectionDashboardResponse>builder()
                        .success(true)
                        .message(
                                "Collection dashboard fetched successfully"
                        )
                        .data(
                                service.getDashboard()
                        )
                        .build()

        );

    }
    
    @GetMapping("/today-schedule")
    public ResponseEntity<
            ApiResponse<List<TodayScheduleResponse>>>
    getTodaySchedule() {

        return ResponseEntity.ok(

                ApiResponse
                        .<List<TodayScheduleResponse>>builder()
                        .success(true)
                        .message(
                                "Today's schedule fetched successfully"
                        )
                        .data(
                                service.getTodaySchedule()
                        )
                        .build()

        );

    }
    
    @GetMapping("/pending")
    public ResponseEntity<
            ApiResponse<List<PendingCollectionResponse>>>
    getPendingCollections() {

        return ResponseEntity.ok(

                ApiResponse
                        .<List<PendingCollectionResponse>>builder()
                        .success(true)
                        .message(
                                "Pending collections fetched successfully"
                        )
                        .data(
                                service.getPendingCollections()
                        )
                        .build()

        );

    }
    @GetMapping("/overdue")
    public ResponseEntity<
            ApiResponse<List<OverdueCollectionResponse>>>
    getOverdueCollections() {

        return ResponseEntity.ok(

                ApiResponse
                        .<List<OverdueCollectionResponse>>builder()
                        .success(true)
                        .message(
                                "Overdue collections fetched successfully"
                        )
                        .data(
                                service.getOverdueCollections()
                        )
                        .build()

        );

    }

    @PostMapping
    public ResponseEntity<ApiResponse<CollectionResponse>>
    collectPayment(
            @RequestBody
            CreateCollectionRequest request
    ) {

        CollectionResponse response =
                service.collectPayment(request);

        return ResponseEntity.ok(

                ApiResponse
                        .<CollectionResponse>builder()
                        .success(true)
                        .message("Collection completed successfully")
                        .data(response)
                        .build()

        );

    }
    
    @GetMapping("/history")
    public ResponseEntity<
            ApiResponse<
                    List<CollectionHistoryResponse>>>
    getCollectionHistory() {

        return ResponseEntity.ok(

                ApiResponse
                        .<List<CollectionHistoryResponse>>
                                builder()

                        .success(true)

                        .message(
                                "Collection history fetched successfully"
                        )

                        .data(
                                service.getCollectionHistory()
                        )

                        .build()

        );

    }
    
    @GetMapping("/loan/{loanId}")
    public ResponseEntity<
            List<CollectionResponse>>
    getLoanCollections(
            @PathVariable UUID loanId
    ) {

        return ResponseEntity.ok(
                service.getLoanCollections(
                        loanId
                )
        );
    }
    @GetMapping("/{collectionId}")
    public ResponseEntity<CollectionResponse>
    getCollectionById(
            @PathVariable UUID collectionId
    ) {

        return ResponseEntity.ok(
                service.getCollectionById(
                        collectionId
                )
        );
    }
}