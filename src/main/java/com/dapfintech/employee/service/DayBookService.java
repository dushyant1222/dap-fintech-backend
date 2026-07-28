package com.dapfintech.employee.service;

import com.dapfintech.employee.dto.DayBookResponse;

import java.util.UUID;

public interface DayBookService {
    DayBookResponse getOrCreateTodayDayBook(UUID employeeId);
}
