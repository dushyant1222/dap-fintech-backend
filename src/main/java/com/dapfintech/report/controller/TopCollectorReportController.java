package com.dapfintech.report.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.report.dto.response.TopCollectorResponse;
import com.dapfintech.report.service.TopCollectorReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reports/employees")
@RequiredArgsConstructor
public class TopCollectorReportController {

    private final TopCollectorReportService
            reportService;

    @GetMapping("/top-collector")
    public ResponseEntity<TopCollectorResponse>
    getTopCollector() {

        return ResponseEntity.ok(
                reportService.getTopCollector()
        );
    }
}