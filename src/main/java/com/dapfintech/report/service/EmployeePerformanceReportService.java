package com.dapfintech.report.service;

import java.util.List;

import com.dapfintech.report.dto.response.EmployeePerformanceResponse;

public interface EmployeePerformanceReportService {

    List<EmployeePerformanceResponse>
    getEmployeePerformanceReport();
}