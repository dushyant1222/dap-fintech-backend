package com.dapfintech.report.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.dapfintech.collection.repository.CustomerVisitRepository;
import com.dapfintech.report.dto.response.EmployeePerformanceResponse;
import com.dapfintech.report.projection.EmployeePerformanceProjection;
import com.dapfintech.report.service.EmployeePerformanceReportService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeePerformanceReportServiceImpl
        implements EmployeePerformanceReportService {

    private final CustomerVisitRepository
            visitRepository;

    @Override
    public List<EmployeePerformanceResponse>
    getEmployeePerformanceReport() {

        return visitRepository
                .getEmployeePerformanceReport()
                .stream()
                .map(
                        this::mapResponse
                )
                .toList();
    }

    private EmployeePerformanceResponse
    mapResponse(
            EmployeePerformanceProjection projection
    ) {

        return EmployeePerformanceResponse
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
                .totalPromiseToPay(
                        projection.getTotalPromiseToPay()
                )
                .build();
    }
}