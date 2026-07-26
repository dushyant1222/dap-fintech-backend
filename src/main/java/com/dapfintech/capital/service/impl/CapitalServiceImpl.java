package com.dapfintech.capital.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.capital.dto.CapitalSummaryResponse;
import com.dapfintech.capital.dto.CreateCapitalInRequest;
import com.dapfintech.capital.dto.CreateCashSettlementRequest;
import com.dapfintech.capital.dto.CreateExpenseRequest;
import com.dapfintech.capital.dto.PivotFilterRequest;
import com.dapfintech.capital.dto.PivotRowResponse;
import com.dapfintech.capital.dto.PivotTableResponse;
import com.dapfintech.capital.entity.CapitalIn;
import com.dapfintech.capital.entity.CashSettlement;
import com.dapfintech.capital.entity.Expense;
import com.dapfintech.capital.repository.CapitalInRepository;
import com.dapfintech.capital.repository.CashSettlementRepository;
import com.dapfintech.capital.repository.ExpenseRepository;
import com.dapfintech.capital.service.CapitalService;
import com.dapfintech.loan.entity.Loan;
import com.dapfintech.loan.entity.LoanCollection;
import com.dapfintech.loan.enums.LoanStatus;
import com.dapfintech.loan.repository.LoanCollectionRepository;
import com.dapfintech.loan.repository.LoanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CapitalServiceImpl implements CapitalService {

    private final CapitalInRepository capitalInRepository;
    private final ExpenseRepository expenseRepository;
    private final CashSettlementRepository cashSettlementRepository;
    private final LoanRepository loanRepository;
    private final LoanCollectionRepository loanCollectionRepository;
    private final UserRepository userRepository;

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Unauthenticated request");
        }
        String mobileNumber = authentication.getName();
        return userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    @Override
    @Transactional
    public CapitalIn addCapitalIn(CreateCapitalInRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Capital amount must be greater than zero");
        }
        User user = getAuthenticatedUser();
        CapitalIn capitalIn = CapitalIn.builder()
                .amount(request.getAmount())
                .capitalDate(LocalDateTime.now())
                .source(request.getSource() != null && !request.getSource().isBlank() ? request.getSource() : "Owner Capital")
                .remarks(request.getRemarks())
                .createdBy(user)
                .build();
        return capitalInRepository.save(capitalIn);
    }

    @Override
    public List<CapitalIn> getAllCapitalIn() {
        return capitalInRepository.findAllByOrderByCapitalDateDesc();
    }

    @Override
    @Transactional
    public Expense addExpense(CreateExpenseRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Expense amount must be greater than zero");
        }
        if (request.getCategory() == null) {
            throw new RuntimeException("Expense category is required");
        }
        User creator = getAuthenticatedUser();
        User employee = null;
        if (request.getEmployeeId() != null) {
            employee = userRepository.findById(request.getEmployeeId()).orElse(null);
        }

        Expense expense = Expense.builder()
                .category(request.getCategory())
                .amount(request.getAmount())
                .expenseDate(LocalDateTime.now())
                .remarks(request.getRemarks())
                .employee(employee)
                .createdBy(creator)
                .build();

        return expenseRepository.save(expense);
    }

    @Override
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAllByOrderByExpenseDateDesc();
    }

    @Override
    @Transactional
    public CashSettlement addCashSettlement(CreateCashSettlementRequest request) {
        if (request.getAmountSettled() == null || request.getAmountSettled().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Settlement amount must be greater than zero");
        }
        if (request.getEmployeeId() == null) {
            throw new RuntimeException("Employee ID is required for cash settlement");
        }

        User admin = getAuthenticatedUser();
        User employee = userRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        CashSettlement settlement = CashSettlement.builder()
                .employee(employee)
                .amountSettled(request.getAmountSettled())
                .settlementDate(LocalDateTime.now())
                .receivedByAdmin(admin)
                .remarks(request.getRemarks())
                .build();

        return cashSettlementRepository.save(settlement);
    }

    @Override
    public List<CashSettlement> getAllCashSettlements() {
        return cashSettlementRepository.findAllByOrderBySettlementDateDesc();
    }

    @Override
    public CapitalSummaryResponse getCapitalSummary() {
        BigDecimal totalCapital = capitalInRepository.getTotalCapitalInjected();
        if (totalCapital == null) totalCapital = BigDecimal.ZERO;

        BigDecimal totalExpenses = expenseRepository.getTotalExpenses();
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;

        BigDecimal totalSettled = cashSettlementRepository.getTotalSettledAmount();
        if (totalSettled == null) totalSettled = BigDecimal.ZERO;

        List<Loan> allLoans = loanRepository.findAll();
        BigDecimal totalDisbursed = BigDecimal.ZERO;
        BigDecimal marketBalance = BigDecimal.ZERO;

        for (Loan loan : allLoans) {
            if (loan.getLoanStatus() == LoanStatus.ACTIVE || loan.getLoanStatus() == LoanStatus.APPROVED || loan.getLoanStatus() == LoanStatus.CLOSED) {
                BigDecimal principal = loan.getApprovedAmount() != null ? loan.getApprovedAmount() : (loan.getLoanAmount() != null ? loan.getLoanAmount() : BigDecimal.ZERO);
                totalDisbursed = totalDisbursed.add(principal);
                if (loan.getLoanStatus() == LoanStatus.ACTIVE || loan.getLoanStatus() == LoanStatus.APPROVED) {
                    marketBalance = marketBalance.add(principal);
                }
            }
        }

        List<LoanCollection> allCollections = loanCollectionRepository.findAll();
        BigDecimal totalCollections = BigDecimal.ZERO;
        for (LoanCollection collection : allCollections) {
            if (collection.getCollectedAmount() != null) {
                totalCollections = totalCollections.add(collection.getCollectedAmount());
            }
        }

        BigDecimal balanceOnEmployees = totalCollections.subtract(totalSettled);
        if (balanceOnEmployees.compareTo(BigDecimal.ZERO) < 0) {
            balanceOnEmployees = BigDecimal.ZERO;
        }

        BigDecimal vaultAvailableCash = totalCapital.add(totalSettled).subtract(totalDisbursed.add(totalExpenses));

        BigDecimal dynamicMarketBalance = marketBalance.subtract(totalCollections);
        if (dynamicMarketBalance.compareTo(BigDecimal.ZERO) < 0) {
            dynamicMarketBalance = BigDecimal.ZERO;
        }

        BigDecimal expectedTotalReturn = marketBalance.multiply(BigDecimal.valueOf(1.10)).setScale(2, RoundingMode.HALF_UP).subtract(totalCollections);
        if (expectedTotalReturn.compareTo(BigDecimal.ZERO) < 0) {
            expectedTotalReturn = BigDecimal.ZERO;
        }

        return CapitalSummaryResponse.builder()
                .totalCapitalInjected(totalCapital)
                .totalDisbursedPrincipal(totalDisbursed)
                .balanceInMarket(dynamicMarketBalance)
                .totalCollections(totalCollections)
                .totalSettledCash(totalSettled)
                .balanceOnEmployees(balanceOnEmployees)
                .totalExpenses(totalExpenses)
                .vaultAvailableCash(vaultAvailableCash)
                .expectedTotalReturn(expectedTotalReturn)
                .build();
    }

    @Override
    public PivotTableResponse getPivotTable(PivotFilterRequest filter) {
        CapitalSummaryResponse summary = getCapitalSummary();
        List<PivotRowResponse> rows = new ArrayList<>();

        List<CapitalIn> capitalIns = capitalInRepository.findAllByOrderByCapitalDateDesc();
        List<Expense> expenses = expenseRepository.findAllByOrderByExpenseDateDesc();
        List<CashSettlement> settlements = cashSettlementRepository.findAllByOrderBySettlementDateDesc();
        List<LoanCollection> collections = loanCollectionRepository.findAllByOrderByCollectionDateDesc();
        List<Loan> loans = loanRepository.findAll();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        Map<String, PivotRowResponse.PivotRowResponseBuilder> map = new HashMap<>();

        if (filter == null) {
            filter = new PivotFilterRequest();
        }

        for (CapitalIn c : capitalIns) {
            if (c.getCapitalDate() != null && filterDate(c.getCapitalDate().toLocalDate(), filter)) {
                String key = c.getCapitalDate().format(formatter);
                map.putIfAbsent(key, PivotRowResponse.builder().groupLabel(key)
                        .capitalIn(BigDecimal.ZERO).disbursedAmount(BigDecimal.ZERO)
                        .marketBalance(BigDecimal.ZERO).collectedAmount(BigDecimal.ZERO)
                        .settledAmount(BigDecimal.ZERO).pendingEmployeeBalance(BigDecimal.ZERO)
                        .expenses(BigDecimal.ZERO).netEarnings(BigDecimal.ZERO));
            }
        }

        for (Loan l : loans) {
            if (l.getApplicationDate() != null && filterDate(l.getApplicationDate().toLocalDate(), filter)) {
                if (filter.getMarketId() == null || (l.getCustomer() != null && l.getCustomer().getMarket() != null && filter.getMarketId().equals(l.getCustomer().getMarket().getId()))) {
                    String key = l.getApplicationDate().format(formatter);
                    map.putIfAbsent(key, PivotRowResponse.builder().groupLabel(key)
                            .capitalIn(BigDecimal.ZERO).disbursedAmount(BigDecimal.ZERO)
                            .marketBalance(BigDecimal.ZERO).collectedAmount(BigDecimal.ZERO)
                            .settledAmount(BigDecimal.ZERO).pendingEmployeeBalance(BigDecimal.ZERO)
                            .expenses(BigDecimal.ZERO).netEarnings(BigDecimal.ZERO));
                }
            }
        }

        for (LoanCollection col : collections) {
            if (col.getCollectionDate() != null && filterDate(col.getCollectionDate().toLocalDate(), filter)) {
                if (filter.getEmployeeId() == null || (col.getCollectedBy() != null && filter.getEmployeeId().equals(col.getCollectedBy().getId()))) {
                    String key = col.getCollectionDate().format(formatter);
                    map.putIfAbsent(key, PivotRowResponse.builder().groupLabel(key)
                            .capitalIn(BigDecimal.ZERO).disbursedAmount(BigDecimal.ZERO)
                            .marketBalance(BigDecimal.ZERO).collectedAmount(BigDecimal.ZERO)
                            .settledAmount(BigDecimal.ZERO).pendingEmployeeBalance(BigDecimal.ZERO)
                            .expenses(BigDecimal.ZERO).netEarnings(BigDecimal.ZERO));
                }
            }
        }

        for (CashSettlement cs : settlements) {
            if (cs.getSettlementDate() != null && filterDate(cs.getSettlementDate().toLocalDate(), filter)) {
                if (filter.getEmployeeId() == null || (cs.getEmployee() != null && filter.getEmployeeId().equals(cs.getEmployee().getId()))) {
                    String key = cs.getSettlementDate().format(formatter);
                    map.putIfAbsent(key, PivotRowResponse.builder().groupLabel(key)
                            .capitalIn(BigDecimal.ZERO).disbursedAmount(BigDecimal.ZERO)
                            .marketBalance(BigDecimal.ZERO).collectedAmount(BigDecimal.ZERO)
                            .settledAmount(BigDecimal.ZERO).pendingEmployeeBalance(BigDecimal.ZERO)
                            .expenses(BigDecimal.ZERO).netEarnings(BigDecimal.ZERO));
                }
            }
        }

        for (Expense e : expenses) {
            if (e.getExpenseDate() != null && filterDate(e.getExpenseDate().toLocalDate(), filter)) {
                if (filter.getEmployeeId() == null || (e.getEmployee() != null && filter.getEmployeeId().equals(e.getEmployee().getId()))) {
                    String key = e.getExpenseDate().format(formatter);
                    map.putIfAbsent(key, PivotRowResponse.builder().groupLabel(key)
                            .capitalIn(BigDecimal.ZERO).disbursedAmount(BigDecimal.ZERO)
                            .marketBalance(BigDecimal.ZERO).collectedAmount(BigDecimal.ZERO)
                            .settledAmount(BigDecimal.ZERO).pendingEmployeeBalance(BigDecimal.ZERO)
                            .expenses(BigDecimal.ZERO).netEarnings(BigDecimal.ZERO));
                }
            }
        }

        for (String key : map.keySet()) {
            BigDecimal capIn = BigDecimal.ZERO;
            BigDecimal disbursed = BigDecimal.ZERO;
            BigDecimal collected = BigDecimal.ZERO;
            BigDecimal settled = BigDecimal.ZERO;
            BigDecimal exp = BigDecimal.ZERO;

            for (CapitalIn c : capitalIns) {
                if (c.getCapitalDate() != null && c.getCapitalDate().format(formatter).equals(key) && filterDate(c.getCapitalDate().toLocalDate(), filter)) {
                    capIn = capIn.add(c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO);
                }
            }
            for (Loan l : loans) {
                if (l.getApplicationDate() != null && l.getApplicationDate().format(formatter).equals(key) && filterDate(l.getApplicationDate().toLocalDate(), filter)) {
                    if (filter.getMarketId() == null || (l.getCustomer() != null && l.getCustomer().getMarket() != null && filter.getMarketId().equals(l.getCustomer().getMarket().getId()))) {
                        BigDecimal p = l.getApprovedAmount() != null ? l.getApprovedAmount() : (l.getLoanAmount() != null ? l.getLoanAmount() : BigDecimal.ZERO);
                        disbursed = disbursed.add(p);
                    }
                }
            }
            for (LoanCollection col : collections) {
                if (col.getCollectionDate() != null && col.getCollectionDate().format(formatter).equals(key) && filterDate(col.getCollectionDate().toLocalDate(), filter)) {
                    if (filter.getEmployeeId() == null || (col.getCollectedBy() != null && filter.getEmployeeId().equals(col.getCollectedBy().getId()))) {
                        collected = collected.add(col.getCollectedAmount() != null ? col.getCollectedAmount() : BigDecimal.ZERO);
                    }
                }
            }
            for (CashSettlement cs : settlements) {
                if (cs.getSettlementDate() != null && cs.getSettlementDate().format(formatter).equals(key) && filterDate(cs.getSettlementDate().toLocalDate(), filter)) {
                    if (filter.getEmployeeId() == null || (cs.getEmployee() != null && filter.getEmployeeId().equals(cs.getEmployee().getId()))) {
                        settled = settled.add(cs.getAmountSettled() != null ? cs.getAmountSettled() : BigDecimal.ZERO);
                    }
                }
            }
            for (Expense e : expenses) {
                if (e.getExpenseDate() != null && e.getExpenseDate().format(formatter).equals(key) && filterDate(e.getExpenseDate().toLocalDate(), filter)) {
                    if (filter.getEmployeeId() == null || (e.getEmployee() != null && filter.getEmployeeId().equals(e.getEmployee().getId()))) {
                        exp = exp.add(e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO);
                    }
                }
            }

            BigDecimal pendingEmp = collected.subtract(settled);
            if (pendingEmp.compareTo(BigDecimal.ZERO) < 0) pendingEmp = BigDecimal.ZERO;
            BigDecimal netEarn = collected.subtract(exp);

            rows.add(PivotRowResponse.builder()
                    .groupLabel(key)
                    .capitalIn(capIn)
                    .disbursedAmount(disbursed)
                    .marketBalance(disbursed.subtract(collected).max(BigDecimal.ZERO))
                    .collectedAmount(collected)
                    .settledAmount(settled)
                    .pendingEmployeeBalance(pendingEmp)
                    .expenses(exp)
                    .netEarnings(netEarn)
                    .build());
        }

        if (rows.isEmpty()) {
            rows.add(PivotRowResponse.builder()
                    .groupLabel("All Time Total")
                    .capitalIn(summary.getTotalCapitalInjected() != null ? summary.getTotalCapitalInjected() : BigDecimal.ZERO)
                    .disbursedAmount(summary.getTotalDisbursedPrincipal() != null ? summary.getTotalDisbursedPrincipal() : BigDecimal.ZERO)
                    .marketBalance(summary.getBalanceInMarket() != null ? summary.getBalanceInMarket() : BigDecimal.ZERO)
                    .collectedAmount(summary.getTotalCollections() != null ? summary.getTotalCollections() : BigDecimal.ZERO)
                    .settledAmount(summary.getTotalSettledCash() != null ? summary.getTotalSettledCash() : BigDecimal.ZERO)
                    .pendingEmployeeBalance(summary.getBalanceOnEmployees() != null ? summary.getBalanceOnEmployees() : BigDecimal.ZERO)
                    .expenses(summary.getTotalExpenses() != null ? summary.getTotalExpenses() : BigDecimal.ZERO)
                    .netEarnings((summary.getTotalCollections() != null ? summary.getTotalCollections() : BigDecimal.ZERO).subtract(summary.getTotalExpenses() != null ? summary.getTotalExpenses() : BigDecimal.ZERO))
                    .build());
        }

        return PivotTableResponse.builder()
                .summary(summary)
                .rows(rows)
                .build();
    }

    private boolean filterDate(LocalDate date, PivotFilterRequest filter) {
        if (date == null) return false;
        if (filter != null && filter.getStartDate() != null && date.isBefore(filter.getStartDate())) {
            return false;
        }
        if (filter != null && filter.getEndDate() != null && date.isAfter(filter.getEndDate())) {
            return false;
        }
        return true;
    }
}
