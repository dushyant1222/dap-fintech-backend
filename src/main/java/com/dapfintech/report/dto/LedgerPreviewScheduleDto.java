package com.dapfintech.report.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LedgerPreviewScheduleDto {
    private Integer installmentNo;
    private LocalDate dueDate;
    private BigDecimal emiAmount;
    private String status;
    private LocalDateTime paidDate;
    private BigDecimal delayPenalty;
    private BigDecimal amountPaid;
}
