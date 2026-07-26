package com.dapfintech.report.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.dapfintech.loan.repository.LoanCollectionRepository;
import com.dapfintech.report.dto.response.MarketPerformanceResponse;
import com.dapfintech.report.projection.MarketPerformanceProjection;
import com.dapfintech.report.service.MarketPerformanceReportService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarketPerformanceReportServiceImpl
        implements MarketPerformanceReportService {

    private final LoanCollectionRepository
            collectionRepository;

    @Override
    public List<MarketPerformanceResponse>
    getMarketPerformanceReport() {

        return collectionRepository
                .getMarketPerformanceReport()
                .stream()
                .map(this::mapResponse)
                .toList();
    }

    private MarketPerformanceResponse
    mapResponse(
            MarketPerformanceProjection projection
    ) {

        return MarketPerformanceResponse
                .builder()
                .marketId(
                        projection.getMarketId()
                )
                .marketName(
                        projection.getMarketName()
                )
                .totalCustomers(
                        projection.getTotalCustomers()
                )
                .totalLoans(
                        projection.getTotalLoans()
                )
                .totalCollection(
                        projection.getTotalCollection()
                )
                .build();
    }
}