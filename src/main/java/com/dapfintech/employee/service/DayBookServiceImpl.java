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
    
    @Autowired
    private com.dapfintech.employee.repository.MarketDayBookRepository marketDayBookRepository;
    
    @Autowired
    private com.dapfintech.market.repository.EmployeeMarketAssignmentRepository assignmentRepository;
    
    @Autowired
    private com.dapfintech.notification.service.NotificationService notificationService;

    @Override
    @Transactional
    public DayBookResponse getOrCreateTodayDayBook(UUID employeeId) {
        LocalDate today = LocalDate.now();
        Optional<DayBook> todayDayBook = dayBookRepository.findByEmployeeIdAndDate(employeeId, today);
        
        DayBook dayBook;
        if (todayDayBook.isPresent()) {
            dayBook = todayDayBook.get();
        } else {
            dayBook = new DayBook();
            dayBook.setEmployeeId(employeeId);
            dayBook.setDate(today);
            dayBook.setStatus(DayBookStatus.OPEN);
            
            List<DayBook> pastBooks = dayBookRepository.findByEmployeeIdOrderByDateDesc(employeeId);
            if (!pastBooks.isEmpty()) {
                dayBook.setOpeningBalance(pastBooks.get(0).getClosingBalance());
            } else {
                dayBook.setOpeningBalance(BigDecimal.ZERO);
            }
            
            dayBook.setClosingBalance(calculateClosingBalance(dayBook));
            dayBook = dayBookRepository.save(dayBook);
        }
        
        return mapToResponse(dayBook);
    }
    
    @Override
    @Transactional
    public DayBookResponse addTransaction(UUID employeeId, DayBookTransactionRequest request) {
        LocalDate today = LocalDate.now();
        DayBook dayBook = dayBookRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElseGet(() -> {
                    getOrCreateTodayDayBook(employeeId);
                    return dayBookRepository.findByEmployeeIdAndDate(employeeId, today).get();
                });
                
        if (dayBook.getStatus() != DayBookStatus.OPEN) {
            throw new RuntimeException("Cannot add transaction to a closed or pending daybook.");
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
                
                // Trigger internal transfer to admin
                User employee = userRepository.findById(employeeId).orElse(null);
                User admin = userRepository.findByRoleRoleName("ADMIN").stream().findFirst().orElse(null);
                if (employee != null && admin != null) {
                    InternalTransfer transfer = InternalTransfer.builder()
                            .sender(employee)
                            .receiver(admin)
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
            case "OUTGOING_TRANSFER":
                dayBook.setOutgoingTransfers(dayBook.getOutgoingTransfers().add(amount));
                break;
            default:
                throw new RuntimeException("Unknown transaction type: " + request.getType());
        }
        
        dayBook.setClosingBalance(calculateClosingBalance(dayBook));
        dayBook = dayBookRepository.save(dayBook);
        return mapToResponse(dayBook);
    }
    
    @Override
    @Transactional
    public DayBookResponse requestClosure(UUID employeeId) {
        LocalDate today = LocalDate.now();
        DayBook dayBook = dayBookRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElseThrow(() -> new RuntimeException("Today's DayBook not found for employee"));
        
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
        LocalDate today = LocalDate.now();
        DayBook dayBook = dayBookRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElseThrow(() -> new RuntimeException("Today's DayBook not found for employee"));
        
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
    public DayBookResponse updateDayBook(UUID dayBookId, UpdateDayBookRequest request) {
        DayBook dayBook = dayBookRepository.findById(dayBookId)
                .orElseThrow(() -> new RuntimeException("DayBook not found"));
                
        if (request.getCollections() != null) dayBook.setCollections(request.getCollections());
        if (request.getSpends() != null) dayBook.setSpends(request.getSpends());
        if (request.getLoansDisbursed() != null) dayBook.setLoansDisbursed(request.getLoansDisbursed());
        if (request.getOfficeRemittance() != null) dayBook.setOfficeRemittance(request.getOfficeRemittance());
        if (request.getIncomingTransfers() != null) dayBook.setIncomingTransfers(request.getIncomingTransfers());
        if (request.getOutgoingTransfers() != null) dayBook.setOutgoingTransfers(request.getOutgoingTransfers());
        
        dayBook.setClosingBalance(calculateClosingBalance(dayBook));
        dayBook = dayBookRepository.save(dayBook);
        return mapToResponse(dayBook);
    }
    
    @Override
    public List<DayBookResponse> getEmployeeDayBooks(UUID employeeId) {
        return dayBookRepository.findByEmployeeIdOrderByDateDesc(employeeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DayBookResponse getDayBookByDate(UUID employeeId, LocalDate date) {
        DayBook dayBook = dayBookRepository.findByEmployeeIdAndDate(employeeId, date)
                .orElseThrow(() -> new RuntimeException("DayBook not found for the given date"));
        return mapToResponse(dayBook);
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


