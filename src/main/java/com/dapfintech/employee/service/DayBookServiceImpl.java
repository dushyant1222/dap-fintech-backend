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
                User admin = userRepository.findByRole_RoleName("ADMIN").stream().findFirst().orElse(null);
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
        return mapToResponse(dayBook);
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
    
    private BigDecimal calculateClosingBalance(DayBook dayBook) {
        if (dayBook.getOpeningBalance() == null) dayBook.setOpeningBalance(BigDecimal.ZERO);
        if (dayBook.getCollections() == null) dayBook.setCollections(BigDecimal.ZERO);
        if (dayBook.getIncomingTransfers() == null) dayBook.setIncomingTransfers(BigDecimal.ZERO);
        if (dayBook.getSpends() == null) dayBook.setSpends(BigDecimal.ZERO);
        if (dayBook.getLoansDisbursed() == null) dayBook.setLoansDisbursed(BigDecimal.ZERO);
        if (dayBook.getOutgoingTransfers() == null) dayBook.setOutgoingTransfers(BigDecimal.ZERO);
        if (dayBook.getOfficeRemittance() == null) dayBook.setOfficeRemittance(BigDecimal.ZERO);
        
        return dayBook.getOpeningBalance()
                .add(dayBook.getCollections())
                .add(dayBook.getIncomingTransfers())
                .subtract(dayBook.getSpends())
                .subtract(dayBook.getLoansDisbursed())
                .subtract(dayBook.getOutgoingTransfers())
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
        response.setSpends(dayBook.getSpends());
        response.setLoansDisbursed(dayBook.getLoansDisbursed());
        response.setOutgoingTransfers(dayBook.getOutgoingTransfers());
        response.setOfficeRemittance(dayBook.getOfficeRemittance());
        response.setClosingBalance(dayBook.getClosingBalance());
        response.setStatus(dayBook.getStatus());
        return response;
    }
}
