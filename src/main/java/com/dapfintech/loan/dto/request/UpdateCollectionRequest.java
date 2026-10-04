package com.dapfintech.loan.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.dapfintech.loan.enums.CollectionMode;

import lombok.Data;

@Data
public class UpdateCollectionRequest {

    private BigDecimal collectedAmount;

    private CollectionMode collectionMode;

    private LocalDateTime collectionDate;

    private String remarks;
}
