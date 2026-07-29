package com.dapfintech.capital.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.capital.dto.CapitalSummaryResponse;
import com.dapfintech.capital.dto.CreateCapitalInRequest;
import com.dapfintech.capital.dto.CreateCashSettlementRequest;
import com.dapfintech.capital.dto.CreateExpenseRequest;
import com.dapfintech.capital.dto.PivotFilterRequest;
import com.dapfintech.capital.dto.PivotTableResponse;
import com.dapfintech.capital.entity.CapitalIn;
import com.dapfintech.capital.entity.CashSettlement;
import com.dapfintech.capital.entity.Expense;
import com.dapfintech.capital.service.CapitalService;
import com.dapfintech.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/capital")
@RequiredArgsConstructor
public class CapitalController {

    private final CapitalService capitalService;

    @PostMapping("/in")
    public ResponseEntity<ApiResponse<CapitalIn>> addCapitalIn(
            @RequestBody CreateCapitalInRequest request
    ) {
        CapitalIn capitalIn = capitalService.addCapitalIn(request);
        return ResponseEntity.ok(
                ApiResponse.<CapitalIn>builder()
                        .success(true)
                        .message("Capital added successfully")
                        .data(capitalIn)
                        .build()
        );
    }
    
    @PostMapping("/out")
    public ResponseEntity<ApiResponse<CapitalIn>> addCapitalOut(
            @RequestBody CreateCapitalInRequest request
    ) {
        CapitalIn capitalOut = capitalService.addCapitalOut(request);
        return ResponseEntity.ok(
                ApiResponse.<CapitalIn>builder()
                        .success(true)
                        .message("Capital out recorded successfully")
                        .data(capitalOut)
                        .build()
        );
    }

    @GetMapping("/in")
    public ResponseEntity<ApiResponse<List<CapitalIn>>> getAllCapitalIn() {
        List<CapitalIn> list = capitalService.getAllCapitalIn();
        return ResponseEntity.ok(
                ApiResponse.<List<CapitalIn>>builder()
                        .success(true)
                        .message("Capital entries retrieved")
                        .data(list)
                        .build()
        );
    }

    @PostMapping("/expenses")
    public ResponseEntity<ApiResponse<Expense>> addExpense(
            @RequestBody CreateExpenseRequest request
    ) {
        Expense expense = capitalService.addExpense(request);
        return ResponseEntity.ok(
                ApiResponse.<Expense>builder()
                        .success(true)
                        .message("Expense recorded successfully")
                        .data(expense)
                        .build()
        );
    }

    @GetMapping("/expenses")
    public ResponseEntity<ApiResponse<List<Expense>>> getAllExpenses() {
        List<Expense> list = capitalService.getAllExpenses();
        return ResponseEntity.ok(
                ApiResponse.<List<Expense>>builder()
                        .success(true)
                        .message("Expenses retrieved")
                        .data(list)
                        .build()
        );
    }

    @PostMapping("/settlements")
    public ResponseEntity<ApiResponse<CashSettlement>> addCashSettlement(
            @RequestBody CreateCashSettlementRequest request
    ) {
        CashSettlement settlement = capitalService.addCashSettlement(request);
        return ResponseEntity.ok(
                ApiResponse.<CashSettlement>builder()
                        .success(true)
                        .message("Cash settlement recorded successfully")
                        .data(settlement)
                        .build()
        );
    }

    @GetMapping("/settlements")
    public ResponseEntity<ApiResponse<List<CashSettlement>>> getAllCashSettlements() {
        List<CashSettlement> list = capitalService.getAllCashSettlements();
        return ResponseEntity.ok(
                ApiResponse.<List<CashSettlement>>builder()
                        .success(true)
                        .message("Cash settlements retrieved")
                        .data(list)
                        .build()
        );
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<CapitalSummaryResponse>> getCapitalSummary() {
        CapitalSummaryResponse summary = capitalService.getCapitalSummary();
        return ResponseEntity.ok(
                ApiResponse.<CapitalSummaryResponse>builder()
                        .success(true)
                        .message("Capital summary retrieved")
                        .data(summary)
                        .build()
        );
    }

    @PostMapping("/pivot-table")
    public ResponseEntity<ApiResponse<PivotTableResponse>> getPivotTable(
            @RequestBody(required = false) PivotFilterRequest filter
    ) {
        if (filter == null) {
            filter = new PivotFilterRequest();
        }
        PivotTableResponse response = capitalService.getPivotTable(filter);
        return ResponseEntity.ok(
                ApiResponse.<PivotTableResponse>builder()
                        .success(true)
                        .message("Pivot table data retrieved")
                        .data(response)
                        .build()
        );
    }
}
