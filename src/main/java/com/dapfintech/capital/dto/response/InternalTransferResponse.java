package com.dapfintech.capital.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.dapfintech.capital.enums.TransferStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InternalTransferResponse {
    private UUID id;
    private UUID senderId;
    private String senderName;
    private UUID receiverId;
    private String receiverName;
    private BigDecimal amount;
    private TransferStatus status;
    private LocalDateTime transferDate;
    private String category;
    private String remarks;
}
