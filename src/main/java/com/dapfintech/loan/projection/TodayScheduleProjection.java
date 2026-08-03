package com.dapfintech.loan.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface TodayScheduleProjection {

    UUID getLoanId();
    
    String getLoanCode();

    UUID getScheduleId();

    UUID getCustomerId();

    String getCustomerName();

    String getMobileNumber();

    Integer getInstallmentNumber();

    LocalDate getDueDate();

    BigDecimal getInstallmentAmount();

    BigDecimal getOutstandingAmount();

}