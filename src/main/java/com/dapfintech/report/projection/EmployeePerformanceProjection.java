package com.dapfintech.report.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface EmployeePerformanceProjection {

    UUID getEmployeeId();

    String getEmployeeName();

    BigDecimal getTotalCollection();

    Long getTotalVisits();

    Long getTotalPromiseToPay();
    
}