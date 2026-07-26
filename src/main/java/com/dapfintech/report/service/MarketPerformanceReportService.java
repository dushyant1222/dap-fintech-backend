package com.dapfintech.report.service;

import java.util.List;

import com.dapfintech.report.dto.response.MarketPerformanceResponse;

public interface MarketPerformanceReportService {

    List<MarketPerformanceResponse>
    getMarketPerformanceReport();
}