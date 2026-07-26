package com.dapfintech.report.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmployeePerformanceResponse {

    private UUID employeeId;

    private String employeeName;

    private BigDecimal totalCollection;

    private Long totalVisits;

    private Long totalPromiseToPay;
}