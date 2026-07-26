package com.dapfintech.loan.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoanClosureResponse {

    private UUID id;

    private UUID loanId;

    private LocalDateTime closureDate;

    private String remarks;
}