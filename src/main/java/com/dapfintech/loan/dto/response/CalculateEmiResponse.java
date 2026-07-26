package com.dapfintech.loan.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CalculateEmiResponse {

    private BigDecimal emiAmount;

    private BigDecimal totalInterest;

    private BigDecimal totalPayable;
}