package com.dapfintech.report.controller;

import com.dapfintech.report.dto.LedgerPreviewDto;
import com.dapfintech.report.service.ReportingService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/collection/excel")
    public ResponseEntity<InputStreamResource> downloadCollectionExcel(
            @RequestParam(required = false) UUID marketId,
            @RequestParam(required = false) UUID customerId) {
            
        ByteArrayInputStream stream = reportingService.generateCollectionReportExcel(marketId, customerId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=collection_report.xlsx");
        
        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(stream));
    }

    @GetMapping("/ledger/excel")
    public ResponseEntity<InputStreamResource> downloadLedgerExcel(@RequestParam UUID loanId) {
        ByteArrayInputStream stream = reportingService.generateLedgerReportExcel(loanId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=ledger_report.xlsx");
        
        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(stream));
    }

    @GetMapping("/ledger/pdf")
    public ResponseEntity<InputStreamResource> downloadLedgerPdf(@RequestParam UUID loanId) {
        ByteArrayInputStream stream = reportingService.generateLedgerReportPdf(loanId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=ledger_report.pdf");
        
        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(stream));
    }

    @GetMapping("/ledger/preview")
    public ResponseEntity<LedgerPreviewDto> getLedgerPreview(@RequestParam UUID loanId) {
        return ResponseEntity.ok(reportingService.getLedgerPreview(loanId));
    }

    @GetMapping("/daybook/pdf")
    public ResponseEntity<InputStreamResource> downloadDaybookPdf(
            @RequestParam UUID employeeId,
            @RequestParam String date) {
        
        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        ByteArrayInputStream stream = reportingService.generateEmployeeDaybookPdf(employeeId, parsedDate);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=daybook_report.pdf");
        
        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(stream));
    }
}
