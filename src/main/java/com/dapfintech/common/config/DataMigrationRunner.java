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

@Component
@RequiredArgsConstructor
@Slf4j
public class DataMigrationRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final LoanRepository loanRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Data Migration for Sequential IDs...");

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
        long custCount = 0;
        for (Customer cust : customers) {
            if (cust.getCustomerCode() == null || !cust.getCustomerCode().startsWith("DAP-CUST-")) {
                custCount++;
                cust.setCustomerCode(String.format("DAP-CUST-%03d", custCount));
                customerRepository.save(cust);
                log.info("Migrated customer: {} -> {}", cust.getFirstName(), cust.getCustomerCode());
            } else {
                custCount++;
            }
        }

        // Migrate Loans
        List<Loan> loans = loanRepository.findAll();
        for (Customer cust : customers) {
            long loanCount = 0;
            for (Loan loan : loans) {
                if (loan.getCustomer() != null && loan.getCustomer().getId().equals(cust.getId())) {
                    if (loan.getLoanCode() == null || loan.getLoanCode().isEmpty()) {
                        loanCount++;
                        loan.setLoanCode(String.format("%s-L%03d", cust.getCustomerCode(), loanCount));
                        loanRepository.save(loan);
                        log.info("Migrated loan: {} -> {}", loan.getId(), loan.getLoanCode());
                    } else {
                        loanCount++;
                    }
                }
            }
        }

        log.info("Data Migration for Sequential IDs completed successfully.");
    }
}
