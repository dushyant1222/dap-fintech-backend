package com.dapfintech.report.service;

import java.time.LocalDate;
import java.util.List;

import com.dapfintech.report.dto.response.CollectionReportResponse;
import com.dapfintech.report.dto.response.EmployeeCollectionReportResponse;
import com.dapfintech.report.dto.response.MarketCollectionReportResponse;

public interface CollectionReportService {

    CollectionReportResponse
    getTodayCollectionReport();

    CollectionReportResponse
    getMonthCollectionReport();
    List<EmployeeCollectionReportResponse>
    getEmployeeCollectionReport();
    
    List<MarketCollectionReportResponse> getMarketCollectionReport();
    
    CollectionReportResponse
    getDateRangeCollectionReport(
            LocalDate fromDate,
            LocalDate toDate
    );
}