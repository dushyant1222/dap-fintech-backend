package com.dapfintech;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class LedgerPatcher {
    public static void main(String[] args) throws Exception {
        String serviceImplPath = "c:/Users/Dushyant Kumar/eclipse-workspace/dap-fintech-backend/src/main/java/com/dapfintech/report/service/ReportingServiceImpl.java";
        String content = new String(Files.readAllBytes(Paths.get(serviceImplPath)));

        String newLedgerExcel = 
            "    @Override\n" +
            "    public ByteArrayInputStream generateLedgerReportExcel(UUID loanId) {\n" +
            "        com.dapfintech.loan.entity.Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException(\"Loan not found\"));\n" +
            "        try (Workbook workbook = new XSSFWorkbook(); java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {\n" +
            "            Sheet sheet = workbook.createSheet(\"Customer Loan Report\");\n" +
            "            \n" +
            "            Row row0 = sheet.createRow(0);\n" +
            "            row0.createCell(0).setCellValue(\"Customer Loan Report\");\n" +
            "            \n" +
            "            Row row1 = sheet.createRow(1);\n" +
            "            row1.createCell(0).setCellValue(\"Market Name:\");\n" +
            "            row1.createCell(1).setCellValue(loan.getCustomer().getMarket() != null ? loan.getCustomer().getMarket().getMarketName() : \"\");\n" +
            "            \n" +
            "            Row row2 = sheet.createRow(2);\n" +
            "            row2.createCell(0).setCellValue(\"Customer Name:\");\n" +
            "            row2.createCell(1).setCellValue(getFullName(loan.getCustomer()));\n" +
            "            \n" +
            "            Row row3 = sheet.createRow(3);\n" +
            "            row3.createCell(0).setCellValue(\"Loan ID:\");\n" +
            "            row3.createCell(1).setCellValue(loan.getLoanCode());\n" +
            "            \n" +
            "            Row row4 = sheet.createRow(4);\n" +
            "            row4.createCell(0).setCellValue(\"Loan Amount:\");\n" +
            "            row4.createCell(1).setCellValue(loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0.0);\n" +
            "            \n" +
            "            Row row5 = sheet.createRow(5);\n" +
            "            row5.createCell(0).setCellValue(\"Interest:\");\n" +
            "            row5.createCell(1).setCellValue(loan.getInterestRate() != null ? loan.getInterestRate().toString() + \"%\" : \"\");\n" +
            "            \n" +
            "            String tenureStr = \"\";\n" +
            "            if (loan.getLoanType() == com.dapfintech.loan.enums.LoanType.EMERGENCY) {\n" +
            "                double val = (loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0) * (loan.getInterestRate() != null ? loan.getInterestRate().doubleValue() : 0) / 100.0;\n" +
            "                tenureStr = val + \" per day\";\n" +
            "            } else {\n" +
            "                if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EDI) tenureStr = \"daily\";\n" +
            "                else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EWI) tenureStr = \"weekly\";\n" +
            "                else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EMI) tenureStr = \"monthly\";\n" +
            "            }\n" +
            "            Row row6 = sheet.createRow(6);\n" +
            "            row6.createCell(0).setCellValue(\"Tenure:\");\n" +
            "            row6.createCell(1).setCellValue(tenureStr);\n" +
            "            \n" +
            "            List<com.dapfintech.loan.entity.LoanCollection> cols = collectionRepository.findAll().stream().filter(c -> c.getLoan() != null && c.getLoan().getId().equals(loanId)).toList();\n" +
            "            double totalCollected = cols.stream().mapToDouble(c -> c.getCollectedAmount() != null ? c.getCollectedAmount().doubleValue() : 0).sum();\n" +
            "            \n" +
            "            Row row7 = sheet.createRow(7);\n" +
            "            row7.createCell(0).setCellValue(\"Amount Collected Till Today:\");\n" +
            "            row7.createCell(1).setCellValue(totalCollected);\n" +
            "            \n" +
            "            Row row9 = sheet.createRow(9);\n" +
            "            row9.createCell(0).setCellValue(\"Loan Status:\");\n" +
            "            row9.createCell(1).setCellValue(loan.getLoanStatus().name());\n" +
            "            \n" +
            "            Row header = sheet.createRow(11);\n" +
            "            String[] colsHeader = {\"Date\", \"Opening Balance\", \"Today EDI/EWI/EMI\", \"Credit\", \"Debit\", \"Remaining Balance\"};\n" +
            "            for (int i=0; i<colsHeader.length; i++) header.createCell(i).setCellValue(colsHeader[i]);\n" +
            "            \n" +
            "            java.time.LocalDate start = loan.getDisbursementDate() != null ? loan.getDisbursementDate().toLocalDate() : java.time.LocalDate.now();\n" +
            "            java.time.LocalDate end = java.time.LocalDate.now();\n" +
            "            if (loan.getLoanStatus() == com.dapfintech.loan.enums.LoanStatus.CLOSED) {\n" +
            "                java.time.LocalDate maxCol = cols.stream().filter(c -> c.getCollectionDate() != null).map(c -> c.getCollectionDate().toLocalDate()).max(java.time.LocalDate::compareTo).orElse(start);\n" +
            "                end = maxCol;\n" +
            "            }\n" +
            "            \n" +
            "            double openingBalance = loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0.0;\n" +
            "            java.util.Map<java.time.LocalDate, Double> dailyCols = new java.util.HashMap<>();\n" +
            "            for (com.dapfintech.loan.entity.LoanCollection c : cols) {\n" +
            "                if (c.getCollectedAmount() == null || c.getCollectionDate() == null) continue;\n" +
            "                java.time.LocalDate d = c.getCollectionDate().toLocalDate();\n" +
            "                dailyCols.put(d, dailyCols.getOrDefault(d, 0.0) + c.getCollectedAmount().doubleValue());\n" +
            "            }\n" +
            "            \n" +
            "            List<com.dapfintech.loan.entity.LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);\n" +
            "            java.util.Map<java.time.LocalDate, Double> dailyEdi = new java.util.HashMap<>();\n" +
            "            for (com.dapfintech.loan.entity.LoanRepaymentSchedule s : schedules) {\n" +
            "                if (s.getDueDate() != null && s.getInstallmentAmount() != null) {\n" +
            "                    dailyEdi.put(s.getDueDate(), dailyEdi.getOrDefault(s.getDueDate(), 0.0) + s.getInstallmentAmount().doubleValue());\n" +
            "                }\n" +
            "            }\n" +
            "            \n" +
            "            java.time.LocalDate curr = start;\n" +
            "            int r = 12;\n" +
            "            double finalRemaining = openingBalance;\n" +
            "            while (!curr.isAfter(end)) {\n" +
            "                Row row = sheet.createRow(r++);\n" +
            "                row.createCell(0).setCellValue(curr.toString());\n" +
            "                row.createCell(1).setCellValue(openingBalance);\n" +
            "                \n" +
            "                double todayEdi = 0.0;\n" +
            "                if (loan.getLoanType() == com.dapfintech.loan.enums.LoanType.EMERGENCY) {\n" +
            "                    todayEdi = (loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0) * (loan.getInterestRate() != null ? loan.getInterestRate().doubleValue() : 0) / 100.0;\n" +
            "                } else {\n" +
            "                    todayEdi = dailyEdi.getOrDefault(curr, 0.0);\n" +
            "                }\n" +
            "                row.createCell(2).setCellValue(todayEdi);\n" +
            "                \n" +
            "                double credit = dailyCols.getOrDefault(curr, 0.0);\n" +
            "                row.createCell(3).setCellValue(credit);\n" +
            "                \n" +
            "                double debit = 0.0;\n" +
            "                row.createCell(4).setCellValue(debit);\n" +
            "                \n" +
            "                double remaining = openingBalance + todayEdi + debit - credit;\n" +
            "                row.createCell(5).setCellValue(remaining);\n" +
            "                \n" +
            "                openingBalance = remaining;\n" +
            "                finalRemaining = remaining;\n" +
            "                curr = curr.plusDays(1);\n" +
            "            }\n" +
            "            \n" +
            "            Row row8 = sheet.createRow(8);\n" +
            "            row8.createCell(0).setCellValue(\"Amount to be Received Till Today:\");\n" +
            "            row8.createCell(1).setCellValue(finalRemaining);\n" +
            "            \n" +
            "            workbook.write(out);\n" +
            "            return new ByteArrayInputStream(out.toByteArray());\n" +
            "        } catch (Exception e) {\n" +
            "            throw new RuntimeException(\"Failed to generate ledger excel\", e);\n" +
            "        }\n" +
            "    }\n\n";

        String newLedgerPdf = 
            "    @Override\n" +
            "    public ByteArrayInputStream generateLedgerReportPdf(UUID loanId) {\n" +
            "        com.dapfintech.loan.entity.Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException(\"Loan not found\"));\n" +
            "        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {\n" +
            "            com.lowagie.text.Document document = new com.lowagie.text.Document();\n" +
            "            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);\n" +
            "            document.open();\n" +
            "            \n" +
            "            document.add(new com.lowagie.text.Paragraph(\"Customer Loan Report\", com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16)));\n" +
            "            document.add(new com.lowagie.text.Paragraph(\"Market Name: \" + (loan.getCustomer().getMarket() != null ? loan.getCustomer().getMarket().getMarketName() : \"\")));\n" +
            "            document.add(new com.lowagie.text.Paragraph(\"Customer Name: \" + getFullName(loan.getCustomer())));\n" +
            "            document.add(new com.lowagie.text.Paragraph(\"Loan ID: \" + loan.getLoanCode()));\n" +
            "            document.add(new com.lowagie.text.Paragraph(\"Loan Amount: \" + (loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0.0)));\n" +
            "            document.add(new com.lowagie.text.Paragraph(\"Interest: \" + (loan.getInterestRate() != null ? loan.getInterestRate().toString() + \"%\" : \"\")));\n" +
            "            \n" +
            "            String tenureStr = \"\";\n" +
            "            if (loan.getLoanType() == com.dapfintech.loan.enums.LoanType.EMERGENCY) {\n" +
            "                double val = (loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0) * (loan.getInterestRate() != null ? loan.getInterestRate().doubleValue() : 0) / 100.0;\n" +
            "                tenureStr = val + \" per day\";\n" +
            "            } else {\n" +
            "                if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EDI) tenureStr = \"daily\";\n" +
            "                else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EWI) tenureStr = \"weekly\";\n" +
            "                else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EMI) tenureStr = \"monthly\";\n" +
            "            }\n" +
            "            document.add(new com.lowagie.text.Paragraph(\"Tenure: \" + tenureStr));\n" +
            "            \n" +
            "            List<com.dapfintech.loan.entity.LoanCollection> cols = collectionRepository.findAll().stream().filter(c -> c.getLoan() != null && c.getLoan().getId().equals(loanId)).toList();\n" +
            "            double totalCollected = cols.stream().mapToDouble(c -> c.getCollectedAmount() != null ? c.getCollectedAmount().doubleValue() : 0).sum();\n" +
            "            document.add(new com.lowagie.text.Paragraph(\"Amount Collected Till Today: \" + totalCollected));\n" +
            "            document.add(new com.lowagie.text.Paragraph(\"Loan Status: \" + loan.getLoanStatus().name()));\n" +
            "            document.add(new com.lowagie.text.Paragraph(\" \"));\n" +
            "            \n" +
            "            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(6);\n" +
            "            table.setWidthPercentage(100);\n" +
            "            String[] colsHeader = {\"Date\", \"Opening Balance\", \"Today EDI/EWI/EMI\", \"Credit\", \"Debit\", \"Remaining Balance\"};\n" +
            "            for (String h : colsHeader) table.addCell(new com.lowagie.text.Phrase(h, com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 10)));\n" +
            "            \n" +
            "            java.time.LocalDate start = loan.getDisbursementDate() != null ? loan.getDisbursementDate().toLocalDate() : java.time.LocalDate.now();\n" +
            "            java.time.LocalDate end = java.time.LocalDate.now();\n" +
            "            if (loan.getLoanStatus() == com.dapfintech.loan.enums.LoanStatus.CLOSED) {\n" +
            "                java.time.LocalDate maxCol = cols.stream().filter(c -> c.getCollectionDate() != null).map(c -> c.getCollectionDate().toLocalDate()).max(java.time.LocalDate::compareTo).orElse(start);\n" +
            "                end = maxCol;\n" +
            "            }\n" +
            "            \n" +
            "            double openingBalance = loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0.0;\n" +
            "            java.util.Map<java.time.LocalDate, Double> dailyCols = new java.util.HashMap<>();\n" +
            "            for (com.dapfintech.loan.entity.LoanCollection c : cols) {\n" +
            "                if (c.getCollectedAmount() == null || c.getCollectionDate() == null) continue;\n" +
            "                java.time.LocalDate d = c.getCollectionDate().toLocalDate();\n" +
            "                dailyCols.put(d, dailyCols.getOrDefault(d, 0.0) + c.getCollectedAmount().doubleValue());\n" +
            "            }\n" +
            "            \n" +
            "            List<com.dapfintech.loan.entity.LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);\n" +
            "            java.util.Map<java.time.LocalDate, Double> dailyEdi = new java.util.HashMap<>();\n" +
            "            for (com.dapfintech.loan.entity.LoanRepaymentSchedule s : schedules) {\n" +
            "                if (s.getDueDate() != null && s.getInstallmentAmount() != null) {\n" +
            "                    dailyEdi.put(s.getDueDate(), dailyEdi.getOrDefault(s.getDueDate(), 0.0) + s.getInstallmentAmount().doubleValue());\n" +
            "                }\n" +
            "            }\n" +
            "            \n" +
            "            java.time.LocalDate curr = start;\n" +
            "            double finalRemaining = openingBalance;\n" +
            "            com.lowagie.text.Font font = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 9);\n" +
            "            while (!curr.isAfter(end)) {\n" +
            "                table.addCell(new com.lowagie.text.Phrase(curr.toString(), font));\n" +
            "                table.addCell(new com.lowagie.text.Phrase(String.valueOf(openingBalance), font));\n" +
            "                \n" +
            "                double todayEdi = 0.0;\n" +
            "                if (loan.getLoanType() == com.dapfintech.loan.enums.LoanType.EMERGENCY) {\n" +
            "                    todayEdi = (loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0) * (loan.getInterestRate() != null ? loan.getInterestRate().doubleValue() : 0) / 100.0;\n" +
            "                } else {\n" +
            "                    todayEdi = dailyEdi.getOrDefault(curr, 0.0);\n" +
            "                }\n" +
            "                table.addCell(new com.lowagie.text.Phrase(String.valueOf(todayEdi), font));\n" +
            "                \n" +
            "                double credit = dailyCols.getOrDefault(curr, 0.0);\n" +
            "                table.addCell(new com.lowagie.text.Phrase(String.valueOf(credit), font));\n" +
            "                \n" +
            "                double debit = 0.0;\n" +
            "                table.addCell(new com.lowagie.text.Phrase(String.valueOf(debit), font));\n" +
            "                \n" +
            "                double remaining = openingBalance + todayEdi + debit - credit;\n" +
            "                table.addCell(new com.lowagie.text.Phrase(String.valueOf(remaining), font));\n" +
            "                \n" +
            "                openingBalance = remaining;\n" +
            "                finalRemaining = remaining;\n" +
            "                curr = curr.plusDays(1);\n" +
            "            }\n" +
            "            \n" +
            "            document.add(table);\n" +
            "            \n" +
            "            document.add(new com.lowagie.text.Paragraph(\"Amount to be Received Till Today: \" + finalRemaining));\n" +
            "            \n" +
            "            document.close();\n" +
            "            return new ByteArrayInputStream(out.toByteArray());\n" +
            "        } catch (Exception e) {\n" +
            "            throw new RuntimeException(\"Failed to generate ledger pdf\", e);\n" +
            "        }\n" +
            "    }\n";

        content = content.replaceAll(
            "(?s)    @Override\\s+public ByteArrayInputStream generateLedgerReportExcel.*?\\}\\s*    @Override\\s+public ByteArrayInputStream generateLedgerReportPdf.*?\\}\\s*    @Override\\s+public LedgerPreviewDto getLedgerPreview",
            Matcher.quoteReplacement(newLedgerExcel + newLedgerPdf + "    @Override\n    public LedgerPreviewDto getLedgerPreview")
        );

        Files.write(Paths.get(serviceImplPath), content.getBytes());
        System.out.println("Patched ReportingServiceImpl.java with Ledger logic");
    }
}
