package com.dapfintech.report.service.impl;

import org.springframework.stereotype.Service;

import com.dapfintech.loan.repository.LoanCollectionRepository;
import com.dapfintech.report.dto.response.TopCollectorResponse;
import com.dapfintech.report.projection.TopCollectorProjection;
import com.dapfintech.report.service.TopCollectorReportService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TopCollectorReportServiceImpl
        implements TopCollectorReportService {

    private final LoanCollectionRepository
            collectionRepository;

    @Override
    public TopCollectorResponse
    getTopCollector() {

        TopCollectorProjection projection =
                collectionRepository
                        .getTopCollector();

        if(projection == null) {

            return TopCollectorResponse
                    .builder()
                    .build();
        }

        return TopCollectorResponse
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
                .build();
    }
}