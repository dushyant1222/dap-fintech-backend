package com.dapfintech.loan.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.dapfintech.loan.enums.CollectionMode;
import com.dapfintech.loan.enums.CollectionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollectionResponse {

    private UUID id;

    private UUID loanId;

    private String receiptNumber;

    private BigDecimal collectedAmount;

    private LocalDateTime collectionDate;

    private CollectionMode collectionMode;

    private CollectionStatus collectionStatus;

    private String remarks;
}