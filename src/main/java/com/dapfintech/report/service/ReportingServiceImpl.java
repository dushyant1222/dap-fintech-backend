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
import com.dapfintech.report.dto.LedgerEntryDto;
import com.dapfintech.loan.enums.LoanType;
import com.dapfintech.loan.enums.RepaymentFrequency;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        java.time.LocalDate today = java.time.LocalDate.now();

        try (Workbook workbook = new XSSFWorkbook(); java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Collection Report");
            
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Collection Report");
            titleCell.setCellStyle(titleStyle);
            
            String marketName = "All Markets";
            if (marketId != null && !loans.isEmpty() && loans.get(0).getCustomer().getMarket() != null) {
                marketName = loans.get(0).getCustomer().getMarket().getMarketName();
            }
            Row subRow = sheet.createRow(1);
            subRow.createCell(0).setCellValue("Market Name: " + marketName);
            
            // Header styling
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Yellow advance cell style
            CellStyle yellowStyle = workbook.createCellStyle();
            yellowStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
            yellowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            yellowStyle.setAlignment(HorizontalAlignment.CENTER);
            yellowStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            org.apache.poi.ss.usermodel.Font yellowFont = workbook.createFont();
            yellowFont.setBold(true);
            yellowFont.setColor(IndexedColors.BLACK.getIndex());
            yellowStyle.setFont(yellowFont);

            // Center style for cross / normal days
            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Row headerRow = sheet.createRow(3);
            String[] baseCols = {
                "Loan Number",
                "Customer Name",
                "Daily EMI",
                "Tenure",
                "Total Amount to be Paid",
                "Balance Required Till Today",
                "Gap Till Today",
                "Loan Issue Date",
                "Loan Close Date",
                "Received Amount",
                "Remaining Balance",
                "Status"
            };

            int colIdx = 0;
            for (String col : baseCols) {
                Cell cell = headerRow.createCell(colIdx++);
                cell.setCellValue(col);
                cell.setCellStyle(headerStyle);
            }
            
            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd-MMM");
            for (java.time.LocalDate d : dateColumns) {
                Cell cell = headerRow.createCell(colIdx++);
                cell.setCellValue("Date-" + d.format(dtf));
                cell.setCellStyle(headerStyle);
            }
            
            int rowIdx = 4;
            
            for (com.dapfintech.loan.entity.Loan loan : loans) {
                List<com.dapfintech.loan.entity.LoanRepaymentSchedule> schedules =
                        scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId());

                double dailyEmi = 0.0;
                if (!schedules.isEmpty() && schedules.get(0).getInstallmentAmount() != null) {
                    dailyEmi = schedules.get(0).getInstallmentAmount().doubleValue();
                } else if (loan.getApprovedAmount() != null && loan.getTenure() != null && loan.getTenure() > 0) {
                    dailyEmi = loan.getApprovedAmount().doubleValue() / loan.getTenure();
                }

                String tenure = "";
                if (loan.getTenure() != null) {
                    if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EDI) tenure = loan.getTenure() + " Days";
                    else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EWI) tenure = loan.getTenure() + " Weeks";
                    else tenure = loan.getTenure() + " Months";
                }

                double totalAmountToBePaid = schedules.stream()
                        .mapToDouble(s -> s.getInstallmentAmount() != null ? s.getInstallmentAmount().doubleValue() : 0.0)
                        .sum();
                if (totalAmountToBePaid <= 0 && loan.getLoanAmount() != null) {
                    totalAmountToBePaid = loan.getLoanAmount().doubleValue() + (loan.getInterestRate() != null ? loan.getLoanAmount().doubleValue() * loan.getInterestRate().doubleValue() / 100.0 : 0.0);
                }

                double balanceRequiredTillToday = schedules.stream()
                        .filter(s -> s.getDueDate() != null && !s.getDueDate().isAfter(today))
                        .mapToDouble(s -> s.getInstallmentAmount() != null ? s.getInstallmentAmount().doubleValue() : 0.0)
                        .sum();

                double receivedAmount = 0.0;
                java.util.Map<java.time.LocalDate, Double> dailyMap = new java.util.HashMap<>();
                for (com.dapfintech.loan.entity.LoanCollection lc : allCollections) {
                    if (lc.getLoan() != null && lc.getLoan().getId().equals(loan.getId()) && lc.getCollectedAmount() != null) {
                        receivedAmount += lc.getCollectedAmount().doubleValue();
                        if (lc.getCollectionDate() != null) {
                            java.time.LocalDate cd = lc.getCollectionDate().toLocalDate();
                            dailyMap.put(cd, dailyMap.getOrDefault(cd, 0.0) + lc.getCollectedAmount().doubleValue());
                        }
                    }
                }

                double gapTillToday = Math.max(0.0, balanceRequiredTillToday - receivedAmount);
                String issueDateStr = loan.getDisbursementDate() != null ? loan.getDisbursementDate().toLocalDate().toString() : "-";
                
                String closeDateStr = "-";
                if (!schedules.isEmpty() && schedules.get(schedules.size() - 1).getDueDate() != null) {
                    closeDateStr = schedules.get(schedules.size() - 1).getDueDate().toString();
                } else if (loan.getDisbursementDate() != null && loan.getDurationInDays() != null) {
                    closeDateStr = loan.getDisbursementDate().toLocalDate().plusDays(loan.getDurationInDays()).toString();
                }

                double remainingBalance = Math.max(0.0, totalAmountToBePaid - receivedAmount);
                String status = loan.getLoanStatus() != null ? loan.getLoanStatus().name() : "ACTIVE";

                Row row = sheet.createRow(rowIdx++);
                int c = 0;
                row.createCell(c++).setCellValue(loan.getLoanCode() != null ? loan.getLoanCode() : "");
                row.createCell(c++).setCellValue(getFullName(loan.getCustomer()));
                row.createCell(c++).setCellValue(dailyEmi);
                row.createCell(c++).setCellValue(tenure);
                row.createCell(c++).setCellValue(totalAmountToBePaid);
                row.createCell(c++).setCellValue(balanceRequiredTillToday);
                row.createCell(c++).setCellValue(gapTillToday);
                row.createCell(c++).setCellValue(issueDateStr);
                row.createCell(c++).setCellValue(closeDateStr);
                row.createCell(c++).setCellValue(receivedAmount);
                row.createCell(c++).setCellValue(remainingBalance);
                row.createCell(c++).setCellValue(status);

                // Advance simulation across chronological dates
                java.time.LocalDate issueDate = loan.getDisbursementDate() != null ? loan.getDisbursementDate().toLocalDate() : null;
                double advanceBalance = 0.0;

                for (java.time.LocalDate d : dateColumns) {
                    Cell cell = row.createCell(c++);
                    if (issueDate != null && d.isBefore(issueDate)) {
                        cell.setCellValue("-");
                        cell.setCellStyle(centerStyle);
                    } else {
                        double collectedToday = dailyMap.getOrDefault(d, 0.0);
                        if (collectedToday > 0) {
                            if (dailyEmi > 0 && collectedToday > dailyEmi) {
                                advanceBalance += (collectedToday - dailyEmi);
                            } else if (dailyEmi > 0 && collectedToday < dailyEmi) {
                                double shortage = dailyEmi - collectedToday;
                                advanceBalance = Math.max(0.0, advanceBalance - shortage);
                            }
                            if (collectedToday == Math.floor(collectedToday)) {
                                cell.setCellValue((long) collectedToday);
                            } else {
                                cell.setCellValue(collectedToday);
                            }
                            cell.setCellStyle(centerStyle);
                        } else {
                            if (dailyEmi > 0 && advanceBalance >= dailyEmi) {
                                cell.setCellValue("AD");
                                cell.setCellStyle(yellowStyle);
                                advanceBalance -= dailyEmi;
                            } else {
                                cell.setCellValue("X");
                                cell.setCellStyle(centerStyle);
                                advanceBalance = 0.0;
                            }
                        }
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
        com.lowagie.text.Document document = new com.lowagie.text.Document(new com.lowagie.text.Rectangle(3200, 842));
        try {
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();
            
            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16);
            document.add(new com.lowagie.text.Paragraph("Collection Report", titleFont));
            
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

            String[] baseCols = {
                "Loan Number",
                "Customer Name",
                "Daily EMI",
                "Tenure",
                "Total Amount to be Paid",
                "Balance Required Till Today",
                "Gap Till Today",
                "Loan Issue Date",
                "Loan Close Date",
                "Received Amount",
                "Remaining Balance",
                "Status"
            };

            int numColumns = baseCols.length + dateColumns.size();
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(numColumns);
            table.setWidthPercentage(100);
            
            com.lowagie.text.Font headFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 8);
            for (String col : baseCols) {
                com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(col, headFont));
                cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                cell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
                cell.setBackgroundColor(new java.awt.Color(230, 230, 230));
                cell.setPadding(4);
                table.addCell(cell);
            }
            
            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd-MMM");
            for (java.time.LocalDate d : dateColumns) {
                com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("Date-" + d.format(dtf), headFont));
                cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                cell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
                cell.setBackgroundColor(new java.awt.Color(230, 230, 230));
                cell.setPadding(4);
                table.addCell(cell);
            }

            List<com.dapfintech.loan.entity.LoanCollection> allCollections = collectionRepository.findAll();
            java.time.LocalDate today = java.time.LocalDate.now();
            
            com.lowagie.text.Font font = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 8);
            com.lowagie.text.Font adFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 8, java.awt.Color.BLACK);
            java.awt.Color yellowColor = new java.awt.Color(255, 235, 59);
            
            for (com.dapfintech.loan.entity.Loan loan : loans) {
                List<com.dapfintech.loan.entity.LoanRepaymentSchedule> schedules =
                        scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId());

                double dailyEmi = 0.0;
                if (!schedules.isEmpty() && schedules.get(0).getInstallmentAmount() != null) {
                    dailyEmi = schedules.get(0).getInstallmentAmount().doubleValue();
                } else if (loan.getApprovedAmount() != null && loan.getTenure() != null && loan.getTenure() > 0) {
                    dailyEmi = loan.getApprovedAmount().doubleValue() / loan.getTenure();
                }

                String tenure = "";
                if (loan.getTenure() != null) {
                    if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EDI) tenure = loan.getTenure() + " Days";
                    else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EWI) tenure = loan.getTenure() + " Weeks";
                    else tenure = loan.getTenure() + " Months";
                }

                double totalAmountToBePaid = schedules.stream()
                        .mapToDouble(s -> s.getInstallmentAmount() != null ? s.getInstallmentAmount().doubleValue() : 0.0)
                        .sum();
                if (totalAmountToBePaid <= 0 && loan.getLoanAmount() != null) {
                    totalAmountToBePaid = loan.getLoanAmount().doubleValue() + (loan.getInterestRate() != null ? loan.getLoanAmount().doubleValue() * loan.getInterestRate().doubleValue() / 100.0 : 0.0);
                }

                double balanceRequiredTillToday = schedules.stream()
                        .filter(s -> s.getDueDate() != null && !s.getDueDate().isAfter(today))
                        .mapToDouble(s -> s.getInstallmentAmount() != null ? s.getInstallmentAmount().doubleValue() : 0.0)
                        .sum();

                double receivedAmount = 0.0;
                java.util.Map<java.time.LocalDate, Double> dailyMap = new java.util.HashMap<>();
                for (com.dapfintech.loan.entity.LoanCollection lc : allCollections) {
                    if (lc.getLoan() != null && lc.getLoan().getId().equals(loan.getId()) && lc.getCollectedAmount() != null) {
                        receivedAmount += lc.getCollectedAmount().doubleValue();
                        if (lc.getCollectionDate() != null) {
                            java.time.LocalDate cd = lc.getCollectionDate().toLocalDate();
                            dailyMap.put(cd, dailyMap.getOrDefault(cd, 0.0) + lc.getCollectedAmount().doubleValue());
                        }
                    }
                }

                double gapTillToday = Math.max(0.0, balanceRequiredTillToday - receivedAmount);
                String issueDateStr = loan.getDisbursementDate() != null ? loan.getDisbursementDate().toLocalDate().toString() : "-";
                
                String closeDateStr = "-";
                if (!schedules.isEmpty() && schedules.get(schedules.size() - 1).getDueDate() != null) {
                    closeDateStr = schedules.get(schedules.size() - 1).getDueDate().toString();
                } else if (loan.getDisbursementDate() != null && loan.getDurationInDays() != null) {
                    closeDateStr = loan.getDisbursementDate().toLocalDate().plusDays(loan.getDurationInDays()).toString();
                }

                double remainingBalance = Math.max(0.0, totalAmountToBePaid - receivedAmount);
                String status = loan.getLoanStatus() != null ? loan.getLoanStatus().name() : "ACTIVE";

                table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(loan.getLoanCode() != null ? loan.getLoanCode() : "", font)));
                table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(getFullName(loan.getCustomer()), font)));
                table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(String.format("%.2f", dailyEmi), font)));
                table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(tenure, font)));
                table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(String.format("%.2f", totalAmountToBePaid), font)));
                table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(String.format("%.2f", balanceRequiredTillToday), font)));
                table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(String.format("%.2f", gapTillToday), font)));
                table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(issueDateStr, font)));
                table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(closeDateStr, font)));
                table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(String.format("%.2f", receivedAmount), font)));
                table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(String.format("%.2f", remainingBalance), font)));
                table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(status, font)));

                // Advance simulation across chronological dates
                java.time.LocalDate issueDate = loan.getDisbursementDate() != null ? loan.getDisbursementDate().toLocalDate() : null;
                double advanceBalance = 0.0;

                for (java.time.LocalDate d : dateColumns) {
                    if (issueDate != null && d.isBefore(issueDate)) {
                        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("-", font));
                        cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                        table.addCell(cell);
                    } else {
                        double collectedToday = dailyMap.getOrDefault(d, 0.0);
                        if (collectedToday > 0) {
                            if (dailyEmi > 0 && collectedToday > dailyEmi) {
                                advanceBalance += (collectedToday - dailyEmi);
                            } else if (dailyEmi > 0 && collectedToday < dailyEmi) {
                                double shortage = dailyEmi - collectedToday;
                                advanceBalance = Math.max(0.0, advanceBalance - shortage);
                            }
                            String valStr = (collectedToday == Math.floor(collectedToday))
                                    ? String.valueOf((long) collectedToday)
                                    : String.format("%.2f", collectedToday);
                            com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(valStr, font));
                            cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                            table.addCell(cell);
                        } else {
                            if (dailyEmi > 0 && advanceBalance >= dailyEmi) {
                                com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("AD", adFont));
                                cell.setBackgroundColor(yellowColor);
                                cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                                cell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
                                table.addCell(cell);
                                advanceBalance -= dailyEmi;
                            } else {
                                com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("X", font));
                                cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                                table.addCell(cell);
                                advanceBalance = 0.0;
                            }
                        }
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

            // Fonts & Styles
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            org.apache.poi.ss.usermodel.Font labelFont = workbook.createFont();
            labelFont.setBold(true);
            labelFont.setFontHeightInPoints((short) 10);

            CellStyle labelStyle = workbook.createCellStyle();
            labelStyle.setFont(labelFont);

            org.apache.poi.ss.usermodel.Font valueFont = workbook.createFont();
            valueFont.setFontHeightInPoints((short) 10);

            CellStyle valueStyle = workbook.createCellStyle();
            valueStyle.setFont(valueFont);

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headFont = workbook.createFont();
            headFont.setBold(true);
            headFont.setColor(IndexedColors.WHITE.getIndex());
            headFont.setFontHeightInPoints((short) 10);
            headerStyle.setFont(headFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setFont(valueFont);
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle numStyle = workbook.createCellStyle();
            numStyle.setFont(valueFont);
            numStyle.setAlignment(HorizontalAlignment.RIGHT);
            numStyle.setBorderTop(BorderStyle.THIN);
            numStyle.setBorderBottom(BorderStyle.THIN);
            numStyle.setBorderLeft(BorderStyle.THIN);
            numStyle.setBorderRight(BorderStyle.THIN);

            // Row 0: Centered Title
            Row row0 = sheet.createRow(0);
            Cell titleCell = row0.createCell(0);
            titleCell.setCellValue("Customer Loan Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));

            // Data Preparation
            String loanNumber = loan.getLoanCode() != null ? loan.getLoanCode() : "";
            String customerName = getFullName(loan.getCustomer());
            String marketName = (loan.getCustomer() != null && loan.getCustomer().getMarket() != null) ? loan.getCustomer().getMarket().getMarketName() : "";
            
            double disbursedAmount = loan.getDisbursedAmount() != null ? loan.getDisbursedAmount().doubleValue() : (loan.getApprovedAmount() != null ? loan.getApprovedAmount().doubleValue() : (loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0.0));
            String interestRateStr = loan.getInterestRate() != null ? loan.getInterestRate().toString() + "%" : "-";

            String loanTypeStr = "";
            if (loan.getLoanType() == com.dapfintech.loan.enums.LoanType.EMERGENCY) {
                loanTypeStr = "EMERGENCY";
            } else if (loan.getRepaymentFrequency() != null) {
                loanTypeStr = loan.getRepaymentFrequency().name();
            } else if (loan.getLoanType() != null) {
                loanTypeStr = loan.getLoanType().name();
            }

            String tenureStr = "";
            if (loan.getLoanType() == com.dapfintech.loan.enums.LoanType.EMERGENCY) {
                double val = disbursedAmount * (loan.getInterestRate() != null ? loan.getInterestRate().doubleValue() : 0) / 100.0;
                tenureStr = (loan.getTenure() != null ? loan.getTenure() + " Days (" : "") + String.format("%.2f/day", val) + (loan.getTenure() != null ? ")" : "");
            } else {
                if (loan.getTenure() != null) {
                    if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EDI) tenureStr = loan.getTenure() + " Days";
                    else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EWI) tenureStr = loan.getTenure() + " Weeks";
                    else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EMI) tenureStr = loan.getTenure() + " Months";
                    else tenureStr = loan.getTenure().toString();
                } else {
                    tenureStr = "-";
                }
            }

            List<com.dapfintech.loan.entity.LoanCollection> cols = collectionRepository.findAll().stream()
                    .filter(c -> c.getLoan() != null && c.getLoan().getId().equals(loanId))
                    .toList();

            java.time.LocalDate today = java.time.LocalDate.now();
            double amountCollectedToday = cols.stream()
                    .filter(c -> c.getCollectionDate() != null && c.getCollectionDate().toLocalDate().isEqual(today))
                    .mapToDouble(c -> c.getCollectedAmount() != null ? c.getCollectedAmount().doubleValue() : 0.0)
                    .sum();

            List<com.dapfintech.loan.entity.LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
            double totalLoanAmount = schedules.stream()
                    .mapToDouble(s -> s.getInstallmentAmount() != null ? s.getInstallmentAmount().doubleValue() : 0.0)
                    .sum();
            if (totalLoanAmount <= 0) {
                double p = disbursedAmount;
                double r = loan.getInterestRate() != null ? loan.getInterestRate().doubleValue() : 0.0;
                totalLoanAmount = p + (p * r / 100.0);
            }

            String statusStr = loan.getLoanStatus() != null ? loan.getLoanStatus().name() : "ACTIVE";
            boolean isActive = !"CLOSED".equalsIgnoreCase(statusStr);

            // Row 2: (Row 1 of details: Left=Loan Number, Middle=Customer Name, Right=Market)
            Row row2 = sheet.createRow(2);
            Cell c2_0 = row2.createCell(0); c2_0.setCellValue("Loan Number:"); c2_0.setCellStyle(labelStyle);
            Cell c2_1 = row2.createCell(1); c2_1.setCellValue(loanNumber); c2_1.setCellStyle(valueStyle);
            Cell c2_2 = row2.createCell(2); c2_2.setCellValue("Customer Name:"); c2_2.setCellStyle(labelStyle);
            Cell c2_3 = row2.createCell(3); c2_3.setCellValue(customerName); c2_3.setCellStyle(valueStyle);
            Cell c2_4 = row2.createCell(4); c2_4.setCellValue("Market:"); c2_4.setCellStyle(labelStyle);
            Cell c2_5 = row2.createCell(5); c2_5.setCellValue(marketName); c2_5.setCellStyle(valueStyle);

            // Row 3: (Row 2 of details: Left=Total Loan Amount, Middle=Interest, Right=Disbursed Amount)
            Row row3 = sheet.createRow(3);
            Cell c3_0 = row3.createCell(0); c3_0.setCellValue("Total Loan Amount:"); c3_0.setCellStyle(labelStyle);
            Cell c3_1 = row3.createCell(1); c3_1.setCellValue(totalLoanAmount); c3_1.setCellStyle(valueStyle);
            Cell c3_2 = row3.createCell(2); c3_2.setCellValue("Interest:"); c3_2.setCellStyle(labelStyle);
            Cell c3_3 = row3.createCell(3); c3_3.setCellValue(interestRateStr); c3_3.setCellStyle(valueStyle);
            Cell c3_4 = row3.createCell(4); c3_4.setCellValue("Disbursed Amount:"); c3_4.setCellStyle(labelStyle);
            Cell c3_5 = row3.createCell(5); c3_5.setCellValue(disbursedAmount); c3_5.setCellStyle(valueStyle);

            // Row 4: (Row 3 of details: Left=Type, Middle=Tenure, Right=Amount Collected Today)
            Row row4 = sheet.createRow(4);
            Cell c4_0 = row4.createCell(0); c4_0.setCellValue("Type:"); c4_0.setCellStyle(labelStyle);
            Cell c4_1 = row4.createCell(1); c4_1.setCellValue(loanTypeStr); c4_1.setCellStyle(valueStyle);
            Cell c4_2 = row4.createCell(2); c4_2.setCellValue("Tenure:"); c4_2.setCellStyle(labelStyle);
            Cell c4_3 = row4.createCell(3); c4_3.setCellValue(tenureStr); c4_3.setCellStyle(valueStyle);
            Cell c4_4 = row4.createCell(4); c4_4.setCellValue("Amount Collected Today:"); c4_4.setCellStyle(labelStyle);
            Cell c4_5 = row4.createCell(5); c4_5.setCellValue(amountCollectedToday); c4_5.setCellStyle(valueStyle);

            // Row 5: (Row 4: Centered Bold Status: ACTIVE / CLOSED)
            Row row5 = sheet.createRow(5);
            Cell statusCell = row5.createCell(0);
            statusCell.setCellValue("Status: " + statusStr);
            CellStyle statusStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font statusFont = workbook.createFont();
            statusFont.setBold(true);
            statusFont.setFontHeightInPoints((short) 11);
            if (isActive) {
                statusFont.setColor(IndexedColors.GREEN.getIndex());
            } else {
                statusFont.setColor(IndexedColors.RED.getIndex());
            }
            statusStyle.setFont(statusFont);
            statusStyle.setAlignment(HorizontalAlignment.CENTER);
            statusStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            statusCell.setCellStyle(statusStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(5, 5, 0, 5));

            // Row 7: Table Headers
            Row headerRow = sheet.createRow(7);
            String[] colsHeader = {"Date", "Opening Balance", "Today EDI/EWI/EMI", "Credit", "Debit", "Remaining Balance"};
            for (int i = 0; i < colsHeader.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(colsHeader[i]);
                cell.setCellStyle(headerStyle);
            }

            // Date Range & Ledger Calculation
            java.time.LocalDate start = loan.getDisbursementDate() != null ? loan.getDisbursementDate().toLocalDate() : (loan.getCreatedAt() != null ? loan.getCreatedAt().toLocalDate() : today);
            java.time.LocalDate end = today;
            if (!isActive) {
                java.time.LocalDate maxCol = cols.stream().filter(c -> c.getCollectionDate() != null).map(c -> c.getCollectionDate().toLocalDate()).max(java.time.LocalDate::compareTo).orElse(start);
                if (!schedules.isEmpty() && schedules.get(schedules.size() - 1).getDueDate() != null) {
                    java.time.LocalDate maxSched = schedules.get(schedules.size() - 1).getDueDate();
                    end = maxCol.isAfter(maxSched) ? maxCol : maxSched;
                } else {
                    end = maxCol;
                }
                if (end.isBefore(start)) end = start;
            }

            java.util.Map<java.time.LocalDate, Double> dailyCols = new java.util.HashMap<>();
            for (com.dapfintech.loan.entity.LoanCollection c : cols) {
                if (c.getCollectedAmount() == null || c.getCollectionDate() == null) continue;
                java.time.LocalDate d = c.getCollectionDate().toLocalDate();
                dailyCols.put(d, dailyCols.getOrDefault(d, 0.0) + c.getCollectedAmount().doubleValue());
            }

            java.util.Map<java.time.LocalDate, Double> dailyEdi = new java.util.HashMap<>();
            for (com.dapfintech.loan.entity.LoanRepaymentSchedule s : schedules) {
                if (s.getDueDate() != null && s.getInstallmentAmount() != null) {
                    dailyEdi.put(s.getDueDate(), dailyEdi.getOrDefault(s.getDueDate(), 0.0) + s.getInstallmentAmount().doubleValue());
                }
            }

            double openingBalance = totalLoanAmount;
            java.time.LocalDate curr = start;
            int r = 8;
            while (!curr.isAfter(end)) {
                Row row = sheet.createRow(r++);
                
                Cell c0 = row.createCell(0); c0.setCellValue(curr.toString()); c0.setCellStyle(dataStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(openingBalance); c1.setCellStyle(numStyle);

                double todayEdi = 0.0;
                if (loan.getLoanType() == com.dapfintech.loan.enums.LoanType.EMERGENCY) {
                    todayEdi = disbursedAmount * (loan.getInterestRate() != null ? loan.getInterestRate().doubleValue() : 0) / 100.0;
                } else {
                    todayEdi = dailyEdi.getOrDefault(curr, 0.0);
                }
                Cell c2 = row.createCell(2); c2.setCellValue(todayEdi); c2.setCellStyle(numStyle);

                double credit = dailyCols.getOrDefault(curr, 0.0);
                Cell c3 = row.createCell(3); c3.setCellValue(credit); c3.setCellStyle(numStyle);

                double debit = 0.0;
                Cell c4 = row.createCell(4); c4.setCellValue(debit); c4.setCellStyle(numStyle);

                double remaining = Math.max(0.0, openingBalance - credit + debit);
                Cell c5 = row.createCell(5); c5.setCellValue(remaining); c5.setCellStyle(numStyle);

                openingBalance = remaining;
                curr = curr.plusDays(1);
            }

            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }

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
            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4, 25, 25, 25, 25);
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            // 1. Centered Title
            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16, new java.awt.Color(184, 134, 58));
            com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("Customer Loan Report", titleFont);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(title);
            document.add(new com.lowagie.text.Paragraph(" "));

            // Data Preparation
            String loanNumber = loan.getLoanCode() != null ? loan.getLoanCode() : "";
            String customerName = getFullName(loan.getCustomer());
            String marketName = (loan.getCustomer() != null && loan.getCustomer().getMarket() != null) ? loan.getCustomer().getMarket().getMarketName() : "";

            double disbursedAmount = loan.getDisbursedAmount() != null ? loan.getDisbursedAmount().doubleValue() : (loan.getApprovedAmount() != null ? loan.getApprovedAmount().doubleValue() : (loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0.0));
            String interestRateStr = loan.getInterestRate() != null ? loan.getInterestRate().toString() + "%" : "-";

            String loanTypeStr = "";
            if (loan.getLoanType() == com.dapfintech.loan.enums.LoanType.EMERGENCY) {
                loanTypeStr = "EMERGENCY";
            } else if (loan.getRepaymentFrequency() != null) {
                loanTypeStr = loan.getRepaymentFrequency().name();
            } else if (loan.getLoanType() != null) {
                loanTypeStr = loan.getLoanType().name();
            }

            String tenureStr = "";
            if (loan.getLoanType() == com.dapfintech.loan.enums.LoanType.EMERGENCY) {
                double val = disbursedAmount * (loan.getInterestRate() != null ? loan.getInterestRate().doubleValue() : 0) / 100.0;
                tenureStr = (loan.getTenure() != null ? loan.getTenure() + " Days (" : "") + String.format("%.2f/day", val) + (loan.getTenure() != null ? ")" : "");
            } else {
                if (loan.getTenure() != null) {
                    if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EDI) tenureStr = loan.getTenure() + " Days";
                    else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EWI) tenureStr = loan.getTenure() + " Weeks";
                    else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EMI) tenureStr = loan.getTenure() + " Months";
                    else tenureStr = loan.getTenure().toString();
                } else {
                    tenureStr = "-";
                }
            }

            List<com.dapfintech.loan.entity.LoanCollection> cols = collectionRepository.findAll().stream()
                    .filter(c -> c.getLoan() != null && c.getLoan().getId().equals(loanId))
                    .toList();

            java.time.LocalDate today = java.time.LocalDate.now();
            double amountCollectedToday = cols.stream()
                    .filter(c -> c.getCollectionDate() != null && c.getCollectionDate().toLocalDate().isEqual(today))
                    .mapToDouble(c -> c.getCollectedAmount() != null ? c.getCollectedAmount().doubleValue() : 0.0)
                    .sum();

            List<com.dapfintech.loan.entity.LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
            double totalLoanAmount = schedules.stream()
                    .mapToDouble(s -> s.getInstallmentAmount() != null ? s.getInstallmentAmount().doubleValue() : 0.0)
                    .sum();
            if (totalLoanAmount <= 0) {
                double p = disbursedAmount;
                double r = loan.getInterestRate() != null ? loan.getInterestRate().doubleValue() : 0.0;
                totalLoanAmount = p + (p * r / 100.0);
            }

            String statusStr = loan.getLoanStatus() != null ? loan.getLoanStatus().name() : "ACTIVE";
            boolean isActive = !"CLOSED".equalsIgnoreCase(statusStr);

            // 2. 3-Column Top Details Grid
            com.lowagie.text.pdf.PdfPTable topGrid = new com.lowagie.text.pdf.PdfPTable(3);
            topGrid.setWidthPercentage(100);
            topGrid.setWidths(new float[]{33f, 34f, 33f});

            com.lowagie.text.Font metaLabelFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 9, java.awt.Color.DARK_GRAY);
            com.lowagie.text.Font metaValFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 9, java.awt.Color.BLACK);

            // Row 1: Left=Loan Number, Middle=Customer Name, Right=Market
            addMetaPdfCell(topGrid, "Loan Number", loanNumber, metaLabelFont, metaValFont);
            addMetaPdfCell(topGrid, "Customer Name", customerName, metaLabelFont, metaValFont);
            addMetaPdfCell(topGrid, "Market", marketName, metaLabelFont, metaValFont);

            // Row 2: Left=Total Loan Amount, Middle=Interest, Right=Disbursed Amount
            addMetaPdfCell(topGrid, "Total Loan Amount", String.format("%.2f", totalLoanAmount), metaLabelFont, metaValFont);
            addMetaPdfCell(topGrid, "Interest", interestRateStr, metaLabelFont, metaValFont);
            addMetaPdfCell(topGrid, "Disbursed Amount", String.format("%.2f", disbursedAmount), metaLabelFont, metaValFont);

            // Row 3: Left=Type, Middle=Tenure, Right=Amount Collected Today
            addMetaPdfCell(topGrid, "Type", loanTypeStr, metaLabelFont, metaValFont);
            addMetaPdfCell(topGrid, "Tenure", tenureStr, metaLabelFont, metaValFont);
            addMetaPdfCell(topGrid, "Amount Collected Today", String.format("%.2f", amountCollectedToday), metaLabelFont, metaValFont);

            document.add(topGrid);
            document.add(new com.lowagie.text.Paragraph(" "));

            // 3. Row 4: Centered Bold Status
            java.awt.Color statusColor = isActive ? new java.awt.Color(46, 125, 50) : new java.awt.Color(198, 40, 40);
            com.lowagie.text.Font statusFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 12, statusColor);
            com.lowagie.text.Paragraph statusPara = new com.lowagie.text.Paragraph("Status: " + statusStr, statusFont);
            statusPara.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(statusPara);
            document.add(new com.lowagie.text.Paragraph(" "));

            // 4. Ledger Table
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{16f, 18f, 18f, 16f, 14f, 18f});

            com.lowagie.text.Font headFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 9, java.awt.Color.WHITE);
            java.awt.Color headerBg = new java.awt.Color(184, 134, 58);

            String[] colsHeader = {"Date", "Opening Balance", "Today EDI/EWI/EMI", "Credit", "Debit", "Remaining Balance"};
            for (String h : colsHeader) {
                com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(h, headFont));
                cell.setBackgroundColor(headerBg);
                cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                cell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
                cell.setPadding(5f);
                table.addCell(cell);
            }

            java.time.LocalDate start = loan.getDisbursementDate() != null ? loan.getDisbursementDate().toLocalDate() : (loan.getCreatedAt() != null ? loan.getCreatedAt().toLocalDate() : today);
            java.time.LocalDate end = today;
            if (!isActive) {
                java.time.LocalDate maxCol = cols.stream().filter(c -> c.getCollectionDate() != null).map(c -> c.getCollectionDate().toLocalDate()).max(java.time.LocalDate::compareTo).orElse(start);
                if (!schedules.isEmpty() && schedules.get(schedules.size() - 1).getDueDate() != null) {
                    java.time.LocalDate maxSched = schedules.get(schedules.size() - 1).getDueDate();
                    end = maxCol.isAfter(maxSched) ? maxCol : maxSched;
                } else {
                    end = maxCol;
                }
                if (end.isBefore(start)) end = start;
            }

            java.util.Map<java.time.LocalDate, Double> dailyCols = new java.util.HashMap<>();
            for (com.dapfintech.loan.entity.LoanCollection c : cols) {
                if (c.getCollectedAmount() == null || c.getCollectionDate() == null) continue;
                java.time.LocalDate d = c.getCollectionDate().toLocalDate();
                dailyCols.put(d, dailyCols.getOrDefault(d, 0.0) + c.getCollectedAmount().doubleValue());
            }

            java.util.Map<java.time.LocalDate, Double> dailyEdi = new java.util.HashMap<>();
            for (com.dapfintech.loan.entity.LoanRepaymentSchedule s : schedules) {
                if (s.getDueDate() != null && s.getInstallmentAmount() != null) {
                    dailyEdi.put(s.getDueDate(), dailyEdi.getOrDefault(s.getDueDate(), 0.0) + s.getInstallmentAmount().doubleValue());
                }
            }

            double openingBalance = totalLoanAmount;
            java.time.LocalDate curr = start;
            com.lowagie.text.Font font = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 8);

            while (!curr.isAfter(end)) {
                com.lowagie.text.pdf.PdfPCell c0 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(curr.toString(), font));
                c0.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                c0.setPadding(4f);
                table.addCell(c0);

                com.lowagie.text.pdf.PdfPCell c1 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(String.format("%.2f", openingBalance), font));
                c1.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
                c1.setPadding(4f);
                table.addCell(c1);

                double todayEdi = 0.0;
                if (loan.getLoanType() == com.dapfintech.loan.enums.LoanType.EMERGENCY) {
                    todayEdi = disbursedAmount * (loan.getInterestRate() != null ? loan.getInterestRate().doubleValue() : 0) / 100.0;
                } else {
                    todayEdi = dailyEdi.getOrDefault(curr, 0.0);
                }
                com.lowagie.text.pdf.PdfPCell c2 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(String.format("%.2f", todayEdi), font));
                c2.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
                c2.setPadding(4f);
                table.addCell(c2);

                double credit = dailyCols.getOrDefault(curr, 0.0);
                com.lowagie.text.pdf.PdfPCell c3 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(String.format("%.2f", credit), font));
                c3.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
                c3.setPadding(4f);
                table.addCell(c3);

                double debit = 0.0;
                com.lowagie.text.pdf.PdfPCell c4 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(String.format("%.2f", debit), font));
                c4.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
                c4.setPadding(4f);
                table.addCell(c4);

                double remaining = Math.max(0.0, openingBalance - credit + debit);
                com.lowagie.text.pdf.PdfPCell c5 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(String.format("%.2f", remaining), font));
                c5.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
                c5.setPadding(4f);
                table.addCell(c5);

                openingBalance = remaining;
                curr = curr.plusDays(1);
            }

            document.add(table);
            document.close();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ledger pdf", e);
        }
    }

    private void addMetaPdfCell(com.lowagie.text.pdf.PdfPTable table, String label, String value, com.lowagie.text.Font labelFont, com.lowagie.text.Font valFont) {
        com.lowagie.text.Paragraph p = new com.lowagie.text.Paragraph();
        p.add(new com.lowagie.text.Chunk(label + ": ", labelFont));
        p.add(new com.lowagie.text.Chunk(value != null ? value : "", valFont));
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(p);
        cell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        cell.setPadding(3f);
        table.addCell(cell);
    }

    @Override
    public LedgerPreviewDto getLedgerPreview(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        List<LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);

        String loanNumber = loan.getLoanCode() != null ? loan.getLoanCode() : "";
        String customerName = getFullName(loan.getCustomer());
        String marketName = (loan.getCustomer() != null && loan.getCustomer().getMarket() != null) ? loan.getCustomer().getMarket().getMarketName() : "";

        double disbursedAmount = loan.getDisbursedAmount() != null ? loan.getDisbursedAmount().doubleValue() : (loan.getApprovedAmount() != null ? loan.getApprovedAmount().doubleValue() : (loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0.0));
        String interestRateStr = loan.getInterestRate() != null ? loan.getInterestRate().toString() + "%" : "-";

        String loanTypeStr = "";
        if (loan.getLoanType() == com.dapfintech.loan.enums.LoanType.EMERGENCY) {
            loanTypeStr = "EMERGENCY";
        } else if (loan.getRepaymentFrequency() != null) {
            loanTypeStr = loan.getRepaymentFrequency().name();
        } else if (loan.getLoanType() != null) {
            loanTypeStr = loan.getLoanType().name();
        }

        String tenureStr = "";
        if (loan.getLoanType() == com.dapfintech.loan.enums.LoanType.EMERGENCY) {
            double val = disbursedAmount * (loan.getInterestRate() != null ? loan.getInterestRate().doubleValue() : 0) / 100.0;
            tenureStr = (loan.getTenure() != null ? loan.getTenure() + " Days (" : "") + String.format("%.2f/day", val) + (loan.getTenure() != null ? ")" : "");
        } else {
            if (loan.getTenure() != null) {
                if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EDI) tenureStr = loan.getTenure() + " Days";
                else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EWI) tenureStr = loan.getTenure() + " Weeks";
                else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EMI) tenureStr = loan.getTenure() + " Months";
                else tenureStr = loan.getTenure().toString();
            } else {
                tenureStr = "-";
            }
        }

        List<com.dapfintech.loan.entity.LoanCollection> cols = collectionRepository.findAll().stream()
                .filter(c -> c.getLoan() != null && c.getLoan().getId().equals(loanId))
                .toList();

        LocalDate today = LocalDate.now();
        double amountCollectedToday = cols.stream()
                .filter(c -> c.getCollectionDate() != null && c.getCollectionDate().toLocalDate().isEqual(today))
                .mapToDouble(c -> c.getCollectedAmount() != null ? c.getCollectedAmount().doubleValue() : 0.0)
                .sum();

        double totalLoanAmount = schedules.stream()
                .mapToDouble(s -> s.getInstallmentAmount() != null ? s.getInstallmentAmount().doubleValue() : 0.0)
                .sum();
        if (totalLoanAmount <= 0) {
            double p = disbursedAmount;
            double r = loan.getInterestRate() != null ? loan.getInterestRate().doubleValue() : 0.0;
            totalLoanAmount = p + (p * r / 100.0);
        }

        String statusStr = loan.getLoanStatus() != null ? loan.getLoanStatus().name() : "ACTIVE";
        boolean isActive = !"CLOSED".equalsIgnoreCase(statusStr);

        LocalDate start = loan.getDisbursementDate() != null ? loan.getDisbursementDate().toLocalDate() : (loan.getCreatedAt() != null ? loan.getCreatedAt().toLocalDate() : today);
        LocalDate end = today;
        if (!isActive) {
            LocalDate maxCol = cols.stream().filter(c -> c.getCollectionDate() != null).map(c -> c.getCollectionDate().toLocalDate()).max(LocalDate::compareTo).orElse(start);
            if (!schedules.isEmpty() && schedules.get(schedules.size() - 1).getDueDate() != null) {
                LocalDate maxSched = schedules.get(schedules.size() - 1).getDueDate();
                end = maxCol.isAfter(maxSched) ? maxCol : maxSched;
            } else {
                end = maxCol;
            }
            if (end.isBefore(start)) end = start;
        }

        Map<LocalDate, Double> dailyCols = new HashMap<>();
        for (LoanCollection c : cols) {
            if (c.getCollectedAmount() == null || c.getCollectionDate() == null) continue;
            LocalDate d = c.getCollectionDate().toLocalDate();
            dailyCols.put(d, dailyCols.getOrDefault(d, 0.0) + c.getCollectedAmount().doubleValue());
        }

        Map<LocalDate, Double> dailyEdi = new HashMap<>();
        for (LoanRepaymentSchedule s : schedules) {
            if (s.getDueDate() != null && s.getInstallmentAmount() != null) {
                dailyEdi.put(s.getDueDate(), dailyEdi.getOrDefault(s.getDueDate(), 0.0) + s.getInstallmentAmount().doubleValue());
            }
        }

        List<LedgerEntryDto> ledgerEntries = new ArrayList<>();
        double openingBal = totalLoanAmount;
        LocalDate curr = start;
        while (!curr.isAfter(end)) {
            double todayEdi = 0.0;
            if (loan.getLoanType() == LoanType.EMERGENCY) {
                todayEdi = disbursedAmount * (loan.getInterestRate() != null ? loan.getInterestRate().doubleValue() : 0) / 100.0;
            } else {
                todayEdi = dailyEdi.getOrDefault(curr, 0.0);
            }
            double credit = dailyCols.getOrDefault(curr, 0.0);
            double debit = 0.0;
            double remaining = Math.max(0.0, openingBal - credit + debit);

            LedgerEntryDto entry = new LedgerEntryDto();
            entry.setDate(curr.toString());
            entry.setOpeningBalance(BigDecimal.valueOf(openingBal));
            entry.setTodayEdi(BigDecimal.valueOf(todayEdi));
            entry.setCredit(BigDecimal.valueOf(credit));
            entry.setDebit(BigDecimal.valueOf(debit));
            entry.setRemainingBalance(BigDecimal.valueOf(remaining));
            ledgerEntries.add(entry);

            openingBal = remaining;
            curr = curr.plusDays(1);
        }

        LedgerPreviewDto dto = new LedgerPreviewDto();
        dto.setLoanCode(loanNumber);
        dto.setLoanNumber(loanNumber);
        dto.setCustomerName(customerName);
        dto.setMarketName(marketName);
        dto.setTotalLoanAmount(BigDecimal.valueOf(totalLoanAmount));
        dto.setInterestRate(interestRateStr);
        dto.setDisbursedAmount(BigDecimal.valueOf(disbursedAmount));
        dto.setLoanType(loanTypeStr);
        dto.setTenure(tenureStr);
        dto.setAmountCollectedToday(BigDecimal.valueOf(amountCollectedToday));
        dto.setStatus(statusStr);
        dto.setStartDate(start);
        dto.setEndDate(end);
        dto.setOutstandingBalance(BigDecimal.valueOf(openingBal));
        dto.setClosingBalance(BigDecimal.valueOf(openingBal));
        dto.setTotalInterest(BigDecimal.valueOf(Math.max(0.0, totalLoanAmount - disbursedAmount)));
        dto.setLedgerEntries(ledgerEntries);

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
