package com.dapfintech.report.projection;

import java.math.BigDecimal;

public interface BucketWiseOverdueProjection {

    String getBucket();

    BigDecimal getOverdueAmount();
}