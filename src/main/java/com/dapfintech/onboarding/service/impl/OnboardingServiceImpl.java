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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
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
import com.dapfintech.employee.dto.DayBookTransactionRequest;
import com.dapfintech.employee.entity.MarketDayBook;
import com.dapfintech.employee.enums.DayBookStatus;
import com.dapfintech.employee.repository.MarketDayBookRepository;
import com.dapfintech.employee.service.DayBookService;
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
    private final PlatformTransactionManager transactionManager;
    private final DayBookService dayBookService;
    private final MarketDayBookRepository marketDayBookRepository;

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
                "Phone no*",
                "Name*",
                "Loan Type (REGULAR/EMERGENCY)*",
                "Daily Collection*",
                "No of Days*",
                "Total Loan amount*",
                "Disbursed amount*",
                "Total Amount To Be Paid",
                "EMI Due Days",
                "Balance Required Till Date",
                "Gap Till Date",
                "Loan Issue Date (DD/MM/YYYY)*",
                "Loan Close date (DD/MM/YYYY)",
                "Received Amount",
                "Bal Amount"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(28);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Realistic sample rows based on client business format
            Object[][] sampleData = {
                {"9876543210", "Rahul Sharma", "REGULAR", 100, 100, 10000, 8500, 10000, 60, 6000, 0, "15/07/2026", "23/10/2026", 6000, 4000},
                {"9876543211", "Amit Verma", "REGULAR", 200, 100, 20000, 17000, 20000, 50, 10000, 2000, "01/08/2026", "09/11/2026", 8000, 12000},
                {"9876543212", "Suresh Kumar", "EMERGENCY", 50, 0, 5000, 5000, 5000, 30, 1500, 0, "10/08/2026", "", 1500, 5000}
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

            // Explicit column widths
            int[] colWidths = {
                18 * 256, // Phone no*
                24 * 256, // Name*
                32 * 256, // Loan Type (REGULAR/EMERGENCY)*
                20 * 256, // Daily Collection*
                16 * 256, // No of Days*
                24 * 256, // Total Loan amount*
                22 * 256, // Disbursed amount*
                26 * 256, // Total Amount To Be Paid
                18 * 256, // EMI Due Days
                26 * 256, // Balance Required Till Date
                16 * 256, // Gap Till Date
                30 * 256, // Loan Issue Date (DD/MM/YYYY)*
                30 * 256, // Loan Close date (DD/MM/YYYY)
                20 * 256, // Received Amount
                16 * 256  // Bal Amount
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
    public OnboardingSummaryResponse importExcel(MultipartFile file) {
        return importExcel(file, null, null, null);
    }

    @Override
    public OnboardingSummaryResponse importExcel(MultipartFile file, UUID defaultMarketId, String defaultMarketName, LocalDate asOfDate) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file is empty");
        }

        // 1. Resolve Target Market if provided
        Market targetMarket = null;
        if (defaultMarketId != null) {
            targetMarket = marketRepository.findById(defaultMarketId).orElse(null);
        }
        if (targetMarket == null && defaultMarketName != null && !defaultMarketName.trim().isEmpty()) {
            String mName = defaultMarketName.trim();
            targetMarket = marketRepository.findByMarketNameIgnoreCase(mName).orElse(null);
            if (targetMarket == null) {
                String mCode = "MKT-" + (mName.length() >= 3 ? mName.substring(0, 3).toUpperCase() : mName.toUpperCase());
                targetMarket = Market.builder()
                        .marketName(mName)
                        .marketCode(mCode)
                        .status(MarketStatus.ACTIVE)
                        .build();
                targetMarket = marketRepository.save(targetMarket);
            }
        }

        // 2. Resolve Effective As-Of Date (defaults to 2026-09-29)
        final LocalDate effectiveAsOfDate = asOfDate != null ? asOfDate : LocalDate.of(2026, 9, 29);
        final Market resolvedMarket = targetMarket;

        List<String> errors = new ArrayList<>();
        List<String> createdLoanCodes = new ArrayList<>();
        int totalRows = 0;
        int successCount = 0;
        int failureCount = 0;

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            // Try row 0 as header; if it looks like a title row (few matches), try row 1
            Row headerRow0 = sheet.getRow(0);
            Row headerRow1 = sheet.getRow(1);
            ColumnMapping mapping0 = detectColumns(headerRow0);
            ColumnMapping mapping1 = detectColumns(headerRow1);
            int score0 = scoreMapping(mapping0);
            int score1 = scoreMapping(mapping1);
            ColumnMapping mapping;
            int dataStartRow;
            if (score1 > score0) {
                mapping = mapping1;
                dataStartRow = 2; // headers were at row 1, data starts at row 2
                log.info("Using row 1 as header row (score {} vs {}). receivedAmountCol={}", score1, score0, mapping.receivedAmountCol);
            } else {
                mapping = mapping0;
                dataStartRow = 1;
                log.info("Using row 0 as header row (score {} vs {}). receivedAmountCol={}", score0, score1, mapping.receivedAmountCol);
            }

            for (int r = dataStartRow; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                totalRows++;

                try {
                    OnboardSingleLoanRequest req = parseRow(row, r + 1, mapping, resolvedMarket, effectiveAsOfDate);
                    Loan createdLoan = transactionTemplate.execute(status -> processSingleOnboarding(req, resolvedMarket, effectiveAsOfDate));
                    if (createdLoan != null) {
                        createdLoanCodes.add(createdLoan.getLoanCode());
                        successCount++;
                    }
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
        LocalDate effectiveDate = request.getAsOfDate() != null ? request.getAsOfDate() : LocalDate.of(2026, 9, 29);
        Loan loan = processSingleOnboarding(request, null, effectiveDate);
        return loanMapper.toResponse(loan);
    }

    private Loan processSingleOnboarding(OnboardSingleLoanRequest req, Market defaultMarket, LocalDate asOfDate) {
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
        } else if (defaultMarket != null) {
            market = defaultMarket;
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

        // Fallback: if no collector specified, use the market's active assigned employee
        if (collector == null && market != null) {
            List<EmployeeMarketAssignment> assignments = assignmentRepository.findByMarketIdAndIsActiveTrue(market.getId());
            if (!assignments.isEmpty()) {
                collector = assignments.get(0).getEmployee();
                log.info("No collectorMobile in Excel row — using market employee {} for daybook", collector.getId());
            }
        }

        // 3. Resolve or Create Customer
        Customer customer = null;
        if (req.getCustomerId() != null) {
            customer = customerRepository.findById(req.getCustomerId()).orElse(null);
        }
        if (customer == null) {
            String mobile = req.getMobileNumber().trim();
            customer = customerRepository.findByMobileNumber(mobile).orElse(null);
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
            }
        }
        if (market == null && customer.getMarket() != null) {
            market = customer.getMarket();
        } else if (market != null && customer.getMarket() == null) {
            customer.setMarket(market);
            customerRepository.save(customer);
        }
        if (collector == null && market != null) {
            List<EmployeeMarketAssignment> assignments = assignmentRepository.findByMarketIdAndIsActiveTrue(market.getId());
            if (!assignments.isEmpty()) {
                collector = assignments.get(0).getEmployee();
            }
        }

        // 4. Generate Loan Code - find next unique number using DB count query (cache-safe)
        String typePrefix = req.getLoanType() == LoanType.EMERGENCY ? "ELN" : "RLN";
        String custPrefix = "NA";
        if (customer.getFirstName() != null && !customer.getFirstName().trim().isEmpty()) {
            String cName = customer.getFirstName().trim().toUpperCase().replaceAll("[^A-Z]", "");
            custPrefix = cName.length() >= 2 ? cName.substring(0, 2) : (cName.length() == 1 ? cName + "X" : "NA");
        }
        String marketPrefix = "NA";
        if (market != null && market.getMarketName() != null && !market.getMarketName().trim().isEmpty()) {
            String mName = market.getMarketName().trim().toUpperCase().replaceAll("[^A-Z]", "");
            marketPrefix = mName.length() >= 2 ? mName.substring(0, 2) : (mName.length() == 1 ? mName + "X" : "NA");
        }
        String baseCode = String.format("%s-%s-%s", typePrefix, custPrefix, marketPrefix);
        // Count how many loans already exist with this baseCode prefix to get next number
        long existingCount = loanRepository.countByLoanCodeStartingWith(baseCode + "-");
        int num = (int) existingCount + 1;
        String loanCode = baseCode + "-" + num;
        // Safety loop in case of gaps or race conditions
        while (loanRepository.existsByLoanCode(loanCode)) {
            num++;
            loanCode = baseCode + "-" + num;
        }

        LocalDate disDate = req.getDisbursementDate() != null ? req.getDisbursementDate() : LocalDate.now();

        // 3.5 Duplicate / Idempotency Protection:
        // Check if an identical loan was created for this customer within the last 10 minutes
        List<Loan> existingCustomerLoans = loanRepository.findByCustomerId(customer.getId());
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        for (Loan el : existingCustomerLoans) {
            if (el.getCreatedAt() != null && el.getCreatedAt().isAfter(tenMinutesAgo)
                    && el.getLoanType() == req.getLoanType()
                    && el.getApprovedAmount() != null && el.getApprovedAmount().compareTo(req.getPrincipalAmount()) == 0
                    && el.getDisbursementDate() != null && el.getDisbursementDate().toLocalDate().isEqual(disDate)) {
                log.info("Duplicate onboarding request detected for customer {} ({}) and loan {}. Returning existing loan code: {}",
                        customer.getFirstName(), customer.getMobileNumber(), el.getId(), el.getLoanCode());
                return el;
            }
        }

        boolean isEmergency = req.getLoanType() == LoanType.EMERGENCY;
        InterestType intType = req.getInterestType() != null ? req.getInterestType() : InterestType.FLAT_DIRECT;
        int loanTenure = isEmergency ? 0 : (req.getTenure() != null ? req.getTenure() : 0);
        RepaymentFrequency freq = isEmergency ? RepaymentFrequency.EDI : (req.getRepaymentFrequency() != null ? req.getRepaymentFrequency() : RepaymentFrequency.EDI);

        BigDecimal disbursed = (req.getDisbursedAmount() != null && req.getDisbursedAmount().compareTo(BigDecimal.ZERO) > 0)
                ? req.getDisbursedAmount()
                : req.getPrincipalAmount();

        // 5. Create Active Loan
        Loan loan = Loan.builder()
                .customer(customer)
                .loanCode(loanCode)
                .loanType(req.getLoanType())
                .loanAmount(req.getPrincipalAmount())
                .approvedAmount(req.getPrincipalAmount())
                .disbursedAmount(disbursed)
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
        LocalDate effectiveCutoff = asOfDate != null ? asOfDate : LocalDate.of(2026, 9, 29);
        if (isEmergency) {
            List<LoanRepaymentSchedule> emergencySchedules = new ArrayList<>();
            BigDecimal dailyInterest = savedLoan.getInterestRate() != null ? savedLoan.getInterestRate() : BigDecimal.ZERO;

            LocalDate today = LocalDate.now();
            LocalDate endDate = today;

            if (dailyInterest.compareTo(BigDecimal.ZERO) > 0) {
                LocalDate currDate = disDate;
                int instNum = 1;

                while (!currDate.isAfter(endDate)) {
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
                    emergencySchedules.add(LoanRepaymentSchedule.builder()
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
                            .build());
                }
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
                    remaining = remaining.subtract(due);
                } else {
                    s.setPaidAmount(remaining);
                    s.setOutstandingAmount(due.subtract(remaining));
                    s.setRepaymentStatus(RepaymentStatus.PENDING);
                    remaining = BigDecimal.ZERO;
                }
            }

            scheduleRepository.saveAll(schedules);

            // Exactly ONE consolidated collection entry on 29 Sep 2026 (cutoff) for total collected amount
            LoanRepaymentSchedule firstSchedule = schedules.isEmpty() ? null : schedules.get(0);
            LoanCollection consolidatedCol = LoanCollection.builder()
                    .loan(savedLoan)
                    .repaymentSchedule(firstSchedule)
                    .receiptNumber("HIST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .collectedAmount(totalCollected)
                    .collectionDate(effectiveCutoff.atTime(17, 0))
                    .collectionMode(CollectionMode.CASH)
                    .collectedBy(collector)
                    .remarks("Historical collection up to 29 Sep 2026")
                    .build();
            collectionRepository.save(consolidatedCol);

            // Also update the employee's DayBook for effectiveCutoff so the ledger reflects the historical collection
            if (collector != null && totalCollected.compareTo(BigDecimal.ZERO) > 0) {
                try {
                    DayBookTransactionRequest dbReq = new DayBookTransactionRequest();
                    dbReq.setType("COLLECTIONS");
                    dbReq.setAmount(totalCollected);
                    dbReq.setRemarks("HIST: " + savedLoan.getLoanCode() + " - "
                            + savedLoan.getCustomer().getFirstName() + " " + (savedLoan.getCustomer().getLastName() != null ? savedLoan.getCustomer().getLastName() : "")
                            + " (up to " + effectiveCutoff + ")");
                    dayBookService.addTransactionForDate(
                            collector.getId(),
                            effectiveCutoff,
                            dbReq
                    );
                } catch (Exception e) {
                    // Non-fatal: daybook update failure does not block onboarding
                    log.warn("Could not update daybook for collector {} during onboarding: {}", collector.getId(), e.getMessage());
                }
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

        // Also record loan disbursement in DayBook on effectiveCutoff for ALL loans
        if (collector != null && disbursed.compareTo(BigDecimal.ZERO) > 0) {
            try {
                boolean isDayBookClosed = false;
                if (market != null) {
                    MarketDayBook mdb = marketDayBookRepository.findByMarketIdAndDate(market.getId(), effectiveCutoff).orElse(null);
                    if (mdb != null && mdb.getStatus() == DayBookStatus.CLOSED) {
                        isDayBookClosed = true;
                    }
                }
                if (!isDayBookClosed) {
                    DayBookTransactionRequest disReq = new DayBookTransactionRequest();
                    disReq.setType("LOANS_DISBURSED");
                    disReq.setAmount(disbursed);
                    disReq.setRemarks("New Loan: " + savedLoan.getCustomer().getFirstName() + " " + (savedLoan.getCustomer().getLastName() != null ? savedLoan.getCustomer().getLastName() : "")
                            + " (" + savedLoan.getLoanCode() + ")");
                    dayBookService.addTransactionForDate(
                            collector.getId(),
                            effectiveCutoff,
                            disReq
                    );
                } else {
                    log.info("Market daybook for market {} on {} is CLOSED. Skipping DayBook disbursement recording for loan {}.",
                            market != null ? market.getMarketName() : "N/A", effectiveCutoff, savedLoan.getLoanCode());
                }
            } catch (Exception e) {
                log.warn("Could not record loan disbursement in daybook for collector {}: {}", collector.getId(), e.getMessage());
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

    private static class ColumnMapping {
        int phoneCol = -1;
        int nameCol = -1;
        int loanTypeCol = -1;
        int dailyCollectionCol = -1;
        int tenureCol = -1;
        int totalLoanAmountCol = -1;
        int disbursedAmountCol = -1;
        int totalAmountCol = -1;
        int emiDueDaysCol = -1;
        int balanceRequiredCol = -1;
        int gapCol = -1;
        int issueDateCol = -1;
        int closeDateCol = -1;
        int receivedAmountCol = -1;
        int balanceAmountCol = -1;

        // Legacy template columns
        int marketCol = -1;
        int collectorCol = -1;
        int addressCol = -1;
        int principalCol = -1;
        int interestRateCol = -1;
        int interestTypeCol = -1;
        int frequencyCol = -1;
        int disbursedCol = -1;
        boolean isLegacyTemplate = false;
    }

    /** Score how many columns were detected - used to pick the best header row */
    private int scoreMapping(ColumnMapping m) {
        if (m == null) return 0;
        int score = 0;
        if (m.phoneCol != -1) score++;
        if (m.nameCol != -1) score++;
        if (m.loanTypeCol != -1) score++;
        if (m.dailyCollectionCol != -1) score++;
        if (m.tenureCol != -1) score++;
        if (m.issueDateCol != -1) score++;
        if (m.receivedAmountCol != -1) score++;
        if (m.balanceAmountCol != -1) score++;
        if (m.marketCol != -1) score++;
        if (m.collectorCol != -1) score++;
        return score;
    }

    private ColumnMapping detectColumns(Row headerRow) {
        ColumnMapping m = new ColumnMapping();
        if (headerRow == null) {
            m.phoneCol = 0;
            m.nameCol = 1;
            m.loanTypeCol = 2;
            m.dailyCollectionCol = 3;
            m.tenureCol = 4;
            m.totalLoanAmountCol = 5;
            m.disbursedAmountCol = 6;
            m.totalAmountCol = 7;
            m.emiDueDaysCol = 8;
            m.balanceRequiredCol = 9;
            m.gapCol = 10;
            m.issueDateCol = 11;
            m.closeDateCol = 12;
            m.receivedAmountCol = 13;
            m.balanceAmountCol = 14;
            return m;
        }

        int lastCell = headerRow.getLastCellNum();
        for (int c = 0; c < lastCell; c++) {
            Cell cell = headerRow.getCell(c);
            String raw = getCellString(cell);
            if (raw == null || raw.trim().isEmpty()) continue;
            String h = raw.toLowerCase().replaceAll("[^a-z0-9]", "");

            if (h.contains("phone") || h.contains("mobile") || h.contains("contact") || h.equals("loanno") || h.equals("loannumber")) {
                if (m.phoneCol == -1) m.phoneCol = c;
            } else if (h.equals("name") || h.equals("customername") || h.equals("clientname") || h.contains("borrower")) {
                m.nameCol = c;
            } else if (h.contains("loantype") || h.contains("regularem") || h.contains("emregul") || h.contains("regular") || h.contains("emergency") || h.equals("type")) {
                m.loanTypeCol = c;
            } else if (h.contains("daily") || h.equals("dailycollection") || h.equals("edi") || h.contains("installment")) {
                m.dailyCollectionCol = c;
            } else if (h.contains("noofdays") || h.equals("days") || h.equals("tenure") || h.contains("duration")) {
                m.tenureCol = c;
            } else if (h.contains("totalloanamount") || h.contains("totalloan")) {
                m.totalLoanAmountCol = c;
            } else if (h.contains("disbursedate") || (h.contains("disburse") && h.contains("date")) || h.contains("issue") || h.equals("loanissuedate") || h.equals("startdate")) {
                m.issueDateCol = c;
            } else if (h.contains("disbursed") || h.contains("disburse")) {
                m.disbursedAmountCol = c;
                m.disbursedCol = c;
            } else if (h.contains("totalamount") || h.contains("tobepaid") || h.equals("totalpayable") || h.equals("total")) {
                m.totalAmountCol = c;
            } else if (h.contains("emidue") || h.equals("duedays")) {
                m.emiDueDaysCol = c;
            } else if (h.contains("balancerequired") || h.contains("requiredtilldate") || h.equals("required")) {
                m.balanceRequiredCol = c;
            } else if (h.contains("gap")) {
                m.gapCol = c;
            } else if (h.contains("close") || h.contains("maturity") || h.equals("loanclosedate") || h.equals("enddate")) {
                m.closeDateCol = c;
            } else if (h.contains("received") || h.contains("collected") || h.equals("paidamount")) {
                m.receivedAmountCol = c;
            } else if (h.contains("balamount") || h.contains("balanceamount") || (h.startsWith("bal") && !h.contains("required")) || h.contains("outstanding")) {
                m.balanceAmountCol = c;
            } else if (h.contains("market")) {
                m.marketCol = c;
            } else if (h.contains("collector")) {
                m.collectorCol = c;
            } else if (h.contains("address")) {
                m.addressCol = c;
            } else if (h.contains("principal")) {
                m.principalCol = c;
            } else if (h.contains("interestrate")) {
                m.interestRateCol = c;
            } else if (h.contains("interesttype")) {
                m.interestTypeCol = c;
            } else if (h.contains("frequency")) {
                m.frequencyCol = c;
            }
        }

        // Check if legacy template
        if (m.principalCol != -1 && m.marketCol != -1) {
            m.isLegacyTemplate = true;
            return m;
        }

        // Positional fallbacks for missing columns in client format
        if (m.phoneCol == -1) m.phoneCol = 0;
        if (m.nameCol == -1) m.nameCol = 1;

        if (m.loanTypeCol == -1) {
            if (m.dailyCollectionCol == 3) {
                m.loanTypeCol = 2;
            } else if (m.dailyCollectionCol > 1) {
                m.loanTypeCol = m.dailyCollectionCol - 1;
            }
        }

        if (m.loanTypeCol == -1 && m.dailyCollectionCol == -1) {
            // Check row count / layout:
            if (lastCell >= 15) {
                m.loanTypeCol = 2;
                m.dailyCollectionCol = 3;
                if (m.tenureCol == -1) m.tenureCol = 4;
                if (m.totalLoanAmountCol == -1) m.totalLoanAmountCol = 5;
                if (m.disbursedAmountCol == -1) m.disbursedAmountCol = 6;
                if (m.totalAmountCol == -1) m.totalAmountCol = 7;
                if (m.emiDueDaysCol == -1) m.emiDueDaysCol = 8;
                if (m.balanceRequiredCol == -1) m.balanceRequiredCol = 9;
                if (m.gapCol == -1) m.gapCol = 10;
                if (m.issueDateCol == -1) m.issueDateCol = 11;
                if (m.closeDateCol == -1) m.closeDateCol = 12;
                if (m.receivedAmountCol == -1) m.receivedAmountCol = 13;
                if (m.balanceAmountCol == -1) m.balanceAmountCol = 14;
            } else if (lastCell == 14) {
                m.loanTypeCol = 2;
                m.dailyCollectionCol = 3;
                if (m.tenureCol == -1) m.tenureCol = 4;
                if (m.totalLoanAmountCol == -1) m.totalLoanAmountCol = 5;
                if (m.disbursedAmountCol == -1) m.disbursedAmountCol = 6;
                if (m.emiDueDaysCol == -1) m.emiDueDaysCol = 7;
                if (m.balanceRequiredCol == -1) m.balanceRequiredCol = 8;
                if (m.gapCol == -1) m.gapCol = 9;
                if (m.issueDateCol == -1) m.issueDateCol = 10;
                if (m.closeDateCol == -1) m.closeDateCol = 11;
                if (m.receivedAmountCol == -1) m.receivedAmountCol = 12;
                if (m.balanceAmountCol == -1) m.balanceAmountCol = 13;
            } else if (lastCell == 13) {
                m.loanTypeCol = 2;
                m.dailyCollectionCol = 3;
                if (m.tenureCol == -1) m.tenureCol = 4;
                if (m.totalAmountCol == -1) m.totalAmountCol = 5;
                if (m.emiDueDaysCol == -1) m.emiDueDaysCol = 6;
                if (m.balanceRequiredCol == -1) m.balanceRequiredCol = 7;
                if (m.gapCol == -1) m.gapCol = 8;
                if (m.issueDateCol == -1) m.issueDateCol = 9;
                if (m.closeDateCol == -1) m.closeDateCol = 10;
                if (m.receivedAmountCol == -1) m.receivedAmountCol = 11;
                if (m.balanceAmountCol == -1) m.balanceAmountCol = 12;
            } else {
                // 12-column layout without Loan Type
                m.dailyCollectionCol = 2;
                if (m.tenureCol == -1) m.tenureCol = 3;
                if (m.totalAmountCol == -1) m.totalAmountCol = 4;
                if (m.emiDueDaysCol == -1) m.emiDueDaysCol = 5;
                if (m.balanceRequiredCol == -1) m.balanceRequiredCol = 6;
                if (m.gapCol == -1) m.gapCol = 7;
                if (m.issueDateCol == -1) m.issueDateCol = 8;
                if (m.closeDateCol == -1) m.closeDateCol = 9;
                if (m.receivedAmountCol == -1) m.receivedAmountCol = 10;
                if (m.balanceAmountCol == -1) m.balanceAmountCol = 11;
            }
        } else {
            if (m.dailyCollectionCol == -1) m.dailyCollectionCol = m.loanTypeCol != -1 ? m.loanTypeCol + 1 : 2;
            if (m.tenureCol == -1) m.tenureCol = m.dailyCollectionCol + 1;
            if (m.totalLoanAmountCol == -1 && m.totalAmountCol == -1) m.totalAmountCol = m.tenureCol + 1;
            if (m.issueDateCol == -1) m.issueDateCol = m.tenureCol + 5;
            if (m.receivedAmountCol == -1) m.receivedAmountCol = m.issueDateCol + 2;
        }

        return m;
    }

    private OnboardSingleLoanRequest parseRow(Row row, int rowNum, ColumnMapping m, Market defaultMarket, LocalDate asOfDate) {
        if (m.isLegacyTemplate) {
            return parseLegacyRow(row, rowNum, m, defaultMarket, asOfDate);
        }

        String custName = m.nameCol != -1 ? getCellString(row.getCell(m.nameCol)) : null;
        if (custName == null || custName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer Name is required");
        }

        String phoneRaw = m.phoneCol != -1 ? getCellString(row.getCell(m.phoneCol)) : null;
        String mobile = cleanMobileNumber(phoneRaw);
        if (mobile.isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        LoanType loanType = LoanType.REGULAR;
        if (m.loanTypeCol != -1) {
            String ltStr = getCellString(row.getCell(m.loanTypeCol));
            if (ltStr != null) {
                String lt = ltStr.trim().toUpperCase();
                if (lt.contains("ELN") || lt.contains("EMERGENCY")) {
                    loanType = LoanType.EMERGENCY;
                }
            }
        }
        if (loanType == LoanType.REGULAR) {
            int lastC = row.getLastCellNum();
            for (int ci = 0; ci < lastC; ci++) {
                String cVal = getCellString(row.getCell(ci));
                if (cVal != null) {
                    String u = cVal.trim().toUpperCase();
                    if (u.equals("EMERGENCY") || u.startsWith("ELN")) {
                        loanType = LoanType.EMERGENCY;
                        break;
                    }
                }
            }
        }

        BigDecimal dailyCollection = m.dailyCollectionCol != -1 ? getCellBigDecimal(row.getCell(m.dailyCollectionCol)) : null;
        Integer tenure = m.tenureCol != -1 ? getCellInteger(row.getCell(m.tenureCol)) : null;
        BigDecimal totalLoanAmount = m.totalLoanAmountCol != -1 ? getCellBigDecimal(row.getCell(m.totalLoanAmountCol)) : null;
        BigDecimal disbursedAmount = m.disbursedAmountCol != -1 ? getCellBigDecimal(row.getCell(m.disbursedAmountCol)) : null;
        BigDecimal totalAmountColVal = m.totalAmountCol != -1 ? getCellBigDecimal(row.getCell(m.totalAmountCol)) : null;

        BigDecimal totalAmount = totalLoanAmount != null ? totalLoanAmount : totalAmountColVal;

        // Auto-calculate missing math parameters
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            if (dailyCollection != null && tenure != null && tenure > 0) {
                totalAmount = dailyCollection.multiply(BigDecimal.valueOf(tenure));
            } else if (dailyCollection != null && dailyCollection.compareTo(BigDecimal.ZERO) > 0) {
                totalAmount = dailyCollection.multiply(BigDecimal.valueOf(100));
            } else {
                totalAmount = BigDecimal.valueOf(10000);
            }
        }

        if (disbursedAmount == null || disbursedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            disbursedAmount = totalAmount;
        }

        if (loanType == LoanType.EMERGENCY) {
            tenure = 0;
        } else {
            if (tenure == null || tenure <= 0) {
                if (dailyCollection != null && dailyCollection.compareTo(BigDecimal.ZERO) > 0) {
                    tenure = totalAmount.divide(dailyCollection, 0, RoundingMode.HALF_UP).intValue();
                } else {
                    tenure = 100;
                }
            }
        }

        if (dailyCollection == null || dailyCollection.compareTo(BigDecimal.ZERO) <= 0) {
            if (tenure != null && tenure > 0) {
                dailyCollection = totalAmount.divide(BigDecimal.valueOf(tenure), 2, RoundingMode.HALF_UP);
            } else {
                dailyCollection = BigDecimal.ZERO;
            }
        }

        LocalDate disDate = m.issueDateCol != -1 ? parseDate(row.getCell(m.issueDateCol)) : null;
        if (disDate == null) {
            disDate = LocalDate.now();
        }

        BigDecimal received = m.receivedAmountCol != -1 ? getCellBigDecimal(row.getCell(m.receivedAmountCol)) : BigDecimal.ZERO;
        if (received == null) {
            received = BigDecimal.ZERO;
        }

        LocalDate effectiveCutoff = asOfDate != null ? asOfDate : LocalDate.of(2026, 9, 29);

        LocalDate lastPaymentDate = null;
        if (m.closeDateCol != -1 && received.compareTo(totalAmount) >= 0) {
            lastPaymentDate = parseDate(row.getCell(m.closeDateCol));
        }
        if (lastPaymentDate == null && received.compareTo(BigDecimal.ZERO) > 0) {
            if (dailyCollection.compareTo(BigDecimal.ZERO) > 0) {
                int paidDays = received.divide(dailyCollection, 0, RoundingMode.DOWN).intValue();
                lastPaymentDate = disDate.plusDays(Math.max(0, paidDays - 1));
                if (lastPaymentDate.isAfter(effectiveCutoff)) {
                    lastPaymentDate = effectiveCutoff;
                }
            } else {
                lastPaymentDate = disDate.isAfter(effectiveCutoff) ? disDate : effectiveCutoff;
            }
        }

        String address = m.addressCol != -1 ? getCellString(row.getCell(m.addressCol)) : "";
        String marketName = m.marketCol != -1 ? getCellString(row.getCell(m.marketCol)) : "";
        if ((marketName == null || marketName.trim().isEmpty()) && defaultMarket != null) {
            marketName = defaultMarket.getMarketName();
        }
        String collectorMobile = m.collectorCol != -1 ? cleanMobileNumber(getCellString(row.getCell(m.collectorCol))) : "";

        BigDecimal interestRate = loanType == LoanType.EMERGENCY ? dailyCollection : BigDecimal.ZERO;

        return OnboardSingleLoanRequest.builder()
                .customerName(custName)
                .mobileNumber(mobile)
                .address(address)
                .marketName(marketName)
                .collectorMobile(collectorMobile)
                .loanType(loanType)
                .disbursementDate(disDate)
                .principalAmount(totalAmount)
                .disbursedAmount(disbursedAmount)
                .interestRate(interestRate)
                .interestType(InterestType.FLAT_DIRECT)
                .tenure(tenure)
                .repaymentFrequency(RepaymentFrequency.EDI)
                .totalCollectedSoFar(received)
                .lastPaymentDate(lastPaymentDate)
                .asOfDate(effectiveCutoff)
                .build();
    }

    private OnboardSingleLoanRequest parseLegacyRow(Row row, int rowNum, ColumnMapping m, Market defaultMarket, LocalDate asOfDate) {
        String custName = getCellString(row.getCell(m.nameCol));
        if (custName == null || custName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer Name is required");
        }

        String mobile = cleanMobileNumber(getCellString(row.getCell(m.phoneCol)));
        if (mobile.isEmpty()) {
            throw new IllegalArgumentException("Mobile Number is required");
        }

        String address = m.addressCol != -1 ? getCellString(row.getCell(m.addressCol)) : "";
        String marketName = m.marketCol != -1 ? getCellString(row.getCell(m.marketCol)) : "";
        if ((marketName == null || marketName.trim().isEmpty()) && defaultMarket != null) {
            marketName = defaultMarket.getMarketName();
        }
        String collectorMobile = m.collectorCol != -1 ? cleanMobileNumber(getCellString(row.getCell(m.collectorCol))) : "";

        LoanType loanType = LoanType.REGULAR;
        if (m.loanTypeCol != -1) {
            String ltStr = getCellString(row.getCell(m.loanTypeCol));
            if (ltStr != null && (ltStr.toUpperCase().contains("ELN") || ltStr.toUpperCase().contains("EMERGENCY"))) {
                loanType = LoanType.EMERGENCY;
            }
        }

        LocalDate disDate = m.issueDateCol != -1 ? parseDate(row.getCell(m.issueDateCol)) : LocalDate.now();
        if (disDate == null) disDate = LocalDate.now();

        BigDecimal principal = m.principalCol != -1 ? getCellBigDecimal(row.getCell(m.principalCol)) : BigDecimal.valueOf(10000);
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            principal = BigDecimal.valueOf(10000);
        }

        InterestType intType = InterestType.FLAT;
        if (m.interestTypeCol != -1) {
            String itStr = getCellString(row.getCell(m.interestTypeCol));
            if (itStr != null && itStr.toUpperCase().contains("DIRECT")) {
                intType = InterestType.FLAT_DIRECT;
            }
        }

        BigDecimal interestRate = m.interestRateCol != -1 ? getCellBigDecimal(row.getCell(m.interestRateCol)) : BigDecimal.ZERO;
        if (interestRate == null) interestRate = BigDecimal.ZERO;

        Integer tenure = m.tenureCol != -1 ? getCellInteger(row.getCell(m.tenureCol)) : 100;
        if (loanType == LoanType.EMERGENCY) tenure = 0;

        RepaymentFrequency freq = RepaymentFrequency.EDI;
        if (m.frequencyCol != -1) {
            String fStr = getCellString(row.getCell(m.frequencyCol));
            if (fStr != null) {
                String fUpper = fStr.trim().toUpperCase();
                if (fUpper.contains("WEEK") || fUpper.equals("EWI")) freq = RepaymentFrequency.EWI;
                else if (fUpper.contains("MONTH") || fUpper.equals("EMI")) freq = RepaymentFrequency.EMI;
            }
        }

        BigDecimal collected = m.receivedAmountCol != -1 ? getCellBigDecimal(row.getCell(m.receivedAmountCol)) : BigDecimal.ZERO;
        if (collected == null) collected = BigDecimal.ZERO;

        LocalDate lastPaymentDate = m.closeDateCol != -1 ? parseDate(row.getCell(m.closeDateCol)) : null;

        BigDecimal disbursed = m.disbursedCol != -1 ? getCellBigDecimal(row.getCell(m.disbursedCol)) : principal;
        if (disbursed == null || disbursed.compareTo(BigDecimal.ZERO) <= 0) disbursed = principal;

        LocalDate effectiveCutoff = asOfDate != null ? asOfDate : LocalDate.of(2026, 9, 29);

        return OnboardSingleLoanRequest.builder()
                .customerName(custName)
                .mobileNumber(mobile)
                .address(address)
                .marketName(marketName)
                .collectorMobile(collectorMobile)
                .loanType(loanType)
                .disbursementDate(disDate)
                .principalAmount(principal)
                .disbursedAmount(disbursed)
                .interestRate(interestRate)
                .interestType(intType)
                .tenure(tenure)
                .repaymentFrequency(freq)
                .totalCollectedSoFar(collected)
                .lastPaymentDate(lastPaymentDate)
                .asOfDate(effectiveCutoff)
                .build();
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
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
                return BigDecimal.valueOf((long) val).toPlainString();
            }
            return BigDecimal.valueOf(val).toPlainString();
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else if (cell.getCellType() == CellType.FORMULA) {
            try {
                return cell.getStringCellValue().trim();
            } catch (Exception e) {
                try {
                    double val = cell.getNumericCellValue();
                    if (val == (long) val) {
                        return BigDecimal.valueOf((long) val).toPlainString();
                    }
                    return BigDecimal.valueOf(val).toPlainString();
                } catch (Exception ignored) {
                    return "";
                }
            }
        }
        return null;
    }

    private BigDecimal getCellBigDecimal(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
        } else if (cell.getCellType() == CellType.STRING || cell.getCellType() == CellType.FORMULA) {
            try {
                String clean = getCellString(cell);
                if (clean == null) return null;
                clean = clean.replaceAll("[^0-9.]", "");
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
        } else if (cell.getCellType() == CellType.STRING || cell.getCellType() == CellType.FORMULA) {
            try {
                String clean = getCellString(cell);
                if (clean == null) return null;
                clean = clean.replaceAll("[^0-9]", "");
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
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            double num = cell.getNumericCellValue();
            if (num >= 35000 && num <= 65000) {
                try {
                    return DateUtil.getLocalDateTime(num).toLocalDate();
                } catch (Exception ignored) {}
            }
        }
        String str = getCellString(cell);
        if (str == null || str.trim().isEmpty()) return null;
        str = str.trim().replaceAll("\\s+", "");

        String[] patterns = {
            "dd/MM/yyyy", "d/M/yyyy", "dd/M/yyyy", "d/MM/yyyy",
            "dd-MM-yyyy", "d-M-yyyy", "dd-M-yyyy", "d-MM-yyyy",
            "yyyy-MM-dd", "yyyy/MM/dd", "dd.MM.yyyy", "d.M.yyyy",
            "dd/MM/yy", "d/M/yy", "dd-MM-yy", "d-M-yy"
        };
        for (String p : patterns) {
            try {
                return LocalDate.parse(str, DateTimeFormatter.ofPattern(p));
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String cleanMobileNumber(String raw) {
        if (raw == null) return "";
        int dot = raw.indexOf('.');
        if (dot != -1) {
            raw = raw.substring(0, dot);
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() > 10 && digits.startsWith("91")) {
            digits = digits.substring(2);
        }
        return digits;
    }
}
