package com.dapfintech.dashboard.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MonthlyCollectionResponse {

    private Integer monthNumber;

    private String monthName;

    private BigDecimal amount;

}