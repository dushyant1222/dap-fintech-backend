package com.dapfintech.employee.controller;

import com.dapfintech.employee.dto.DayBookResponse;
import com.dapfintech.employee.service.DayBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
}
