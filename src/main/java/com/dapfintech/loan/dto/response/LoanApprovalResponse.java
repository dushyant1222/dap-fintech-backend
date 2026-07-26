package com.dapfintech.loan.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.dapfintech.loan.enums.ApprovalDecision;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoanApprovalResponse {

    private UUID id;

    private ApprovalDecision decision;

    private String remarks;

    private LocalDateTime approvalDate;
}