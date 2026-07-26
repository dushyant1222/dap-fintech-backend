package com.dapfintech.report.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BucketWiseOverdueResponse {

    private String bucket;

    private BigDecimal overdueAmount;
}