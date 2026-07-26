package com.dapfintech.dashboard.service;

import java.util.List;

import com.dapfintech.dashboard.dto.response.AdminDashboardResponse;
import com.dapfintech.dashboard.dto.response.BusinessGrowthResponse;
import com.dapfintech.dashboard.dto.response.MonthlyCollectionResponse;

public interface AdminDashboardService {

    AdminDashboardResponse
    getDashboard();
    List<MonthlyCollectionResponse>
    getMonthlyCollection();
    BusinessGrowthResponse
    getBusinessGrowthAnalytics(String timeframe);
}