package com.dapfintech.wallet.dto;

import com.dapfintech.wallet.enums.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PersonalLedgerRequest {
    private UUID adminId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String category;
    private String remarks;
    private LocalDateTime transactionDate;
}
