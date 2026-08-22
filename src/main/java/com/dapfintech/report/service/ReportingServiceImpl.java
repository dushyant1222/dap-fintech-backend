package com.dapfintech.report.service;

import com.dapfintech.loan.entity.Loan;
import com.dapfintech.loan.entity.LoanCollection;
import com.dapfintech.loan.entity.LoanRepaymentSchedule;
import com.dapfintech.loan.repository.LoanCollectionRepository;
import com.dapfintech.loan.repository.LoanRepository;
import com.dapfintech.loan.repository.LoanRepaymentScheduleRepository;
import com.dapfintech.report.dto.LedgerPreviewDto;
import com.dapfintech.report.dto.LedgerPreviewScheduleDto;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ReportingServiceImpl implements ReportingService {

    private final LoanCollectionRepository collectionRepository;
    private final LoanRepository loanRepository;
    private final LoanRepaymentScheduleRepository scheduleRepository;

    public ReportingServiceImpl(LoanCollectionRepository collectionRepository,
                                LoanRepository loanRepository,
                                LoanRepaymentScheduleRepository scheduleRepository) {
        this.collectionRepository = collectionRepository;
        this.loanRepository = loanRepository;
        this.scheduleRepository = scheduleRepository;
    }

    private String getFullName(com.dapfintech.customer.entity.Customer c) {
        if (c == null) return "";
        return c.getFirstName() + " " + c.getLastName();
    }

    @Override
    public ByteArrayInputStream generateCollectionReportExcel(UUID marketId, UUID customerId) {
        List<LoanCollection> collections = collectionRepository.findAll();
        
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Collections");
            
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Receipt No", "Date", "Customer Name", "Loan Code", "Amount Collected", "Mode", "Collected By"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
            }
            
            int rowIdx = 1;
            for (LoanCollection col : collections) {
                if (col.getLoan() == null || col.getLoan().getCustomer() == null) continue;
                if (customerId != null && !col.getLoan().getCustomer().getId().equals(customerId)) continue;
                if (marketId != null && !col.getLoan().getCustomer().getMarket().getId().equals(marketId)) continue;
                
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(col.getReceiptNumber());
                row.createCell(1).setCellValue(col.getCollectionDate() != null ? col.getCollectionDate().toString() : "");
                row.createCell(2).setCellValue(getFullName(col.getLoan().getCustomer()));
                row.createCell(3).setCellValue(col.getLoan().getLoanCode());
                row.createCell(4).setCellValue(col.getCollectedAmount() != null ? col.getCollectedAmount().doubleValue() : 0.0);
                row.createCell(5).setCellValue(col.getCollectionMode() != null ? col.getCollectionMode().name() : "");
                row.createCell(6).setCellValue(col.getCollectedBy() != null ? col.getCollectedBy().getFullName() : "");
            }
            
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate collection excel", e);
        }
    }

    @Override
    public ByteArrayInputStream generateLedgerReportExcel(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        List<LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Ledger Report");

            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("Customer:");
            row0.createCell(1).setCellValue(getFullName(loan.getCustomer()));

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Loan Code:");
            row1.createCell(1).setCellValue(loan.getLoanCode());

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Disbursed Amount:");
            row2.createCell(1).setCellValue(loan.getDisbursedAmount() != null ? loan.getDisbursedAmount().doubleValue() : 0.0);

            Row headerRow = sheet.createRow(4);
            String[] columns = {"Installment", "Due Date", "EMI Amount", "Status", "Paid Amount"};
            for (int i = 0; i < columns.length; i++) {
                headerRow.createCell(i).setCellValue(columns[i]);
            }

            int rowIdx = 5;
            for (LoanRepaymentSchedule s : schedules) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(s.getInstallmentNumber() != null ? s.getInstallmentNumber() : 0);
                row.createCell(1).setCellValue(s.getDueDate() != null ? s.getDueDate().toString() : "");
                row.createCell(2).setCellValue(s.getInstallmentAmount() != null ? s.getInstallmentAmount().doubleValue() : 0.0);
                row.createCell(3).setCellValue(s.getRepaymentStatus() != null ? s.getRepaymentStatus().name() : "");
                row.createCell(4).setCellValue(s.getPaidAmount() != null ? s.getPaidAmount().doubleValue() : 0.0);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ledger excel", e);
        }
    }

    @Override
    public ByteArrayInputStream generateLedgerReportPdf(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        List<LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Customer Ledger Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Customer Name: " + getFullName(loan.getCustomer())));
            document.add(new Paragraph("Loan Code: " + loan.getLoanCode()));
            document.add(new Paragraph("Disbursed Amount: " + (loan.getDisbursedAmount() != null ? loan.getDisbursedAmount() : "0")));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            
            String[] headers = {"Installment", "Due Date", "EMI Amount", "Status", "Paid Amount"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            for (LoanRepaymentSchedule s : schedules) {
                table.addCell(String.valueOf(s.getInstallmentNumber()));
                table.addCell(s.getDueDate() != null ? s.getDueDate().toString() : "");
                table.addCell(s.getInstallmentAmount() != null ? s.getInstallmentAmount().toString() : "0");
                table.addCell(s.getRepaymentStatus() != null ? s.getRepaymentStatus().name() : "");
                table.addCell(s.getPaidAmount() != null ? s.getPaidAmount().toString() : "0");
            }

            document.add(table);
            document.close();
            
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ledger pdf", e);
        }
    }

    @Override
    public LedgerPreviewDto getLedgerPreview(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        List<LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);

        LedgerPreviewDto dto = new LedgerPreviewDto();
        dto.setLoanCode(loan.getLoanCode());
        dto.setCustomerName(getFullName(loan.getCustomer()));
        dto.setMarketName(loan.getCustomer() != null && loan.getCustomer().getMarket() != null ? loan.getCustomer().getMarket().getMarketName() : "");
        dto.setStartDate(loan.getDisbursementDate() != null ? loan.getDisbursementDate().toLocalDate() : null);
        dto.setDisbursedAmount(loan.getDisbursedAmount());
        dto.setTotalInterest(BigDecimal.ZERO);
        dto.setLoanType(loan.getRepaymentFrequency() != null ? loan.getRepaymentFrequency().name() : "");
        dto.setStatus(loan.getLoanStatus() != null ? loan.getLoanStatus().name() : "");
        
        BigDecimal outstanding = BigDecimal.ZERO;
        for (LoanRepaymentSchedule s : schedules) {
            if (s.getDueAmount() != null) outstanding = outstanding.add(s.getDueAmount());
        }
        dto.setOutstandingBalance(outstanding);
        
        List<LedgerPreviewScheduleDto> schedDtos = new ArrayList<>();
        for (LoanRepaymentSchedule s : schedules) {
            LedgerPreviewScheduleDto sd = new LedgerPreviewScheduleDto();
            sd.setInstallmentNo(s.getInstallmentNumber());
            sd.setDueDate(s.getDueDate());
            sd.setEmiAmount(s.getInstallmentAmount());
            sd.setStatus(s.getRepaymentStatus() != null ? s.getRepaymentStatus().name() : "");
            sd.setPaidDate(s.getUpdatedAt());
            sd.setAmountPaid(s.getPaidAmount());
            schedDtos.add(sd);
        }
        dto.setSchedules(schedDtos);
        
        return dto;
    }

    @Override
    public ByteArrayInputStream generateEmployeeDaybookPdf(UUID employeeId, LocalDate date) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Employee Daybook Ledger", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Employee ID: " + employeeId));
            document.add(new Paragraph("Date: " + date.toString()));
            document.add(new Paragraph(" "));
            
            document.add(new Paragraph("Daybook data would be listed here."));

            document.close();
            
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate daybook pdf", e);
        }
    }
}
