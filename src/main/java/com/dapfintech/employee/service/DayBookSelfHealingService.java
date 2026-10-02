package com.dapfintech.employee.service;

import com.dapfintech.capital.entity.InternalTransfer;
import com.dapfintech.capital.repository.InternalTransferRepository;
import com.dapfintech.employee.entity.DayBook;
import com.dapfintech.employee.entity.DayBookTransaction;
import com.dapfintech.employee.entity.MarketDayBook;
import com.dapfintech.employee.repository.DayBookRepository;
import com.dapfintech.employee.repository.DayBookTransactionRepository;
import com.dapfintech.employee.repository.MarketDayBookRepository;
import com.dapfintech.loan.repository.LoanRepository;
import com.dapfintech.market.entity.Market;
import com.dapfintech.market.repository.MarketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dapfintech.employee.enums.DayBookStatus;
import com.dapfintech.market.entity.EmployeeMarketAssignment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class DayBookSelfHealingService {

    @Autowired
    private DayBookTransactionRepository dayBookTransactionRepository;

    @Autowired
    private DayBookRepository dayBookRepository;

    @Autowired
    private InternalTransferRepository internalTransferRepository;

    @Autowired
    private MarketDayBookRepository marketDayBookRepository;

    @Autowired
    private MarketRepository marketRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private DayBookService dayBookService;

    @Autowired
    private com.dapfintech.market.repository.EmployeeMarketAssignmentRepository assignmentRepository;

    private static final Pattern LOAN_CODE_PATTERN = Pattern.compile("(?i)\\b(DAP-LN-[A-Z0-9-]+|[A-Z]{2,4}-[A-Z0-9]+-[A-Z0-9]+-\\d+)\\b");

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void healDayBookData() {
        log.info("[DayBookSelfHealing] Starting DayBook data integrity check and self-healing...");

        try {
            linkOrphanTransactionsToDayBooks();
            int duplicateTransfersRemoved = cleanDuplicateOfficeRemittanceTransfers();
            int orphanLoanTxsRemoved = cleanOrphanLoanTransactions();
            recalculateAllDayBookBalances();
            resyncMarketDayBooks();
            syncMarketDayBookClosures();

            log.info("[DayBookSelfHealing] Completed successfully! Duplicate transfers removed: {}, Orphan loan txs removed: {}",
                    duplicateTransfersRemoved, orphanLoanTxsRemoved);
        } catch (Exception e) {
            log.error("[DayBookSelfHealing] Error during self-healing: {}", e.getMessage(), e);
        }
    }

    private void linkOrphanTransactionsToDayBooks() {
        List<DayBookTransaction> allTxs = dayBookTransactionRepository.findAll();
        for (DayBookTransaction tx : allTxs) {
            if (tx.getDayBook() == null && tx.getCreatedAt() != null && tx.getEmployeeId() != null) {
                LocalDate txDate = tx.getCreatedAt().toLocalDate();
                dayBookRepository.findByEmployeeIdAndDate(tx.getEmployeeId(), txDate).ifPresent(db -> {
                    tx.setDayBook(db);
                    dayBookTransactionRepository.save(tx);
                });
            }
        }
    }

    private int cleanDuplicateOfficeRemittanceTransfers() {
        int removedCount = 0;
        List<DayBookTransaction> allTxs = dayBookTransactionRepository.findAll();
        Set<DayBook> affectedDayBooks = new HashSet<>();

        for (DayBookTransaction tx : allTxs) {
            String type = tx.getType() != null ? tx.getType().toUpperCase() : "";
            String remarks = tx.getRemarks() != null ? tx.getRemarks().toLowerCase() : "";

            // Identify duplicate outgoing transfers created for office remittance
            if ((type.contains("OUTGOING_TRANSFER") || type.contains("CASH_OUTGOING_TRANSFER"))
                    && (remarks.contains("office remittance") || remarks.contains("remittance to office") || remarks.contains("auto-generated office remittance"))) {

                DayBook db = tx.getDayBook();
                if (db != null) {
                    if (type.contains("CASH") && db.getCashOutgoingTransfers() != null) {
                        db.setCashOutgoingTransfers(db.getCashOutgoingTransfers().subtract(tx.getAmount()).max(BigDecimal.ZERO));
                    } else if (db.getOutgoingTransfers() != null) {
                        db.setOutgoingTransfers(db.getOutgoingTransfers().subtract(tx.getAmount()).max(BigDecimal.ZERO));
                    }
                    affectedDayBooks.add(db);
                }
                dayBookTransactionRepository.delete(tx);
                removedCount++;
            }
        }

        for (DayBook db : affectedDayBooks) {
            recalculateAndSave(db);
        }

        return removedCount;
    }

    private int cleanOrphanLoanTransactions() {
        int removedCount = 0;
        List<DayBookTransaction> allTxs = dayBookTransactionRepository.findAll();
        Set<DayBook> affectedDayBooks = new HashSet<>();

        for (DayBookTransaction tx : allTxs) {
            String type = tx.getType() != null ? tx.getType().toUpperCase() : "";
            if ("LOANS_DISBURSED".equals(type) || "COLLECTIONS".equals(type)) {
                String remarks = tx.getRemarks() != null ? tx.getRemarks() : "";
                Matcher matcher = LOAN_CODE_PATTERN.matcher(remarks);
                if (matcher.find()) {
                    String loanCode = matcher.group(1);
                    if (!loanRepository.existsByLoanCode(loanCode)) {
                        log.info("[DayBookSelfHealing] Deleting orphan tx {} for deleted loan {}", tx.getId(), loanCode);
                        DayBook db = tx.getDayBook();
                        if (db != null) {
                            if ("LOANS_DISBURSED".equals(type) && db.getLoansDisbursed() != null) {
                                db.setLoansDisbursed(db.getLoansDisbursed().subtract(tx.getAmount()).max(BigDecimal.ZERO));
                            } else if ("COLLECTIONS".equals(type) && db.getCollections() != null) {
                                db.setCollections(db.getCollections().subtract(tx.getAmount()).max(BigDecimal.ZERO));
                            }
                            affectedDayBooks.add(db);
                        }
                        dayBookTransactionRepository.delete(tx);
                        removedCount++;
                    }
                }
            }
        }

        for (DayBook db : affectedDayBooks) {
            recalculateAndSave(db);
        }

        return removedCount;
    }

    private void recalculateAllDayBookBalances() {
        List<DayBook> allDayBooks = dayBookRepository.findAll();
        // Group by employee and sort by date ascending
        Set<java.util.UUID> employeeIds = new HashSet<>();
        for (DayBook db : allDayBooks) {
            if (db.getEmployeeId() != null) {
                employeeIds.add(db.getEmployeeId());
            }
        }

        for (java.util.UUID empId : employeeIds) {
            List<DayBook> empBooks = dayBookRepository.findByEmployeeIdOrderByDateAsc(empId);
            BigDecimal prevClosing = BigDecimal.ZERO;
            for (int i = 0; i < empBooks.size(); i++) {
                DayBook db = empBooks.get(i);
                if (i > 0) {
                    db.setOpeningBalance(prevClosing);
                }
                recalculateAndSave(db);
                prevClosing = db.getClosingBalance();
            }
        }
    }

    private void resyncMarketDayBooks() {
        List<Market> markets = marketRepository.findAll();
        LocalDate start = LocalDate.of(2026, 9, 29);
        LocalDate today = LocalDate.now();

        for (Market m : markets) {
            LocalDate curr = start;
            while (!curr.isAfter(today)) {
                try {
                    dayBookService.syncMarketDayBook(m.getId(), curr);
                } catch (Exception ignored) {}
                curr = curr.plusDays(1);
            }
        }
    }

    private void recalculateAndSave(DayBook db) {
        BigDecimal open = db.getOpeningBalance() != null ? db.getOpeningBalance() : BigDecimal.ZERO;
        BigDecimal col = db.getCollections() != null ? db.getCollections() : BigDecimal.ZERO;
        BigDecimal inOnline = db.getIncomingTransfers() != null ? db.getIncomingTransfers() : BigDecimal.ZERO;
        BigDecimal inCash = db.getCashIncomingTransfers() != null ? db.getCashIncomingTransfers() : BigDecimal.ZERO;
        BigDecimal spends = db.getSpends() != null ? db.getSpends() : BigDecimal.ZERO;
        BigDecimal loans = db.getLoansDisbursed() != null ? db.getLoansDisbursed() : BigDecimal.ZERO;
        BigDecimal outOnline = db.getOutgoingTransfers() != null ? db.getOutgoingTransfers() : BigDecimal.ZERO;
        BigDecimal outCash = db.getCashOutgoingTransfers() != null ? db.getCashOutgoingTransfers() : BigDecimal.ZERO;
        BigDecimal remit = db.getOfficeRemittance() != null ? db.getOfficeRemittance() : BigDecimal.ZERO;

        BigDecimal closing = open.add(col).add(inOnline).add(inCash)
                .subtract(spends).subtract(loans).subtract(outOnline).subtract(outCash).subtract(remit);

        db.setClosingBalance(closing);
        dayBookRepository.save(db);
    }

    private void syncMarketDayBookClosures() {
        List<Market> markets = marketRepository.findAll();
        for (Market market : markets) {
            List<EmployeeMarketAssignment> assignments = assignmentRepository.findByMarketIdAndIsActiveTrue(market.getId());
            if (assignments == null || assignments.isEmpty()) continue;

            List<UUID> empIds = assignments.stream()
                    .map(a -> a.getEmployee().getId())
                    .collect(java.util.stream.Collectors.toList());

            Set<LocalDate> closedDates = new HashSet<>();
            for (UUID empId : empIds) {
                List<DayBook> closedBooks = dayBookRepository.findByEmployeeIdAndStatus(empId, DayBookStatus.CLOSED);
                for (DayBook cb : closedBooks) {
                    if (cb.getDate() != null) {
                        closedDates.add(cb.getDate());
                    }
                }
            }

            for (LocalDate date : closedDates) {
                marketDayBookRepository.findByMarketIdAndDate(market.getId(), date).ifPresent(mdb -> {
                    if (mdb.getStatus() != DayBookStatus.CLOSED) {
                        mdb.setStatus(DayBookStatus.CLOSED);
                        marketDayBookRepository.save(mdb);
                    }
                });

                for (UUID empId : empIds) {
                    dayBookRepository.findByEmployeeIdAndDate(empId, date).ifPresent(db -> {
                        if (db.getStatus() != DayBookStatus.CLOSED) {
                            db.setStatus(DayBookStatus.CLOSED);
                            dayBookRepository.save(db);
                        }
                    });
                }
            }
        }
    }
}
