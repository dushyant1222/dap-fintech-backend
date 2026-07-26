package com.dapfintech.capital.dto;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class PivotFilterRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private UUID employeeId;
    private UUID marketId;
    private String groupBy; // "DAY", "MONTH", "EMPLOYEE"
}
