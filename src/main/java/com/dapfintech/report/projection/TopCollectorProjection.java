package com.dapfintech.report.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface TopCollectorProjection {

    UUID getEmployeeId();

    String getEmployeeName();

    BigDecimal getTotalCollection();
}