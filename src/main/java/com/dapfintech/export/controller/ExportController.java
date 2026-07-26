package com.dapfintech.export.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.export.service.ExcelExportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExcelExportService
            excelExportService;
    
    @GetMapping("/collections")
    public ResponseEntity<
            ByteArrayResource>
    exportCollections() {

        ByteArrayResource file =
                excelExportService
                        .exportCollections();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=collections.xlsx"
                )
                .contentType(
                        MediaType.APPLICATION_OCTET_STREAM
                )
                .body(file);
    }

    @GetMapping("/overdue-customers")
    public ResponseEntity<
            ByteArrayResource>
    exportOverdueCustomers() {

        ByteArrayResource file =
                excelExportService
                        .exportOverdueCustomers();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=overdue-customers.xlsx"
                )
                .contentType(
                        MediaType.APPLICATION_OCTET_STREAM
                )
                .body(file);
    }
}