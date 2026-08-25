package com.dapfintech.capital.controller;

import com.dapfintech.capital.dto.PivotFilterRequest;
import com.dapfintech.capital.dto.PivotRowResponse;
import com.dapfintech.capital.dto.PivotTableResponse;
import com.dapfintech.capital.service.CapitalService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/capital/export")
@RequiredArgsConstructor
public class CapitalExportController {

    private final CapitalService capitalService;

    @GetMapping("/excel")
    public ResponseEntity<ByteArrayResource> exportCapitalExcel(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String marketId,
            @RequestParam(required = false) String employeeId) throws IOException {

        PivotFilterRequest filter = new PivotFilterRequest();
        if (startDate != null && !startDate.isEmpty()) {
            filter.setStartDate(LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            filter.setEndDate(LocalDate.parse(endDate));
        }
        if (marketId != null && !marketId.isEmpty() && !marketId.equals("null")) {
            filter.setMarketId(java.util.UUID.fromString(marketId));
        }
        if (employeeId != null && !employeeId.isEmpty() && !employeeId.equals("null")) {
            filter.setEmployeeId(java.util.UUID.fromString(employeeId));
        }

        PivotTableResponse response = capitalService.getPivotTable(filter);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Metrix Ledger");

            // Header Row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Date/Label", "Capital In", "Disbursed", "Market Balance", "Collected", "Pending Emp", "Interest", "Net Earnings"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            int rowIdx = 1;
            for (PivotRowResponse rowResp : response.getRows()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(rowResp.getGroupLabel() != null ? rowResp.getGroupLabel() : "");
                row.createCell(1).setCellValue(rowResp.getCapitalIn() != null ? rowResp.getCapitalIn().doubleValue() : 0.0);
                row.createCell(2).setCellValue(rowResp.getDisbursedAmount() != null ? rowResp.getDisbursedAmount().doubleValue() : 0.0);
                row.createCell(3).setCellValue(rowResp.getMarketBalance() != null ? rowResp.getMarketBalance().doubleValue() : 0.0);
                row.createCell(4).setCellValue(rowResp.getCollectedAmount() != null ? rowResp.getCollectedAmount().doubleValue() : 0.0);
                row.createCell(5).setCellValue(rowResp.getPendingEmployeeBalance() != null ? rowResp.getPendingEmployeeBalance().doubleValue() : 0.0);
                row.createCell(6).setCellValue(rowResp.getInterest() != null ? rowResp.getInterest().doubleValue() : 0.0);
                row.createCell(7).setCellValue(rowResp.getNetEarnings() != null ? rowResp.getNetEarnings().doubleValue() : 0.0);
            }

            workbook.write(out);
            ByteArrayResource resource = new ByteArrayResource(out.toByteArray());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=capital_metrix.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(resource.contentLength())
                    .body(resource);
        }
    }
}
