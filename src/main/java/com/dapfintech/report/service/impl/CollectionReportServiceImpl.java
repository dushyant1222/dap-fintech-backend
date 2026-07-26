package com.dapfintech.report.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.dapfintech.loan.repository.LoanCollectionRepository;
import com.dapfintech.report.dto.response.CollectionReportResponse;
import com.dapfintech.report.service.CollectionReportService;

import lombok.RequiredArgsConstructor;
import java.util.List;
import com.dapfintech.report.dto.response.EmployeeCollectionReportResponse;
import com.dapfintech.report.dto.response.MarketCollectionReportResponse;
import com.dapfintech.report.projection.EmployeeCollectionProjection;
import com.dapfintech.report.projection.MarketCollectionProjection;

@Service
@RequiredArgsConstructor
public class CollectionReportServiceImpl
        implements CollectionReportService {

    private final LoanCollectionRepository
            collectionRepository;
    
    @Override
    public CollectionReportResponse
    getDateRangeCollectionReport(
            LocalDate fromDate,
            LocalDate toDate
    ) {

        BigDecimal totalCollection =
                collectionRepository
                        .getCollectionBetweenDates(
                                fromDate,
                                toDate
                        );

        Long totalTransactions =
                collectionRepository
                        .getCollectionCountBetweenDates(
                                fromDate,
                                toDate
                        );

        BigDecimal averageCollection =
                totalTransactions == 0
                        ? BigDecimal.ZERO
                        : totalCollection.divide(
                                BigDecimal.valueOf(
                                        totalTransactions
                                ),
                                2,
                                java.math.RoundingMode.HALF_UP
                        );

        return CollectionReportResponse
                .builder()
                .totalCollection(
                        totalCollection
                )
                .totalTransactions(
                        totalTransactions
                )
                .averageCollection(
                        averageCollection
                )
                .build();
    }
    
    @Override
    public List<MarketCollectionReportResponse>
    getMarketCollectionReport() {

        return collectionRepository
                .getMarketCollectionReport()
                .stream()
                .map(
                        this::mapMarketReport
                )
                .toList();
    }
    
    @Override
    public List<EmployeeCollectionReportResponse>
    getEmployeeCollectionReport() {

        return collectionRepository
                .getEmployeeCollectionReport()
                .stream()
                .map(
                        this::mapEmployeeReport
                )
                .toList();
    }
    

    @Override
    public CollectionReportResponse
    getTodayCollectionReport() {

        BigDecimal totalCollection =
                collectionRepository
                        .getTodayCollection();

        Long totalTransactions =
                collectionRepository
                        .getTodayCollectionCount();

        BigDecimal averageCollection =
                totalTransactions == 0
                        ? BigDecimal.ZERO
                        : totalCollection.divide(
                                BigDecimal.valueOf(
                                        totalTransactions
                                ),
                                2,
                                java.math.RoundingMode.HALF_UP
                        );

        return CollectionReportResponse
                .builder()
                .totalCollection(
                        totalCollection
                )
                .totalTransactions(
                        totalTransactions
                )
                .averageCollection(
                        averageCollection
                )
                .build();
    }

    @Override
    public CollectionReportResponse
    getMonthCollectionReport() {

        BigDecimal totalCollection =
                collectionRepository
                        .getMonthCollection();

        Long totalTransactions =
                collectionRepository
                        .getMonthCollectionCount();

        BigDecimal averageCollection =
                totalTransactions == 0
                        ? BigDecimal.ZERO
                        : totalCollection.divide(
                                BigDecimal.valueOf(
                                        totalTransactions
                                ),
                                2,
                                java.math.RoundingMode.HALF_UP
                        );

        return CollectionReportResponse
                .builder()
                .totalCollection(
                        totalCollection
                )
                .totalTransactions(
                        totalTransactions
                )
                .averageCollection(
                        averageCollection
                )
                .build();
    }
    private EmployeeCollectionReportResponse
    mapEmployeeReport(
            EmployeeCollectionProjection projection
    ) {

        return EmployeeCollectionReportResponse
                .builder()
                .employeeId(
                        projection.getEmployeeId()
                )
                .employeeName(
                        projection.getEmployeeName()
                )
                .totalCollection(
                        projection.getTotalCollection()
                )
                .totalTransactions(
                        projection.getTotalTransactions()
                )
                .build();
    }
    
    private MarketCollectionReportResponse
    mapMarketReport(
            MarketCollectionProjection projection
    ) {

        return MarketCollectionReportResponse
                .builder()
                .marketId(
                        projection.getMarketId()
                )
                .marketName(
                        projection.getMarketName()
                )
                .totalCollection(
                        projection.getTotalCollection()
                )
                .totalTransactions(
                        projection.getTotalTransactions()
                )
                .build();
    }
}