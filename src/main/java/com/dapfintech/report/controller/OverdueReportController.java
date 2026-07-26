package com.dapfintech.report.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.report.dto.response.BucketWiseOverdueResponse;
import com.dapfintech.report.dto.response.OverdueCustomerResponse;
import com.dapfintech.report.dto.response.OverdueSummaryResponse;
import com.dapfintech.report.service.OverdueReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reports/overdue")
@RequiredArgsConstructor
public class OverdueReportController {

    private final OverdueReportService
            overdueReportService;
    
    @GetMapping("/buckets")
    public ResponseEntity<
            List<BucketWiseOverdueResponse>>
    getBucketWiseOverdueReport() {

        return ResponseEntity.ok(
                overdueReportService
                        .getBucketWiseOverdueReport()
        );
    }

    @GetMapping("/summary")
    public ResponseEntity<
            OverdueSummaryResponse>
    getOverdueSummary() {

        return ResponseEntity.ok(
                overdueReportService
                        .getOverdueSummary()
        );
    }
    
    @GetMapping("/customers")
    public ResponseEntity<
            List<OverdueCustomerResponse>>
    getOverdueCustomers() {

        return ResponseEntity.ok(
                overdueReportService
                        .getOverdueCustomers()
        );
    }
}