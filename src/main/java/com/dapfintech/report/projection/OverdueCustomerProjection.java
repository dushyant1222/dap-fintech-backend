package com.dapfintech.report.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface OverdueCustomerProjection {

    UUID getCustomerId();

    String getCustomerName();

    String getMobileNumber();

    String getMarketName();

    UUID getLoanId();

    BigDecimal getOverdueAmount();

    Integer getOverdueDays();
}