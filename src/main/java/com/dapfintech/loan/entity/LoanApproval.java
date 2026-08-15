package com.dapfintech.loan.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.dapfintech.auth.entity.User;
import com.dapfintech.common.base.BaseEntity;
import com.dapfintech.loan.enums.ApprovalDecision;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loan_approvals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApproval extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision")
    private ApprovalDecision decision;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;
}
