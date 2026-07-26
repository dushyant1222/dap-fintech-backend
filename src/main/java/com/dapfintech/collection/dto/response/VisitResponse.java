package com.dapfintech.collection.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.dapfintech.collection.enums.VisitStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VisitResponse {

    private UUID id;

    private UUID customerId;

    private String customerName;

    private UUID employeeId;

    private String employeeName;

    private LocalDateTime visitDate;

    private VisitStatus visitStatus;

    private String remarks;

    private BigDecimal promiseAmount;

    private LocalDate promiseDate;
}