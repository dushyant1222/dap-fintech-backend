package com.dapfintech.loan.dto.request;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class UpdatePenaltySettingsRequest {
    private BigDecimal penaltyRate;
    private BigDecimal penaltyWaivedPercent;
    private String remarks;
}
