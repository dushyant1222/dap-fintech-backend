package com.dapfintech.report.service;

import com.dapfintech.loan.entity.Loan;
import com.dapfintech.loan.entity.LoanCollection;
import com.dapfintech.loan.entity.LoanRepaymentSchedule;
import com.dapfintech.loan.repository.LoanCollectionRepository;
import com.dapfintech.loan.repository.LoanRepository;
import com.dapfintech.loan.repository.LoanRepaymentScheduleRepository;
import com.dapfintech.employee.repository.DayBookRepository;
import com.dapfintech.employee.entity.DayBook;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.auth.entity.User;
import com.dapfintech.loan.entity.LoanCollection;

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
    private final DayBookRepository dayBookRepository;
    private final UserRepository userRepository;


    public ReportingServiceImpl(LoanCollectionRepository collectionRepository, DayBookRepository dayBookRepository, UserRepository userRepository,
                                LoanRepository loanRepository,
                                LoanRepaymentScheduleRepository scheduleRepository) {
        this.collectionRepository = collectionRepository;
        this.loanRepository = loanRepository;
        this.scheduleRepository = scheduleRepository;
        this.dayBookRepository = dayBookRepository;
        this.userRepository = userRepository;
    }

    private String getFullName(com.dapfintech.customer.entity.Customer c) {
        if (c == null) return "";
        return c.getFirstName() + " " + c.getLastName();
    }

    @Override
    public ByteArrayInputStream generateCollectionReportExcel(UUID marketId, UUID customerId) {
        List<com.dapfintech.loan.entity.Loan> loans = loanRepository.findAll();
        loans.removeIf(l -> l.getCustomer() == null);
        if (marketId != null) loans.removeIf(l -> l.getCustomer().getMarket() == null || !l.getCustomer().getMarket().getId().equals(marketId));
        if (customerId != null) loans.removeIf(l -> !l.getCustomer().getId().equals(customerId));
        loans.removeIf(l -> l.getDisbursementDate() == null);
        
        if (loans.isEmpty()) {
            try (Workbook wb = new XSSFWorkbook(); java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                wb.createSheet("Collections").createRow(0).createCell(0).setCellValue("No data found");
                wb.write(out);
                return new ByteArrayInputStream(out.toByteArray());
            } catch(Exception e) { throw new RuntimeException(e); }
        }

        java.time.LocalDate minDate = loans.stream().map(l -> l.getDisbursementDate().toLocalDate()).min(java.time.LocalDate::compareTo).orElse(java.time.LocalDate.now());
        java.time.LocalDate maxDate = java.time.LocalDate.now();
        for (com.dapfintech.loan.entity.Loan l : loans) {
            java.time.LocalDate end = l.getDisbursementDate().toLocalDate();
            if (l.getDurationInDays() != null) end = end.plusDays(l.getDurationInDays());
            if (end.isAfter(maxDate)) maxDate = end;
        }
        if (maxDate.isAfter(java.time.LocalDate.now())) maxDate = java.time.LocalDate.now();
        
        List<java.time.LocalDate> dateColumns = new java.util.ArrayList<>();
        java.time.LocalDate curr = minDate;
        while (!curr.isAfter(maxDate)) {
            dateColumns.add(curr);
            curr = curr.plusDays(1);
        }

        List<com.dapfintech.loan.entity.LoanCollection> allCollections = collectionRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook(); java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Collection Report");
            
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("Collection Report");
            
            String marketName = "All Markets";
            if (marketId != null && !loans.isEmpty() && loans.get(0).getCustomer().getMarket() != null) {
                marketName = loans.get(0).getCustomer().getMarket().getMarketName();
            }
            Row subRow = sheet.createRow(1);
            subRow.createCell(0).setCellValue("Market Name: " + marketName);
            
            Row headerRow = sheet.createRow(3);
            String[] baseCols = {"S.No", "Customer Name", "Loan Issue Date", "Loan Code", "Loan Status", "Total Loan Amount", "Loan Type", "Tenure", "Collected Till Date", "Remaining Balance"};
            int colIdx = 0;
            for (String col : baseCols) {
                headerRow.createCell(colIdx++).setCellValue(col);
            }
            
            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd-MMM");
            for (java.time.LocalDate d : dateColumns) {
                headerRow.createCell(colIdx++).setCellValue("Date-" + d.format(dtf));
            }
            
            int rowIdx = 4;
            int sNo = 1;
            
            for (com.dapfintech.loan.entity.Loan loan : loans) {
                Row row = sheet.createRow(rowIdx++);
                int c = 0;
                row.createCell(c++).setCellValue(sNo++);
                row.createCell(c++).setCellValue(getFullName(loan.getCustomer()));
                row.createCell(c++).setCellValue(loan.getDisbursementDate().toLocalDate().toString());
                row.createCell(c++).setCellValue(loan.getLoanCode());
                row.createCell(c++).setCellValue(loan.getLoanStatus().name());
                row.createCell(c++).setCellValue(loan.getDisbursedAmount() != null ? loan.getDisbursedAmount().doubleValue() : 0.0);
                row.createCell(c++).setCellValue(loan.getLoanType() != null ? loan.getLoanType().name() : "");
                
                String tenure = "";
                if (loan.getTenure() != null) {
                    if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EDI) tenure = loan.getTenure() + " days";
                    else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EWI) tenure = loan.getTenure() + " weeks";
                    else tenure = loan.getTenure() + " months";
                }
                row.createCell(c++).setCellValue(tenure);
                
                double collected = 0;
                java.util.Map<java.time.LocalDate, Double> dailyMap = new java.util.HashMap<>();
                for (com.dapfintech.loan.entity.LoanCollection lc : allCollections) {
                    if (lc.getLoan() != null && lc.getLoan().getId().equals(loan.getId()) && lc.getCollectedAmount() != null) {
                        collected += lc.getCollectedAmount().doubleValue();
                        java.time.LocalDate cd = lc.getCollectionDate().toLocalDate();
                        dailyMap.put(cd, dailyMap.getOrDefault(cd, 0.0) + lc.getCollectedAmount().doubleValue());
                    }
                }
                
                row.createCell(c++).setCellValue(collected);
                double total = loan.getDisbursedAmount() != null ? loan.getDisbursedAmount().doubleValue() : 0;
                row.createCell(c++).setCellValue(Math.max(0, total - collected));
                
                for (java.time.LocalDate d : dateColumns) {
                    Cell cell = row.createCell(c++);
                    if (dailyMap.containsKey(d)) {
                        cell.setCellValue(dailyMap.get(d));
                    } else {
                        cell.setCellValue("X");
                    }
                }
            }
            
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate collection excel", e);
        }
    }

    @Override
    public ByteArrayInputStream generateCollectionReportPdf(UUID marketId, UUID customerId) {
        List<com.dapfintech.loan.entity.Loan> loans = loanRepository.findAll();
        loans.removeIf(l -> l.getCustomer() == null);
        if (marketId != null) loans.removeIf(l -> l.getCustomer().getMarket() == null || !l.getCustomer().getMarket().getId().equals(marketId));
        if (customerId != null) loans.removeIf(l -> !l.getCustomer().getId().equals(customerId));
        loans.removeIf(l -> l.getDisbursementDate() == null);
        
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        com.lowagie.text.Document document = new com.lowagie.text.Document(new com.lowagie.text.Rectangle(2500, 842));
        try {
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();
            document.add(new com.lowagie.text.Paragraph("Collection Report"));
            
            String marketName = "All Markets";
            if (marketId != null && !loans.isEmpty() && loans.get(0).getCustomer().getMarket() != null) {
                marketName = loans.get(0).getCustomer().getMarket().getMarketName();
            }
            document.add(new com.lowagie.text.Paragraph("Market Name: " + marketName));
            document.add(new com.lowagie.text.Paragraph(" "));

            if (loans.isEmpty()) {
                document.add(new com.lowagie.text.Paragraph("No data found"));
                document.close();
                return new ByteArrayInputStream(out.toByteArray());
            }

            java.time.LocalDate minDate = loans.stream().map(l -> l.getDisbursementDate().toLocalDate()).min(java.time.LocalDate::compareTo).orElse(java.time.LocalDate.now());
            java.time.LocalDate maxDate = java.time.LocalDate.now();
            for (com.dapfintech.loan.entity.Loan l : loans) {
                java.time.LocalDate end = l.getDisbursementDate().toLocalDate();
                if (l.getDurationInDays() != null) end = end.plusDays(l.getDurationInDays());
                if (end.isAfter(maxDate)) maxDate = end;
            }
            if (maxDate.isAfter(java.time.LocalDate.now())) maxDate = java.time.LocalDate.now();
            
            List<java.time.LocalDate> dateColumns = new java.util.ArrayList<>();
            java.time.LocalDate curr = minDate;
            while (!curr.isAfter(maxDate)) {
                dateColumns.add(curr);
                curr = curr.plusDays(1);
            }

            String[] baseCols = {"S.No", "Customer Name", "Loan Issue Date", "Loan Code", "Loan Status", "Total Loan Amount", "Loan Type", "Tenure", "Collected Till Date", "Remaining Balance"};
            int numColumns = baseCols.length + dateColumns.size();
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(numColumns);
            table.setWidthPercentage(100);
            
            for (String col : baseCols) {
                table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(col, com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 9))));
            }
            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd-MMM");
            for (java.time.LocalDate d : dateColumns) {
                table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("Date-" + d.format(dtf), com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 9))));
            }

            List<com.dapfintech.loan.entity.LoanCollection> allCollections = collectionRepository.findAll();
            int sNo = 1;
            com.lowagie.text.Font font = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 8);
            
            for (com.dapfintech.loan.entity.Loan loan : loans) {
                table.addCell(new com.lowagie.text.Phrase(String.valueOf(sNo++), font));
                table.addCell(new com.lowagie.text.Phrase(getFullName(loan.getCustomer()), font));
                table.addCell(new com.lowagie.text.Phrase(loan.getDisbursementDate().toLocalDate().toString(), font));
                table.addCell(new com.lowagie.text.Phrase(loan.getLoanCode(), font));
                table.addCell(new com.lowagie.text.Phrase(loan.getLoanStatus().name(), font));
                table.addCell(new com.lowagie.text.Phrase(loan.getDisbursedAmount() != null ? String.valueOf(loan.getDisbursedAmount().doubleValue()) : "0.0", font));
                table.addCell(new com.lowagie.text.Phrase(loan.getLoanType() != null ? loan.getLoanType().name() : "", font));
                
                String tenure = "";
                if (loan.getTenure() != null) {
                    if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EDI) tenure = loan.getTenure() + " days";
                    else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EWI) tenure = loan.getTenure() + " weeks";
                    else tenure = loan.getTenure() + " months";
                }
                table.addCell(new com.lowagie.text.Phrase(tenure, font));
                
                double collected = 0;
                java.util.Map<java.time.LocalDate, Double> dailyMap = new java.util.HashMap<>();
                for (com.dapfintech.loan.entity.LoanCollection lc : allCollections) {
                    if (lc.getLoan() != null && lc.getLoan().getId().equals(loan.getId()) && lc.getCollectedAmount() != null) {
                        collected += lc.getCollectedAmount().doubleValue();
                        java.time.LocalDate cd = lc.getCollectionDate().toLocalDate();
                        dailyMap.put(cd, dailyMap.getOrDefault(cd, 0.0) + lc.getCollectedAmount().doubleValue());
                    }
                }
                
                table.addCell(new com.lowagie.text.Phrase(String.valueOf(collected), font));
                double total = loan.getDisbursedAmount() != null ? loan.getDisbursedAmount().doubleValue() : 0;
                table.addCell(new com.lowagie.text.Phrase(String.valueOf(Math.max(0, total - collected)), font));
                
                for (java.time.LocalDate d : dateColumns) {
                    if (dailyMap.containsKey(d)) {
                        table.addCell(new com.lowagie.text.Phrase(String.valueOf(dailyMap.get(d)), font));
                    } else {
                        table.addCell(new com.lowagie.text.Phrase("X", font));
                    }
                }
            }
            
            document.add(table);
            document.close();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate collection PDF", e);
        }
    }

    @Override
    public ByteArrayInputStream generateLedgerReportExcel(UUID loanId) {
        com.dapfintech.loan.entity.Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        try (Workbook workbook = new XSSFWorkbook(); java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Customer Loan Report");
            
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("Customer Loan Report");
            
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Market Name:");
            row1.createCell(1).setCellValue(loan.getCustomer().getMarket() != null ? loan.getCustomer().getMarket().getMarketName() : "");
            
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Customer Name:");
            row2.createCell(1).setCellValue(getFullName(loan.getCustomer()));
            
            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("Loan ID:");
            row3.createCell(1).setCellValue(loan.getLoanCode());
            
            Row row4 = sheet.createRow(4);
            row4.createCell(0).setCellValue("Loan Amount:");
            row4.createCell(1).setCellValue(loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0.0);
            
            Row row5 = sheet.createRow(5);
            row5.createCell(0).setCellValue("Interest:");
            row5.createCell(1).setCellValue(loan.getInterestRate() != null ? loan.getInterestRate().toString() + "%" : "");
            
            String tenureStr = "";
            if (loan.getLoanType() == com.dapfintech.loan.enums.LoanType.EMERGENCY) {
                double val = (loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0) * (loan.getInterestRate() != null ? loan.getInterestRate().doubleValue() : 0) / 100.0;
                tenureStr = val + " per day";
            } else {
                if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EDI) tenureStr = "daily";
                else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EWI) tenureStr = "weekly";
                else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EMI) tenureStr = "monthly";
            }
            Row row6 = sheet.createRow(6);
            row6.createCell(0).setCellValue("Tenure:");
            row6.createCell(1).setCellValue(tenureStr);
            
            List<com.dapfintech.loan.entity.LoanCollection> cols = collectionRepository.findAll().stream().filter(c -> c.getLoan() != null && c.getLoan().getId().equals(loanId)).toList();
            double totalCollected = cols.stream().mapToDouble(c -> c.getCollectedAmount() != null ? c.getCollectedAmount().doubleValue() : 0).sum();
            
            Row row7 = sheet.createRow(7);
            row7.createCell(0).setCellValue("Amount Collected Till Today:");
            row7.createCell(1).setCellValue(totalCollected);
            
            Row row9 = sheet.createRow(9);
            row9.createCell(0).setCellValue("Loan Status:");
            row9.createCell(1).setCellValue(loan.getLoanStatus().name());
            
            Row header = sheet.createRow(11);
            String[] colsHeader = {"Date", "Opening Balance", "Today EDI/EWI/EMI", "Credit", "Debit", "Remaining Balance"};
            for (int i=0; i<colsHeader.length; i++) header.createCell(i).setCellValue(colsHeader[i]);
            
            java.time.LocalDate start = loan.getDisbursementDate() != null ? loan.getDisbursementDate().toLocalDate() : java.time.LocalDate.now();
            java.time.LocalDate end = java.time.LocalDate.now();
            if (loan.getLoanStatus() == com.dapfintech.loan.enums.LoanStatus.CLOSED) {
                java.time.LocalDate maxCol = cols.stream().filter(c -> c.getCollectionDate() != null).map(c -> c.getCollectionDate().toLocalDate()).max(java.time.LocalDate::compareTo).orElse(start);
                end = maxCol;
            }
            
            double openingBalance = loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0.0;
            java.util.Map<java.time.LocalDate, Double> dailyCols = new java.util.HashMap<>();
            for (com.dapfintech.loan.entity.LoanCollection c : cols) {
                if (c.getCollectedAmount() == null || c.getCollectionDate() == null) continue;
                java.time.LocalDate d = c.getCollectionDate().toLocalDate();
                dailyCols.put(d, dailyCols.getOrDefault(d, 0.0) + c.getCollectedAmount().doubleValue());
            }
            
            List<com.dapfintech.loan.entity.LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
            java.util.Map<java.time.LocalDate, Double> dailyEdi = new java.util.HashMap<>();
            for (com.dapfintech.loan.entity.LoanRepaymentSchedule s : schedules) {
                if (s.getDueDate() != null && s.getInstallmentAmount() != null) {
                    dailyEdi.put(s.getDueDate(), dailyEdi.getOrDefault(s.getDueDate(), 0.0) + s.getInstallmentAmount().doubleValue());
                }
            }
            
            java.time.LocalDate curr = start;
            int r = 12;
            double finalRemaining = openingBalance;
            while (!curr.isAfter(end)) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(curr.toString());
                row.createCell(1).setCellValue(openingBalance);
                
                double todayEdi = 0.0;
                if (loan.getLoanType() == com.dapfintech.loan.enums.LoanType.EMERGENCY) {
                    todayEdi = (loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0) * (loan.getInterestRate() != null ? loan.getInterestRate().doubleValue() : 0) / 100.0;
                } else {
                    todayEdi = dailyEdi.getOrDefault(curr, 0.0);
                }
                row.createCell(2).setCellValue(todayEdi);
                
                double credit = dailyCols.getOrDefault(curr, 0.0);
                row.createCell(3).setCellValue(credit);
                
                double debit = 0.0;
                row.createCell(4).setCellValue(debit);
                
                double remaining = openingBalance + todayEdi + debit - credit;
                row.createCell(5).setCellValue(remaining);
                
                openingBalance = remaining;
                finalRemaining = remaining;
                curr = curr.plusDays(1);
            }
            
            Row row8 = sheet.createRow(8);
            row8.createCell(0).setCellValue("Amount to be Received Till Today:");
            row8.createCell(1).setCellValue(finalRemaining);
            
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ledger excel", e);
        }
    }

    @Override
    public ByteArrayInputStream generateLedgerReportPdf(UUID loanId) {
        com.dapfintech.loan.entity.Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();
            
            document.add(new com.lowagie.text.Paragraph("Customer Loan Report", com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16)));
            document.add(new com.lowagie.text.Paragraph("Market Name: " + (loan.getCustomer().getMarket() != null ? loan.getCustomer().getMarket().getMarketName() : "")));
            document.add(new com.lowagie.text.Paragraph("Customer Name: " + getFullName(loan.getCustomer())));
            document.add(new com.lowagie.text.Paragraph("Loan ID: " + loan.getLoanCode()));
            document.add(new com.lowagie.text.Paragraph("Loan Amount: " + (loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0.0)));
            document.add(new com.lowagie.text.Paragraph("Interest: " + (loan.getInterestRate() != null ? loan.getInterestRate().toString() + "%" : "")));
            
            String tenureStr = "";
            if (loan.getLoanType() == com.dapfintech.loan.enums.LoanType.EMERGENCY) {
                double val = (loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0) * (loan.getInterestRate() != null ? loan.getInterestRate().doubleValue() : 0) / 100.0;
                tenureStr = val + " per day";
            } else {
                if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EDI) tenureStr = "daily";
                else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EWI) tenureStr = "weekly";
                else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EMI) tenureStr = "monthly";
            }
            document.add(new com.lowagie.text.Paragraph("Tenure: " + tenureStr));
            
            List<com.dapfintech.loan.entity.LoanCollection> cols = collectionRepository.findAll().stream().filter(c -> c.getLoan() != null && c.getLoan().getId().equals(loanId)).toList();
            double totalCollected = cols.stream().mapToDouble(c -> c.getCollectedAmount() != null ? c.getCollectedAmount().doubleValue() : 0).sum();
            document.add(new com.lowagie.text.Paragraph("Amount Collected Till Today: " + totalCollected));
            document.add(new com.lowagie.text.Paragraph("Loan Status: " + loan.getLoanStatus().name()));
            document.add(new com.lowagie.text.Paragraph(" "));
            
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(6);
            table.setWidthPercentage(100);
            String[] colsHeader = {"Date", "Opening Balance", "Today EDI/EWI/EMI", "Credit", "Debit", "Remaining Balance"};
            for (String h : colsHeader) table.addCell(new com.lowagie.text.Phrase(h, com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 10)));
            
            java.time.LocalDate start = loan.getDisbursementDate() != null ? loan.getDisbursementDate().toLocalDate() : java.time.LocalDate.now();
            java.time.LocalDate end = java.time.LocalDate.now();
            if (loan.getLoanStatus() == com.dapfintech.loan.enums.LoanStatus.CLOSED) {
                java.time.LocalDate maxCol = cols.stream().filter(c -> c.getCollectionDate() != null).map(c -> c.getCollectionDate().toLocalDate()).max(java.time.LocalDate::compareTo).orElse(start);
                end = maxCol;
            }
            
            double openingBalance = loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0.0;
            java.util.Map<java.time.LocalDate, Double> dailyCols = new java.util.HashMap<>();
            for (com.dapfintech.loan.entity.LoanCollection c : cols) {
                if (c.getCollectedAmount() == null || c.getCollectionDate() == null) continue;
                java.time.LocalDate d = c.getCollectionDate().toLocalDate();
                dailyCols.put(d, dailyCols.getOrDefault(d, 0.0) + c.getCollectedAmount().doubleValue());
            }
            
            List<com.dapfintech.loan.entity.LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
            java.util.Map<java.time.LocalDate, Double> dailyEdi = new java.util.HashMap<>();
            for (com.dapfintech.loan.entity.LoanRepaymentSchedule s : schedules) {
                if (s.getDueDate() != null && s.getInstallmentAmount() != null) {
                    dailyEdi.put(s.getDueDate(), dailyEdi.getOrDefault(s.getDueDate(), 0.0) + s.getInstallmentAmount().doubleValue());
                }
            }
            
            java.time.LocalDate curr = start;
            double finalRemaining = openingBalance;
            com.lowagie.text.Font font = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 9);
            while (!curr.isAfter(end)) {
                table.addCell(new com.lowagie.text.Phrase(curr.toString(), font));
                table.addCell(new com.lowagie.text.Phrase(String.valueOf(openingBalance), font));
                
                double todayEdi = 0.0;
                if (loan.getLoanType() == com.dapfintech.loan.enums.LoanType.EMERGENCY) {
                    todayEdi = (loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0) * (loan.getInterestRate() != null ? loan.getInterestRate().doubleValue() : 0) / 100.0;
                } else {
                    todayEdi = dailyEdi.getOrDefault(curr, 0.0);
                }
                table.addCell(new com.lowagie.text.Phrase(String.valueOf(todayEdi), font));
                
                double credit = dailyCols.getOrDefault(curr, 0.0);
                table.addCell(new com.lowagie.text.Phrase(String.valueOf(credit), font));
                
                double debit = 0.0;
                table.addCell(new com.lowagie.text.Phrase(String.valueOf(debit), font));
                
                double remaining = openingBalance + todayEdi + debit - credit;
                table.addCell(new com.lowagie.text.Phrase(String.valueOf(remaining), font));
                
                openingBalance = remaining;
                finalRemaining = remaining;
                curr = curr.plusDays(1);
            }
            
            document.add(table);
            
            document.add(new com.lowagie.text.Paragraph("Amount to be Received Till Today: " + finalRemaining));
            
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
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 18);
            com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("Employee Daybook Ledger", titleFont);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(title);
            document.add(new com.lowagie.text.Paragraph(" "));

            User emp = userRepository.findById(employeeId).orElse(null);
            String empName = emp != null ? (emp.getFullName()) : employeeId.toString();

            document.add(new com.lowagie.text.Paragraph("Employee Name: " + empName));
            document.add(new com.lowagie.text.Paragraph("Date: " + date.toString()));
            document.add(new com.lowagie.text.Paragraph(" "));

            DayBook db = dayBookRepository.findByEmployeeIdAndDate(employeeId, date).orElse(null);
            if (db == null) {
                document.add(new com.lowagie.text.Paragraph("No Daybook data found for this date."));
            } else {
                com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(2);
                table.setWidthPercentage(100);
                com.lowagie.text.Font headFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 12);
                table.addCell(new com.lowagie.text.Phrase("Description", headFont));
                table.addCell(new com.lowagie.text.Phrase("Amount", headFont));

                table.addCell("Opening Balance"); table.addCell(String.valueOf(db.getOpeningBalance()));
                table.addCell("Cash Collections"); table.addCell(String.valueOf(db.getCollections()));
                table.addCell("Transfers Received"); table.addCell(String.valueOf(db.getIncomingTransfers()));
                table.addCell("Total Spends"); table.addCell(String.valueOf(db.getSpends()));
                table.addCell("Loans Disbursed"); table.addCell(String.valueOf(db.getLoansDisbursed()));
                table.addCell("Transfers Sent"); table.addCell(String.valueOf(db.getOutgoingTransfers()));
                table.addCell("Office Remittance"); table.addCell(String.valueOf(db.getOfficeRemittance()));
                table.addCell("Closing Balance"); table.addCell(String.valueOf(db.getClosingBalance()));
                table.addCell("Status"); table.addCell(db.getStatus().name());
                document.add(table);
            }

            document.add(new com.lowagie.text.Paragraph(" "));
            document.add(new com.lowagie.text.Paragraph("Collection Transactions:", titleFont));
            document.add(new com.lowagie.text.Paragraph(" "));

            java.util.List<LoanCollection> cols = collectionRepository.findAll().stream()
                    .filter(c -> c.getCollectedBy() != null && c.getCollectedBy().getId().equals(employeeId) && c.getCollectionDate() != null && c.getCollectionDate().toLocalDate().equals(date))
                    .toList();

            if (cols.isEmpty()) {
                document.add(new com.lowagie.text.Paragraph("No collections recorded."));
            } else {
                com.lowagie.text.pdf.PdfPTable colTable = new com.lowagie.text.pdf.PdfPTable(4);
                colTable.setWidthPercentage(100);
                com.lowagie.text.Font headFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 10);
                colTable.addCell(new com.lowagie.text.Phrase("Time", headFont));
                colTable.addCell(new com.lowagie.text.Phrase("Customer", headFont));
                colTable.addCell(new com.lowagie.text.Phrase("Loan ID", headFont));
                colTable.addCell(new com.lowagie.text.Phrase("Amount", headFont));

                java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
                for (LoanCollection c : cols) {
                    colTable.addCell(c.getCollectionDate().format(timeFormatter));
                    colTable.addCell(c.getLoan() != null && c.getLoan().getCustomer() != null ? getFullName(c.getLoan().getCustomer()) : "");
                    colTable.addCell(c.getLoan() != null ? c.getLoan().getLoanCode() : "");
                    colTable.addCell(String.valueOf(c.getCollectedAmount()));
                }
                document.add(colTable);
            }

            document.close();
            return new java.io.ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate daybook pdf", e);
        }
    }
    @Override
    public ByteArrayInputStream generateEmployeeDaybookExcel(UUID employeeId, LocalDate date) {
        try (Workbook workbook = new XSSFWorkbook(); java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Employee Daybook");
            
            User emp = userRepository.findById(employeeId).orElse(null);
            String empName = emp != null ? (emp.getFullName()) : employeeId.toString();
            
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("Employee Name:");
            row0.createCell(1).setCellValue(empName);
            
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Date:");
            row1.createCell(1).setCellValue(date.toString());
            
            DayBook db = dayBookRepository.findByEmployeeIdAndDate(employeeId, date).orElse(null);
            if (db != null) {
                sheet.createRow(3).createCell(0).setCellValue("Opening Balance:"); sheet.getRow(3).createCell(1).setCellValue(db.getOpeningBalance() != null ? db.getOpeningBalance().doubleValue() : 0.0);
                sheet.createRow(4).createCell(0).setCellValue("Cash Collections:"); sheet.getRow(4).createCell(1).setCellValue(db.getCollections() != null ? db.getCollections().doubleValue() : 0.0);
                sheet.createRow(5).createCell(0).setCellValue("Transfers Received:"); sheet.getRow(5).createCell(1).setCellValue(db.getIncomingTransfers() != null ? db.getIncomingTransfers().doubleValue() : 0.0);
                sheet.createRow(6).createCell(0).setCellValue("Total Spends:"); sheet.getRow(6).createCell(1).setCellValue(db.getSpends() != null ? db.getSpends().doubleValue() : 0.0);
                sheet.createRow(7).createCell(0).setCellValue("Loans Disbursed:"); sheet.getRow(7).createCell(1).setCellValue(db.getLoansDisbursed() != null ? db.getLoansDisbursed().doubleValue() : 0.0);
                sheet.createRow(8).createCell(0).setCellValue("Transfers Sent:"); sheet.getRow(8).createCell(1).setCellValue(db.getOutgoingTransfers() != null ? db.getOutgoingTransfers().doubleValue() : 0.0);
                sheet.createRow(9).createCell(0).setCellValue("Office Remittance:"); sheet.getRow(9).createCell(1).setCellValue(db.getOfficeRemittance() != null ? db.getOfficeRemittance().doubleValue() : 0.0);
                sheet.createRow(10).createCell(0).setCellValue("Closing Balance:"); sheet.getRow(10).createCell(1).setCellValue(db.getClosingBalance() != null ? db.getClosingBalance().doubleValue() : 0.0);
                sheet.createRow(11).createCell(0).setCellValue("Status:"); sheet.getRow(11).createCell(1).setCellValue(db.getStatus().name());
            }
            
            Row row13 = sheet.createRow(13);
            row13.createCell(0).setCellValue("Collection Transactions:");
            
            Row header = sheet.createRow(14);
            header.createCell(0).setCellValue("Time");
            header.createCell(1).setCellValue("Customer");
            header.createCell(2).setCellValue("Loan ID");
            header.createCell(3).setCellValue("Amount");
            
            java.util.List<LoanCollection> cols = collectionRepository.findAll().stream()
                    .filter(c -> c.getCollectedBy() != null && c.getCollectedBy().getId().equals(employeeId) && c.getCollectionDate() != null && c.getCollectionDate().toLocalDate().equals(date))
                    .toList();
            
            int r = 15;
            java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
            for (LoanCollection c : cols) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(c.getCollectionDate().format(timeFormatter));
                row.createCell(1).setCellValue(c.getLoan() != null && c.getLoan().getCustomer() != null ? getFullName(c.getLoan().getCustomer()) : "");
                row.createCell(2).setCellValue(c.getLoan() != null ? c.getLoan().getLoanCode() : "");
                row.createCell(3).setCellValue(c.getCollectedAmount() != null ? c.getCollectedAmount().doubleValue() : 0.0);
            }
            
            workbook.write(out);
            return new java.io.ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate daybook excel", e);
        }
    }
}
