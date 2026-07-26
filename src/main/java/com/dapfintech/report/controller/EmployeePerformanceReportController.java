package com.dapfintech.report.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.report.dto.response.EmployeePerformanceResponse;
import com.dapfintech.report.service.EmployeePerformanceReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reports/employees")
@RequiredArgsConstructor
public class EmployeePerformanceReportController {

    private final EmployeePerformanceReportService
            reportService;

    @GetMapping("/performance")
    public ResponseEntity<
            List<EmployeePerformanceResponse>>
    getEmployeePerformanceReport() {

        return ResponseEntity.ok(
                reportService
                        .getEmployeePerformanceReport()
        );
    }
}