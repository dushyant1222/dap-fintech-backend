package com.dapfintech.capital.dto.request;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class InternalTransferRequest {
    
    @NotNull(message = "Receiver ID is required")
    private UUID receiverId;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    private String category;
    private String remarks;
}
