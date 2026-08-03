package com.dapfintech.loan.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollectionHistoryResponse {

    private UUID collectionId;

    private UUID loanId;

    private String loanCode;

    private String receiptNumber;

    private String customerName;

    private String mobileNumber;

    private BigDecimal collectedAmount;

    private String collectionMode;

    private String collectionStatus;

    private LocalDateTime collectionDate;

    private String collectedBy;

}