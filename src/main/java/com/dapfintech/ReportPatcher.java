package com.dapfintech;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ReportPatcher {
    public static void main(String[] args) throws Exception {
        String serviceImplPath = "c:/Users/Dushyant Kumar/eclipse-workspace/dap-fintech-backend/src/main/java/com/dapfintech/report/service/ReportingServiceImpl.java";
        String content = new String(Files.readAllBytes(Paths.get(serviceImplPath)));

        String newExcelMethod = 
            "    @Override\n" +
            "    public ByteArrayInputStream generateCollectionReportExcel(UUID marketId, UUID customerId) {\n" +
            "        List<com.dapfintech.loan.entity.Loan> loans = loanRepository.findAll();\n" +
            "        loans.removeIf(l -> l.getCustomer() == null);\n" +
            "        if (marketId != null) loans.removeIf(l -> l.getCustomer().getMarket() == null || !l.getCustomer().getMarket().getId().equals(marketId));\n" +
            "        if (customerId != null) loans.removeIf(l -> !l.getCustomer().getId().equals(customerId));\n" +
            "        loans.removeIf(l -> l.getDisbursementDate() == null);\n" +
            "        \n" +
            "        if (loans.isEmpty()) {\n" +
            "            try (Workbook wb = new XSSFWorkbook(); java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {\n" +
            "                wb.createSheet(\"Collections\").createRow(0).createCell(0).setCellValue(\"No data found\");\n" +
            "                wb.write(out);\n" +
            "                return new ByteArrayInputStream(out.toByteArray());\n" +
            "            } catch(Exception e) { throw new RuntimeException(e); }\n" +
            "        }\n" +
            "\n" +
            "        java.time.LocalDate minDate = loans.stream().map(l -> l.getDisbursementDate().toLocalDate()).min(java.time.LocalDate::compareTo).orElse(java.time.LocalDate.now());\n" +
            "        java.time.LocalDate maxDate = java.time.LocalDate.now();\n" +
            "        for (com.dapfintech.loan.entity.Loan l : loans) {\n" +
            "            java.time.LocalDate end = l.getDisbursementDate().toLocalDate();\n" +
            "            if (l.getDurationInDays() != null) end = end.plusDays(l.getDurationInDays());\n" +
            "            if (end.isAfter(maxDate)) maxDate = end;\n" +
            "        }\n" +
            "        if (maxDate.isAfter(java.time.LocalDate.now())) maxDate = java.time.LocalDate.now();\n" +
            "        \n" +
            "        List<java.time.LocalDate> dateColumns = new java.util.ArrayList<>();\n" +
            "        java.time.LocalDate curr = minDate;\n" +
            "        while (!curr.isAfter(maxDate)) {\n" +
            "            dateColumns.add(curr);\n" +
            "            curr = curr.plusDays(1);\n" +
            "        }\n" +
            "\n" +
            "        List<com.dapfintech.loan.entity.LoanCollection> allCollections = collectionRepository.findAll();\n" +
            "\n" +
            "        try (Workbook workbook = new XSSFWorkbook(); java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {\n" +
            "            Sheet sheet = workbook.createSheet(\"Collection Report\");\n" +
            "            \n" +
            "            Row titleRow = sheet.createRow(0);\n" +
            "            titleRow.createCell(0).setCellValue(\"Collection Report\");\n" +
            "            \n" +
            "            String marketName = \"All Markets\";\n" +
            "            if (marketId != null && !loans.isEmpty() && loans.get(0).getCustomer().getMarket() != null) {\n" +
            "                marketName = loans.get(0).getCustomer().getMarket().getName();\n" +
            "            }\n" +
            "            Row subRow = sheet.createRow(1);\n" +
            "            subRow.createCell(0).setCellValue(\"Market Name: \" + marketName);\n" +
            "            \n" +
            "            Row headerRow = sheet.createRow(3);\n" +
            "            String[] baseCols = {\"S.No\", \"Customer Name\", \"Loan Issue Date\", \"Loan Code\", \"Loan Status\", \"Total Loan Amount\", \"Loan Type\", \"Tenure\", \"Collected Till Date\", \"Remaining Balance\"};\n" +
            "            int colIdx = 0;\n" +
            "            for (String col : baseCols) {\n" +
            "                headerRow.createCell(colIdx++).setCellValue(col);\n" +
            "            }\n" +
            "            \n" +
            "            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern(\"dd-MMM\");\n" +
            "            for (java.time.LocalDate d : dateColumns) {\n" +
            "                headerRow.createCell(colIdx++).setCellValue(\"Date-\" + d.format(dtf));\n" +
            "            }\n" +
            "            \n" +
            "            int rowIdx = 4;\n" +
            "            int sNo = 1;\n" +
            "            \n" +
            "            for (com.dapfintech.loan.entity.Loan loan : loans) {\n" +
            "                Row row = sheet.createRow(rowIdx++);\n" +
            "                int c = 0;\n" +
            "                row.createCell(c++).setCellValue(sNo++);\n" +
            "                row.createCell(c++).setCellValue(getFullName(loan.getCustomer()));\n" +
            "                row.createCell(c++).setCellValue(loan.getDisbursementDate().toLocalDate().toString());\n" +
            "                row.createCell(c++).setCellValue(loan.getLoanCode());\n" +
            "                row.createCell(c++).setCellValue(loan.getLoanStatus().name());\n" +
            "                row.createCell(c++).setCellValue(loan.getDisbursedAmount() != null ? loan.getDisbursedAmount().doubleValue() : 0.0);\n" +
            "                row.createCell(c++).setCellValue(loan.getLoanType() != null ? loan.getLoanType().name() : \"\");\n" +
            "                \n" +
            "                String tenure = \"\";\n" +
            "                if (loan.getTenure() != null) {\n" +
            "                    if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EDI) tenure = loan.getTenure() + \" days\";\n" +
            "                    else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EWI) tenure = loan.getTenure() + \" weeks\";\n" +
            "                    else tenure = loan.getTenure() + \" months\";\n" +
            "                }\n" +
            "                row.createCell(c++).setCellValue(tenure);\n" +
            "                \n" +
            "                double collected = 0;\n" +
            "                java.util.Map<java.time.LocalDate, Double> dailyMap = new java.util.HashMap<>();\n" +
            "                for (com.dapfintech.loan.entity.LoanCollection lc : allCollections) {\n" +
            "                    if (lc.getLoan() != null && lc.getLoan().getId().equals(loan.getId()) && lc.getCollectedAmount() != null) {\n" +
            "                        collected += lc.getCollectedAmount().doubleValue();\n" +
            "                        java.time.LocalDate cd = lc.getCollectionDate().toLocalDate();\n" +
            "                        dailyMap.put(cd, dailyMap.getOrDefault(cd, 0.0) + lc.getCollectedAmount().doubleValue());\n" +
            "                    }\n" +
            "                }\n" +
            "                \n" +
            "                row.createCell(c++).setCellValue(collected);\n" +
            "                double total = loan.getDisbursedAmount() != null ? loan.getDisbursedAmount().doubleValue() : 0;\n" +
            "                row.createCell(c++).setCellValue(Math.max(0, total - collected));\n" +
            "                \n" +
            "                for (java.time.LocalDate d : dateColumns) {\n" +
            "                    Cell cell = row.createCell(c++);\n" +
            "                    if (dailyMap.containsKey(d)) {\n" +
            "                        cell.setCellValue(dailyMap.get(d));\n" +
            "                    } else {\n" +
            "                        cell.setCellValue(\"X\");\n" +
            "                    }\n" +
            "                }\n" +
            "            }\n" +
            "            \n" +
            "            workbook.write(out);\n" +
            "            return new ByteArrayInputStream(out.toByteArray());\n" +
            "        } catch (Exception e) {\n" +
            "            throw new RuntimeException(\"Failed to generate collection excel\", e);\n" +
            "        }\n" +
            "    }\n\n";

        String newPdfMethod =
            "    @Override\n" +
            "    public ByteArrayInputStream generateCollectionReportPdf(UUID marketId, UUID customerId) {\n" +
            "        List<com.dapfintech.loan.entity.Loan> loans = loanRepository.findAll();\n" +
            "        loans.removeIf(l -> l.getCustomer() == null);\n" +
            "        if (marketId != null) loans.removeIf(l -> l.getCustomer().getMarket() == null || !l.getCustomer().getMarket().getId().equals(marketId));\n" +
            "        if (customerId != null) loans.removeIf(l -> !l.getCustomer().getId().equals(customerId));\n" +
            "        loans.removeIf(l -> l.getDisbursementDate() == null);\n" +
            "        \n" +
            "        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();\n" +
            "        com.lowagie.text.Document document = new com.lowagie.text.Document(new com.lowagie.text.Rectangle(2500, 842));\n" +
            "        try {\n" +
            "            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);\n" +
            "            document.open();\n" +
            "            document.add(new com.lowagie.text.Paragraph(\"Collection Report\"));\n" +
            "            \n" +
            "            String marketName = \"All Markets\";\n" +
            "            if (marketId != null && !loans.isEmpty() && loans.get(0).getCustomer().getMarket() != null) {\n" +
            "                marketName = loans.get(0).getCustomer().getMarket().getName();\n" +
            "            }\n" +
            "            document.add(new com.lowagie.text.Paragraph(\"Market Name: \" + marketName));\n" +
            "            document.add(new com.lowagie.text.Paragraph(\" \"));\n" +
            "\n" +
            "            if (loans.isEmpty()) {\n" +
            "                document.add(new com.lowagie.text.Paragraph(\"No data found\"));\n" +
            "                document.close();\n" +
            "                return new ByteArrayInputStream(out.toByteArray());\n" +
            "            }\n" +
            "\n" +
            "            java.time.LocalDate minDate = loans.stream().map(l -> l.getDisbursementDate().toLocalDate()).min(java.time.LocalDate::compareTo).orElse(java.time.LocalDate.now());\n" +
            "            java.time.LocalDate maxDate = java.time.LocalDate.now();\n" +
            "            for (com.dapfintech.loan.entity.Loan l : loans) {\n" +
            "                java.time.LocalDate end = l.getDisbursementDate().toLocalDate();\n" +
            "                if (l.getDurationInDays() != null) end = end.plusDays(l.getDurationInDays());\n" +
            "                if (end.isAfter(maxDate)) maxDate = end;\n" +
            "            }\n" +
            "            if (maxDate.isAfter(java.time.LocalDate.now())) maxDate = java.time.LocalDate.now();\n" +
            "            \n" +
            "            List<java.time.LocalDate> dateColumns = new java.util.ArrayList<>();\n" +
            "            java.time.LocalDate curr = minDate;\n" +
            "            while (!curr.isAfter(maxDate)) {\n" +
            "                dateColumns.add(curr);\n" +
            "                curr = curr.plusDays(1);\n" +
            "            }\n" +
            "\n" +
            "            String[] baseCols = {\"S.No\", \"Customer Name\", \"Loan Issue Date\", \"Loan Code\", \"Loan Status\", \"Total Loan Amount\", \"Loan Type\", \"Tenure\", \"Collected Till Date\", \"Remaining Balance\"};\n" +
            "            int numColumns = baseCols.length + dateColumns.size();\n" +
            "            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(numColumns);\n" +
            "            table.setWidthPercentage(100);\n" +
            "            \n" +
            "            for (String col : baseCols) {\n" +
            "                table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(col, com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 9))));\n" +
            "            }\n" +
            "            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern(\"dd-MMM\");\n" +
            "            for (java.time.LocalDate d : dateColumns) {\n" +
            "                table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(\"Date-\" + d.format(dtf), com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 9))));\n" +
            "            }\n" +
            "\n" +
            "            List<com.dapfintech.loan.entity.LoanCollection> allCollections = collectionRepository.findAll();\n" +
            "            int sNo = 1;\n" +
            "            com.lowagie.text.Font font = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 8);\n" +
            "            \n" +
            "            for (com.dapfintech.loan.entity.Loan loan : loans) {\n" +
            "                table.addCell(new com.lowagie.text.Phrase(String.valueOf(sNo++), font));\n" +
            "                table.addCell(new com.lowagie.text.Phrase(getFullName(loan.getCustomer()), font));\n" +
            "                table.addCell(new com.lowagie.text.Phrase(loan.getDisbursementDate().toLocalDate().toString(), font));\n" +
            "                table.addCell(new com.lowagie.text.Phrase(loan.getLoanCode(), font));\n" +
            "                table.addCell(new com.lowagie.text.Phrase(loan.getLoanStatus().name(), font));\n" +
            "                table.addCell(new com.lowagie.text.Phrase(loan.getDisbursedAmount() != null ? String.valueOf(loan.getDisbursedAmount().doubleValue()) : \"0.0\", font));\n" +
            "                table.addCell(new com.lowagie.text.Phrase(loan.getLoanType() != null ? loan.getLoanType().name() : \"\", font));\n" +
            "                \n" +
            "                String tenure = \"\";\n" +
            "                if (loan.getTenure() != null) {\n" +
            "                    if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EDI) tenure = loan.getTenure() + \" days\";\n" +
            "                    else if (loan.getRepaymentFrequency() == com.dapfintech.loan.enums.RepaymentFrequency.EWI) tenure = loan.getTenure() + \" weeks\";\n" +
            "                    else tenure = loan.getTenure() + \" months\";\n" +
            "                }\n" +
            "                table.addCell(new com.lowagie.text.Phrase(tenure, font));\n" +
            "                \n" +
            "                double collected = 0;\n" +
            "                java.util.Map<java.time.LocalDate, Double> dailyMap = new java.util.HashMap<>();\n" +
            "                for (com.dapfintech.loan.entity.LoanCollection lc : allCollections) {\n" +
            "                    if (lc.getLoan() != null && lc.getLoan().getId().equals(loan.getId()) && lc.getCollectedAmount() != null) {\n" +
            "                        collected += lc.getCollectedAmount().doubleValue();\n" +
            "                        java.time.LocalDate cd = lc.getCollectionDate().toLocalDate();\n" +
            "                        dailyMap.put(cd, dailyMap.getOrDefault(cd, 0.0) + lc.getCollectedAmount().doubleValue());\n" +
            "                    }\n" +
            "                }\n" +
            "                \n" +
            "                table.addCell(new com.lowagie.text.Phrase(String.valueOf(collected), font));\n" +
            "                double total = loan.getDisbursedAmount() != null ? loan.getDisbursedAmount().doubleValue() : 0;\n" +
            "                table.addCell(new com.lowagie.text.Phrase(String.valueOf(Math.max(0, total - collected)), font));\n" +
            "                \n" +
            "                for (java.time.LocalDate d : dateColumns) {\n" +
            "                    if (dailyMap.containsKey(d)) {\n" +
            "                        table.addCell(new com.lowagie.text.Phrase(String.valueOf(dailyMap.get(d)), font));\n" +
            "                    } else {\n" +
            "                        table.addCell(new com.lowagie.text.Phrase(\"X\", font));\n" +
            "                    }\n" +
            "                }\n" +
            "            }\n" +
            "            \n" +
            "            document.add(table);\n" +
            "            document.close();\n" +
            "            return new ByteArrayInputStream(out.toByteArray());\n" +
            "        } catch (Exception e) {\n" +
            "            throw new RuntimeException(\"Failed to generate collection PDF\", e);\n" +
            "        }\n" +
            "    }\n\n";

        content = content.replaceAll(
            "(?s)    @Override\\s+public ByteArrayInputStream generateCollectionReportExcel.*?\\}\\s*    @Override\\s+public ByteArrayInputStream generateLedgerReportExcel",
            Matcher.quoteReplacement(newExcelMethod + newPdfMethod + "    @Override\n    public ByteArrayInputStream generateLedgerReportExcel")
        );

        Files.write(Paths.get(serviceImplPath), content.getBytes());
        System.out.println("Patched ReportingServiceImpl.java");
    }
}
