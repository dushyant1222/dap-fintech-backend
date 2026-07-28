package com.dapfintech.employee.controller;

import com.dapfintech.employee.dto.DayBookResponse;
import com.dapfintech.employee.dto.DayBookTransactionRequest;
import com.dapfintech.employee.service.DayBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/employees/{employeeId}/daybooks")
public class DayBookController {

    @Autowired
    private DayBookService dayBookService;

    @GetMapping("/today")
    public ResponseEntity<DayBookResponse> getOrCreateTodayDayBook(@PathVariable UUID employeeId) {
        DayBookResponse response = dayBookService.getOrCreateTodayDayBook(employeeId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/today/transactions")
    public ResponseEntity<DayBookResponse> addTransaction(
            @PathVariable UUID employeeId,
            @RequestBody DayBookTransactionRequest request) {
        DayBookResponse response = dayBookService.addTransaction(employeeId, request);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/today/request-closure")
    public ResponseEntity<DayBookResponse> requestClosure(@PathVariable UUID employeeId) {
        DayBookResponse response = dayBookService.requestClosure(employeeId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<List<DayBookResponse>> getEmployeeDayBooks(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(dayBookService.getEmployeeDayBooks(employeeId));
    }
}
