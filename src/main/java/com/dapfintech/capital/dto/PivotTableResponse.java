package com.dapfintech.capital.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PivotTableResponse {
    private CapitalSummaryResponse summary;
    private List<PivotRowResponse> rows;
}
