package com.dapfintech.loan.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import com.dapfintech.loan.enums.CollectionMode;

import lombok.Data;

@Data
public class CreateCollectionRequest {

    private UUID loanId;

    private UUID scheduleId;

    private BigDecimal collectedAmount;

    private CollectionMode collectionMode;

    private String remarks;
    
    private BigDecimal latitude;

    private BigDecimal longitude;

    private java.time.LocalDateTime collectionDate;
}