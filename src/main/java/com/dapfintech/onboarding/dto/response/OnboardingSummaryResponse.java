package com.dapfintech.onboarding.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingSummaryResponse {

    private int totalRows;

    private int successCount;

    private int failureCount;

    private List<String> errors;

    private List<String> createdLoanCodes;
}
