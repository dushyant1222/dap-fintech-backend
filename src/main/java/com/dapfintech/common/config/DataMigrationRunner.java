package com.dapfintech.common.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.customer.entity.Customer;
import com.dapfintech.customer.repository.CustomerRepository;
import com.dapfintech.loan.entity.Loan;
import com.dapfintech.loan.repository.LoanRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataMigrationRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final LoanRepository loanRepository;
    private final com.dapfintech.auth.repository.PermissionRepository permissionRepository;
    private final com.dapfintech.loan.repository.LoanCollectionRepository collectionRepository;
    private final com.dapfintech.employee.service.DayBookService dayBookService;
    private final com.dapfintech.market.repository.EmployeeMarketAssignmentRepository assignmentRepository;
    private final com.dapfintech.market.repository.MarketRepository marketRepository;
    private final com.dapfintech.employee.repository.DayBookTransactionRepository dayBookTransactionRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting Data Migration and Permission Seeding...");

        // Ensure Master Admin email is updated to singh.amitjadoun@gmail.com
        try {
            final String masterEmail = "singh.amitjadoun@gmail.com";
            List<User> masterAdmins = userRepository.findByRoleRoleName("MASTER_ADMIN");
            for (User ma : masterAdmins) {
                ma.setEmail(masterEmail);
                userRepository.save(ma);
                log.info("Updated MASTER_ADMIN {} email to {}", ma.getFullName(), masterEmail);
            }
            userRepository.findByMobileNumber("9311111335").ifPresent(u -> {
                u.setEmail(masterEmail);
                userRepository.save(u);
                log.info("Updated user 9311111335 email to {}", masterEmail);
            });
        } catch (Exception e) {
            log.warn("Could not auto-migrate master admin email: {}", e.getMessage());
        }

        // Seed Standard Permissions if missing
        String[][] standardPermissions = {
            {"DASHBOARD", "Dashboard", "Access Employee Dashboard overview and metrics"},
            {"CUSTOMERS", "Customers", "View assigned customer list and customer profiles"},
            {"CREATE_CUSTOMER", "Customers", "Register new customers in assigned markets"},
            {"EDIT_CUSTOMER", "Customers", "Edit customer details and KYC documents"},
            {"LOANS", "Loans", "View loan lists and detailed loan schedules"},
            {"CREATE_LOAN", "Loans", "Create new loan applications for customers"},
            {"APPROVE_LOAN", "Loans", "Disburse and approve eligible loans"},
            {"COLLECTIONS", "Collections", "View daily collection schedule and customer dues"},
            {"OFFLINE_COLLECTION", "Collections", "Collect EMI payments and issue instant receipts"},
            {"EXPENSES", "Accounting & Daybook", "Record daily field spends and expenses"},
            {"WALLET", "Accounting & Daybook", "Receive and send internal fund transfers"},
            {"CLOSE_LEDGER", "Accounting & Daybook", "Submit daybook ledger closure request to Admin"},
            {"ENQUIRIES", "Enquiry Management", "View and process customer market enquiries"},
            {"REPORTS", "Reports", "Access and view market summary and daily ledger reports"},
            {"GPS_REQUIRED", "Security & Tracking", "Enforce live GPS location verification during collections"},
            {"CAMERA_REQUIRED", "Security & Tracking", "Enforce camera capture during collection and KYC"},
            {"DATA_ONBOARDING", "Data Onboarding", "Onboard historical customer loans and spreadsheet bulk import"}
        };

        for (String[] p : standardPermissions) {
            if (permissionRepository.findByPermissionKey(p[0]).isEmpty()) {
                com.dapfintech.auth.entity.Permission perm = com.dapfintech.auth.entity.Permission.builder()
                        .permissionKey(p[0])
                        .moduleName(p[1])
                        .description(p[2])
                        .build();
                permissionRepository.save(perm);
                log.info("Seeded permission: {} ({})", p[0], p[1]);
            }
        }

        // Migrate Employees
        List<User> employees = userRepository.findByRoleRoleName("EMPLOYEE");
        long empCount = 0;
        for (User emp : employees) {
            if (emp.getEmployeeCode() == null || emp.getEmployeeCode().isEmpty()) {
                empCount++;
                emp.setEmployeeCode(String.format("DAP-EMP-%03d", empCount));
                userRepository.save(emp);
                log.info("Migrated employee: {} -> {}", emp.getFullName(), emp.getEmployeeCode());
            } else {
                empCount++;
            }
        }

        // Migrate Customers
        List<Customer> customers = customerRepository.findAll();
        for (Customer cust : customers) {
            String marketPrefix = "NA";
            if (cust.getMarket() != null && cust.getMarket().getMarketName() != null && !cust.getMarket().getMarketName().trim().isEmpty()) {
                String mName = cust.getMarket().getMarketName().trim().toUpperCase();
                marketPrefix = mName.length() >= 2 ? mName.substring(0, 2) : mName;
            }
            // Count existing customers in this market that already have the new format up to this point
            // This is just a migration script so we can just use an in-memory counter if we want, or a global counter.
            // Actually to prevent overlaps if some have been migrated, we should just assign them sequentially.
        }

        // We need a better way to count per market in memory to avoid 1000s of queries.
        java.util.Map<java.util.UUID, Long> marketCounters = new java.util.HashMap<>();
        long globalCounter = 0;
        
        for (Customer cust : customers) {
            if (cust.getCustomerCode() != null && !cust.getCustomerCode().isEmpty()) {
                continue;
            }
            String marketPrefix = "NA";
            long count = 0;
            if (cust.getMarket() != null && cust.getMarket().getMarketName() != null && !cust.getMarket().getMarketName().trim().isEmpty()) {
                String mName = cust.getMarket().getMarketName().trim().toUpperCase();
                marketPrefix = mName.length() >= 2 ? mName.substring(0, 2) : mName;
                count = marketCounters.getOrDefault(cust.getMarket().getId(), 0L);
                marketCounters.put(cust.getMarket().getId(), count + 1);
            } else {
                count = globalCounter++;
            }
            cust.setCustomerCode(String.format("CUST-%s-%d", marketPrefix, count + 1));
            customerRepository.save(cust);
            log.info("Migrated customer: {} -> {}", cust.getFirstName(), cust.getCustomerCode());
        }

        // Migrate Loans
        List<Loan> loans = loanRepository.findAll();
        for (Customer cust : customers) {
            long loanCount = 0;
            for (Loan loan : loans) {
                if (loan.getCustomer() != null && loan.getCustomer().getId().equals(cust.getId())) {
                    if (loan.getLoanCode() != null && !loan.getLoanCode().isEmpty()) {
                        loanCount++;
                        continue;
                    }
                    String typePrefix = loan.getLoanType() == com.dapfintech.loan.enums.LoanType.REGULAR ? "RLN" : "ELN";
                    String custPrefix = "NA";
                    if (cust.getFirstName() != null && !cust.getFirstName().trim().isEmpty()) {
                        String cName = cust.getFirstName().trim().toUpperCase();
                        custPrefix = cName.length() >= 2 ? cName.substring(0, 2) : cName;
                    }
                    String marketPrefix = "NA";
                    if (cust.getMarket() != null && cust.getMarket().getMarketName() != null && !cust.getMarket().getMarketName().trim().isEmpty()) {
                        String mName = cust.getMarket().getMarketName().trim().toUpperCase();
                        marketPrefix = mName.length() >= 2 ? mName.substring(0, 2) : mName;
                    }
                    
                    loan.setLoanCode(String.format("%s-%s-%s-%d", typePrefix, custPrefix, marketPrefix, loanCount + 1));
                    loanRepository.save(loan);
                    log.info("Migrated loan: {} -> {}", loan.getId(), loan.getLoanCode());
                    loanCount++;
                }
            }
        }

        // Consolidate historical collections dated before 2026-09-29 into a single entry on 2026-09-29
        try {
            List<com.dapfintech.loan.entity.LoanCollection> histCols = collectionRepository.findByReceiptNumberStartingWith("HIST-");
            if (!histCols.isEmpty()) {
                java.time.LocalDateTime targetTime = java.time.LocalDateTime.of(2026, 9, 29, 17, 0);
                java.util.Map<com.dapfintech.loan.entity.Loan, List<com.dapfintech.loan.entity.LoanCollection>> byLoan = histCols.stream()
                        .filter(c -> c.getLoan() != null)
                        .collect(java.util.stream.Collectors.groupingBy(com.dapfintech.loan.entity.LoanCollection::getLoan));

                for (java.util.Map.Entry<com.dapfintech.loan.entity.Loan, List<com.dapfintech.loan.entity.LoanCollection>> entry : byLoan.entrySet()) {
                    List<com.dapfintech.loan.entity.LoanCollection> cols = entry.getValue();
                    if (cols.size() > 1 || cols.stream().anyMatch(c -> c.getCollectionDate() != null && c.getCollectionDate().isBefore(java.time.LocalDateTime.of(2026, 9, 29, 0, 0)))) {
                        java.math.BigDecimal total = cols.stream()
                                .map(c -> c.getCollectedAmount() != null ? c.getCollectedAmount() : java.math.BigDecimal.ZERO)
                                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

                        com.dapfintech.loan.entity.LoanCollection keep = cols.get(0);
                        keep.setCollectedAmount(total);
                        keep.setCollectionDate(targetTime);
                        keep.setRemarks("Consolidated historical collection up to 29 Sep 2026");
                        collectionRepository.save(keep);

                        for (int i = 1; i < cols.size(); i++) {
                            collectionRepository.delete(cols.get(i));
                        }
                        log.info("Consolidated {} historical collections for loan {} into single 29 Sep entry of amount {}", cols.size(), entry.getKey().getLoanCode(), total);
                    }
                }
            }

            // Backfill collectedBy on all HIST- collections and sync employee daybooks for 2026-09-29
            java.time.LocalDate cutoffDate = java.time.LocalDate.of(2026, 9, 29);
            List<com.dapfintech.loan.entity.LoanCollection> allHist = collectionRepository.findByReceiptNumberStartingWith("HIST-");
            for (com.dapfintech.loan.entity.LoanCollection col : allHist) {
                if (col.getCollectedBy() == null && col.getLoan() != null && col.getLoan().getCustomer() != null && col.getLoan().getCustomer().getMarket() != null) {
                    java.util.UUID marketId = col.getLoan().getCustomer().getMarket().getId();
                    List<com.dapfintech.market.entity.EmployeeMarketAssignment> assigns = assignmentRepository.findByMarketIdAndIsActiveTrue(marketId);
                    if (!assigns.isEmpty()) {
                        col.setCollectedBy(assigns.get(0).getEmployee());
                        collectionRepository.save(col);
                    }
                }
            }

            // Auto-sync daybooks for all active employees for 2026-09-29
            List<User> allEmployees = userRepository.findByRoleRoleName("EMPLOYEE");
            for (User emp : allEmployees) {
                dayBookService.getOrCreateDayBook(emp.getId(), cutoffDate);
                dayBookService.getTransactions(emp.getId(), cutoffDate);
            }
        } catch (Exception e) {
            log.warn("Could not auto-consolidate historical collections or sync daybooks: {}", e.getMessage());
        }

        // Auto-heal onboarded loans, customer markets, and daybooks (especially loans disbursed from 2026-09-30 onwards)
        try {
            log.info("Starting auto-healing of onboarded loans, customer markets, and daybooks...");
            List<Loan> allLoans = loanRepository.findAll();
            List<com.dapfintech.market.entity.Market> activeMarkets = marketRepository.findAll();

            for (Loan l : allLoans) {
                if (l.getDisbursementDate() == null) continue;

                Customer c = l.getCustomer();
                if (c == null) continue;

                // 1. If customer has no market, resolve it
                if (c.getMarket() == null) {
                    com.dapfintech.market.entity.Market resolvedMarket = null;
                    if (l.getCreatedBy() != null) {
                        var asgs = assignmentRepository.findByEmployeeIdAndIsActiveTrue(l.getCreatedBy().getId());
                        if (!asgs.isEmpty()) resolvedMarket = asgs.get(0).getMarket();
                    }
                    if (resolvedMarket == null && c.getCreatedBy() != null) {
                        var asgs = assignmentRepository.findByEmployeeIdAndIsActiveTrue(c.getCreatedBy().getId());
                        if (!asgs.isEmpty()) resolvedMarket = asgs.get(0).getMarket();
                    }
                    if (resolvedMarket == null && !activeMarkets.isEmpty()) {
                        String code = l.getLoanCode() != null ? l.getLoanCode().toUpperCase() : "";
                        for (var m : activeMarkets) {
                            String mPrefix = m.getMarketName().length() >= 2 ? m.getMarketName().substring(0, 2).toUpperCase() : m.getMarketName().toUpperCase();
                            if (code.contains("-" + mPrefix + "-") || code.contains("-" + mPrefix)) {
                                resolvedMarket = m;
                                break;
                            }
                        }
                        if (resolvedMarket == null) {
                            resolvedMarket = activeMarkets.get(0);
                        }
                    }
                    if (resolvedMarket != null) {
                        c.setMarket(resolvedMarket);
                        customerRepository.save(c);
                        log.info("Auto-healed customer {} market to {}", c.getId(), resolvedMarket.getMarketName());
                    }
                }

                // 2. If loan has no createdBy, resolve it
                if (l.getCreatedBy() == null) {
                    User resolvedUser = null;
                    if (c.getCreatedBy() != null) {
                        resolvedUser = c.getCreatedBy();
                    } else if (c.getMarket() != null) {
                        var asgs = assignmentRepository.findByMarketIdAndIsActiveTrue(c.getMarket().getId());
                        if (!asgs.isEmpty()) resolvedUser = asgs.get(0).getEmployee();
                    }
                    if (resolvedUser == null) {
                        List<User> emps = userRepository.findByRoleRoleName("EMPLOYEE");
                        if (!emps.isEmpty()) resolvedUser = emps.get(0);
                    }
                    if (resolvedUser != null) {
                        l.setCreatedBy(resolvedUser);
                        loanRepository.save(l);
                        log.info("Auto-healed loan {} createdBy to {}", l.getLoanCode(), resolvedUser.getFullName());
                    }
                }

                // 3. For any loan disbursed after 2026-09-29, ensure a DayBookTransaction exists on its disbursement date
                java.time.LocalDate disDate = l.getDisbursementDate().toLocalDate();
                if (c.getMarket() != null && !disDate.isBefore(java.time.LocalDate.of(2026, 9, 30))) {
                    java.util.UUID mId = c.getMarket().getId();
                    java.time.LocalDateTime start = disDate.atStartOfDay();
                    java.time.LocalDateTime end = disDate.plusDays(1).atStartOfDay();

                    List<com.dapfintech.employee.entity.DayBookTransaction> existingTx =
                            dayBookTransactionRepository.findByMarketIdAndCreatedAtBetween(mId, start, end);

                    boolean hasTxForThisLoan = existingTx.stream().anyMatch(t ->
                            "LOANS_DISBURSED".equalsIgnoreCase(t.getType())
                                    && t.getRemarks() != null
                                    && l.getLoanCode() != null
                                    && t.getRemarks().contains(l.getLoanCode())
                    );

                    if (!hasTxForThisLoan) {
                        java.math.BigDecimal disAmt = l.getDisbursedAmount() != null ? l.getDisbursedAmount() : (l.getApprovedAmount() != null ? l.getApprovedAmount() : java.math.BigDecimal.ZERO);
                        if (disAmt.compareTo(java.math.BigDecimal.ZERO) > 0) {
                            com.dapfintech.employee.entity.DayBookTransaction tx = new com.dapfintech.employee.entity.DayBookTransaction();
                            tx.setMarketId(mId);
                            tx.setEmployeeId(l.getCreatedBy() != null ? l.getCreatedBy().getId() : (c.getCreatedBy() != null ? c.getCreatedBy().getId() : null));
                            if (tx.getEmployeeId() == null) {
                                var asgs = assignmentRepository.findByMarketIdAndIsActiveTrue(mId);
                                if (!asgs.isEmpty()) tx.setEmployeeId(asgs.get(0).getEmployee().getId());
                            }
                            if (tx.getEmployeeId() != null) {
                                tx.setType("LOANS_DISBURSED");
                                tx.setAmount(disAmt);
                                String custName = c.getFirstName() + (c.getLastName() != null ? " " + c.getLastName() : "");
                                tx.setRemarks("New Loan: " + custName + " (" + l.getLoanCode() + ")");
                                tx.setCreatedAt(l.getDisbursementDate());
                                dayBookTransactionRepository.save(tx);
                                log.info("Auto-created missing DayBookTransaction for loan {} on {} (amount {})", l.getLoanCode(), disDate, disAmt);
                            }
                        }
                    }

                    // Always trigger sync for this market and date
                    dayBookService.syncMarketDayBook(mId, disDate);
                    if (l.getCreatedBy() != null) {
                        dayBookService.getOrCreateDayBook(l.getCreatedBy().getId(), disDate);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Auto-healing of onboarded loans encountered error: {}", e.getMessage(), e);
        }

        log.info("Data Migration for Sequential IDs, Historical Collections, and DayBook Sync completed successfully.");
    }
}
