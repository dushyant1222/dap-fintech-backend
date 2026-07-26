package com.dapfintech.report.service;

import java.util.List;

import com.dapfintech.report.dto.response.BucketWiseOverdueResponse;
import com.dapfintech.report.dto.response.OverdueCustomerResponse;
import com.dapfintech.report.dto.response.OverdueSummaryResponse;

public interface OverdueReportService {

    List<OverdueCustomerResponse>
    getOverdueCustomers();
    
    OverdueSummaryResponse
    getOverdueSummary();
    
    List<BucketWiseOverdueResponse>
    getBucketWiseOverdueReport();
}