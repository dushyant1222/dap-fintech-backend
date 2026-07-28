package com.dapfintech.employee.service;

import com.dapfintech.employee.dto.DayBookResponse;
import com.dapfintech.employee.entity.DayBook;
import com.dapfintech.employee.enums.DayBookStatus;
import com.dapfintech.employee.repository.DayBookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DayBookServiceImpl implements DayBookService {

    @Autowired
    private DayBookRepository dayBookRepository;

    @Override
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
            
            // Set opening balance to most recent past DayBook closing balance (if any)
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
