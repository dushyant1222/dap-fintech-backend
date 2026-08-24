package com.dapfintech;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class DaybookPatcher {
    public static void main(String[] args) throws Exception {
        String path = "c:/Users/Dushyant Kumar/eclipse-workspace/dap-fintech-backend/src/main/java/com/dapfintech/report/service/ReportingServiceImpl.java";
        String content = new String(Files.readAllBytes(Paths.get(path)));

        // Add imports
        if (!content.contains("import com.dapfintech.employee.repository.DayBookRepository;")) {
            content = content.replace("import com.dapfintech.loan.repository.LoanRepaymentScheduleRepository;",
                                      "import com.dapfintech.loan.repository.LoanRepaymentScheduleRepository;\n" +
                                      "import com.dapfintech.employee.repository.DayBookRepository;\n" +
                                      "import com.dapfintech.employee.entity.DayBook;\n" +
                                      "import com.dapfintech.auth.repository.UserRepository;\n" +
                                      "import com.dapfintech.auth.entity.User;\n" +
                                      "import com.dapfintech.loan.entity.LoanCollection;\n");
        }

        // Add dependencies (be careful, don't break constructor injection, wait!)
        // WAIT! It's @RequiredArgsConstructor in ReportingServiceImpl.java! 
        // If I just add fields, they get auto-injected by Spring, but what if they are missing in tests? 
        // We'll just add them to the end of the fields list.
        if (!content.contains("private final DayBookRepository dayBookRepository;")) {
            content = content.replace("private final LoanRepaymentScheduleRepository scheduleRepository;",
                                      "private final LoanRepaymentScheduleRepository scheduleRepository;\n" +
                                      "    private final DayBookRepository dayBookRepository;\n" +
                                      "    private final UserRepository userRepository;\n");
        }

        // Replace PDF logic
        String newPdf = 
            "    @Override\n" +
            "    public ByteArrayInputStream generateEmployeeDaybookPdf(UUID employeeId, LocalDate date) {\n" +
            "        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {\n" +
            "            com.lowagie.text.Document document = new com.lowagie.text.Document();\n" +
            "            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);\n" +
            "            document.open();\n" +
            "\n" +
            "            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 18);\n" +
            "            com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph(\"Employee Daybook Ledger\", titleFont);\n" +
            "            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);\n" +
            "            document.add(title);\n" +
            "            document.add(new com.lowagie.text.Paragraph(\" \"));\n" +
            "\n" +
            "            User emp = userRepository.findById(employeeId).orElse(null);\n" +
            "            String empName = emp != null ? (emp.getFirstName() + \" \" + emp.getLastName()) : employeeId.toString();\n" +
            "\n" +
            "            document.add(new com.lowagie.text.Paragraph(\"Employee Name: \" + empName));\n" +
            "            document.add(new com.lowagie.text.Paragraph(\"Date: \" + date.toString()));\n" +
            "            document.add(new com.lowagie.text.Paragraph(\" \"));\n" +
            "\n" +
            "            DayBook db = dayBookRepository.findByEmployeeIdAndDate(employeeId, date).orElse(null);\n" +
            "            if (db == null) {\n" +
            "                document.add(new com.lowagie.text.Paragraph(\"No Daybook data found for this date.\"));\n" +
            "            } else {\n" +
            "                com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(2);\n" +
            "                table.setWidthPercentage(100);\n" +
            "                com.lowagie.text.Font headFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 12);\n" +
            "                table.addCell(new com.lowagie.text.Phrase(\"Description\", headFont));\n" +
            "                table.addCell(new com.lowagie.text.Phrase(\"Amount\", headFont));\n" +
            "\n" +
            "                table.addCell(\"Opening Balance\"); table.addCell(String.valueOf(db.getOpeningBalance()));\n" +
            "                table.addCell(\"Cash Collections\"); table.addCell(String.valueOf(db.getCollections()));\n" +
            "                table.addCell(\"Transfers Received\"); table.addCell(String.valueOf(db.getIncomingTransfers()));\n" +
            "                table.addCell(\"Total Spends\"); table.addCell(String.valueOf(db.getSpends()));\n" +
            "                table.addCell(\"Loans Disbursed\"); table.addCell(String.valueOf(db.getLoansDisbursed()));\n" +
            "                table.addCell(\"Transfers Sent\"); table.addCell(String.valueOf(db.getOutgoingTransfers()));\n" +
            "                table.addCell(\"Office Remittance\"); table.addCell(String.valueOf(db.getOfficeRemittance()));\n" +
            "                table.addCell(\"Closing Balance\"); table.addCell(String.valueOf(db.getClosingBalance()));\n" +
            "                table.addCell(\"Status\"); table.addCell(db.getStatus().name());\n" +
            "                document.add(table);\n" +
            "            }\n" +
            "\n" +
            "            document.add(new com.lowagie.text.Paragraph(\" \"));\n" +
            "            document.add(new com.lowagie.text.Paragraph(\"Collection Transactions:\", titleFont));\n" +
            "            document.add(new com.lowagie.text.Paragraph(\" \"));\n" +
            "\n" +
            "            java.util.List<LoanCollection> cols = collectionRepository.findAll().stream()\n" +
            "                    .filter(c -> c.getCollectedBy() != null && c.getCollectedBy().getId().equals(employeeId) && c.getCollectionDate() != null && c.getCollectionDate().toLocalDate().equals(date))\n" +
            "                    .toList();\n" +
            "\n" +
            "            if (cols.isEmpty()) {\n" +
            "                document.add(new com.lowagie.text.Paragraph(\"No collections recorded.\"));\n" +
            "            } else {\n" +
            "                com.lowagie.text.pdf.PdfPTable colTable = new com.lowagie.text.pdf.PdfPTable(4);\n" +
            "                colTable.setWidthPercentage(100);\n" +
            "                com.lowagie.text.Font headFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 10);\n" +
            "                colTable.addCell(new com.lowagie.text.Phrase(\"Time\", headFont));\n" +
            "                colTable.addCell(new com.lowagie.text.Phrase(\"Customer\", headFont));\n" +
            "                colTable.addCell(new com.lowagie.text.Phrase(\"Loan ID\", headFont));\n" +
            "                colTable.addCell(new com.lowagie.text.Phrase(\"Amount\", headFont));\n" +
            "\n" +
            "                java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern(\"hh:mm a\");\n" +
            "                for (LoanCollection c : cols) {\n" +
            "                    colTable.addCell(c.getCollectionDate().format(timeFormatter));\n" +
            "                    colTable.addCell(c.getLoan() != null && c.getLoan().getCustomer() != null ? getFullName(c.getLoan().getCustomer()) : \"\");\n" +
            "                    colTable.addCell(c.getLoan() != null ? c.getLoan().getLoanCode() : \"\");\n" +
            "                    colTable.addCell(String.valueOf(c.getCollectedAmount()));\n" +
            "                }\n" +
            "                document.add(colTable);\n" +
            "            }\n" +
            "\n" +
            "            document.close();\n" +
            "            return new java.io.ByteArrayInputStream(out.toByteArray());\n" +
            "        } catch (Exception e) {\n" +
            "            throw new RuntimeException(\"Failed to generate daybook pdf\", e);\n" +
            "        }\n" +
            "    }\n";

        // Replace Excel logic as well just in case
        String newExcel = 
            "    @Override\n" +
            "    public ByteArrayInputStream generateEmployeeDaybookExcel(UUID employeeId, LocalDate date) {\n" +
            "        try (Workbook workbook = new XSSFWorkbook(); java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {\n" +
            "            Sheet sheet = workbook.createSheet(\"Employee Daybook\");\n" +
            "            \n" +
            "            User emp = userRepository.findById(employeeId).orElse(null);\n" +
            "            String empName = emp != null ? (emp.getFirstName() + \" \" + emp.getLastName()) : employeeId.toString();\n" +
            "            \n" +
            "            Row row0 = sheet.createRow(0);\n" +
            "            row0.createCell(0).setCellValue(\"Employee Name:\");\n" +
            "            row0.createCell(1).setCellValue(empName);\n" +
            "            \n" +
            "            Row row1 = sheet.createRow(1);\n" +
            "            row1.createCell(0).setCellValue(\"Date:\");\n" +
            "            row1.createCell(1).setCellValue(date.toString());\n" +
            "            \n" +
            "            DayBook db = dayBookRepository.findByEmployeeIdAndDate(employeeId, date).orElse(null);\n" +
            "            if (db != null) {\n" +
            "                sheet.createRow(3).createCell(0).setCellValue(\"Opening Balance:\"); sheet.getRow(3).createCell(1).setCellValue(db.getOpeningBalance() != null ? db.getOpeningBalance().doubleValue() : 0.0);\n" +
            "                sheet.createRow(4).createCell(0).setCellValue(\"Cash Collections:\"); sheet.getRow(4).createCell(1).setCellValue(db.getCollections() != null ? db.getCollections().doubleValue() : 0.0);\n" +
            "                sheet.createRow(5).createCell(0).setCellValue(\"Transfers Received:\"); sheet.getRow(5).createCell(1).setCellValue(db.getIncomingTransfers() != null ? db.getIncomingTransfers().doubleValue() : 0.0);\n" +
            "                sheet.createRow(6).createCell(0).setCellValue(\"Total Spends:\"); sheet.getRow(6).createCell(1).setCellValue(db.getSpends() != null ? db.getSpends().doubleValue() : 0.0);\n" +
            "                sheet.createRow(7).createCell(0).setCellValue(\"Loans Disbursed:\"); sheet.getRow(7).createCell(1).setCellValue(db.getLoansDisbursed() != null ? db.getLoansDisbursed().doubleValue() : 0.0);\n" +
            "                sheet.createRow(8).createCell(0).setCellValue(\"Transfers Sent:\"); sheet.getRow(8).createCell(1).setCellValue(db.getOutgoingTransfers() != null ? db.getOutgoingTransfers().doubleValue() : 0.0);\n" +
            "                sheet.createRow(9).createCell(0).setCellValue(\"Office Remittance:\"); sheet.getRow(9).createCell(1).setCellValue(db.getOfficeRemittance() != null ? db.getOfficeRemittance().doubleValue() : 0.0);\n" +
            "                sheet.createRow(10).createCell(0).setCellValue(\"Closing Balance:\"); sheet.getRow(10).createCell(1).setCellValue(db.getClosingBalance() != null ? db.getClosingBalance().doubleValue() : 0.0);\n" +
            "                sheet.createRow(11).createCell(0).setCellValue(\"Status:\"); sheet.getRow(11).createCell(1).setCellValue(db.getStatus().name());\n" +
            "            }\n" +
            "            \n" +
            "            Row row13 = sheet.createRow(13);\n" +
            "            row13.createCell(0).setCellValue(\"Collection Transactions:\");\n" +
            "            \n" +
            "            Row header = sheet.createRow(14);\n" +
            "            header.createCell(0).setCellValue(\"Time\");\n" +
            "            header.createCell(1).setCellValue(\"Customer\");\n" +
            "            header.createCell(2).setCellValue(\"Loan ID\");\n" +
            "            header.createCell(3).setCellValue(\"Amount\");\n" +
            "            \n" +
            "            java.util.List<LoanCollection> cols = collectionRepository.findAll().stream()\n" +
            "                    .filter(c -> c.getCollectedBy() != null && c.getCollectedBy().getId().equals(employeeId) && c.getCollectionDate() != null && c.getCollectionDate().toLocalDate().equals(date))\n" +
            "                    .toList();\n" +
            "            \n" +
            "            int r = 15;\n" +
            "            java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern(\"hh:mm a\");\n" +
            "            for (LoanCollection c : cols) {\n" +
            "                Row row = sheet.createRow(r++);\n" +
            "                row.createCell(0).setCellValue(c.getCollectionDate().format(timeFormatter));\n" +
            "                row.createCell(1).setCellValue(c.getLoan() != null && c.getLoan().getCustomer() != null ? getFullName(c.getLoan().getCustomer()) : \"\");\n" +
            "                row.createCell(2).setCellValue(c.getLoan() != null ? c.getLoan().getLoanCode() : \"\");\n" +
            "                row.createCell(3).setCellValue(c.getCollectedAmount() != null ? c.getCollectedAmount().doubleValue() : 0.0);\n" +
            "            }\n" +
            "            \n" +
            "            workbook.write(out);\n" +
            "            return new java.io.ByteArrayInputStream(out.toByteArray());\n" +
            "        } catch (Exception e) {\n" +
            "            throw new RuntimeException(\"Failed to generate daybook excel\", e);\n" +
            "        }\n" +
            "    }\n";

        content = content.replaceAll(
            "(?s)    @Override\\s+public ByteArrayInputStream generateEmployeeDaybookPdf.*?\\}\\s*    @Override\\s+public ByteArrayInputStream generateEmployeeDaybookExcel.*?\\}\\s*\\}",
            Matcher.quoteReplacement(newPdf + newExcel + "}\n")
        );

        Files.write(Paths.get(path), content.getBytes());
        System.out.println("Patched ReportingServiceImpl.java with Daybook Logic");
    }
}
