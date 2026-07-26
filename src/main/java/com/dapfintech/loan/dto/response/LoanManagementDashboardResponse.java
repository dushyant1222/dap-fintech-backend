package com.dapfintech.loan.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoanManagementDashboardResponse {

    private Long totalLoans;

    private Long pendingApprovalLoans;

    private Long approvedLoans;

    private Long rejectedLoans;

    private Long activeLoans;

    private Long closedLoans;

}