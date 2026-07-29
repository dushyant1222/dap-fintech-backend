package com.dapfintech.employee.controller;

import com.dapfintech.employee.dto.DayBookResponse;
import com.dapfintech.employee.dto.UpdateDayBookRequest;
import com.dapfintech.employee.service.DayBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/daybooks")
public class AdminDayBookController {

    @Autowired
    private DayBookService dayBookService;

    @PutMapping("/{dayBookId}/approve")
    public ResponseEntity<DayBookResponse> approveClosure(@PathVariable UUID dayBookId) {
        DayBookResponse response = dayBookService.approveClosure(dayBookId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{dayBookId}/reject")
    public ResponseEntity<DayBookResponse> rejectClosure(@PathVariable UUID dayBookId) {
        DayBookResponse response = dayBookService.rejectClosure(dayBookId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{dayBookId}")
    public ResponseEntity<DayBookResponse> updateDayBook(
            @PathVariable UUID dayBookId,
            @RequestBody UpdateDayBookRequest request) {
        DayBookResponse response = dayBookService.updateDayBook(dayBookId, request);
        return ResponseEntity.ok(response);
    }
}
