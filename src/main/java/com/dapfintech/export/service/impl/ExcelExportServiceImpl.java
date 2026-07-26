package com.dapfintech.export.service.impl;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import com.dapfintech.export.service.ExcelExportService;
import com.dapfintech.loan.entity.LoanCollection;
import com.dapfintech.loan.repository.LoanCollectionRepository;
import com.dapfintech.report.dto.response.OverdueCustomerResponse;
import com.dapfintech.report.service.OverdueReportService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExcelExportServiceImpl
        implements ExcelExportService {

    private final OverdueReportService
            overdueReportService;
    private final LoanCollectionRepository
    collectionRepository;
    
    @Override
    public ByteArrayResource
    exportCollections() {

        try {

            List<LoanCollection> collections =
                    collectionRepository
                            .findAllByOrderByCollectionDateDesc();

            XSSFWorkbook workbook =
                    new XSSFWorkbook();

            XSSFSheet sheet =
                    workbook.createSheet(
                            "Collections"
                    );

            Row header =
                    sheet.createRow(0);

            header.createCell(0)
                    .setCellValue(
                            "Receipt Number"
                    );

            header.createCell(1)
                    .setCellValue(
                            "Collection Date"
                    );

            header.createCell(2)
                    .setCellValue(
                            "Collected Amount"
                    );

            header.createCell(3)
                    .setCellValue(
                            "Collection Mode"
                    );

            header.createCell(4)
                    .setCellValue(
                            "Employee"
                    );

            int rowNum = 1;

            for(
                LoanCollection collection
                : collections
            ) {

                Row row =
                        sheet.createRow(
                                rowNum++
                        );

                row.createCell(0)
                        .setCellValue(
                                collection.getReceiptNumber()
                        );

                row.createCell(1)
                        .setCellValue(
                                collection
                                        .getCollectionDate()
                                        .toString()
                        );

                row.createCell(2)
                        .setCellValue(
                                collection
                                        .getCollectedAmount()
                                        .doubleValue()
                        );

                row.createCell(3)
                        .setCellValue(
                                collection
                                        .getCollectionMode()
                                        .name()
                        );

                row.createCell(4)
                        .setCellValue(
                                collection.getCollectedBy() != null
                                ? collection
                                        .getCollectedBy()
                                        .getFullName()
                                : "N/A"
                        );
            }

            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            workbook.write(out);

            workbook.close();

            return new ByteArrayResource(
                    out.toByteArray()
            );

        } catch(Exception e) {

            throw new RuntimeException(
                    "Failed to export collection report"
            );
        }
    }

    @Override
    public ByteArrayResource
    exportOverdueCustomers() {

        try {

            List<OverdueCustomerResponse>
                    customers =
                    overdueReportService
                            .getOverdueCustomers();

            XSSFWorkbook workbook =
                    new XSSFWorkbook();

            XSSFSheet sheet =
                    workbook.createSheet(
                            "Overdue Customers"
                    );

            Row header =
                    sheet.createRow(0);

            header.createCell(0)
                    .setCellValue(
                            "Customer Name"
                    );

            header.createCell(1)
                    .setCellValue(
                            "Mobile Number"
                    );

            header.createCell(2)
                    .setCellValue(
                            "Market"
                    );

            header.createCell(3)
                    .setCellValue(
                            "Overdue Amount"
                    );

            header.createCell(4)
                    .setCellValue(
                            "Overdue Days"
                    );

            int rowNum = 1;

            for(
                OverdueCustomerResponse customer
                : customers
            ) {

                Row row =
                        sheet.createRow(
                                rowNum++
                        );

                row.createCell(0)
                        .setCellValue(
                                customer.getCustomerName()
                        );

                row.createCell(1)
                        .setCellValue(
                                customer.getMobileNumber()
                        );

                row.createCell(2)
                        .setCellValue(
                                customer.getMarketName()
                        );

                row.createCell(3)
                        .setCellValue(
                                customer
                                        .getOverdueAmount()
                                        .doubleValue()
                        );

                row.createCell(4)
                        .setCellValue(
                                customer
                                        .getOverdueDays()
                        );
            }

            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            workbook.write(out);

            workbook.close();

            return new ByteArrayResource(
                    out.toByteArray()
            );

        } catch(Exception e) {

            throw new RuntimeException(
                    "Failed to export report"
            );
        }
    }
}