package com.dapfintech.employee.service;

import com.dapfintech.employee.dto.DayBookResponse;
import com.dapfintech.employee.dto.DayBookTransactionRequest;
import com.dapfintech.employee.dto.UpdateDayBookRequest;

import java.util.UUID;
import java.util.List;

public interface DayBookService {
    DayBookResponse getOrCreateTodayDayBook(UUID employeeId);
    DayBookResponse addTransaction(UUID employeeId, DayBookTransactionRequest request);
    DayBookResponse requestClosure(UUID employeeId);
    DayBookResponse cancelClosure(UUID employeeId);
    DayBookResponse approveClosure(UUID dayBookId);
    DayBookResponse rejectClosure(UUID dayBookId);
    DayBookResponse updateDayBook(UUID dayBookId, UpdateDayBookRequest request);
    DayBookResponse getDayBookByDate(UUID employeeId, java.time.LocalDate date);
    List<DayBookResponse> getEmployeeDayBooks(UUID employeeId);
    java.util.List<com.dapfintech.employee.entity.DayBookTransaction> getTransactions(UUID employeeId, java.time.LocalDate date);
}
