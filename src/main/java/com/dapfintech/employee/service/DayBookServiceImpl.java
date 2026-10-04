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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    private List<EmployeeMarketAssignment> getAssignmentsForEmployee(UUID employeeId) {
        List<EmployeeMarketAssignment> list = assignmentRepository.findActiveOrNullByEmployeeId(employeeId);
        if (list == null || list.isEmpty()) {
            list = assignmentRepository.findByEmployeeId(employeeId);
        }
        return list != null ? list : Collections.emptyList();
    }

    private List<EmployeeMarketAssignment> getAssignmentsForMarket(UUID marketId) {
        List<EmployeeMarketAssignment> list = assignmentRepository.findActiveOrNullByMarketId(marketId);
        if (list == null || list.isEmpty()) {
            list = assignmentRepository.findByMarketId(marketId);
        }
        return list != null ? list : Collections.emptyList();
    }

    // =========================================================================
    // SEQUENTIAL DATE RESOLUTION
    // =========================================================================

    public boolean isMarketClosedOnDate(UUID marketId, LocalDate date) {
        if (marketId == null || date == null) return false;
        if (marketDayBookRepository.findByMarketIdAndDate(marketId, date)
                .map(m -> m.getStatus() == DayBookStatus.CLOSED).orElse(false)) {
            return true;
        }
        List<EmployeeMarketAssignment> assigns = getAssignmentsForMarket(marketId);
        for (EmployeeMarketAssignment asg : assigns) {
            if (asg.getEmployee() != null) {
                if (dayBookRepository.findByEmployeeIdAndDate(asg.getEmployee().getId(), date)
                        .map(d -> d.getStatus() == DayBookStatus.CLOSED).orElse(false)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public LocalDate getActiveMarketDayBookDate(UUID marketId) {
        if (marketId == null) {
            return LocalDate.now();
        }

        // 1. Check from marketDayBookRepository for latest closed date
        LocalDate latestClosed = null;
        List<MarketDayBook> pastClosed = marketDayBookRepository
                .findByMarketIdAndStatusOrderByDateDesc(marketId, DayBookStatus.CLOSED);
        if (!pastClosed.isEmpty()) {
            latestClosed = pastClosed.get(0).getDate();
        }

        // 2. Also check all employee daybooks for this market for any closed dates
        List<EmployeeMarketAssignment> assigns = getAssignmentsForMarket(marketId);
        for (EmployeeMarketAssignment asg : assigns) {
            if (asg.getEmployee() != null) {
                List<DayBook> closedEmp = dayBookRepository
                        .findByEmployeeIdAndStatusOrderByDateDesc(asg.getEmployee().getId(), DayBookStatus.CLOSED);
                if (!closedEmp.isEmpty()) {
                    LocalDate empLatestClosed = closedEmp.get(0).getDate();
                    if (latestClosed == null || empLatestClosed.isAfter(latestClosed)) {
                        latestClosed = empLatestClosed;
                    }
                }
            }
        }

        // 3. If there is any closed date, advance candidate until isMarketClosedOnDate is false
        if (latestClosed != null) {
            LocalDate candidate = latestClosed.plusDays(1);
            while (isMarketClosedOnDate(marketId, candidate)) {
                candidate = candidate.plusDays(1);
            }
            return candidate;
        }

        // 4. If no closed date exists anywhere, check earliest existing unclosed market daybook
        List<MarketDayBook> all = marketDayBookRepository.findByMarketIdOrderByDateAsc(marketId);
        for (MarketDayBook m : all) {
            if (m.getStatus() != DayBookStatus.CLOSED && !isMarketClosedOnDate(marketId, m.getDate())) {
                return m.getDate();
            }
        }

        return LocalDate.now();
    }

    @Override
    public LocalDate getActiveDayBookDate(UUID employeeId) {
        if (employeeId == null) {
            return LocalDate.now();
        }

        // Always resolve date from employee's assigned market
        List<EmployeeMarketAssignment> asgList = getAssignmentsForEmployee(employeeId);
        for (EmployeeMarketAssignment asg : asgList) {
            if (asg.getMarket() != null) {
                return getActiveMarketDayBookDate(asg.getMarket().getId());
            }
        }

        // Fallback for unassigned employee: check employee's own closed daybooks
        List<DayBook> pastClosed = dayBookRepository
                .findByEmployeeIdAndStatusOrderByDateDesc(employeeId, DayBookStatus.CLOSED);

        if (!pastClosed.isEmpty()) {
            LocalDate latestClosed = pastClosed.get(0).getDate();
            LocalDate candidate = latestClosed.plusDays(1);
            while (dayBookRepository.findByEmployeeIdAndDate(employeeId, candidate)
                    .map(d -> d.getStatus() == DayBookStatus.CLOSED).orElse(false)) {
                candidate = candidate.plusDays(1);
            }
            return candidate;
        }

        List<DayBook> all = dayBookRepository.findByEmployeeIdOrderByDateAsc(employeeId);
        for (DayBook d : all) {
            if (d.getStatus() != DayBookStatus.CLOSED) {
                return d.getDate();
            }
        }

        return LocalDate.now();
    }

    // =========================================================================
    // EMPLOYEE DAYBOOK OPERATIONS (MARKET-WISE SYNCHRONIZED)
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
        List<EmployeeMarketAssignment> empAsgs = getAssignmentsForEmployee(employeeId);
        Market empMarket = !empAsgs.isEmpty() ? empAsgs.get(0).getMarket() : null;

        if (empMarket != null) {
            UUID marketId = empMarket.getId();
            MarketDayBookResponse mdb = getOrCreateMarketDayBook(marketId, date);

            // Synchronize employee DayBook with Market DayBook so both tables stay clean
            DayBook dayBook = dayBookRepository.findByEmployeeIdAndDate(employeeId, date)
                    .orElseGet(() -> {
                        DayBook ndb = new DayBook();
                        ndb.setEmployeeId(employeeId);
                        ndb.setDate(date);
                        return dayBookRepository.save(ndb);
                    });

            dayBook.setOpeningBalance(mdb.getOpeningBalance());
            dayBook.setCollections(mdb.getCollections());
            dayBook.setIncomingTransfers(mdb.getIncomingTransfers());
            dayBook.setCashIncomingTransfers(mdb.getCashIncomingTransfers());
            dayBook.setSpends(mdb.getSpends());
            dayBook.setLoansDisbursed(mdb.getLoansDisbursed());
            dayBook.setOutgoingTransfers(mdb.getOutgoingTransfers());
            dayBook.setCashOutgoingTransfers(mdb.getCashOutgoingTransfers());
            dayBook.setOfficeRemittance(mdb.getOfficeRemittance());
            dayBook.setTransferToOthers(mdb.getTransferToOthers());
            dayBook.setClosingBalance(mdb.getClosingBalance());
            dayBook.setStatus(mdb.getStatus());
            dayBookRepository.save(dayBook);

            DayBookResponse res = mapToResponse(dayBook);
            res.setMarketId(marketId);
            res.setMarketName(empMarket.getMarketName());
            res.setPreviousDayClosed(mdb.isPreviousDayClosed());
            res.setUnclosedDate(mdb.getUnclosedDate());
            return res;
        }

        // Fallback for unassigned employee
        DayBook dayBook = dayBookRepository.findByEmployeeIdAndDate(employeeId, date)
                .orElseGet(() -> {
                    DayBook ndb = new DayBook();
                    ndb.setEmployeeId(employeeId);
                    ndb.setDate(date);
                    ndb.setStatus(DayBookStatus.OPEN);
                    return dayBookRepository.save(ndb);
                });
        return mapToResponse(dayBook);
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
        List<EmployeeMarketAssignment> empAsgs = getAssignmentsForEmployee(employeeId);
        Market empMarket = !empAsgs.isEmpty() ? empAsgs.get(0).getMarket() : null;

        UUID marketId = empMarket != null ? empMarket.getId() : null;
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO;
        String type = request.getType() != null ? request.getType().toUpperCase() : "";

        if (marketId != null) {
            MarketDayBook mdb = marketDayBookRepository.findByMarketIdAndDate(marketId, date)
                    .orElseGet(() -> {
                        getOrCreateMarketDayBook(marketId, date);
                        return marketDayBookRepository.findByMarketIdAndDate(marketId, date).get();
                    });

            // Sequential DayBook rule: If any past MarketDayBook before `date` is not closed, no values can go into `date`'s DayBook!
            List<MarketDayBook> pastBooks = marketDayBookRepository.findByMarketIdOrderByDateDesc(marketId);
            MarketDayBook unclosedPast = pastBooks.stream()
                    .filter(pb -> pb.getDate().isBefore(date) && pb.getStatus() != DayBookStatus.CLOSED)
                    .findFirst()
                    .orElse(null);

            if (unclosedPast != null) {
                throw new RuntimeException("Cannot add transaction to DayBook for " + date +
                        " because previous Market DayBook for " + unclosedPast.getDate() +
                        " is not closed yet. Please close " + unclosedPast.getDate() + " first.");
            }

            if (mdb.getStatus() == DayBookStatus.CLOSED) {
                // If this is a collection, allow posting it to this date and propagate balances forward
                if (!type.equals("COLLECTIONS")) {
                    throw new RuntimeException("Cannot add transaction to a closed Market DayBook for " + date + ".");
                }
            }

            boolean isTransfer = type.contains("TRANSFER") || type.contains("REMITTANCE");
            if (mdb.getStatus() == DayBookStatus.PENDING_CLOSURE && !isTransfer && !type.equals("COLLECTIONS")) {
                throw new RuntimeException("Cannot add transaction to a daybook that is pending closure.");
            }

            if (mdb.getTotalSpends() == null) mdb.setTotalSpends(BigDecimal.ZERO);
            if (mdb.getTotalCollections() == null) mdb.setTotalCollections(BigDecimal.ZERO);
            if (mdb.getTotalLoansDisbursed() == null) mdb.setTotalLoansDisbursed(BigDecimal.ZERO);
            if (mdb.getTotalOfficeRemittance() == null) mdb.setTotalOfficeRemittance(BigDecimal.ZERO);
            if (mdb.getTotalIncomingTransfers() == null) mdb.setTotalIncomingTransfers(BigDecimal.ZERO);
            if (mdb.getTotalCashIncomingTransfers() == null) mdb.setTotalCashIncomingTransfers(BigDecimal.ZERO);
            if (mdb.getTotalOutgoingTransfers() == null) mdb.setTotalOutgoingTransfers(BigDecimal.ZERO);
            if (mdb.getTotalCashOutgoingTransfers() == null) mdb.setTotalCashOutgoingTransfers(BigDecimal.ZERO);
            if (mdb.getTotalTransferToOthers() == null) mdb.setTotalTransferToOthers(BigDecimal.ZERO);

            switch (type) {
                case "SPENDS":
                    if (request.getRemarks() == null || request.getRemarks().trim().isEmpty()) {
                        throw new RuntimeException("Remarks are mandatory for spends.");
                    }
                    mdb.setTotalSpends(mdb.getTotalSpends().add(amount));
                    break;
                case "TRANSFER_TO_OTHERS":
                case "PAY_TO_OTHERS":
                    if (request.getRemarks() == null || request.getRemarks().trim().isEmpty()) {
                        throw new RuntimeException("Remarks are mandatory for Transfer to Others (specify who to pay).");
                    }
                    mdb.setTotalTransferToOthers(mdb.getTotalTransferToOthers().add(amount));
                    break;
                case "COLLECTIONS":
                    mdb.setTotalCollections(mdb.getTotalCollections().add(amount));
                    break;
                case "LOANS_DISBURSED":
                    mdb.setTotalLoansDisbursed(mdb.getTotalLoansDisbursed().add(amount));
                    break;
                case "OFFICE_REMITTANCE":
                    mdb.setTotalOfficeRemittance(mdb.getTotalOfficeRemittance().add(amount));
                    // Trigger internal transfer to master admin
                    User empUser = userRepository.findById(employeeId).orElse(null);
                    User masterAdmin = userRepository.findByRoleRoleName("MASTER_ADMIN").stream().findFirst()
                            .orElseGet(() -> userRepository.findByRoleRoleName("ADMIN").stream().findFirst().orElse(null));
                    if (empUser != null && masterAdmin != null) {
                        InternalTransfer transfer = InternalTransfer.builder()
                                .sender(empUser)
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
                    mdb.setTotalIncomingTransfers(mdb.getTotalIncomingTransfers().add(amount));
                    break;
                case "CASH_INCOMING_TRANSFER":
                    mdb.setTotalCashIncomingTransfers(mdb.getTotalCashIncomingTransfers().add(amount));
                    break;
                case "OUTGOING_TRANSFER":
                    mdb.setTotalOutgoingTransfers(mdb.getTotalOutgoingTransfers().add(amount));
                    break;
                case "CASH_OUTGOING_TRANSFER":
                    mdb.setTotalCashOutgoingTransfers(mdb.getTotalCashOutgoingTransfers().add(amount));
                    break;
                default:
                    throw new RuntimeException("Unknown transaction type: " + type);
            }

            updateMarketClosingBalance(mdb);
            marketDayBookRepository.save(mdb);
            propagateMarketClosingBalanceForward(mdb);

            // Record transaction for the market
            DayBookTransaction tx = new DayBookTransaction();
            tx.setMarketId(marketId);
            tx.setMarketDayBook(mdb);
            tx.setEmployeeId(employeeId);
            tx.setType(type);
            tx.setAmount(amount);
            tx.setRemarks(request.getRemarks());
            tx.setCreatedAt(date.isEqual(LocalDate.now()) ? LocalDateTime.now() : date.atTime(LocalTime.now()));
            dayBookTransactionRepository.save(tx);

            // Synchronize all employees in this market
            List<EmployeeMarketAssignment> marketEmployees = getAssignmentsForMarket(marketId);
            for (EmployeeMarketAssignment asg : marketEmployees) {
                if (asg.getEmployee() != null) {
                    DayBook edb = dayBookRepository.findByEmployeeIdAndDate(asg.getEmployee().getId(), date)
                            .orElseGet(() -> {
                                DayBook newDb = new DayBook();
                                newDb.setEmployeeId(asg.getEmployee().getId());
                                newDb.setDate(date);
                                return dayBookRepository.save(newDb);
                            });
                    edb.setOpeningBalance(mdb.getTotalOpeningBalance());
                    edb.setCollections(mdb.getTotalCollections());
                    edb.setIncomingTransfers(mdb.getTotalIncomingTransfers());
                    edb.setCashIncomingTransfers(mdb.getTotalCashIncomingTransfers());
                    edb.setSpends(mdb.getTotalSpends());
                    edb.setLoansDisbursed(mdb.getTotalLoansDisbursed());
                    edb.setOutgoingTransfers(mdb.getTotalOutgoingTransfers());
                    edb.setCashOutgoingTransfers(mdb.getTotalCashOutgoingTransfers());
                    edb.setOfficeRemittance(mdb.getTotalOfficeRemittance());
                    edb.setTransferToOthers(mdb.getTotalTransferToOthers());
                    edb.setClosingBalance(mdb.getTotalClosingBalance());
                    edb.setStatus(mdb.getStatus());
                    dayBookRepository.save(edb);
                    propagateClosingBalanceForward(edb);
                }
            }

            return getOrCreateDayBook(employeeId, date);
        }

        throw new RuntimeException("Employee is not assigned to any active market.");
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
        List<EmployeeMarketAssignment> asgs = getAssignmentsForEmployee(employeeId);
        if (!asgs.isEmpty() && asgs.get(0).getMarket() != null) {
            UUID marketId = asgs.get(0).getMarket().getId();
            requestMarketClosure(marketId, date);
            return getOrCreateDayBook(employeeId, date);
        }
        throw new RuntimeException("Employee is not assigned to any market.");
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
        List<EmployeeMarketAssignment> asgs = getAssignmentsForEmployee(employeeId);
        if (!asgs.isEmpty() && asgs.get(0).getMarket() != null) {
            UUID marketId = asgs.get(0).getMarket().getId();
            cancelMarketClosure(marketId, date);
            return getOrCreateDayBook(employeeId, date);
        }
        throw new RuntimeException("Employee is not assigned to any market.");
    }

    @Override
    @Transactional
    public DayBookResponse approveClosure(UUID dayBookId) {
        DayBook dayBook = dayBookRepository.findById(dayBookId)
                .orElseThrow(() -> new RuntimeException("DayBook not found"));
        UUID employeeId = dayBook.getEmployeeId();
        LocalDate targetDate = dayBook.getDate();

        List<EmployeeMarketAssignment> asgs = getAssignmentsForEmployee(employeeId);
        if (!asgs.isEmpty() && asgs.get(0).getMarket() != null) {
            approveMarketClosure(asgs.get(0).getMarket().getId(), targetDate);
        } else {
            dayBook.setStatus(DayBookStatus.CLOSED);
            dayBookRepository.save(dayBook);
            propagateClosingBalanceForward(dayBook);
        }
        return mapToResponse(dayBook);
    }

    @Override
    @Transactional
    public DayBookResponse rejectClosure(UUID dayBookId) {
        DayBook dayBook = dayBookRepository.findById(dayBookId)
                .orElseThrow(() -> new RuntimeException("DayBook not found"));
        UUID employeeId = dayBook.getEmployeeId();
        LocalDate date = dayBook.getDate();

        List<EmployeeMarketAssignment> asgs = getAssignmentsForEmployee(employeeId);
        if (!asgs.isEmpty() && asgs.get(0).getMarket() != null) {
            rejectMarketClosure(asgs.get(0).getMarket().getId(), date);
        } else {
            dayBook.setStatus(DayBookStatus.OPEN);
            dayBookRepository.save(dayBook);
        }
        return mapToResponse(dayBook);
    }

    @Override
    @Transactional
    public DayBookResponse reopenDayBook(UUID dayBookId) {
        DayBook dayBook = dayBookRepository.findById(dayBookId)
                .orElseThrow(() -> new RuntimeException("DayBook not found"));
        UUID employeeId = dayBook.getEmployeeId();
        LocalDate date = dayBook.getDate();

        List<EmployeeMarketAssignment> asgs = getAssignmentsForEmployee(employeeId);
        if (!asgs.isEmpty() && asgs.get(0).getMarket() != null) {
            reopenMarketDayBook(asgs.get(0).getMarket().getId(), date);
        } else {
            dayBook.setStatus(DayBookStatus.OPEN);
            dayBookRepository.save(dayBook);
        }
        return mapToResponse(dayBook);
    }

    @Override
    @Transactional
    public DayBookResponse updateDayBook(UUID dayBookId, UpdateDayBookRequest request) {
        DayBook dayBook = dayBookRepository.findById(dayBookId)
                .orElseThrow(() -> new RuntimeException("DayBook not found"));

        List<EmployeeMarketAssignment> asgs = getAssignmentsForEmployee(dayBook.getEmployeeId());
        if (!asgs.isEmpty() && asgs.get(0).getMarket() != null) {
            updateMarketDayBook(asgs.get(0).getMarket().getId(), dayBook.getDate(), request);
            return getOrCreateDayBook(dayBook.getEmployeeId(), dayBook.getDate());
        }

        if (request.getOpeningBalance() != null) dayBook.setOpeningBalance(request.getOpeningBalance());
        if (request.getCollections() != null) dayBook.setCollections(request.getCollections());
        if (request.getIncomingTransfers() != null) dayBook.setIncomingTransfers(request.getIncomingTransfers());
        if (request.getCashIncomingTransfers() != null) dayBook.setCashIncomingTransfers(request.getCashIncomingTransfers());
        if (request.getSpends() != null) dayBook.setSpends(request.getSpends());
        if (request.getLoansDisbursed() != null) dayBook.setLoansDisbursed(request.getLoansDisbursed());
        if (request.getOutgoingTransfers() != null) dayBook.setOutgoingTransfers(request.getOutgoingTransfers());
        if (request.getCashOutgoingTransfers() != null) dayBook.setCashOutgoingTransfers(request.getCashOutgoingTransfers());
        if (request.getOfficeRemittance() != null) dayBook.setOfficeRemittance(request.getOfficeRemittance());
        if (request.getTransferToOthers() != null) dayBook.setTransferToOthers(request.getTransferToOthers());

        dayBook.setClosingBalance(calculateClosingBalance(dayBook));
        dayBook = dayBookRepository.save(dayBook);
        propagateClosingBalanceForward(dayBook);

        return mapToResponse(dayBook);
    }

    @Override
    @Transactional
    public DayBookResponse getDayBookByDate(UUID employeeId, LocalDate date) {
        return getOrCreateDayBook(employeeId, date);
    }

    @Override
    public List<DayBookResponse> getEmployeeDayBooks(UUID employeeId) {
        List<EmployeeMarketAssignment> asgs = getAssignmentsForEmployee(employeeId);
        if (!asgs.isEmpty() && asgs.get(0).getMarket() != null) {
            UUID marketId = asgs.get(0).getMarket().getId();
            List<MarketDayBook> mdbs = marketDayBookRepository.findByMarketIdOrderByDateDesc(marketId);
            return mdbs.stream().map(m -> {
                DayBookResponse r = new DayBookResponse();
                r.setId(m.getId());
                r.setMarketId(marketId);
                r.setMarketName(asgs.get(0).getMarket().getMarketName());
                r.setEmployeeId(employeeId);
                r.setDate(m.getDate());
                r.setOpeningBalance(m.getTotalOpeningBalance());
                r.setCollections(m.getTotalCollections());
                r.setIncomingTransfers(m.getTotalIncomingTransfers());
                r.setCashIncomingTransfers(m.getTotalCashIncomingTransfers());
                r.setSpends(m.getTotalSpends());
                r.setLoansDisbursed(m.getTotalLoansDisbursed());
                r.setOutgoingTransfers(m.getTotalOutgoingTransfers());
                r.setCashOutgoingTransfers(m.getTotalCashOutgoingTransfers());
                r.setOfficeRemittance(m.getTotalOfficeRemittance());
                r.setTransferToOthers(m.getTotalTransferToOthers());
                r.setClosingBalance(m.getTotalClosingBalance());
                r.setStatus(m.getStatus());
                return r;
            }).collect(Collectors.toList());
        }
        return dayBookRepository.findByEmployeeIdOrderByDateDesc(employeeId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<DayBookTransaction> getTransactions(UUID employeeId, LocalDate date) {
        List<EmployeeMarketAssignment> asgs = getAssignmentsForEmployee(employeeId);
        if (!asgs.isEmpty() && asgs.get(0).getMarket() != null) {
            return getMarketTransactions(asgs.get(0).getMarket().getId(), date);
        }
        return Collections.emptyList();
    }

    // =========================================================================
    // MARKET DAYBOOK OPERATIONS (MARKET AS PRIMARY UNIT)
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

        // Determine if previous market day was closed
        List<MarketDayBook> pastBooks = marketDayBookRepository.findByMarketIdOrderByDateDesc(marketId);
        MarketDayBook unclosedPast = pastBooks.stream()
                .filter(pb -> pb.getDate().isBefore(date) && pb.getStatus() != DayBookStatus.CLOSED)
                .findFirst()
                .orElse(null);

        boolean prevClosed = unclosedPast == null;
        LocalDate unclosedDate = unclosedPast != null ? unclosedPast.getDate() : null;

        List<EmployeeMarketAssignment> assignments = getAssignmentsForMarket(marketId);
        List<String> marketEmployeeNames = assignments.stream()
                .filter(a -> a.getEmployee() != null)
                .map(a -> a.getEmployee().getFullName())
                .collect(Collectors.toList());

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
                .transferToOthers(mdb.getTotalTransferToOthers() != null ? mdb.getTotalTransferToOthers() : BigDecimal.ZERO)
                .closingBalance(mdb.getTotalClosingBalance() != null ? mdb.getTotalClosingBalance() : BigDecimal.ZERO)
                .status(mdb.getStatus())
                .previousDayClosed(prevClosed)
                .unclosedDate(unclosedDate)
                .marketEmployeeNames(marketEmployeeNames)
                .employeeSummaries(Collections.emptyList()) // No employee-level splits shown
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
                    return marketDayBookRepository.save(newMdb);
                });

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

        // 3. Opening balance from previous market daybook
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
        updateMarketClosingBalance(mdb);
        marketDayBookRepository.save(mdb);
        propagateMarketClosingBalanceForward(mdb);
    }

    private void updateMarketClosingBalance(MarketDayBook m) {
        if (m.getTotalOpeningBalance() == null) m.setTotalOpeningBalance(BigDecimal.ZERO);
        if (m.getTotalCollections() == null) m.setTotalCollections(BigDecimal.ZERO);
        if (m.getTotalIncomingTransfers() == null) m.setTotalIncomingTransfers(BigDecimal.ZERO);
        if (m.getTotalCashIncomingTransfers() == null) m.setTotalCashIncomingTransfers(BigDecimal.ZERO);
        if (m.getTotalSpends() == null) m.setTotalSpends(BigDecimal.ZERO);
        if (m.getTotalLoansDisbursed() == null) m.setTotalLoansDisbursed(BigDecimal.ZERO);
        if (m.getTotalOutgoingTransfers() == null) m.setTotalOutgoingTransfers(BigDecimal.ZERO);
        if (m.getTotalCashOutgoingTransfers() == null) m.setTotalCashOutgoingTransfers(BigDecimal.ZERO);
        if (m.getTotalOfficeRemittance() == null) m.setTotalOfficeRemittance(BigDecimal.ZERO);
        if (m.getTotalTransferToOthers() == null) m.setTotalTransferToOthers(BigDecimal.ZERO);

        BigDecimal c = m.getTotalOpeningBalance()
                .add(m.getTotalCollections())
                .add(m.getTotalIncomingTransfers())
                .add(m.getTotalCashIncomingTransfers())
                .subtract(m.getTotalSpends())
                .subtract(m.getTotalLoansDisbursed())
                .subtract(m.getTotalOutgoingTransfers())
                .subtract(m.getTotalCashOutgoingTransfers())
                .subtract(m.getTotalOfficeRemittance())
                .subtract(m.getTotalTransferToOthers());
        m.setTotalClosingBalance(c);
    }

    private void propagateMarketClosingBalanceForward(MarketDayBook startMdb) {
        if (startMdb == null || startMdb.getDate() == null || startMdb.getMarketId() == null) return;
        UUID marketId = startMdb.getMarketId();
        LocalDate currDate = startMdb.getDate();
        BigDecimal prevClosing = startMdb.getTotalClosingBalance();

        List<MarketDayBook> subsequent = marketDayBookRepository.findByMarketIdOrderByDateAsc(marketId);
        for (MarketDayBook nextMdb : subsequent) {
            if (nextMdb.getDate().isAfter(currDate)) {
                nextMdb.setTotalOpeningBalance(prevClosing);
                updateMarketClosingBalance(nextMdb);
                marketDayBookRepository.save(nextMdb);
                prevClosing = nextMdb.getTotalClosingBalance();
            }
        }
    }

    @Override
    @Transactional
    public MarketDayBookResponse approveMarketClosure(UUID marketId, LocalDate date) {
        MarketDayBook mdb = marketDayBookRepository.findByMarketIdAndDate(marketId, date)
                .orElseGet(() -> {
                    MarketDayBook newMdb = new MarketDayBook();
                    newMdb.setMarketId(marketId);
                    newMdb.setDate(date);
                    newMdb.setStatus(DayBookStatus.CLOSED);
                    return marketDayBookRepository.save(newMdb);
                });

        mdb.setStatus(DayBookStatus.CLOSED);
        marketDayBookRepository.save(mdb);

        syncMarketDayBook(marketId, date);

        mdb = marketDayBookRepository.findByMarketIdAndDate(marketId, date).orElse(mdb);
        mdb.setStatus(DayBookStatus.CLOSED);
        marketDayBookRepository.save(mdb);
        propagateMarketClosingBalanceForward(mdb);

        // Also close all active employee daybooks in this market
        List<EmployeeMarketAssignment> assignments = getAssignmentsForMarket(marketId);
        for (EmployeeMarketAssignment asg : assignments) {
            if (asg.getEmployee() == null) continue;
            UUID empId = asg.getEmployee().getId();
            DayBook edb = dayBookRepository.findByEmployeeIdAndDate(empId, date)
                    .orElseGet(() -> {
                        DayBook newDb = new DayBook();
                        newDb.setEmployeeId(empId);
                        newDb.setDate(date);
                        newDb.setStatus(DayBookStatus.CLOSED);
                        return dayBookRepository.save(newDb);
                    });
            edb.setStatus(DayBookStatus.CLOSED);
            dayBookRepository.save(edb);
            propagateClosingBalanceForward(edb);
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

        List<EmployeeMarketAssignment> assignments = getAssignmentsForMarket(marketId);
        for (EmployeeMarketAssignment asg : assignments) {
            if (asg.getEmployee() == null) continue;
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
        return rejectMarketClosure(marketId, date);
    }

    @Override
    @Transactional
    public MarketDayBookResponse requestMarketClosure(UUID marketId, LocalDate date) {
        MarketDayBook mdb = marketDayBookRepository.findByMarketIdAndDate(marketId, date)
                .orElseGet(() -> {
                    MarketDayBook newMdb = new MarketDayBook();
                    newMdb.setMarketId(marketId);
                    newMdb.setDate(date);
                    newMdb.setStatus(DayBookStatus.PENDING_CLOSURE);
                    return marketDayBookRepository.save(newMdb);
                });

        mdb.setStatus(DayBookStatus.PENDING_CLOSURE);
        marketDayBookRepository.save(mdb);

        List<EmployeeMarketAssignment> assignments = getAssignmentsForMarket(marketId);
        for (EmployeeMarketAssignment asg : assignments) {
            if (asg.getEmployee() == null) continue;
            dayBookRepository.findByEmployeeIdAndDate(asg.getEmployee().getId(), date).ifPresent(edb -> {
                edb.setStatus(DayBookStatus.PENDING_CLOSURE);
                dayBookRepository.save(edb);
            });
        }

        return getOrCreateMarketDayBook(marketId, date);
    }

    @Override
    @Transactional
    public MarketDayBookResponse cancelMarketClosure(UUID marketId, LocalDate date) {
        return rejectMarketClosure(marketId, date);
    }

    @Override
    @Transactional
    public MarketDayBookResponse updateMarketDayBook(UUID marketId, LocalDate date, UpdateDayBookRequest request) {
        MarketDayBook mdb = marketDayBookRepository.findByMarketIdAndDate(marketId, date)
                .orElseGet(() -> {
                    MarketDayBook newMdb = new MarketDayBook();
                    newMdb.setMarketId(marketId);
                    newMdb.setDate(date);
                    newMdb.setStatus(DayBookStatus.OPEN);
                    return marketDayBookRepository.save(newMdb);
                });

        if (request.getOpeningBalance() != null) mdb.setTotalOpeningBalance(request.getOpeningBalance());
        if (request.getCollections() != null) mdb.setTotalCollections(request.getCollections());
        if (request.getIncomingTransfers() != null) mdb.setTotalIncomingTransfers(request.getIncomingTransfers());
        if (request.getCashIncomingTransfers() != null) mdb.setTotalCashIncomingTransfers(request.getCashIncomingTransfers());
        if (request.getSpends() != null) mdb.setTotalSpends(request.getSpends());
        if (request.getLoansDisbursed() != null) mdb.setTotalLoansDisbursed(request.getLoansDisbursed());
        if (request.getOutgoingTransfers() != null) mdb.setTotalOutgoingTransfers(request.getOutgoingTransfers());
        if (request.getCashOutgoingTransfers() != null) mdb.setTotalCashOutgoingTransfers(request.getCashOutgoingTransfers());
        if (request.getOfficeRemittance() != null) mdb.setTotalOfficeRemittance(request.getOfficeRemittance());
        if (request.getTransferToOthers() != null) mdb.setTotalTransferToOthers(request.getTransferToOthers());

        updateMarketClosingBalance(mdb);
        marketDayBookRepository.save(mdb);
        propagateMarketClosingBalanceForward(mdb);

        // Sync all employee daybooks in this market
        List<EmployeeMarketAssignment> assigns = getAssignmentsForMarket(marketId);
        for (EmployeeMarketAssignment asg : assigns) {
            if (asg.getEmployee() != null) {
                dayBookRepository.findByEmployeeIdAndDate(asg.getEmployee().getId(), date).ifPresent(edb -> {
                    edb.setOpeningBalance(mdb.getTotalOpeningBalance());
                    edb.setCollections(mdb.getTotalCollections());
                    edb.setIncomingTransfers(mdb.getTotalIncomingTransfers());
                    edb.setCashIncomingTransfers(mdb.getTotalCashIncomingTransfers());
                    edb.setSpends(mdb.getTotalSpends());
                    edb.setLoansDisbursed(mdb.getTotalLoansDisbursed());
                    edb.setOutgoingTransfers(mdb.getTotalOutgoingTransfers());
                    edb.setCashOutgoingTransfers(mdb.getTotalCashOutgoingTransfers());
                    edb.setOfficeRemittance(mdb.getTotalOfficeRemittance());
                    edb.setTransferToOthers(mdb.getTotalTransferToOthers());
                    edb.setClosingBalance(mdb.getTotalClosingBalance());
                    dayBookRepository.save(edb);
                    propagateClosingBalanceForward(edb);
                });
            }
        }

        return getOrCreateMarketDayBook(marketId, date);
    }

    @Override
    @Transactional
    public List<DayBookTransaction> getMarketTransactions(UUID marketId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<DayBookTransaction> list = new ArrayList<>();

        // 1. Transactions directly tagged with marketId
        List<DayBookTransaction> byMarket = dayBookTransactionRepository.findByMarketIdAndCreatedAtBetween(marketId, start, end);
        if (byMarket != null) {
            list.addAll(byMarket);
        }

        // 2. Transactions tagged by employees of this market
        List<EmployeeMarketAssignment> assignments = getAssignmentsForMarket(marketId);
        for (EmployeeMarketAssignment asg : assignments) {
            if (asg.getEmployee() == null) continue;
            UUID empId = asg.getEmployee().getId();
            List<DayBookTransaction> byEmp = dayBookTransactionRepository.findByEmployeeIdAndCreatedAtBetween(empId, start, end);
            if (byEmp != null) {
                for (DayBookTransaction tx : byEmp) {
                    if (!list.contains(tx)) {
                        list.add(tx);
                    }
                }
            }
        }

        // 3. Ensure COLLECTIONS transactions are represented
        boolean hasCollections = list.stream().anyMatch(t -> "COLLECTIONS".equalsIgnoreCase(t.getType()));
        if (!hasCollections) {
            List<LoanCollection> cols = loanCollectionRepository.findCollectionsForEmployeeOrMarketsBetween(
                    UUID.randomUUID(), Collections.singletonList(marketId), start, end
            );
            if (cols != null && !cols.isEmpty()) {
                for (LoanCollection c : cols) {
                    if (c.getCollectedAmount() == null || c.getCollectedAmount().compareTo(BigDecimal.ZERO) <= 0) continue;
                    DayBookTransaction tx = new DayBookTransaction();
                    tx.setMarketId(marketId);
                    tx.setEmployeeId(c.getCollectedBy() != null ? c.getCollectedBy().getId() : null);
                    tx.setType("COLLECTIONS");
                    tx.setAmount(c.getCollectedAmount());
                    String custName = (c.getLoan() != null && c.getLoan().getCustomer() != null)
                            ? (c.getLoan().getCustomer().getFirstName() + " " + (c.getLoan().getCustomer().getLastName() != null ? c.getLoan().getCustomer().getLastName() : ""))
                            : "";
                    String loanCode = c.getLoan() != null ? c.getLoan().getLoanCode() : "";
                    String rem = c.getRemarks();
                    if (rem == null || rem.trim().isEmpty()) {
                        rem = "Collection: " + custName + " (" + loanCode + ")";
                    }
                    tx.setRemarks(rem);
                    tx.setCreatedAt(c.getCollectionDate() != null ? c.getCollectionDate() : date.atTime(17, 0));
                    list.add(tx);
                }
            }
        }

        // 4. Ensure LOANS_DISBURSED transactions are represented
        boolean hasLoansDisbursed = list.stream().anyMatch(t -> "LOANS_DISBURSED".equalsIgnoreCase(t.getType()));
        if (!hasLoansDisbursed) {
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
            if (disbursedLoans != null && !disbursedLoans.isEmpty()) {
                for (Loan l : disbursedLoans) {
                    BigDecimal disAmount = l.getDisbursedAmount() != null ? l.getDisbursedAmount() : (l.getApprovedAmount() != null ? l.getApprovedAmount() : BigDecimal.ZERO);
                    if (disAmount.compareTo(BigDecimal.ZERO) <= 0) continue;

                    DayBookTransaction tx = new DayBookTransaction();
                    tx.setMarketId(marketId);
                    tx.setEmployeeId(l.getCreatedBy() != null ? l.getCreatedBy().getId() : null);
                    tx.setType("LOANS_DISBURSED");
                    tx.setAmount(disAmount);
                    String custName = l.getCustomer() != null
                            ? (l.getCustomer().getFirstName() + " " + (l.getCustomer().getLastName() != null ? l.getCustomer().getLastName() : ""))
                            : "";
                    String loanCode = l.getLoanCode() != null ? l.getLoanCode() : "";
                    tx.setRemarks("New Loan: " + custName + " (" + loanCode + ")");
                    tx.setCreatedAt(l.getDisbursementDate() != null ? l.getDisbursementDate() : date.atTime(10, 0));
                    list.add(tx);
                }
            }
        }

        // Populate employee names on all transactions
        for (DayBookTransaction tx : list) {
            if (tx.getEmployeeId() != null) {
                userRepository.findById(tx.getEmployeeId()).ifPresent(u -> tx.setEmployeeName(u.getFullName()));
            }
        }

        list.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
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
        if (dayBook.getTransferToOthers() == null) dayBook.setTransferToOthers(BigDecimal.ZERO);

        return dayBook.getOpeningBalance()
                .add(dayBook.getCollections())
                .add(dayBook.getIncomingTransfers())
                .add(dayBook.getCashIncomingTransfers())
                .subtract(dayBook.getSpends())
                .subtract(dayBook.getLoansDisbursed())
                .subtract(dayBook.getOutgoingTransfers())
                .subtract(dayBook.getCashOutgoingTransfers())
                .subtract(dayBook.getOfficeRemittance())
                .subtract(dayBook.getTransferToOthers());
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
        response.setTransferToOthers(dayBook.getTransferToOthers() != null ? dayBook.getTransferToOthers() : BigDecimal.ZERO);
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

        Set<UUID> affectedMarketIds = new HashSet<>();
        for (DayBookTransaction tx : matchedTxs) {
            if (tx.getMarketId() != null) {
                affectedMarketIds.add(tx.getMarketId());
            }
            dayBookTransactionRepository.delete(tx);
        }

        for (UUID mId : affectedMarketIds) {
            syncMarketDayBook(mId, LocalDate.now());
        }
    }

    @Scheduled(cron = "0 0 0 * * ?") // Midnight
    @Transactional
    public void forceClosePendingDayBooks() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<MarketDayBook> pendingMarkets = marketDayBookRepository.findByDateAndStatusNot(yesterday, DayBookStatus.CLOSED);
        for (MarketDayBook mdb : pendingMarkets) {
            approveMarketClosure(mdb.getMarketId(), yesterday);
        }
    }
}
