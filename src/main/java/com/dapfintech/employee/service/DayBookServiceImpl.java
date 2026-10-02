package com.dapfintech.employee.service;

import com.dapfintech.employee.dto.DayBookResponse;
import com.dapfintech.employee.dto.DayBookTransactionRequest;
import com.dapfintech.employee.dto.UpdateDayBookRequest;
import com.dapfintech.employee.entity.DayBook;
import com.dapfintech.employee.enums.DayBookStatus;
import com.dapfintech.employee.repository.DayBookRepository;
import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.capital.entity.InternalTransfer;
import com.dapfintech.capital.enums.TransferStatus;
import com.dapfintech.capital.repository.InternalTransferRepository;
import com.dapfintech.loan.entity.Loan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    @org.springframework.beans.factory.annotation.Autowired
    private com.dapfintech.employee.repository.DayBookTransactionRepository dayBookTransactionRepository;
    
    @Autowired
    private com.dapfintech.employee.repository.MarketDayBookRepository marketDayBookRepository;
    
    @Autowired
    private com.dapfintech.market.repository.EmployeeMarketAssignmentRepository assignmentRepository;
    
    @Autowired
    private com.dapfintech.notification.service.NotificationService notificationService;

    @Autowired
    private com.dapfintech.loan.repository.LoanCollectionRepository loanCollectionRepository;

    @Autowired
    private com.dapfintech.loan.repository.LoanRepository loanRepository;

    @Override
    @Transactional
    public DayBookResponse getOrCreateTodayDayBook(UUID employeeId) {
        return getOrCreateDayBook(employeeId, LocalDate.now());
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

        // Auto-sync collections & loans disbursed from DB for employee's assigned markets on this date
        syncDayBookFromDb(dayBook, employeeId, date);

        return mapToResponse(dayBook);
    }

    private void syncDayBookFromDb(DayBook dayBook, UUID employeeId, LocalDate date) {
        if (dayBook == null || dayBook.getStatus() == DayBookStatus.CLOSED) {
            return;
        }
        try {
            List<com.dapfintech.market.entity.EmployeeMarketAssignment> assignments = 
                    assignmentRepository.findByEmployeeIdAndIsActiveTrue(employeeId);
            List<UUID> marketIds = assignments.stream()
                    .filter(a -> a.getMarket() != null)
                    .map(a -> a.getMarket().getId())
                    .collect(Collectors.toList());

            // 1. Sync Collections
            List<com.dapfintech.loan.entity.LoanCollection> cols;
            if (!marketIds.isEmpty()) {
                cols = loanCollectionRepository.findCollectionsForEmployeeOrMarketsBetween(
                        employeeId, marketIds, date.atStartOfDay(), date.plusDays(1).atStartOfDay()
                );
            } else {
                cols = loanCollectionRepository.findCollectionsByEmployeeBetween(
                        employeeId, date.atStartOfDay(), date.plusDays(1).atStartOfDay()
                );
            }

            boolean modified = false;
            if (cols != null && !cols.isEmpty()) {
                BigDecimal actualCollectionSum = cols.stream()
                        .map(c -> c.getCollectedAmount() != null ? c.getCollectedAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (actualCollectionSum.compareTo(BigDecimal.ZERO) > 0) {
                    if (dayBook.getCollections() == null || dayBook.getCollections().compareTo(actualCollectionSum) != 0) {
                        dayBook.setCollections(actualCollectionSum);
                        modified = true;
                    }

                    // Also ensure collectedBy is set on any collection missing it
                    User emp = userRepository.findById(employeeId).orElse(null);
                    if (emp != null) {
                        for (com.dapfintech.loan.entity.LoanCollection c : cols) {
                            if (c.getCollectedBy() == null) {
                                c.setCollectedBy(emp);
                                loanCollectionRepository.save(c);
                            }
                        }
                    }
                }
            }

            // 2. Sync Loans Disbursed (New Loans)
            List<Loan> disbursedLoans;
            if (date.isEqual(LocalDate.of(2026, 9, 29))) {
                java.time.LocalDateTime cutoffEnd = date.atTime(23, 59, 59);
                if (!marketIds.isEmpty()) {
                    disbursedLoans = loanRepository.findDisbursedLoansForEmployeeOrMarketsUpTo(
                            employeeId, marketIds, cutoffEnd
                    );
                } else {
                    disbursedLoans = loanRepository.findDisbursedLoansByEmployeeUpTo(
                            employeeId, cutoffEnd
                    );
                }
            } else {
                if (!marketIds.isEmpty()) {
                    disbursedLoans = loanRepository.findDisbursedLoansForEmployeeOrMarketsBetween(
                            employeeId, marketIds, date.atStartOfDay(), date.plusDays(1).atStartOfDay()
                    );
                } else {
                    disbursedLoans = loanRepository.findDisbursedLoansByEmployeeBetween(
                            employeeId, date.atStartOfDay(), date.plusDays(1).atStartOfDay()
                    );
                }
            }

            if (disbursedLoans != null && !disbursedLoans.isEmpty()) {
                BigDecimal actualDisbursedSum = disbursedLoans.stream()
                        .map(l -> l.getDisbursedAmount() != null ? l.getDisbursedAmount() : (l.getApprovedAmount() != null ? l.getApprovedAmount() : BigDecimal.ZERO))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (actualDisbursedSum.compareTo(BigDecimal.ZERO) > 0) {
                    if (dayBook.getLoansDisbursed() == null || dayBook.getLoansDisbursed().compareTo(actualDisbursedSum) != 0) {
                        dayBook.setLoansDisbursed(actualDisbursedSum);
                        modified = true;
                    }
                }
            }

            // 3. Sync Accepted Transfers for this Employee and Date
            List<InternalTransfer> inTransfers = internalTransferRepository.findByReceiverIdAndStatusAndTransferDateBetween(
                    employeeId, TransferStatus.ACCEPTED, date.atStartOfDay(), date.plusDays(1).atStartOfDay()
            );
            if (inTransfers != null && !inTransfers.isEmpty()) {
                BigDecimal onlineIn = inTransfers.stream()
                        .filter(t -> t.getTransferMode() != com.dapfintech.capital.enums.TransferMode.CASH)
                        .map(InternalTransfer::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal cashIn = inTransfers.stream()
                        .filter(t -> t.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH)
                        .map(InternalTransfer::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (onlineIn.compareTo(BigDecimal.ZERO) > 0 && (dayBook.getIncomingTransfers() == null || dayBook.getIncomingTransfers().compareTo(onlineIn) != 0)) {
                    dayBook.setIncomingTransfers(onlineIn);
                    modified = true;
                }
                if (cashIn.compareTo(BigDecimal.ZERO) > 0 && (dayBook.getCashIncomingTransfers() == null || dayBook.getCashIncomingTransfers().compareTo(cashIn) != 0)) {
                    dayBook.setCashIncomingTransfers(cashIn);
                    modified = true;
                }
            }

            // 4. Sync Outgoing Transfers
            List<InternalTransfer> outTransfers = internalTransferRepository.findBySenderIdAndStatusAndTransferDateBetween(
                    employeeId, TransferStatus.ACCEPTED, date.atStartOfDay(), date.plusDays(1).atStartOfDay()
            );
            if (outTransfers != null && !outTransfers.isEmpty()) {
                BigDecimal onlineOut = outTransfers.stream()
                        .filter(t -> t.getTransferMode() != com.dapfintech.capital.enums.TransferMode.CASH)
                        .map(InternalTransfer::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal cashOut = outTransfers.stream()
                        .filter(t -> t.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH)
                        .map(InternalTransfer::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (onlineOut.compareTo(BigDecimal.ZERO) > 0 && (dayBook.getOutgoingTransfers() == null || dayBook.getOutgoingTransfers().compareTo(onlineOut) != 0)) {
                    dayBook.setOutgoingTransfers(onlineOut);
                    modified = true;
                }
                if (cashOut.compareTo(BigDecimal.ZERO) > 0 && (dayBook.getCashOutgoingTransfers() == null || dayBook.getCashOutgoingTransfers().compareTo(cashOut) != 0)) {
                    dayBook.setCashOutgoingTransfers(cashOut);
                    modified = true;
                }
            }

            if (modified) {
                dayBook.setClosingBalance(calculateClosingBalance(dayBook));
                dayBookRepository.save(dayBook);
                propagateClosingBalanceForward(dayBook);
            }
        } catch (Exception e) {
            // Non-fatal sync
        }
    }
    
    @Override
    @Transactional
    public DayBookResponse addTransaction(UUID employeeId, DayBookTransactionRequest request) {
        return addTransactionForDate(employeeId, LocalDate.now(), request);
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
        
        // Ensure non-null defaults
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
                
                // Trigger internal transfer to master admin
                User employee = userRepository.findById(employeeId).orElse(null);
                User masterAdmin = userRepository.findByRoleRoleName("MASTER_ADMIN").stream().findFirst()
                        .orElseGet(() -> userRepository.findByRoleRoleName("ADMIN").stream().findFirst().orElse(null));
                if (employee != null && masterAdmin != null) {
                    InternalTransfer transfer = InternalTransfer.builder()
                            .sender(employee)
                            .receiver(masterAdmin)
                            .amount(amount)
                            .transferDate(java.time.LocalDateTime.now())
                            .status(TransferStatus.PENDING)
                            .category("OFFICE_REMITTANCE")
                            .remarks("Auto-generated office remittance")
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

        // Save transaction history for dropdown details
        com.dapfintech.employee.entity.DayBookTransaction tx = new com.dapfintech.employee.entity.DayBookTransaction();
        tx.setDayBook(dayBook);
        tx.setEmployeeId(employeeId);
        tx.setType(request.getType().toUpperCase());
        tx.setAmount(amount);
        tx.setRemarks(request.getRemarks());
        tx.setCreatedAt(date.isEqual(LocalDate.now()) ? java.time.LocalDateTime.now() : date.atTime(java.time.LocalTime.now()));
        dayBookTransactionRepository.save(tx);

        return mapToResponse(dayBook);
    }
    
    @Override
    @Transactional
    public DayBookResponse requestClosure(UUID employeeId) {
        return requestClosureForDate(employeeId, LocalDate.now());
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
        return cancelClosureForDate(employeeId, LocalDate.now());
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
        
        // Carry forward closing balance to subsequent daybooks
        propagateClosingBalanceForward(dayBook);

        checkAndCloseMarketDayBook(dayBook);
        
        return mapToResponse(dayBook);
    }
    
    private void checkAndCloseMarketDayBook(DayBook closedDayBook) {
        UUID employeeId = closedDayBook.getEmployeeId();
        LocalDate today = closedDayBook.getDate();
        
        com.dapfintech.market.entity.EmployeeMarketAssignment assignment = 
            assignmentRepository.findByEmployeeIdAndIsActiveTrue(employeeId).stream().findFirst().orElse(null);
            
        if (assignment == null) return;
        
        UUID marketId = assignment.getMarket().getId();
        
        List<com.dapfintech.market.entity.EmployeeMarketAssignment> marketEmployees = 
            assignmentRepository.findByMarketIdAndIsActiveTrue(marketId);
            
        boolean allClosed = true;
        List<DayBook> allDayBooks = new java.util.ArrayList<>();
        
        for (com.dapfintech.market.entity.EmployeeMarketAssignment empAssignment : marketEmployees) {
            UUID empId = empAssignment.getEmployee().getId();
            if (empId.equals(employeeId)) {
                allDayBooks.add(closedDayBook);
                continue;
            }
            
            Optional<DayBook> empDayBookOpt = dayBookRepository.findByEmployeeIdAndDate(empId, today);
            if (empDayBookOpt.isEmpty() || empDayBookOpt.get().getStatus() != DayBookStatus.CLOSED) {
                allClosed = false;
                break;
            }
            allDayBooks.add(empDayBookOpt.get());
        }
        
        if (allClosed) {
            com.dapfintech.employee.entity.MarketDayBook marketDayBook = 
                marketDayBookRepository.findByMarketIdAndDate(marketId, today)
                    .orElse(new com.dapfintech.employee.entity.MarketDayBook());
                    
            marketDayBook.setMarketId(marketId);
            marketDayBook.setDate(today);
            marketDayBook.setStatus(DayBookStatus.CLOSED);
            
            BigDecimal tOpen = BigDecimal.ZERO, tColl = BigDecimal.ZERO, tInc = BigDecimal.ZERO, tSpend = BigDecimal.ZERO;
            BigDecimal tLoan = BigDecimal.ZERO, tOut = BigDecimal.ZERO, tRemit = BigDecimal.ZERO, tClose = BigDecimal.ZERO;
            
            for (DayBook db : allDayBooks) {
                tOpen = tOpen.add(db.getOpeningBalance() != null ? db.getOpeningBalance() : BigDecimal.ZERO);
                tColl = tColl.add(db.getCollections() != null ? db.getCollections() : BigDecimal.ZERO);
                tInc = tInc.add(db.getIncomingTransfers() != null ? db.getIncomingTransfers() : BigDecimal.ZERO);
                tSpend = tSpend.add(db.getSpends() != null ? db.getSpends() : BigDecimal.ZERO);
                tLoan = tLoan.add(db.getLoansDisbursed() != null ? db.getLoansDisbursed() : BigDecimal.ZERO);
                tOut = tOut.add(db.getOutgoingTransfers() != null ? db.getOutgoingTransfers() : BigDecimal.ZERO);
                tRemit = tRemit.add(db.getOfficeRemittance() != null ? db.getOfficeRemittance() : BigDecimal.ZERO);
                tClose = tClose.add(db.getClosingBalance() != null ? db.getClosingBalance() : BigDecimal.ZERO);
            }
            
            marketDayBook.setTotalOpeningBalance(tOpen);
            marketDayBook.setTotalCollections(tColl);
            marketDayBook.setTotalIncomingTransfers(tInc);
            marketDayBook.setTotalSpends(tSpend);
            marketDayBook.setTotalLoansDisbursed(tLoan);
            marketDayBook.setTotalOutgoingTransfers(tOut);
            marketDayBook.setTotalOfficeRemittance(tRemit);
            marketDayBook.setTotalClosingBalance(tClose);
            
            marketDayBookRepository.save(marketDayBook);
            
            notificationService.createNotification(
                "Market Daybook Closed",
                "Market " + assignment.getMarket().getMarketName() + " daybook closed for today. Total Collection: Rs. " + tColl
            );
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
        
        // Also if MarketDayBook exists for that market and date, reopen it
        UUID employeeId = dayBook.getEmployeeId();
        LocalDate date = dayBook.getDate();
        com.dapfintech.market.entity.EmployeeMarketAssignment assignment = 
            assignmentRepository.findByEmployeeIdAndIsActiveTrue(employeeId).stream().findFirst().orElse(null);
        if (assignment != null) {
            marketDayBookRepository.findByMarketIdAndDate(assignment.getMarket().getId(), date).ifPresent(mdb -> {
                mdb.setStatus(DayBookStatus.OPEN);
                marketDayBookRepository.save(mdb);
            });
        }
        
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
        dayBook = dayBookRepository.save(dayBook);
        propagateClosingBalanceForward(dayBook);
        return mapToResponse(dayBook);
    }
    
    @Override
    public List<DayBookResponse> getEmployeeDayBooks(UUID employeeId) {
        return dayBookRepository.findByEmployeeIdOrderByDateDesc(employeeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public java.util.List<com.dapfintech.employee.entity.DayBookTransaction> getTransactions(UUID employeeId, LocalDate date) {
        java.time.LocalDateTime start = date.atStartOfDay();
        java.time.LocalDateTime end = date.plusDays(1).atStartOfDay();
        java.time.LocalDateTime endInclusive = date.atTime(23, 59, 59, 999999);
        List<com.dapfintech.employee.entity.DayBookTransaction> list = 
                new java.util.ArrayList<>(dayBookTransactionRepository.findByEmployeeIdAndCreatedAtBetween(employeeId, start, endInclusive));

        // Check if COLLECTIONS transactions are present
        boolean hasCollections = list.stream().anyMatch(t -> "COLLECTIONS".equalsIgnoreCase(t.getType()));
        if (!hasCollections) {
            List<com.dapfintech.market.entity.EmployeeMarketAssignment> assignments = 
                    assignmentRepository.findByEmployeeIdAndIsActiveTrue(employeeId);
            List<UUID> marketIds = assignments.stream()
                    .filter(a -> a.getMarket() != null)
                    .map(a -> a.getMarket().getId())
                    .collect(Collectors.toList());

            List<com.dapfintech.loan.entity.LoanCollection> cols;
            if (!marketIds.isEmpty()) {
                cols = loanCollectionRepository.findCollectionsForEmployeeOrMarketsBetween(
                        employeeId, marketIds, start, end
                );
            } else {
                cols = loanCollectionRepository.findCollectionsByEmployeeBetween(
                        employeeId, start, end
                );
            }

            if (cols != null && !cols.isEmpty()) {
                DayBook dayBook = dayBookRepository.findByEmployeeIdAndDate(employeeId, date).orElse(null);
                for (com.dapfintech.loan.entity.LoanCollection c : cols) {
                    if (c.getCollectedAmount() == null || c.getCollectedAmount().compareTo(BigDecimal.ZERO) <= 0) continue;
                    com.dapfintech.employee.entity.DayBookTransaction tx = new com.dapfintech.employee.entity.DayBookTransaction();
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
            List<com.dapfintech.market.entity.EmployeeMarketAssignment> assignments = 
                    assignmentRepository.findByEmployeeIdAndIsActiveTrue(employeeId);
            List<UUID> marketIds = assignments.stream()
                    .filter(a -> a.getMarket() != null)
                    .map(a -> a.getMarket().getId())
                    .collect(Collectors.toList());

            List<Loan> disbursedLoans;
            if (date.isEqual(LocalDate.of(2026, 9, 29))) {
                java.time.LocalDateTime cutoffEnd = date.atTime(23, 59, 59);
                if (!marketIds.isEmpty()) {
                    disbursedLoans = loanRepository.findDisbursedLoansForEmployeeOrMarketsUpTo(
                            employeeId, marketIds, cutoffEnd
                    );
                } else {
                    disbursedLoans = loanRepository.findDisbursedLoansByEmployeeUpTo(
                            employeeId, cutoffEnd
                    );
                }
            } else {
                if (!marketIds.isEmpty()) {
                    disbursedLoans = loanRepository.findDisbursedLoansForEmployeeOrMarketsBetween(
                            employeeId, marketIds, start, end
                    );
                } else {
                    disbursedLoans = loanRepository.findDisbursedLoansByEmployeeBetween(
                            employeeId, start, end
                    );
                }
            }

            if (disbursedLoans != null && !disbursedLoans.isEmpty()) {
                DayBook dayBook = dayBookRepository.findByEmployeeIdAndDate(employeeId, date).orElse(null);
                for (Loan l : disbursedLoans) {
                    BigDecimal disAmount = l.getDisbursedAmount() != null ? l.getDisbursedAmount() : (l.getApprovedAmount() != null ? l.getApprovedAmount() : BigDecimal.ZERO);
                    if (disAmount.compareTo(BigDecimal.ZERO) <= 0) continue;

                    com.dapfintech.employee.entity.DayBookTransaction tx = new com.dapfintech.employee.entity.DayBookTransaction();
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
                    com.dapfintech.employee.entity.DayBookTransaction tx = new com.dapfintech.employee.entity.DayBookTransaction();
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
                    com.dapfintech.employee.entity.DayBookTransaction tx = new com.dapfintech.employee.entity.DayBookTransaction();
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

        // Check if any prior daybook is unclosed
        List<DayBook> pastBooks = dayBookRepository.findByEmployeeIdOrderByDateDesc(dayBook.getEmployeeId());
        DayBook unclosedPast = pastBooks.stream()
                .filter(pb -> pb.getDate().isBefore(dayBook.getDate()) && pb.getStatus() != DayBookStatus.CLOSED)
                .findFirst()
                .orElse(null);

        if (unclosedPast != null) {
            response.setPreviousDayClosed(false);
            response.setUnclosedDate(unclosedPast.getDate());
        } else {
            LocalDate yesterday = dayBook.getDate().minusDays(1);
            boolean yesterdayExists = pastBooks.stream().anyMatch(pb -> pb.getDate().equals(yesterday));
            if (!yesterdayExists && dayBook.getDate().equals(LocalDate.now()) && yesterday.isAfter(LocalDate.of(2026, 9, 29))) {
                response.setPreviousDayClosed(false);
                response.setUnclosedDate(yesterday);
            } else {
                response.setPreviousDayClosed(true);
                response.setUnclosedDate(null);
            }
        }

        return response;
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 0 * * ?") // Midnight
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


