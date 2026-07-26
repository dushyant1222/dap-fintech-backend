package com.dapfintech.collection.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.dapfintech.collection.enums.VisitStatus;

import lombok.Data;

@Data
public class CreateVisitRequest {

    private UUID customerId;

    private VisitStatus visitStatus;

    private String remarks;

    private BigDecimal promiseAmount;

    private LocalDate promiseDate;

    private BigDecimal latitude;

    private BigDecimal longitude;
}