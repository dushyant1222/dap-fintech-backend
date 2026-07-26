package com.dapfintech.report.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.report.dto.response.MarketPerformanceResponse;
import com.dapfintech.report.service.MarketPerformanceReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reports/markets")
@RequiredArgsConstructor
public class MarketPerformanceReportController {

    private final MarketPerformanceReportService
            reportService;

    @GetMapping("/performance")
    public ResponseEntity<
            List<MarketPerformanceResponse>>
    getMarketPerformanceReport() {

        return ResponseEntity.ok(
                reportService
                        .getMarketPerformanceReport()
        );
    }
}