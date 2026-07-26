package com.dapfintech.loan.mapper;

import org.springframework.stereotype.Component;

import com.dapfintech.loan.dto.response.LoanApprovalResponse;
import com.dapfintech.loan.entity.LoanApproval;

@Component
public class LoanApprovalMapper {

    public LoanApprovalResponse toResponse(
            LoanApproval approval
    ) {

        return LoanApprovalResponse.builder()
                .id(approval.getId())
                .decision(approval.getDecision())
                .remarks(approval.getRemarks())
                .approvalDate(approval.getApprovalDate())
                .build();
    }
}