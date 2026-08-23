package com.dapfintech.report.service;

import com.dapfintech.report.dto.LedgerPreviewDto;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;

import java.util.UUID;

public interface ReportingService {
    ByteArrayInputStream generateCollectionReportExcel(UUID marketId, UUID customerId);
    
    ByteArrayInputStream generateLedgerReportExcel(UUID loanId);
    
    ByteArrayInputStream generateLedgerReportPdf(UUID loanId);
    
    LedgerPreviewDto getLedgerPreview(UUID loanId);
    
    ByteArrayInputStream generateEmployeeDaybookExcel(UUID employeeId, LocalDate date);
    
    ByteArrayInputStream generateEmployeeDaybookPdf(UUID employeeId, LocalDate date);
}
