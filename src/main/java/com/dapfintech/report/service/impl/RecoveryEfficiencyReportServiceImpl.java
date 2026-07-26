package com.dapfintech.report.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import com.dapfintech.collection.repository.CustomerVisitRepository;
import com.dapfintech.report.dto.response.RecoveryEfficiencyResponse;
import com.dapfintech.report.projection.RecoveryEfficiencyProjection;
import com.dapfintech.report.service.RecoveryEfficiencyReportService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecoveryEfficiencyReportServiceImpl
        implements RecoveryEfficiencyReportService {

    private final CustomerVisitRepository
            visitRepository;

    @Override
    public List<RecoveryEfficiencyResponse>
    getRecoveryEfficiencyReport() {

        return visitRepository
                .getRecoveryEfficiencyReport()
                .stream()
                .map(
                        this::mapResponse
                )
                .toList();
    }

    private RecoveryEfficiencyResponse
    mapResponse(
            RecoveryEfficiencyProjection projection
    ) {

        BigDecimal recoveryPerVisit =
                projection.getTotalVisits() == 0
                        ? BigDecimal.ZERO
                        : projection.getTotalCollection()
                                .divide(
                                        BigDecimal.valueOf(
                                                projection.getTotalVisits()
                                        ),
                                        2,
                                        RoundingMode.HALF_UP
                                );

        return RecoveryEfficiencyResponse
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
                .totalVisits(
                        projection.getTotalVisits()
                )
                .recoveryPerVisit(
                        recoveryPerVisit
                )
                .build();
    }
}