package com.dapfintech.report.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.report.dto.response.RecoveryEfficiencyResponse;
import com.dapfintech.report.service.RecoveryEfficiencyReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reports/employees")
@RequiredArgsConstructor
public class RecoveryEfficiencyReportController {

    private final RecoveryEfficiencyReportService
            reportService;

    @GetMapping("/recovery-efficiency")
    public ResponseEntity<
            List<RecoveryEfficiencyResponse>>
    getRecoveryEfficiencyReport() {

        return ResponseEntity.ok(
                reportService
                        .getRecoveryEfficiencyReport()
        );
    }
}