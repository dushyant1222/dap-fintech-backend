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

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting Data Migration and Permission Seeding...");

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
            {"CAMERA_REQUIRED", "Security & Tracking", "Enforce camera capture during collection and KYC"}
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

        log.info("Data Migration for Sequential IDs completed successfully.");
    }
}
