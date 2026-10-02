package com.dapfintech.employee.service;

import com.dapfintech.employee.dto.DayBookResponse;
import com.dapfintech.employee.dto.DayBookTransactionRequest;
import com.dapfintech.employee.dto.MarketDayBookResponse;
import com.dapfintech.employee.dto.UpdateDayBookRequest;

import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

public interface DayBookService {
    DayBookResponse getOrCreateTodayDayBook(UUID employeeId);
    DayBookResponse getOrCreateDayBook(UUID employeeId, LocalDate date);
    DayBookResponse addTransaction(UUID employeeId, DayBookTransactionRequest request);
    DayBookResponse addTransactionForDate(UUID employeeId, LocalDate date, DayBookTransactionRequest request);
    DayBookResponse requestClosure(UUID employeeId);
    DayBookResponse requestClosureForDate(UUID employeeId, LocalDate date);
    DayBookResponse cancelClosure(UUID employeeId);
    DayBookResponse cancelClosureForDate(UUID employeeId, LocalDate date);
    DayBookResponse approveClosure(UUID dayBookId);
    DayBookResponse rejectClosure(UUID dayBookId);
    DayBookResponse reopenDayBook(UUID dayBookId);
    DayBookResponse updateDayBook(UUID dayBookId, UpdateDayBookRequest request);
    DayBookResponse getDayBookByDate(UUID employeeId, LocalDate date);
    List<DayBookResponse> getEmployeeDayBooks(UUID employeeId);
    List<com.dapfintech.employee.entity.DayBookTransaction> getTransactions(UUID employeeId, LocalDate date);

    // Sequential Date Resolution
    LocalDate getActiveDayBookDate(UUID employeeId);
    LocalDate getActiveMarketDayBookDate(UUID marketId);

    // Market DayBook Operations
    MarketDayBookResponse getOrCreateMarketDayBook(UUID marketId, LocalDate date);
    MarketDayBookResponse getTodayMarketDayBook(UUID marketId);
    MarketDayBookResponse approveMarketClosure(UUID marketId, LocalDate date);
    MarketDayBookResponse rejectMarketClosure(UUID marketId, LocalDate date);
    MarketDayBookResponse reopenMarketDayBook(UUID marketId, LocalDate date);
    List<com.dapfintech.employee.entity.DayBookTransaction> getMarketTransactions(UUID marketId, LocalDate date);

    // Loan cleanup & sync
    void cleanDayBookForDeletedLoan(String loanCode, UUID loanId);
    void syncMarketDayBook(UUID marketId, LocalDate date);
}
