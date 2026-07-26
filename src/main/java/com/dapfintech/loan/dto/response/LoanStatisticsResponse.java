package com.dapfintech.loan.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoanStatisticsResponse {

    private Long totalLoans;

    private Long draftLoans;

    private Long approvedLoans;

    private Long activeLoans;

    private Long rejectedLoans;

    private Long closedLoans;

}