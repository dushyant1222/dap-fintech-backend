package com.dapfintech.report.controller;

import com.dapfintech.report.dto.LedgerPreviewDto;
import com.dapfintech.report.service.ReportingService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
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
    public ResponseEntity<Resource> downloadCollectionExcel(
            @RequestParam(required = false) UUID marketId,
            @RequestParam(required = false) UUID customerId) throws Exception {
            
        ByteArrayInputStream stream = reportingService.generateCollectionReportExcel(marketId, customerId);
        byte[] bytes = stream.readAllBytes();
        
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"collection_report.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(bytes.length)
                .body(new org.springframework.core.io.ByteArrayResource(bytes));
    }

    @GetMapping("/collection/pdf")
    public ResponseEntity<Resource> downloadCollectionPdf(
            @RequestParam(required = false) UUID marketId,
            @RequestParam(required = false) UUID customerId) throws Exception {
            
        ByteArrayInputStream stream = reportingService.generateCollectionReportPdf(marketId, customerId);
        byte[] bytes = stream.readAllBytes();
        
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"collection_report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .body(new org.springframework.core.io.ByteArrayResource(bytes));
    }

    @GetMapping("/ledger/excel")
    public ResponseEntity<Resource> downloadLedgerExcel(@RequestParam UUID loanId) throws Exception {
        ByteArrayInputStream stream = reportingService.generateLedgerReportExcel(loanId);
        byte[] bytes = stream.readAllBytes();
        
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ledger_report.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(bytes.length)
                .body(new org.springframework.core.io.ByteArrayResource(bytes));
    }

    @GetMapping("/ledger/pdf")
    public ResponseEntity<Resource> downloadLedgerPdf(@RequestParam UUID loanId) throws Exception {
        ByteArrayInputStream stream = reportingService.generateLedgerReportPdf(loanId);
        byte[] bytes = stream.readAllBytes();
        
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ledger_report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .body(new org.springframework.core.io.ByteArrayResource(bytes));
    }

    @GetMapping("/ledger/preview")
    public ResponseEntity<LedgerPreviewDto> getLedgerPreview(@RequestParam UUID loanId) {
        return ResponseEntity.ok(reportingService.getLedgerPreview(loanId));
    }

    @GetMapping("/daybook/pdf")
    public ResponseEntity<Resource> downloadDaybookPdf(
            @RequestParam UUID employeeId,
            @RequestParam String date) throws Exception {
        
        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        ByteArrayInputStream stream = reportingService.generateEmployeeDaybookPdf(employeeId, parsedDate);
        byte[] bytes = stream.readAllBytes();
        
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"daybook_report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .body(new org.springframework.core.io.ByteArrayResource(bytes));
    }

    @GetMapping("/daybook/excel")
    public ResponseEntity<Resource> downloadDaybookExcel(
            @RequestParam UUID employeeId,
            @RequestParam String date) throws Exception {
        
        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        ByteArrayInputStream stream = reportingService.generateEmployeeDaybookExcel(employeeId, parsedDate);
        byte[] bytes = stream.readAllBytes();
        
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"daybook_report.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(bytes.length)
                .body(new org.springframework.core.io.ByteArrayResource(bytes));
    }
}
