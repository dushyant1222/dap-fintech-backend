package com.dapfintech.onboarding.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.customer.entity.Customer;
import com.dapfintech.customer.enums.CustomerStatus;
import com.dapfintech.customer.enums.Gender;
import com.dapfintech.customer.repository.CustomerRepository;
import com.dapfintech.loan.dto.response.LoanResponse;
import com.dapfintech.loan.entity.Loan;
import com.dapfintech.loan.entity.LoanCollection;
import com.dapfintech.loan.entity.LoanRepaymentSchedule;
import com.dapfintech.loan.enums.CollectionMode;
import com.dapfintech.loan.enums.InterestType;
import com.dapfintech.loan.enums.LoanStatus;
import com.dapfintech.loan.enums.LoanType;
import com.dapfintech.loan.enums.RepaymentFrequency;
import com.dapfintech.loan.enums.RepaymentStatus;
import com.dapfintech.loan.mapper.LoanMapper;
import com.dapfintech.loan.repository.LoanCollectionRepository;
import com.dapfintech.loan.repository.LoanRepository;
import com.dapfintech.loan.repository.LoanRepaymentScheduleRepository;
import com.dapfintech.loan.service.LoanRepaymentScheduleService;
import com.dapfintech.market.entity.EmployeeMarketAssignment;
import com.dapfintech.market.entity.Market;
import com.dapfintech.market.enums.MarketStatus;
import com.dapfintech.market.repository.EmployeeMarketAssignmentRepository;
import com.dapfintech.market.repository.MarketRepository;
import com.dapfintech.onboarding.dto.request.OnboardSingleLoanRequest;
import com.dapfintech.onboarding.dto.response.OnboardingSummaryResponse;
import com.dapfintech.onboarding.service.OnboardingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingServiceImpl implements OnboardingService {

    private final CustomerRepository customerRepository;
    private final MarketRepository marketRepository;
    private final UserRepository userRepository;
    private final EmployeeMarketAssignmentRepository assignmentRepository;
    private final LoanRepository loanRepository;
    private final LoanRepaymentScheduleRepository scheduleRepository;
    private final LoanCollectionRepository collectionRepository;
    private final LoanRepaymentScheduleService repaymentScheduleService;
    private final LoanMapper loanMapper;

    @Override
    public ByteArrayInputStream generateOnboardingTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Historical Loans Onboarding");

            // Header styling
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GOLD.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.BLACK.getIndex());
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            String[] headers = {
                "Customer Name*",
                "Mobile Number*",
                "Address",
                "Market Name",
                "Collector Mobile",
                "Loan Type (REGULAR/EMERGENCY)*",
                "Disbursement Date (DD/MM/YYYY)*",
                "Principal Amount*",
                "Interest Type (FLAT / FLAT_DIRECT)",
                "Interest Rate or Flat Amount*",
                "Tenure (Days/Weeks/Months - 0 for Emergency)*",
                "Frequency (EDI/EWI/EMI)*",
                "Total Collected So Far",
                "Last Payment Date (DD/MM/YYYY)"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(28);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample Rows:
            // 1. Regular Loan (Flat % rate, 100 days EDI)
            // 2. Regular Loan (Flat Direct Amount, e.g. ₹2000 flat interest, 100 days EDI)
            // 3. Emergency Loan (Daily Interest 1%, No Tenure (0), EDI)
            Object[][] sampleData = {
                {"Rahul Sharma", "9876543210", "Shop 12, Main Bazar", "Civil Lines", "9123456780", "REGULAR", "15/07/2026", 10000, "FLAT", 20, 100, "EDI", 6400, "09/09/2026"},
                {"Amit Verma", "9876543211", "House 45, Gandhi Nagar", "Aminabad", "9123456780", "REGULAR", "01/08/2026", 20000, "FLAT_DIRECT", 2000, 100, "EDI", 12000, "08/09/2026"},
                {"Suresh Kumar", "9876543212", "Shop 8, Vegetable Market", "Chowk", "", "EMERGENCY", "10/08/2026", 5000, "FLAT", 1, 0, "EDI", 1500, "09/09/2026"}
            };

            for (int r = 0; r < sampleData.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < sampleData[r].length; c++) {
                    Cell cell = row.createCell(c);
                    Object val = sampleData[r][c];
                    if (val instanceof Number) {
                        cell.setCellValue(((Number) val).doubleValue());
                    } else {
                        cell.setCellValue(val != null ? val.toString() : "");
                    }
                }
            }

            // Explicit safe column widths (Avoid AWT font metrics on headless Linux)
            int[] colWidths = {
                24 * 256, // Customer Name*
                18 * 256, // Mobile Number*
                28 * 256, // Address
                20 * 256, // Market Name
                18 * 256, // Collector Mobile
                30 * 256, // Loan Type (REGULAR/EMERGENCY)*
                34 * 256, // Disbursement Date (DD/MM/YYYY)*
                20 * 256, // Principal Amount*
                32 * 256, // Interest Type (FLAT / FLAT_DIRECT)
                28 * 256, // Interest Rate or Flat Amount*
                36 * 256, // Tenure (Days/Weeks/Months - 0 for Emergency)*
                26 * 256, // Frequency (EDI/EWI/EMI)*
                24 * 256, // Total Collected So Far
                32 * 256  // Last Payment Date (DD/MM/YYYY)
            };
            for (int i = 0; i < colWidths.length; i++) {
                sheet.setColumnWidth(i, colWidths[i]);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            log.error("Failed to generate onboarding template", e);
            throw new RuntimeException("Failed to generate template", e);
        }
    }

    @Override
    @Transactional
    public OnboardingSummaryResponse importExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file is empty");
        }

        List<String> errors = new ArrayList<>();
        List<String> createdLoanCodes = new ArrayList<>();
        int totalRows = 0;
        int successCount = 0;
        int failureCount = 0;

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            for (int r = 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                totalRows++;

                try {
                    OnboardSingleLoanRequest req = parseRow(row, r + 1);
                    Loan createdLoan = processSingleOnboarding(req);
                    createdLoanCodes.add(createdLoan.getLoanCode());
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    String msg = String.format("Row %d: %s", r + 1, e.getMessage());
                    log.warn("Onboarding row error: {}", msg);
                    errors.add(msg);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Excel file", e);
            throw new RuntimeException("Failed to read Excel file: " + e.getMessage(), e);
        }

        return OnboardingSummaryResponse.builder()
                .totalRows(totalRows)
                .successCount(successCount)
                .failureCount(failureCount)
                .errors(errors)
                .createdLoanCodes(createdLoanCodes)
                .build();
    }

    @Override
    @Transactional
    public LoanResponse onboardSingleLoan(OnboardSingleLoanRequest request) {
        Loan loan = processSingleOnboarding(request);
        return loanMapper.toResponse(loan);
    }

    private Loan processSingleOnboarding(OnboardSingleLoanRequest req) {
        // 1. Resolve or Create Market
        Market market = null;
        if (req.getMarketName() != null && !req.getMarketName().trim().isEmpty()) {
            String mName = req.getMarketName().trim();
            market = marketRepository.findByMarketNameIgnoreCase(mName).orElse(null);
            if (market == null) {
                String mCode = "MKT-" + (mName.length() >= 3 ? mName.substring(0, 3).toUpperCase() : mName.toUpperCase());
                market = Market.builder()
                        .marketName(mName)
                        .marketCode(mCode)
                        .status(MarketStatus.ACTIVE)
                        .build();
                market = marketRepository.save(market);
            }
        }

        // 2. Resolve Collector / Employee
        User collector = null;
        if (req.getCollectorMobile() != null && !req.getCollectorMobile().trim().isEmpty()) {
            String cMobile = req.getCollectorMobile().trim();
            collector = userRepository.findByMobileNumber(cMobile).orElse(null);
            if (collector != null && market != null) {
                // Ensure market assignment exists
                boolean assigned = assignmentRepository.existsByEmployeeIdAndMarketIdAndIsActiveTrue(collector.getId(), market.getId());
                if (!assigned) {
                    EmployeeMarketAssignment assignment = EmployeeMarketAssignment.builder()
                            .employee(collector)
                            .market(market)
                            .assignedDate(LocalDateTime.now())
                            .isActive(true)
                            .build();
                    assignmentRepository.save(assignment);
                }
            }
        }

        // 3. Resolve or Create Customer
        String mobile = req.getMobileNumber().trim();
        Customer customer = customerRepository.findByMobileNumber(mobile).orElse(null);
        if (customer == null) {
            String fullName = req.getCustomerName().trim();
            String[] parts = fullName.split("\\s+", 2);
            String firstName = parts[0];
            String lastName = parts.length > 1 ? parts[1] : "";

            customer = Customer.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .mobileNumber(mobile)
                    .market(market)
                    .status(CustomerStatus.ACTIVE)
                    .build();
            customer.setCustomerCode(generateCustomerCode(market));
            customer = customerRepository.save(customer);
        } else {
            if (market != null && customer.getMarket() == null) {
                customer.setMarket(market);
                customerRepository.save(customer);
            }
        }

        // 4. Generate Loan Code
        String typePrefix = req.getLoanType() == LoanType.EMERGENCY ? "ELN" : "RLN";
        String custPrefix = "NA";
        if (customer.getFirstName() != null && !customer.getFirstName().trim().isEmpty()) {
            String cName = customer.getFirstName().trim().toUpperCase();
            custPrefix = cName.length() >= 2 ? cName.substring(0, 2) : cName;
        }
        String marketPrefix = "NA";
        if (market != null && market.getMarketName() != null && !market.getMarketName().trim().isEmpty()) {
            String mName = market.getMarketName().trim().toUpperCase();
            marketPrefix = mName.length() >= 2 ? mName.substring(0, 2) : mName;
        }
        long customerLoanCount = loanRepository.countByCustomerId(customer.getId());
        String loanCode = String.format("%s-%s-%s-%d", typePrefix, custPrefix, marketPrefix, customerLoanCount + 1);

        LocalDate disDate = req.getDisbursementDate() != null ? req.getDisbursementDate() : LocalDate.now();

        boolean isEmergency = req.getLoanType() == LoanType.EMERGENCY;
        InterestType intType = isEmergency ? InterestType.FLAT : (req.getInterestType() != null ? req.getInterestType() : InterestType.FLAT);
        int loanTenure = isEmergency ? 0 : (req.getTenure() != null ? req.getTenure() : 0);
        RepaymentFrequency freq = isEmergency ? RepaymentFrequency.EDI : (req.getRepaymentFrequency() != null ? req.getRepaymentFrequency() : RepaymentFrequency.EDI);

        // 5. Create Active Loan
        Loan loan = Loan.builder()
                .customer(customer)
                .loanCode(loanCode)
                .loanType(req.getLoanType())
                .loanAmount(req.getPrincipalAmount())
                .approvedAmount(req.getPrincipalAmount())
                .disbursedAmount(req.getPrincipalAmount())
                .interestRate(req.getInterestRate())
                .interestType(intType)
                .tenure(loanTenure)
                .repaymentFrequency(freq)
                .loanStatus(LoanStatus.ACTIVE)
                .applicationDate(disDate.atTime(9, 0))
                .approvalDate(disDate.atTime(9, 0))
                .disbursementDate(disDate.atTime(9, 0))
                .createdBy(collector)
                .build();

        Loan savedLoan = loanRepository.save(loan);

        // 6. Generate Historical Schedule
        if (isEmergency) {
            // For emergency loans: Generate daily interest schedules from disbursement date up to today
            BigDecimal principal = savedLoan.getApprovedAmount();
            BigDecimal dailyInterest = principal
                    .multiply(savedLoan.getInterestRate())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            LocalDate currDate = disDate;
            LocalDate today = LocalDate.now();
            List<LoanRepaymentSchedule> emergencySchedules = new ArrayList<>();
            int instNum = 1;

            while (!currDate.isAfter(today)) {
                LoanRepaymentSchedule sched = LoanRepaymentSchedule.builder()
                        .loan(savedLoan)
                        .installmentNumber(instNum++)
                        .dueDate(currDate)
                        .principalAmount(BigDecimal.ZERO)
                        .interestAmount(dailyInterest)
                        .installmentAmount(dailyInterest)
                        .dueAmount(dailyInterest)
                        .paidAmount(BigDecimal.ZERO)
                        .outstandingAmount(dailyInterest)
                        .repaymentStatus(RepaymentStatus.PENDING)
                        .build();
                emergencySchedules.add(sched);
                currDate = currDate.plusDays(1);
            }

            if (emergencySchedules.isEmpty()) {
                // If disbursement date is today or future, at least 1 schedule item
                LoanRepaymentSchedule sched = LoanRepaymentSchedule.builder()
                        .loan(savedLoan)
                        .installmentNumber(1)
                        .dueDate(disDate)
                        .principalAmount(BigDecimal.ZERO)
                        .interestAmount(dailyInterest)
                        .installmentAmount(dailyInterest)
                        .dueAmount(dailyInterest)
                        .paidAmount(BigDecimal.ZERO)
                        .outstandingAmount(dailyInterest)
                        .repaymentStatus(RepaymentStatus.PENDING)
                        .build();
                emergencySchedules.add(sched);
            }

            scheduleRepository.saveAll(emergencySchedules);
        } else {
            repaymentScheduleService.generateSchedule(savedLoan.getId());
        }

        // 7. Apply Past Collections
        BigDecimal totalCollected = req.getTotalCollectedSoFar() != null ? req.getTotalCollectedSoFar() : BigDecimal.ZERO;
        if (totalCollected.compareTo(BigDecimal.ZERO) > 0) {
            List<LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(savedLoan.getId());
            BigDecimal remaining = totalCollected;
            List<LoanCollection> historicalCollections = new ArrayList<>();

            for (LoanRepaymentSchedule s : schedules) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    s.setPaidAmount(BigDecimal.ZERO);
                    s.setOutstandingAmount(s.getInstallmentAmount());
                    s.setRepaymentStatus(RepaymentStatus.PENDING);
                    continue;
                }

                BigDecimal due = s.getInstallmentAmount() != null ? s.getInstallmentAmount() : BigDecimal.ZERO;
                if (remaining.compareTo(due) >= 0) {
                    s.setPaidAmount(due);
                    s.setOutstandingAmount(BigDecimal.ZERO);
                    s.setRepaymentStatus(RepaymentStatus.PAID);
                    LocalDate pDate = s.getDueDate() != null ? s.getDueDate() : disDate;

                    LoanCollection col = LoanCollection.builder()
                            .loan(savedLoan)
                            .repaymentSchedule(s)
                            .receiptNumber("HIST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                            .collectedAmount(due)
                            .collectionDate(pDate.atTime(17, 0))
                            .collectionMode(CollectionMode.CASH)
                            .collectedBy(collector)
                            .build();
                    historicalCollections.add(col);

                    remaining = remaining.subtract(due);
                } else {
                    s.setPaidAmount(remaining);
                    s.setOutstandingAmount(due.subtract(remaining));
                    s.setRepaymentStatus(RepaymentStatus.PENDING);
                    LocalDate pDate = req.getLastPaymentDate() != null ? req.getLastPaymentDate() : (s.getDueDate() != null ? s.getDueDate() : disDate);

                    LoanCollection col = LoanCollection.builder()
                            .loan(savedLoan)
                            .repaymentSchedule(s)
                            .receiptNumber("HIST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                            .collectedAmount(remaining)
                            .collectionDate(pDate.atTime(17, 0))
                            .collectionMode(CollectionMode.CASH)
                            .collectedBy(collector)
                            .build();
                    historicalCollections.add(col);

                    remaining = BigDecimal.ZERO;
                }
            }

            scheduleRepository.saveAll(schedules);
            if (!historicalCollections.isEmpty()) {
                collectionRepository.saveAll(historicalCollections);
            }

            // Check if fully paid off
            if (isEmergency) {
                // Emergency loan is only closed if principal + all interest up to today was paid
                BigDecimal totalInterestDue = schedules.stream()
                        .map(s -> s.getInstallmentAmount() != null ? s.getInstallmentAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalRequiredForClosure = savedLoan.getApprovedAmount().add(totalInterestDue);
                if (totalCollected.compareTo(totalRequiredForClosure) >= 0) {
                    savedLoan.setLoanStatus(LoanStatus.CLOSED);
                    savedLoan = loanRepository.save(savedLoan);
                }
            } else {
                BigDecimal totalScheduleDue = schedules.stream()
                        .map(s -> s.getInstallmentAmount() != null ? s.getInstallmentAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (totalCollected.compareTo(totalScheduleDue) >= 0) {
                    savedLoan.setLoanStatus(LoanStatus.CLOSED);
                    savedLoan = loanRepository.save(savedLoan);
                }
            }
        }

        return savedLoan;
    }

    private String generateCustomerCode(Market market) {
        String marketPrefix = "NA";
        long count = 0;
        if (market != null && market.getMarketName() != null && !market.getMarketName().trim().isEmpty()) {
            String mName = market.getMarketName().trim().toUpperCase();
            marketPrefix = mName.length() >= 2 ? mName.substring(0, 2) : mName;
            count = customerRepository.countByMarketId(market.getId());
        } else {
            count = customerRepository.count();
        }
        return String.format("CUST-%s-%d", marketPrefix, count + 1);
    }

    private OnboardSingleLoanRequest parseRow(Row row, int rowNum) {
        String custName = getCellString(row.getCell(0));
        if (custName == null || custName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer Name is required");
        }

        String mobile = getCellString(row.getCell(1));
        if (mobile == null || mobile.trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile Number is required");
        }
        mobile = cleanMobileNumber(mobile);

        String address = getCellString(row.getCell(2));
        String marketName = getCellString(row.getCell(3));
        String collectorMobile = cleanMobileNumber(getCellString(row.getCell(4)));

        String loanTypeStr = getCellString(row.getCell(5));
        LoanType loanType = LoanType.REGULAR;
        if (loanTypeStr != null) {
            String lt = loanTypeStr.trim().toUpperCase();
            if (lt.contains("ELN") || lt.contains("EMERGENCY")) {
                loanType = LoanType.EMERGENCY;
            }
        }

        LocalDate disDate = parseDate(row.getCell(6));
        if (disDate == null) {
            throw new IllegalArgumentException("Disbursement Date is invalid or missing");
        }

        BigDecimal principal = getCellBigDecimal(row.getCell(7));
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Principal Amount must be greater than 0");
        }

        // Support both 14-column (with Interest Type at col 8) and 13-column layouts
        InterestType intType = InterestType.FLAT;
        BigDecimal interestRate = BigDecimal.ZERO;
        Integer tenure = 0;
        RepaymentFrequency freq = RepaymentFrequency.EDI;
        BigDecimal collected = BigDecimal.ZERO;
        LocalDate lastPaymentDate = null;

        String col8Str = getCellString(row.getCell(8));
        boolean hasInterestTypeCol = col8Str != null && (col8Str.toUpperCase().contains("FLAT") || col8Str.toUpperCase().contains("DIRECT"));

        if (hasInterestTypeCol) {
            if (col8Str.toUpperCase().contains("DIRECT")) {
                intType = InterestType.FLAT_DIRECT;
            } else {
                intType = InterestType.FLAT;
            }
            interestRate = getCellBigDecimal(row.getCell(9));
            tenure = getCellInteger(row.getCell(10));
            String freqStr = getCellString(row.getCell(11));
            if (freqStr != null) {
                String fUpper = freqStr.trim().toUpperCase();
                if (fUpper.contains("WEEK") || fUpper.equals("EWI")) freq = RepaymentFrequency.EWI;
                else if (fUpper.contains("MONTH") || fUpper.equals("EMI")) freq = RepaymentFrequency.EMI;
            }
            collected = getCellBigDecimal(row.getCell(12));
            lastPaymentDate = parseDate(row.getCell(13));
        } else {
            // Legacy 13-column
            interestRate = getCellBigDecimal(row.getCell(8));
            tenure = getCellInteger(row.getCell(9));
            String freqStr = getCellString(row.getCell(10));
            if (freqStr != null) {
                String fUpper = freqStr.trim().toUpperCase();
                if (fUpper.contains("WEEK") || fUpper.equals("EWI")) freq = RepaymentFrequency.EWI;
                else if (fUpper.contains("MONTH") || fUpper.equals("EMI")) freq = RepaymentFrequency.EMI;
            }
            collected = getCellBigDecimal(row.getCell(11));
            lastPaymentDate = parseDate(row.getCell(12));
        }

        if (interestRate == null) {
            interestRate = BigDecimal.ZERO;
        }

        if (loanType == LoanType.REGULAR) {
            if (tenure == null || tenure <= 0) {
                throw new IllegalArgumentException("Tenure must be greater than 0 for regular loans");
            }
        } else {
            tenure = 0;
            freq = RepaymentFrequency.EDI;
            intType = InterestType.FLAT;
        }

        if (collected == null) {
            collected = BigDecimal.ZERO;
        }

        return OnboardSingleLoanRequest.builder()
                .customerName(custName)
                .mobileNumber(mobile)
                .address(address)
                .marketName(marketName)
                .collectorMobile(collectorMobile)
                .loanType(loanType)
                .disbursementDate(disDate)
                .principalAmount(principal)
                .interestRate(interestRate)
                .interestType(intType)
                .tenure(tenure)
                .repaymentFrequency(freq)
                .totalCollectedSoFar(collected)
                .lastPaymentDate(lastPaymentDate)
                .build();
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String str = getCellString(cell);
                if (str != null && !str.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate().toString();
            }
            double val = cell.getNumericCellValue();
            if (val == (long) val) {
                return String.valueOf((long) val);
            }
            return String.valueOf(val);
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        return null;
    }

    private BigDecimal getCellBigDecimal(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                String clean = cell.getStringCellValue().replaceAll("[^0-9.]", "");
                if (clean.isEmpty()) return null;
                return new BigDecimal(clean).setScale(2, RoundingMode.HALF_UP);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private Integer getCellInteger(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                String clean = cell.getStringCellValue().replaceAll("[^0-9]", "");
                if (clean.isEmpty()) return null;
                return Integer.parseInt(clean);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private LocalDate parseDate(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String str = getCellString(cell);
        if (str == null || str.trim().isEmpty()) return null;
        str = str.trim();

        // Try standard formats: DD/MM/YYYY, YYYY-MM-DD, DD-MM-YYYY
        String[] patterns = {"dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy", "d/M/yyyy", "d-M-yyyy"};
        for (String p : patterns) {
            try {
                return LocalDate.parse(str, DateTimeFormatter.ofPattern(p));
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private String cleanMobileNumber(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() > 10 && digits.startsWith("91")) {
            digits = digits.substring(2);
        }
        return digits;
    }
}
