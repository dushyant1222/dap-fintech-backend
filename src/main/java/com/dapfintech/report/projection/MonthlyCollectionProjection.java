package com.dapfintech.report.projection;

import java.math.BigDecimal;

public interface MonthlyCollectionProjection {

    Integer getMonthNumber();

    String getMonthName();

    BigDecimal getAmount();

}