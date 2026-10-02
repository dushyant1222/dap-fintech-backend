package com.dapfintech.loan.dto.request;

import java.math.BigDecimal;
import com.dapfintech.loan.enums.CollectionMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddClosedLoanPenaltyRequest {
    private BigDecimal amount;
    private CollectionMode collectionMode;
    private String remarks;
    private Boolean createCollectionRecord;
}
