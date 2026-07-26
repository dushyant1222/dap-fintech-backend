package com.dapfintech.capital.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.dapfintech.capital.enums.ExpenseCategory;
import lombok.Data;

@Data
public class CreateExpenseRequest {
    private ExpenseCategory category;
    private BigDecimal amount;
    private String remarks;
    private UUID employeeId;
}
