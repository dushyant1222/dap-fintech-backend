package com.dapfintech.report.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface RecoveryEfficiencyProjection {

    UUID getEmployeeId();

    String getEmployeeName();

    BigDecimal getTotalCollection();

    Long getTotalVisits();
}