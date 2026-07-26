package com.dapfintech.report.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopCollectorResponse {

    private UUID employeeId;

    private String employeeName;

    private BigDecimal totalCollection;
}