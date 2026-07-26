package com.dapfintech.capital.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CreateCapitalInRequest {
    private BigDecimal amount;
    private String source;
    private String remarks;
}
