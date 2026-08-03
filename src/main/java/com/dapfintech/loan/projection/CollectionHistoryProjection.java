package com.dapfintech.loan.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface CollectionHistoryProjection {

    UUID getCollectionId();

    UUID getLoanId();
    
    String getLoanCode();

    String getReceiptNumber();

    String getCustomerName();

    String getMobileNumber();

    BigDecimal getCollectedAmount();

    String getCollectionMode();

    String getCollectionStatus();

    LocalDateTime getCollectionDate();

    String getCollectedBy();

}