package com.dapfintech.report.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.report.dto.response.CollectionReportResponse;
import com.dapfintech.report.dto.response.EmployeeCollectionReportResponse;
import com.dapfintech.report.dto.response.MarketCollectionReportResponse;
import com.dapfintech.report.service.CollectionReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reports/collections")
@RequiredArgsConstructor
public class CollectionReportController {

    private final CollectionReportService
            collectionReportService;
    
    @GetMapping("/date-range")
    public ResponseEntity<
            CollectionReportResponse>
    getDateRangeReport(

            @RequestParam
            LocalDate fromDate,

            @RequestParam
            LocalDate toDate
    ) {

        return ResponseEntity.ok(
                collectionReportService
                        .getDateRangeCollectionReport(
                                fromDate,
                                toDate
                        )
        );
    }
    
    @GetMapping("/employees")
    public ResponseEntity<
            List<EmployeeCollectionReportResponse>>
    getEmployeeCollectionReport() {

        return ResponseEntity.ok(
                collectionReportService
                        .getEmployeeCollectionReport()
        );
    }

    @GetMapping("/today")
    public ResponseEntity<
            CollectionReportResponse>
    getTodayReport() {

        return ResponseEntity.ok(
                collectionReportService
                        .getTodayCollectionReport()
        );
    }
    
    @GetMapping("/markets")
    public ResponseEntity<
            List<MarketCollectionReportResponse>>
    getMarketCollectionReport() {

        return ResponseEntity.ok(
                collectionReportService
                        .getMarketCollectionReport()
        );
    }

    @GetMapping("/month")
    public ResponseEntity<
            CollectionReportResponse>
    getMonthReport() {

        return ResponseEntity.ok(
                collectionReportService
                        .getMonthCollectionReport()
        );
    }
}