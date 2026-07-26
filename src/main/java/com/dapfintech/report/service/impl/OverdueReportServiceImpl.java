package com.dapfintech.report.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.dapfintech.loan.repository.LoanRepaymentScheduleRepository;
import com.dapfintech.report.dto.response.BucketWiseOverdueResponse;
import com.dapfintech.report.dto.response.OverdueCustomerResponse;
import com.dapfintech.report.dto.response.OverdueSummaryResponse;
import com.dapfintech.report.projection.BucketWiseOverdueProjection;
import com.dapfintech.report.projection.OverdueCustomerProjection;
import com.dapfintech.report.service.OverdueReportService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OverdueReportServiceImpl
        implements OverdueReportService {

    private final LoanRepaymentScheduleRepository
            scheduleRepository;
    
    
    @Override
    public OverdueSummaryResponse
    getOverdueSummary() {

        return OverdueSummaryResponse
                .builder()
                .totalOverdueCustomers(
                        scheduleRepository
                                .getTotalOverdueCustomers()
                )
                .totalOverdueLoans(
                        scheduleRepository
                                .getTotalOverdueLoans()
                )
                .totalOverdueAmount(
                        scheduleRepository
                                .getTotalOverdueAmount()
                )
                .build();
    }
    
    @Override
    public List<BucketWiseOverdueResponse>
    getBucketWiseOverdueReport() {

        return scheduleRepository
                .getBucketWiseOverdueReport()
                .stream()
                .map(
                        p -> BucketWiseOverdueResponse
                                .builder()
                                .bucket(
                                        p.getBucket()
                                )
                                .overdueAmount(
                                        p.getOverdueAmount()
                                )
                                .build()
                )
                .toList();
    }

    @Override
    public List<OverdueCustomerResponse>
    getOverdueCustomers() {

        return scheduleRepository
                .getOverdueCustomers()
                .stream()
                .map(
                        this::mapResponse
                )
                .toList();
    }

    private OverdueCustomerResponse
    mapResponse(
            OverdueCustomerProjection projection
    ) {

        return OverdueCustomerResponse
                .builder()
                .customerId(
                        projection.getCustomerId()
                )
                .customerName(
                        projection.getCustomerName()
                )
                .mobileNumber(
                        projection.getMobileNumber()
                )
                .marketName(
                        projection.getMarketName()
                )
                .loanId(
                        projection.getLoanId()
                )
                .overdueAmount(
                        projection.getOverdueAmount()
                )
                .overdueDays(
                        projection.getOverdueDays()
                )
                .build();
    }
}