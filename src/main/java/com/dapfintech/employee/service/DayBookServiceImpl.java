package com.dapfintech.employee.service;

import com.dapfintech.employee.dto.DayBookResponse;
import com.dapfintech.employee.dto.DayBookTransactionRequest;
import com.dapfintech.employee.dto.EmployeeDayBookSummary;
import com.dapfintech.employee.dto.MarketDayBookResponse;
import com.dapfintech.employee.dto.UpdateDayBookRequest;
import com.dapfintech.employee.entity.DayBook;
import com.dapfintech.employee.entity.DayBookTransaction;
import com.dapfintech.employee.entity.MarketDayBook;
import com.dapfintech.employee.enums.DayBookStatus;
import com.dapfintech.employee.repository.DayBookRepository;
import com.dapfintech.employee.repository.DayBookTransactionRepository;
import com.dapfintech.employee.repository.MarketDayBookRepository;
import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.capital.entity.InternalTransfer;
import com.dapfintech.capital.enums.TransferStatus;
import com.dapfintech.capital.repository.InternalTransferRepository;
import com.dapfintech.loan.entity.Loan;
import com.dapfintech.loan.entity.LoanCollection;
import com.dapfintech.loan.repository.LoanCollectionRepository;
import com.dapfintech.loan.repository.LoanRepository;
import com.dapfintech.market.entity.EmployeeMarketAssignment;
import com.dapfintech.market.entity.Market;
import com.dapfintech.market.repository.EmployeeMarketAssignmentRepository;
import com.dapfintech.market.repository.MarketRepository;
import com.dapfintech.notification.service.NotificationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DayBookServiceImpl implements DayBookService {

    @Autowired
    private DayBookRepository dayBookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InternalTransferRepository internalTransferRepository;

    @Autowired
    private DayBookTransactionRepository dayBookTransactionRepository;

    @Autowired
    private MarketDayBookRepository marketDayBookRepository;

    @Autowired
    private EmployeeMarketAssignmentRepository assignmentRepository;

    @Autowired
    private MarketRepository marketRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private LoanCollectionRepository loanCollectionRepository;

    @Autowired
    private LoanRepository loanRepository;

    // =========================================================================
    // SEQUENTIAL DATE RESOLUTION
    // =========================================================================

    @Override
    public LocalDate getActiveMarketDayBookDate(UUID marketId) {
        if (marketId == null) {
            return LocalDate.now();
        }

        List<MarketDayBook> pastClosed = marketDayBookRepository
                .findByMarketIdAndStatusOrderByDateDesc(marketId, DayBookStatus.CLOSED);

        if (!pastClosed.isEmpty()) {
            LocalDate latestClosed = pastClosed.get(0).getDate();
            LocalDate candidate = latestClosed.plusDays(1);
            if (candidate.isAfter(LocalDate.now())) {
                return LocalDate.now();
            }
            return candidate;
        }

        // If no closed market daybook exists, check earliest existing market daybook
        List<MarketDayBook> all = marketDayBookRepository.findByMarketIdOrderByDateAsc(marketId);
        if (!all.isEmpty()) {
            return all.get(0).getDate();
        }

        // Check if there is an employee daybook for this market
        List<EmployeeMarketAssignment> assigns = assignmentRepository.findByMarketIdAndIsActiveTrue(marketId);
        for (EmployeeMarketAssignment asg : assigns) {
            if (asg.getEmployee() != null) {
                List<DayBook> closedEmp = dayBookRepository
                        .findByEmployeeIdAndStatusOrderByDateDesc(asg.getEmployee().getId(), DayBookStatus.CLOSED);
                if (!closedEmp.isEmpty()) {
                    LocalDate candidate = closedEmp.get(0).getDate().plusDays(1);
                    if (candidate.isAfter(LocalDate.now())) {
                        return LocalDate.now();
                    }
                    return candidate;
                }
            }
        }

        return LocalDate.now();
    }

    @Override
    public LocalDate getActiveDayBookDate(UUID employeeId) {
        if (employeeId == null) {
            return LocalDate.now();
        }

        // Check if employee is assigned to a market
        Optional<EmployeeMarketAssignment> asgOpt = assignmentRepository.findFirstByEmployeeIdAndIsActiveTrue(employeeId);
        if (asgOpt.isPresent() && asgOpt.get().getMarket() != null) {
            return getActiveMarketDayBookDate(asgOpt.get().getMarket().getId());
        }

        // Fallback for unassigned employee: check employee's own closed daybooks
        List<DayBook> pastClosed = dayBookRepository
                .findByEmployeeIdAndStatusOrderByDateDesc(employeeId, DayBookStatus.CLOSED);

        if (!pastClosed.isEmpty()) {
            LocalDate latestClosed = pastClosed.get(0).getDate();
            LocalDate candidate = latestClosed.plusDays(1);
            if (candidate.isAfter(LocalDate.now())) {
                return LocalDate.now();
            }
            return candidate;
        }

        List<DayBook> all = dayBookRepository.findByEmployeeIdOrderByDateAsc(employeeId);
        if (!all.isEmpty()) {
            return all.get(0).getDate();
        }

        return LocalDate.now();
    }

    // =========================================================================
    // EMPLOYEE DAYBOOK OPERATIONS
    // =========================================================================

    @Override
    @Transactional
    public DayBookResponse getOrCreateTodayDayBook(UUID employeeId) {
        LocalDate activeDate = getActiveDayBookDate(employeeId);
        return getOrCreateDayBook(employeeId, activeDate);
    }

    @Override
    @Transactional
    public DayBookResponse getOrCreateDayBook(UUID employeeId, LocalDate date) {
        DayBook dayBook;
        Optional<DayBook> existing = dayBookRepository.findByEmployeeIdAndDate(employeeId, date);
        if (existing.isPresent()) {
            dayBook = existing.get();
        } else {
            dayBook = new DayBook();
            dayBook.setEmployeeId(employeeId);
            dayBook.setDate(date);
            dayBook.setStatus(DayBookStatus.OPEN);

            List<DayBook> pastBooks = dayBookRepository.findByEmployeeIdOrderByDateDesc(employeeId);
            BigDecimal openingBal = BigDecimal.ZERO;
            for (DayBook pb : pastBooks) {
                if (pb.getDate().isBefore(date)) {
                    openingBal = pb.getClosingBalance() != null ? pb.getClosingBalance() : BigDecimal.ZERO;
                    break;
                }
            }
            dayBook.setOpeningBalance(openingBal);
            dayBook.setClosingBalance(calculateClosingBalance(dayBook));
            dayBook = dayBookRepository.save(dayBook);
        }

        // Auto-sync collections & loans disbursed for this employee on this date
        syncDayBookFromDb(dayBook, employeeId, date);

        return mapToResponse(dayBook);
    }

    private void syncDayBookFromDb(DayBook dayBook, UUID employeeId, LocalDate date) {
        if (dayBook == null || dayBook.getStatus() == DayBookStatus.CLOSED) {
            return;
        }
        try {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            // 1. Sync Collections collected strictly by this employee
            List<LoanCollection> cols = loanCollectionRepository.findCollectionsByEmployeeBetween(
                    employeeId, start, end
            );

            boolean modified = false;
            BigDecimal actualCollectionSum = BigDecimal.ZERO;
            if (cols != null && !cols.isEmpty()) {
                actualCollectionSum = cols.stream()
                        .map(c -> c.getCollectedAmount() != null ? c.getCollectedAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            if (dayBook.getCollections() == null || dayBook.getCollections().compareTo(actualCollectionSum) != 0) {
                dayBook.setCollections(actualCollectionSum);
                modified = true;
            }

            // 2. Sync Loans Disbursed created strictly by this employee
            List<Loan> disbursedLoans;
            if (date.isEqual(LocalDate.of(2026, 9, 29))) {
                LocalDateTime cutoffEnd = date.atTime(23, 59, 59);
                disbursedLoans = loanRepository.findDisbursedLoansByEmployeeUpTo(employeeId, cutoffEnd);
            } else {
                disbursedLoans = loanRepository.findDisbursedLoansByEmployeeBetween(employeeId, start, end);
            }

            BigDecimal actualDisbursedSum = BigDecimal.ZERO;
            if (disbursedLoans != null && !disbursedLoans.isEmpty()) {
                actualDisbursedSum = disbursedLoans.stream()
                        .map(l -> l.getDisbursedAmount() != null ? l.getDisbursedAmount() : (l.getApprovedAmount() != null ? l.getApprovedAmount() : BigDecimal.ZERO))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            if (dayBook.getLoansDisbursed() == null || dayBook.getLoansDisbursed().compareTo(actualDisbursedSum) != 0) {
                dayBook.setLoansDisbursed(actualDisbursedSum);
                modified = true;
            }

            // 3. Sync Accepted Transfers for this Employee and Date
            List<InternalTransfer> inTransfers = internalTransferRepository.findByReceiverIdAndStatusAndTransferDateBetween(
                    employeeId, TransferStatus.ACCEPTED, start, end
            );
            BigDecimal onlineIn = BigDecimal.ZERO;
            BigDecimal cashIn = BigDecimal.ZERO;
            if (inTransfers != null && !inTransfers.isEmpty()) {
                onlineIn = inTransfers.stream()
                        .filter(t -> t.getTransferMode() != com.dapfintech.capital.enums.TransferMode.CASH)
                        .map(InternalTransfer::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                cashIn = inTransfers.stream()
                        .filter(t -> t.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH)
                        .map(InternalTransfer::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            if (dayBook.getIncomingTransfers() == null || dayBook.getIncomingTransfers().compareTo(onlineIn) != 0) {
                dayBook.setIncomingTransfers(onlineIn);
                modified = true;
            }
            if (dayBook.getCashIncomingTransfers() == null || dayBook.getCashIncomingTransfers().compareTo(cashIn) != 0) {
                dayBook.setCashIncomingTransfers(cashIn);
                modified = true;
            }

            // 4. Sync Outgoing Transfers (excluding OFFICE_REMITTANCE which is tracked in officeRemittance)
            List<InternalTransfer> outTransfers = internalTransferRepository.findBySenderIdAndStatusAndTransferDateBetween(
                    employeeId, TransferStatus.ACCEPTED, start, end
            );
            BigDecimal onlineOut = BigDecimal.ZERO;
            BigDecimal cashOut = BigDecimal.ZERO;
            if (outTransfers != null && !outTransfers.isEmpty()) {
                onlineOut = outTransfers.stream()
                        .filter(t -> !"OFFICE_REMITTANCE".equalsIgnoreCase(t.getCategory()))
                        .filter(t -> t.getTransferMode() != com.dapfintech.capital.enums.TransferMode.CASH)
                        .map(InternalTransfer::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                cashOut = outTransfers.stream()
                        .filter(t -> !"OFFICE_REMITTANCE".equalsIgnoreCase(t.getCategory()))
                        .filter(t -> t.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH)
                        .map(InternalTransfer::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            if (dayBook.getOutgoingTransfers() == null || dayBook.getOutgoingTransfers().compareTo(onlineOut) != 0) {
                dayBook.setOutgoingTransfers(onlineOut);
                modified = true;
            }
            if (dayBook.getCashOutgoingTransfers() == null || dayBook.getCashOutgoingTransfers().compareTo(cashOut) != 0) {
                dayBook.setCashOutgoingTransfers(cashOut);
                modified = true;
            }

            if (modified) {
                dayBook.setClosingBalance(calculateClosingBalance(dayBook));
                dayBookRepository.save(dayBook);
                propagateClosingBalanceForward(dayBook);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    @Transactional
    public DayBookResponse addTransaction(UUID employeeId, DayBookTransactionRequest request) {
        LocalDate activeDate = getActiveDayBookDate(employeeId);
        return addTransactionForDate(employeeId, activeDate, request);
    }

    @Override
    @Transactional
    public DayBookResponse addTransactionForDate(UUID employeeId, LocalDate date, DayBookTransactionRequest request) {
        DayBook dayBook = dayBookRepository.findByEmployeeIdAndDate(employeeId, date)
                .orElseGet(() -> {
                    getOrCreateDayBook(employeeId, date);
                    return dayBookRepository.findByEmployeeIdAndDate(employeeId, date).get();
                });

        if (dayBook.getStatus() == DayBookStatus.CLOSED) {
            throw new RuntimeException("Cannot add transaction to a closed DayBook for " + date + ". Please reopen the DayBook first if you need to modify it.");
        }
        boolean isTransfer = request.getType() != null && (request.getType().contains("TRANSFER") || request.getType().contains("REMITTANCE"));
        if (dayBook.getStatus() != DayBookStatus.OPEN && !isTransfer) {
            throw new RuntimeException("Cannot add transaction to a daybook that is pending closure.");
        }

        BigDecimal amount = request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO;

        if (dayBook.getSpends() == null) dayBook.setSpends(BigDecimal.ZERO);
        if (dayBook.getCollections() == null) dayBook.setCollections(BigDecimal.ZERO);
        if (dayBook.getLoansDisbursed() == null) dayBook.setLoansDisbursed(BigDecimal.ZERO);
        if (dayBook.getOfficeRemittance() == null) dayBook.setOfficeRemittance(BigDecimal.ZERO);
        if (dayBook.getIncomingTransfers() == null) dayBook.setIncomingTransfers(BigDecimal.ZERO);
        if (dayBook.getOutgoingTransfers() == null) dayBook.setOutgoingTransfers(BigDecimal.ZERO);

        switch (request.getType().toUpperCase()) {
            case "SPENDS":
                if (request.getRemarks() == null || request.getRemarks().trim().isEmpty()) {
                    throw new RuntimeException("Remarks are mandatory for spends.");
                }
                dayBook.setSpends(dayBook.getSpends().add(amount));
                break;
            case "COLLECTIONS":
                dayBook.setCollections(dayBook.getCollections().add(amount));
                break;
            case "LOANS_DISBURSED":
                dayBook.setLoansDisbursed(dayBook.getLoansDisbursed().add(amount));
                break;
            case "OFFICE_REMITTANCE":
                dayBook.setOfficeRemittance(dayBook.getOfficeRemittance().add(amount));

                // Trigger internal transfer to master admin with exact daybook date
                User employee = userRepository.findById(employeeId).orElse(null);
                User masterAdmin = userRepository.findByRoleRoleName("MASTER_ADMIN").stream().findFirst()
                        .orElseGet(() -> userRepository.findByRoleRoleName("ADMIN").stream().findFirst().orElse(null));
                if (employee != null && masterAdmin != null) {
                    Market empMarket = assignmentRepository.findFirstByEmployeeIdAndIsActiveTrue(employeeId)
                            .map(EmployeeMarketAssignment::getMarket).orElse(null);

                    InternalTransfer transfer = InternalTransfer.builder()
                            .sender(employee)
                            .receiver(masterAdmin)
                            .senderMarket(empMarket)
                            .amount(amount)
                            .transferDate(date.atTime(LocalTime.now()))
                            .status(TransferStatus.PENDING)
                            .category("OFFICE_REMITTANCE")
                            .transferMode(com.dapfintech.capital.enums.TransferMode.CASH)
                            .remarks("Office Remittance: " + (request.getRemarks() != null ? request.getRemarks() : "Cash to Office"))
                            .build();
                    internalTransferRepository.save(transfer);
                }
                break;
            case "INCOMING_TRANSFER":
                dayBook.setIncomingTransfers(dayBook.getIncomingTransfers().add(amount));
                break;
            case "CASH_INCOMING_TRANSFER":
                if (dayBook.getCashIncomingTransfers() == null) dayBook.setCashIncomingTransfers(BigDecimal.ZERO);
                dayBook.setCashIncomingTransfers(dayBook.getCashIncomingTransfers().add(amount));
                break;
            case "OUTGOING_TRANSFER":
                dayBook.setOutgoingTransfers(dayBook.getOutgoingTransfers().add(amount));
                break;
            case "CASH_OUTGOING_TRANSFER":
                if (dayBook.getCashOutgoingTransfers() == null) dayBook.setCashOutgoingTransfers(BigDecimal.ZERO);
                dayBook.setCashOutgoingTransfers(dayBook.getCashOutgoingTransfers().add(amount));
                break;
            default:
                throw new RuntimeException("Unknown transaction type: " + request.getType());
        }

        dayBook.setClosingBalance(calculateClosingBalance(dayBook));
        dayBook = dayBookRepository.save(dayBook);

        // Propagate closing balance forward
        propagateClosingBalanceForward(dayBook);

        // Save transaction history
        DayBookTransaction tx = new DayBookTransaction();
        tx.setDayBook(dayBook);
        tx.setEmployeeId(employeeId);
        tx.setType(request.getType().toUpperCase());
        tx.setAmount(amount);
        tx.setRemarks(request.getRemarks());
        tx.setCreatedAt(date.isEqual(LocalDate.now()) ? LocalDateTime.now() : date.atTime(LocalTime.now()));
        dayBookTransactionRepository.save(tx);

        // Sync market daybook if applicable
        assignmentRepository.findFirstByEmployeeIdAndIsActiveTrue(employeeId)
                .ifPresent(asg -> syncMarketDayBook(asg.getMarket().getId(), date));

        return mapToResponse(dayBook);
    }

    @Override
    @Transactional
    public DayBookResponse requestClosure(UUID employeeId) {
        LocalDate activeDate = getActiveDayBookDate(employeeId);
        return requestClosureForDate(employeeId, activeDate);
    }

    @Override
    @Transactional
    public DayBookResponse requestClosureForDate(UUID employeeId, LocalDate date) {
        DayBook dayBook = dayBookRepository.findByEmployeeIdAndDate(employeeId, date)
                .orElseThrow(() -> new RuntimeException("DayBook not found for employee on " + date));

        if (dayBook.getStatus() != DayBookStatus.OPEN) {
            throw new RuntimeException("Daybook is already pending closure or closed.");
        }

        dayBook.setStatus(DayBookStatus.PENDING_CLOSURE);
        dayBook = dayBookRepository.save(dayBook);
        return mapToResponse(dayBook);
    }

    @Override
    @Transactional
    public DayBookResponse cancelClosure(UUID employeeId) {
        LocalDate activeDate = getActiveDayBookDate(employeeId);
        return cancelClosureForDate(employeeId, activeDate);
    }

    @Override
    @Transactional
    public DayBookResponse cancelClosureForDate(UUID employeeId, LocalDate date) {
        DayBook dayBook = dayBookRepository.findByEmployeeIdAndDate(employeeId, date)
                .orElseThrow(() -> new RuntimeException("DayBook not found for employee on " + date));

        if (dayBook.getStatus() != DayBookStatus.PENDING_CLOSURE) {
            throw new RuntimeException("Daybook is not pending closure.");
        }

        dayBook.setStatus(DayBookStatus.OPEN);
        dayBook = dayBookRepository.save(dayBook);
        return mapToResponse(dayBook);
    }

    @Override
    @Transactional
    public DayBookResponse approveClosure(UUID dayBookId) {
        DayBook dayBook = dayBookRepository.findById(dayBookId)
                .orElseThrow(() -> new RuntimeException("DayBook not found"));

        dayBook.setStatus(DayBookStatus.CLOSED);
        dayBook = dayBookRepository.save(dayBook);

        propagateClosingBalanceForward(dayBook);
        checkAndCloseMarketDayBook(dayBook);

        return mapToResponse(dayBook);
    }

    private void checkAndCloseMarketDayBook(DayBook closedDayBook) {
        UUID employeeId = closedDayBook.getEmployeeId();
        LocalDate targetDate = closedDayBook.getDate();

        EmployeeMarketAssignment assignment =
                assignmentRepository.findFirstByEmployeeIdAndIsActiveTrue(employeeId).orElse(null);

        if (assignment == null || assignment.getMarket() == null) return;

        UUID marketId = assignment.getMarket().getId();

        List<EmployeeMarketAssignment> marketEmployees =
                assignmentRepository.findByMarketIdAndIsActiveTrue(marketId);

        boolean allClosed = true;
        for (EmployeeMarketAssignment empAssignment : marketEmployees) {
            UUID empId = empAssignment.getEmployee().getId();
            if (empId.equals(employeeId)) continue;

            Optional<DayBook> empDayBookOpt = dayBookRepository.findByEmployeeIdAndDate(empId, targetDate);
            if (empDayBookOpt.isEmpty() || empDayBookOpt.get().getStatus() != DayBookStatus.CLOSED) {
                allClosed = false;
                break;
            }
        }

        if (allClosed) {
            syncMarketDayBook(marketId, targetDate);
            marketDayBookRepository.findByMarketIdAndDate(marketId, targetDate).ifPresent(mdb -> {
                mdb.setStatus(DayBookStatus.CLOSED);
                marketDayBookRepository.save(mdb);
                propagateMarketClosingBalanceForward(mdb);
                notificationService.createNotification(
                        "Market Daybook Closed",
                        "Market " + assignment.getMarket().getMarketName() + " daybook closed for " + targetDate + ". Total Collection: Rs. " + mdb.getTotalCollections()
                );
            });
        }
    }

    @Override
    @Transactional
    public DayBookResponse rejectClosure(UUID dayBookId) {
        DayBook dayBook = dayBookRepository.findById(dayBookId)
                .orElseThrow(() -> new RuntimeException("DayBook not found"));

        dayBook.setStatus(DayBookStatus.OPEN);
        dayBook = dayBookRepository.save(dayBook);
        return mapToResponse(dayBook);
    }

    @Override
    @Transactional
    public DayBookResponse reopenDayBook(UUID dayBookId) {
        DayBook dayBook = dayBookRepository.findById(dayBookId)
                .orElseThrow(() -> new RuntimeException("DayBook not found"));

        dayBook.setStatus(DayBookStatus.OPEN);
        dayBook = dayBookRepository.save(dayBook);

        UUID employeeId = dayBook.getEmployeeId();
        LocalDate date = dayBook.getDate();
        assignmentRepository.findFirstByEmployeeIdAndIsActiveTrue(employeeId).ifPresent(assignment -> {
            marketDayBookRepository.findByMarketIdAndDate(assignment.getMarket().getId(), date).ifPresent(mdb -> {
                mdb.setStatus(DayBookStatus.OPEN);
                marketDayBookRepository.save(mdb);
            });
        });

        return mapToResponse(dayBook);
    }

    @Override
    @Transactional
    public DayBookResponse updateDayBook(UUID dayBookId, UpdateDayBookRequest request) {
        DayBook dayBook = dayBookRepository.findById(dayBookId)
                .orElseThrow(() -> new RuntimeException("DayBook not found"));

        if (request.getOpeningBalance() != null) dayBook.setOpeningBalance(request.getOpeningBalance());
        if (request.getCollections() != null) dayBook.setCollections(request.getCollections());
        if (request.getSpends() != null) dayBook.setSpends(request.getSpends());
        if (request.getLoansDisbursed() != null) dayBook.setLoansDisbursed(request.getLoansDisbursed());
        if (request.getOfficeRemittance() != null) dayBook.setOfficeRemittance(request.getOfficeRemittance());
        if (request.getIncomingTransfers() != null) dayBook.setIncomingTransfers(request.getIncomingTransfers());
        if (request.getCashIncomingTransfers() != null) dayBook.setCashIncomingTransfers(request.getCashIncomingTransfers());
        if (request.getOutgoingTransfers() != null) dayBook.setOutgoingTransfers(request.getOutgoingTransfers());
        if (request.getCashOutgoingTransfers() != null) dayBook.setCashOutgoingTransfers(request.getCashOutgoingTransfers());

        dayBook.setClosingBalance(calculateClosingBalance(dayBook));
        DayBook savedDayBook = dayBookRepository.save(dayBook);
        propagateClosingBalanceForward(savedDayBook);

        LocalDate bookDate = savedDayBook.getDate();
        assignmentRepository.findFirstByEmployeeIdAndIsActiveTrue(savedDayBook.getEmployeeId())
                .ifPresent(asg -> syncMarketDayBook(asg.getMarket().getId(), bookDate));

        return mapToResponse(savedDayBook);
    }

    @Override
    public List<DayBookResponse> getEmployeeDayBooks(UUID employeeId) {
        return dayBookRepository.findByEmployeeIdOrderByDateDesc(employeeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<DayBookTransaction> getTransactions(UUID employeeId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        LocalDateTime endInclusive = date.atTime(23, 59, 59, 999999);

        List<DayBookTransaction> list = new ArrayList<>(
                dayBookTransactionRepository.findByEmployeeIdAndCreatedAtBetween(employeeId, start, endInclusive)
        );

        // Check if COLLECTIONS transactions are present
        boolean hasCollections = list.stream().anyMatch(t -> "COLLECTIONS".equalsIgnoreCase(t.getType()));
        if (!hasCollections) {
            List<LoanCollection> cols = loanCollectionRepository.findCollectionsByEmployeeBetween(
                    employeeId, start, end
            );
            if (cols != null && !cols.isEmpty()) {
                DayBook dayBook = dayBookRepository.findByEmployeeIdAndDate(employeeId, date).orElse(null);
                for (LoanCollection c : cols) {
                    if (c.getCollectedAmount() == null || c.getCollectedAmount().compareTo(BigDecimal.ZERO) <= 0) continue;
                    DayBookTransaction tx = new DayBookTransaction();
                    tx.setDayBook(dayBook);
                    tx.setEmployeeId(employeeId);
                    tx.setType("COLLECTIONS");
                    tx.setAmount(c.getCollectedAmount());
                    String custName = (c.getLoan() != null && c.getLoan().getCustomer() != null)
                            ? (c.getLoan().getCustomer().getFirstName() + " " + (c.getLoan().getCustomer().getLastName() != null ? c.getLoan().getCustomer().getLastName() : ""))
                            : "";
                    String loanCode = c.getLoan() != null ? c.getLoan().getLoanCode() : "";
                    String rem = c.getRemarks();
                    if (rem == null || rem.trim().isEmpty()) {
                        rem = "Collection: " + custName + " (" + loanCode + ")";
                    } else if (!rem.contains(loanCode) && !loanCode.isEmpty()) {
                        rem = rem + " (" + loanCode + ")";
                    }
                    tx.setRemarks(rem);
                    tx.setCreatedAt(c.getCollectionDate() != null ? c.getCollectionDate() : date.atTime(17, 0));
                    tx = dayBookTransactionRepository.save(tx);
                    list.add(tx);
                }
            }
        }

        // Check if LOANS_DISBURSED transactions are present
        boolean hasLoansDisbursed = list.stream().anyMatch(t -> "LOANS_DISBURSED".equalsIgnoreCase(t.getType()));
        if (!hasLoansDisbursed) {
            List<Loan> disbursedLoans;
            if (date.isEqual(LocalDate.of(2026, 9, 29))) {
                LocalDateTime cutoffEnd = date.atTime(23, 59, 59);
                disbursedLoans = loanRepository.findDisbursedLoansByEmployeeUpTo(employeeId, cutoffEnd);
            } else {
                disbursedLoans = loanRepository.findDisbursedLoansByEmployeeBetween(employeeId, start, end);
            }

            if (disbursedLoans != null && !disbursedLoans.isEmpty()) {
                DayBook dayBook = dayBookRepository.findByEmployeeIdAndDate(employeeId, date).orElse(null);
                for (Loan l : disbursedLoans) {
                    BigDecimal disAmount = l.getDisbursedAmount() != null ? l.getDisbursedAmount() : (l.getApprovedAmount() != null ? l.getApprovedAmount() : BigDecimal.ZERO);
                    if (disAmount.compareTo(BigDecimal.ZERO) <= 0) continue;

                    DayBookTransaction tx = new DayBookTransaction();
                    tx.setDayBook(dayBook);
                    tx.setEmployeeId(employeeId);
                    tx.setType("LOANS_DISBURSED");
                    tx.setAmount(disAmount);
                    String custName = l.getCustomer() != null
                            ? (l.getCustomer().getFirstName() + " " + (l.getCustomer().getLastName() != null ? l.getCustomer().getLastName() : ""))
                            : "";
                    String loanCode = l.getLoanCode() != null ? l.getLoanCode() : "";
                    tx.setRemarks("New Loan: " + custName + " (" + loanCode + ")");
                    tx.setCreatedAt(l.getDisbursementDate() != null ? l.getDisbursementDate() : date.atTime(10, 0));
                    tx = dayBookTransactionRepository.save(tx);
                    list.add(tx);
                }
            }
        }

        // Check if TRANSFER transactions are present in list
        boolean hasTransfers = list.stream().anyMatch(t -> t.getType() != null && t.getType().contains("TRANSFER"));
        if (!hasTransfers) {
            List<InternalTransfer> inTransfers = internalTransferRepository.findByReceiverIdAndStatusAndTransferDateBetween(
                    employeeId, TransferStatus.ACCEPTED, start, end
            );
            if (inTransfers != null) {
                for (InternalTransfer it : inTransfers) {
                    DayBookTransaction tx = new DayBookTransaction();
                    tx.setEmployeeId(employeeId);
                    tx.setType(it.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH ? "CASH_INCOMING_TRANSFER" : "INCOMING_TRANSFER");
                    tx.setAmount(it.getAmount());
                    String rem = (it.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH ? "Cash from: " : "Online from: ")
                            + it.getSender().getFullName()
                            + (it.getRemarks() != null && !it.getRemarks().isBlank() ? " (" + it.getRemarks() + ")" : "");
                    tx.setRemarks(rem);
                    tx.setCreatedAt(it.getTransferDate());
                    list.add(tx);
                }
            }
            List<InternalTransfer> outTransfers = internalTransferRepository.findBySenderIdAndStatusAndTransferDateBetween(
                    employeeId, TransferStatus.ACCEPTED, start, end
            );
            if (outTransfers != null) {
                for (InternalTransfer ot : outTransfers) {
                    if ("OFFICE_REMITTANCE".equalsIgnoreCase(ot.getCategory())) continue;
                    DayBookTransaction tx = new DayBookTransaction();
                    tx.setEmployeeId(employeeId);
                    tx.setType(ot.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH ? "CASH_OUTGOING_TRANSFER" : "OUTGOING_TRANSFER");
                    tx.setAmount(ot.getAmount());
                    String rem = (ot.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH ? "Cash to: " : "Online to: ")
                            + ot.getReceiver().getFullName()
                            + (ot.getRemarks() != null && !ot.getRemarks().isBlank() ? " (" + ot.getRemarks() + ")" : "");
                    tx.setRemarks(rem);
                    tx.setCreatedAt(ot.getTransferDate());
                    list.add(tx);
                }
            }
        }

        return list;
    }

    private void propagateClosingBalanceForward(DayBook startDayBook) {
        if (startDayBook == null || startDayBook.getDate() == null) return;
        UUID empId = startDayBook.getEmployeeId();
        LocalDate currDate = startDayBook.getDate();
        BigDecimal prevClosing = startDayBook.getClosingBalance();

        List<DayBook> subsequentDayBooks = dayBookRepository.findByEmployeeIdOrderByDateAsc(empId);
        for (DayBook nextDb : subsequentDayBooks) {
            if (nextDb.getDate().isAfter(currDate)) {
                nextDb.setOpeningBalance(prevClosing);
                nextDb.setClosingBalance(calculateClosingBalance(nextDb));
                dayBookRepository.save(nextDb);
                prevClosing = nextDb.getClosingBalance();
            }
        }
    }

    @Override
    @Transactional
    public DayBookResponse getDayBookByDate(UUID employeeId, LocalDate date) {
        return getOrCreateDayBook(employeeId, date);
    }

    private BigDecimal calculateClosingBalance(DayBook dayBook) {
        if (dayBook.getOpeningBalance() == null) dayBook.setOpeningBalance(BigDecimal.ZERO);
        if (dayBook.getCollections() == null) dayBook.setCollections(BigDecimal.ZERO);
        if (dayBook.getIncomingTransfers() == null) dayBook.setIncomingTransfers(BigDecimal.ZERO);
        if (dayBook.getCashIncomingTransfers() == null) dayBook.setCashIncomingTransfers(BigDecimal.ZERO);
        if (dayBook.getSpends() == null) dayBook.setSpends(BigDecimal.ZERO);
        if (dayBook.getLoansDisbursed() == null) dayBook.setLoansDisbursed(BigDecimal.ZERO);
        if (dayBook.getOutgoingTransfers() == null) dayBook.setOutgoingTransfers(BigDecimal.ZERO);
        if (dayBook.getCashOutgoingTransfers() == null) dayBook.setCashOutgoingTransfers(BigDecimal.ZERO);
        if (dayBook.getOfficeRemittance() == null) dayBook.setOfficeRemittance(BigDecimal.ZERO);

        return dayBook.getOpeningBalance()
                .add(dayBook.getCollections())
                .add(dayBook.getIncomingTransfers())
                .add(dayBook.getCashIncomingTransfers())
                .subtract(dayBook.getSpends())
                .subtract(dayBook.getLoansDisbursed())
                .subtract(dayBook.getOutgoingTransfers())
                .subtract(dayBook.getCashOutgoingTransfers())
                .subtract(dayBook.getOfficeRemittance());
    }

    private DayBookResponse mapToResponse(DayBook dayBook) {
        DayBookResponse response = new DayBookResponse();
        response.setId(dayBook.getId());
        response.setEmployeeId(dayBook.getEmployeeId());
        response.setDate(dayBook.getDate());
        response.setOpeningBalance(dayBook.getOpeningBalance());
        response.setCollections(dayBook.getCollections());
        response.setIncomingTransfers(dayBook.getIncomingTransfers());
        response.setCashIncomingTransfers(dayBook.getCashIncomingTransfers());
        response.setSpends(dayBook.getSpends());
        response.setLoansDisbursed(dayBook.getLoansDisbursed());
        response.setOutgoingTransfers(dayBook.getOutgoingTransfers());
        response.setCashOutgoingTransfers(dayBook.getCashOutgoingTransfers());
        response.setOfficeRemittance(dayBook.getOfficeRemittance());
        response.setClosingBalance(dayBook.getClosingBalance());
        response.setStatus(dayBook.getStatus());

        List<DayBook> pastBooks = dayBookRepository.findByEmployeeIdOrderByDateDesc(dayBook.getEmployeeId());
        DayBook unclosedPast = pastBooks.stream()
                .filter(pb -> pb.getDate().isBefore(dayBook.getDate()) && pb.getStatus() != DayBookStatus.CLOSED)
                .findFirst()
                .orElse(null);

        if (unclosedPast != null) {
            response.setPreviousDayClosed(false);
            response.setUnclosedDate(unclosedPast.getDate());
        } else {
            response.setPreviousDayClosed(true);
            response.setUnclosedDate(null);
        }

        return response;
    }

    // =========================================================================
    // MARKET DAYBOOK OPERATIONS (MARKET-WISE AGGREGATION & CLOSURE)
    // =========================================================================

    @Override
    @Transactional
    public MarketDayBookResponse getTodayMarketDayBook(UUID marketId) {
        LocalDate activeDate = getActiveMarketDayBookDate(marketId);
        return getOrCreateMarketDayBook(marketId, activeDate);
    }

    @Override
    @Transactional
    public MarketDayBookResponse getOrCreateMarketDayBook(UUID marketId, LocalDate date) {
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new RuntimeException("Market not found"));

        syncMarketDayBook(marketId, date);

        MarketDayBook mdb = marketDayBookRepository.findByMarketIdAndDate(marketId, date)
                .orElseGet(() -> {
                    MarketDayBook newMdb = new MarketDayBook();
                    newMdb.setMarketId(marketId);
                    newMdb.setDate(date);
                    newMdb.setStatus(DayBookStatus.OPEN);
                    return marketDayBookRepository.save(newMdb);
                });

        // Build employee summaries for all active employees assigned to this market
        List<EmployeeMarketAssignment> assignments = assignmentRepository.findByMarketIdAndIsActiveTrue(marketId);
        List<EmployeeDayBookSummary> summaries = new ArrayList<>();

        for (EmployeeMarketAssignment asg : assignments) {
            User emp = asg.getEmployee();
            if (emp == null) continue;

            DayBook empDb = dayBookRepository.findByEmployeeIdAndDate(emp.getId(), date)
                    .orElseGet(() -> {
                        getOrCreateDayBook(emp.getId(), date);
                        return dayBookRepository.findByEmployeeIdAndDate(emp.getId(), date).orElse(null);
                    });

            if (empDb != null) {
                summaries.add(EmployeeDayBookSummary.builder()
                        .employeeId(emp.getId())
                        .employeeName(emp.getFullName())
                        .employeeCode(emp.getEmployeeCode())
                        .dayBookId(empDb.getId())
                        .openingBalance(empDb.getOpeningBalance())
                        .collections(empDb.getCollections())
                        .incomingTransfers(empDb.getIncomingTransfers())
                        .cashIncomingTransfers(empDb.getCashIncomingTransfers())
                        .spends(empDb.getSpends())
                        .loansDisbursed(empDb.getLoansDisbursed())
                        .outgoingTransfers(empDb.getOutgoingTransfers())
                        .cashOutgoingTransfers(empDb.getCashOutgoingTransfers())
                        .officeRemittance(empDb.getOfficeRemittance())
                        .closingBalance(empDb.getClosingBalance())
                        .status(empDb.getStatus())
                        .build());
            }
        }

        // Determine if previous market day was closed
        List<MarketDayBook> pastBooks = marketDayBookRepository.findByMarketIdOrderByDateDesc(marketId);
        MarketDayBook unclosedPast = pastBooks.stream()
                .filter(pb -> pb.getDate().isBefore(date) && pb.getStatus() != DayBookStatus.CLOSED)
                .findFirst()
                .orElse(null);

        boolean prevClosed = unclosedPast == null;
        LocalDate unclosedDate = unclosedPast != null ? unclosedPast.getDate() : null;

        return MarketDayBookResponse.builder()
                .id(mdb.getId())
                .marketId(marketId)
                .marketName(market.getMarketName())
                .date(mdb.getDate())
                .openingBalance(mdb.getTotalOpeningBalance() != null ? mdb.getTotalOpeningBalance() : BigDecimal.ZERO)
                .collections(mdb.getTotalCollections() != null ? mdb.getTotalCollections() : BigDecimal.ZERO)
                .incomingTransfers(mdb.getTotalIncomingTransfers() != null ? mdb.getTotalIncomingTransfers() : BigDecimal.ZERO)
                .cashIncomingTransfers(mdb.getTotalCashIncomingTransfers() != null ? mdb.getTotalCashIncomingTransfers() : BigDecimal.ZERO)
                .spends(mdb.getTotalSpends() != null ? mdb.getTotalSpends() : BigDecimal.ZERO)
                .loansDisbursed(mdb.getTotalLoansDisbursed() != null ? mdb.getTotalLoansDisbursed() : BigDecimal.ZERO)
                .outgoingTransfers(mdb.getTotalOutgoingTransfers() != null ? mdb.getTotalOutgoingTransfers() : BigDecimal.ZERO)
                .cashOutgoingTransfers(mdb.getTotalCashOutgoingTransfers() != null ? mdb.getTotalCashOutgoingTransfers() : BigDecimal.ZERO)
                .officeRemittance(mdb.getTotalOfficeRemittance() != null ? mdb.getTotalOfficeRemittance() : BigDecimal.ZERO)
                .closingBalance(mdb.getTotalClosingBalance() != null ? mdb.getTotalClosingBalance() : BigDecimal.ZERO)
                .status(mdb.getStatus())
                .previousDayClosed(prevClosed)
                .unclosedDate(unclosedDate)
                .employeeSummaries(summaries)
                .build();
    }

    @Override
    @Transactional
    public void syncMarketDayBook(UUID marketId, LocalDate date) {
        MarketDayBook mdb = marketDayBookRepository.findByMarketIdAndDate(marketId, date)
                .orElseGet(() -> {
                    MarketDayBook newMdb = new MarketDayBook();
                    newMdb.setMarketId(marketId);
                    newMdb.setDate(date);
                    newMdb.setStatus(DayBookStatus.OPEN);
                    return newMdb;
                });

        if (mdb.getStatus() == DayBookStatus.CLOSED) {
            return;
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        // 1. Total Collections for loans in this market
        List<LoanCollection> cols = loanCollectionRepository.findCollectionsForEmployeeOrMarketsBetween(
                UUID.randomUUID(), Collections.singletonList(marketId), start, end
        );
        BigDecimal totalColl = BigDecimal.ZERO;
        if (cols != null && !cols.isEmpty()) {
            totalColl = cols.stream()
                    .map(c -> c.getCollectedAmount() != null ? c.getCollectedAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        mdb.setTotalCollections(totalColl);

        // 2. Total Loans Disbursed in this market
        List<Loan> disbursedLoans;
        if (date.isEqual(LocalDate.of(2026, 9, 29))) {
            LocalDateTime cutoffEnd = date.atTime(23, 59, 59);
            disbursedLoans = loanRepository.findDisbursedLoansForEmployeeOrMarketsUpTo(
                    UUID.randomUUID(), Collections.singletonList(marketId), cutoffEnd
            );
        } else {
            disbursedLoans = loanRepository.findDisbursedLoansForEmployeeOrMarketsBetween(
                    UUID.randomUUID(), Collections.singletonList(marketId), start, end
            );
        }
        BigDecimal totalDisbursed = BigDecimal.ZERO;
        if (disbursedLoans != null && !disbursedLoans.isEmpty()) {
            totalDisbursed = disbursedLoans.stream()
                    .map(l -> l.getDisbursedAmount() != null ? l.getDisbursedAmount() : (l.getApprovedAmount() != null ? l.getApprovedAmount() : BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        mdb.setTotalLoansDisbursed(totalDisbursed);

        // 3. Spends, Remittance, and Transfers from employees in this market
        List<EmployeeMarketAssignment> assignments = assignmentRepository.findByMarketIdAndIsActiveTrue(marketId);
        List<UUID> empIds = assignments.stream().map(a -> a.getEmployee().getId()).collect(Collectors.toList());

        BigDecimal totalSpends = BigDecimal.ZERO;
        BigDecimal totalOfficeRemit = BigDecimal.ZERO;
        BigDecimal totalOnlineIn = BigDecimal.ZERO;
        BigDecimal totalCashIn = BigDecimal.ZERO;
        BigDecimal totalOnlineOut = BigDecimal.ZERO;
        BigDecimal totalCashOut = BigDecimal.ZERO;

        for (UUID empId : empIds) {
            Optional<DayBook> empDbOpt = dayBookRepository.findByEmployeeIdAndDate(empId, date);
            if (empDbOpt.isPresent()) {
                DayBook edb = empDbOpt.get();
                if (edb.getSpends() != null) totalSpends = totalSpends.add(edb.getSpends());
                if (edb.getOfficeRemittance() != null) totalOfficeRemit = totalOfficeRemit.add(edb.getOfficeRemittance());
                if (edb.getIncomingTransfers() != null) totalOnlineIn = totalOnlineIn.add(edb.getIncomingTransfers());
                if (edb.getCashIncomingTransfers() != null) totalCashIn = totalCashIn.add(edb.getCashIncomingTransfers());
                if (edb.getOutgoingTransfers() != null) totalOnlineOut = totalOnlineOut.add(edb.getOutgoingTransfers());
                if (edb.getCashOutgoingTransfers() != null) totalCashOut = totalCashOut.add(edb.getCashOutgoingTransfers());
            }
        }

        mdb.setTotalSpends(totalSpends);
        mdb.setTotalOfficeRemittance(totalOfficeRemit);
        mdb.setTotalIncomingTransfers(totalOnlineIn);
        mdb.setTotalCashIncomingTransfers(totalCashIn);
        mdb.setTotalOutgoingTransfers(totalOnlineOut);
        mdb.setTotalCashOutgoingTransfers(totalCashOut);

        // Calculate opening balance from previous market daybook
        List<MarketDayBook> pastBooks = marketDayBookRepository.findByMarketIdOrderByDateDesc(marketId);
        BigDecimal openingBal = BigDecimal.ZERO;
        for (MarketDayBook pb : pastBooks) {
            if (pb.getDate().isBefore(date)) {
                openingBal = pb.getTotalClosingBalance() != null ? pb.getTotalClosingBalance() : BigDecimal.ZERO;
                break;
            }
        }
        mdb.setTotalOpeningBalance(openingBal);

        // Calculate closing balance
        BigDecimal closing = openingBal
                .add(totalColl)
                .add(totalOnlineIn)
                .add(totalCashIn)
                .subtract(totalSpends)
                .subtract(totalDisbursed)
                .subtract(totalOnlineOut)
                .subtract(totalCashOut)
                .subtract(totalOfficeRemit);

        mdb.setTotalClosingBalance(closing);
        marketDayBookRepository.save(mdb);
        propagateMarketClosingBalanceForward(mdb);
    }

    private void propagateMarketClosingBalanceForward(MarketDayBook startMdb) {
        if (startMdb == null || startMdb.getDate() == null) return;
        UUID marketId = startMdb.getMarketId();
        LocalDate currDate = startMdb.getDate();
        BigDecimal prevClosing = startMdb.getTotalClosingBalance();

        List<MarketDayBook> subsequent = marketDayBookRepository.findByMarketIdOrderByDateAsc(marketId);
        for (MarketDayBook nextMdb : subsequent) {
            if (nextMdb.getDate().isAfter(currDate)) {
                nextMdb.setTotalOpeningBalance(prevClosing);
                BigDecimal c = nextMdb.getTotalOpeningBalance()
                        .add(nextMdb.getTotalCollections() != null ? nextMdb.getTotalCollections() : BigDecimal.ZERO)
                        .add(nextMdb.getTotalIncomingTransfers() != null ? nextMdb.getTotalIncomingTransfers() : BigDecimal.ZERO)
                        .add(nextMdb.getTotalCashIncomingTransfers() != null ? nextMdb.getTotalCashIncomingTransfers() : BigDecimal.ZERO)
                        .subtract(nextMdb.getTotalSpends() != null ? nextMdb.getTotalSpends() : BigDecimal.ZERO)
                        .subtract(nextMdb.getTotalLoansDisbursed() != null ? nextMdb.getTotalLoansDisbursed() : BigDecimal.ZERO)
                        .subtract(nextMdb.getTotalOutgoingTransfers() != null ? nextMdb.getTotalOutgoingTransfers() : BigDecimal.ZERO)
                        .subtract(nextMdb.getTotalCashOutgoingTransfers() != null ? nextMdb.getTotalCashOutgoingTransfers() : BigDecimal.ZERO)
                        .subtract(nextMdb.getTotalOfficeRemittance() != null ? nextMdb.getTotalOfficeRemittance() : BigDecimal.ZERO);
                nextMdb.setTotalClosingBalance(c);
                marketDayBookRepository.save(nextMdb);
                prevClosing = nextMdb.getTotalClosingBalance();
            }
        }
    }

    @Override
    @Transactional
    public MarketDayBookResponse approveMarketClosure(UUID marketId, LocalDate date) {
        syncMarketDayBook(marketId, date);

        MarketDayBook mdb = marketDayBookRepository.findByMarketIdAndDate(marketId, date)
                .orElseThrow(() -> new RuntimeException("Market DayBook not found for " + date));

        mdb.setStatus(DayBookStatus.CLOSED);
        marketDayBookRepository.save(mdb);
        propagateMarketClosingBalanceForward(mdb);

        // Also close all active employee daybooks in this market for this date
        List<EmployeeMarketAssignment> assignments = assignmentRepository.findByMarketIdAndIsActiveTrue(marketId);
        for (EmployeeMarketAssignment asg : assignments) {
            dayBookRepository.findByEmployeeIdAndDate(asg.getEmployee().getId(), date).ifPresent(edb -> {
                edb.setStatus(DayBookStatus.CLOSED);
                dayBookRepository.save(edb);
                propagateClosingBalanceForward(edb);
            });
        }

        return getOrCreateMarketDayBook(marketId, date);
    }

    @Override
    @Transactional
    public MarketDayBookResponse rejectMarketClosure(UUID marketId, LocalDate date) {
        MarketDayBook mdb = marketDayBookRepository.findByMarketIdAndDate(marketId, date)
                .orElseThrow(() -> new RuntimeException("Market DayBook not found for " + date));

        mdb.setStatus(DayBookStatus.OPEN);
        marketDayBookRepository.save(mdb);

        List<EmployeeMarketAssignment> assignments = assignmentRepository.findByMarketIdAndIsActiveTrue(marketId);
        for (EmployeeMarketAssignment asg : assignments) {
            dayBookRepository.findByEmployeeIdAndDate(asg.getEmployee().getId(), date).ifPresent(edb -> {
                edb.setStatus(DayBookStatus.OPEN);
                dayBookRepository.save(edb);
            });
        }

        return getOrCreateMarketDayBook(marketId, date);
    }

    @Override
    @Transactional
    public MarketDayBookResponse reopenMarketDayBook(UUID marketId, LocalDate date) {
        MarketDayBook mdb = marketDayBookRepository.findByMarketIdAndDate(marketId, date)
                .orElseThrow(() -> new RuntimeException("Market DayBook not found for " + date));

        mdb.setStatus(DayBookStatus.OPEN);
        marketDayBookRepository.save(mdb);

        List<EmployeeMarketAssignment> assignments = assignmentRepository.findByMarketIdAndIsActiveTrue(marketId);
        for (EmployeeMarketAssignment asg : assignments) {
            dayBookRepository.findByEmployeeIdAndDate(asg.getEmployee().getId(), date).ifPresent(edb -> {
                edb.setStatus(DayBookStatus.OPEN);
                dayBookRepository.save(edb);
            });
        }

        return getOrCreateMarketDayBook(marketId, date);
    }

    @Override
    @Transactional
    public List<DayBookTransaction> getMarketTransactions(UUID marketId, LocalDate date) {
        List<EmployeeMarketAssignment> assignments = assignmentRepository.findByMarketIdAndIsActiveTrue(marketId);
        List<DayBookTransaction> all = new ArrayList<>();
        for (EmployeeMarketAssignment asg : assignments) {
            all.addAll(getTransactions(asg.getEmployee().getId(), date));
        }
        all.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return all;
    }

    // =========================================================================
    // LOAN DELETION DAYBOOK CLEANUP
    // =========================================================================

    @Override
    @Transactional
    public void cleanDayBookForDeletedLoan(String loanCode, UUID loanId) {
        if (loanCode == null || loanCode.trim().isEmpty()) {
            return;
        }

        List<DayBookTransaction> matchedTxs = dayBookTransactionRepository.findByRemarksContaining(loanCode);
        if (matchedTxs == null || matchedTxs.isEmpty()) {
            return;
        }

        List<DayBook> affectedDayBooks = new ArrayList<>();
        for (DayBookTransaction tx : matchedTxs) {
            DayBook db = tx.getDayBook();
            if (db != null && !affectedDayBooks.contains(db)) {
                affectedDayBooks.add(db);
            }
            dayBookTransactionRepository.delete(tx);
        }

        for (DayBook db : affectedDayBooks) {
            // Re-sync this daybook from DB (since loan and its collections will be deleted)
            syncDayBookFromDb(db, db.getEmployeeId(), db.getDate());
            db.setClosingBalance(calculateClosingBalance(db));
            dayBookRepository.save(db);
            propagateClosingBalanceForward(db);

            assignmentRepository.findFirstByEmployeeIdAndIsActiveTrue(db.getEmployeeId())
                    .ifPresent(asg -> syncMarketDayBook(asg.getMarket().getId(), db.getDate()));
        }
    }

    @Scheduled(cron = "0 0 0 * * ?") // Midnight
    @Transactional
    public void forceClosePendingDayBooks() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<DayBook> pendingBooks = dayBookRepository.findByDateAndStatusNot(yesterday, DayBookStatus.CLOSED);

        for (DayBook db : pendingBooks) {
            db.setStatus(DayBookStatus.CLOSED);
            dayBookRepository.save(db);
            checkAndCloseMarketDayBook(db);
        }
    }
}
