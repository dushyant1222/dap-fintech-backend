package com.dapfintech.loan.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dapfintech.audit.service.AuditLogService;
import com.dapfintech.loan.dto.request.ApproveLoanRequest;
import com.dapfintech.loan.dto.request.RejectLoanRequest;
import com.dapfintech.loan.dto.request.SubmitLoanRequest;
import com.dapfintech.loan.dto.response.LoanApprovalResponse;
import com.dapfintech.loan.entity.Loan;
import com.dapfintech.loan.entity.LoanApproval;
import com.dapfintech.loan.enums.ApprovalDecision;
import com.dapfintech.loan.enums.LoanStatus;
import com.dapfintech.loan.mapper.LoanApprovalMapper;
import com.dapfintech.loan.repository.LoanApprovalRepository;
import com.dapfintech.loan.repository.LoanRepository;
import com.dapfintech.loan.service.LoanApprovalService;
import com.dapfintech.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanApprovalServiceImpl
        implements LoanApprovalService {

    private final LoanRepository loanRepository;
    private final LoanApprovalRepository loanApprovalRepository;
    private final LoanApprovalMapper loanApprovalMapper;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Override
    public void submitLoan(
            UUID loanId,
            SubmitLoanRequest request
    ) {

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Loan not found"
                        )
                );

        /*
         * A normal submission is only allowed
         * from DRAFT status.
         */
        if (loan.getLoanStatus() != LoanStatus.DRAFT) {

            throw new RuntimeException(
                    "Only draft loans can be submitted for approval"
            );
        }

        /*
         * Change:
         *
         * DRAFT -> PENDING_APPROVAL
         */
        loan.setLoanStatus(
                LoanStatus.PENDING_APPROVAL
        );

        loanRepository.save(loan);

        /*
         * Create approval timeline entry.
         */
        LoanApproval approval =
                LoanApproval.builder()
                        .loan(loan)
                        .decision(
                                ApprovalDecision.SUBMITTED
                        )
                        .remarks(
                                request != null
                                        ? request.getRemarks()
                                        : null
                        )
                        .approvalDate(
                                LocalDateTime.now()
                        )
                        .build();

        loanApprovalRepository.save(approval);

        /*
         * Notification for admin.
         */
        notificationService.notifyAllAdmins(
                "Loan Submitted for Approval",
                "Loan " +
                        loan.getId() +
                        " has been submitted for approval."
        );
        if (loan != null && loan.getCreatedBy() != null) {
            notificationService.createNotificationForUser(
                "Loan Submitted",
                "Your loan application has been submitted for approval.",
                loan.getCreatedBy()
            );
        }

        /*
         * Audit trail.
         */
        auditLogService.log(
                "SYSTEM",
                "SUBMIT_LOAN_FOR_APPROVAL",
                "LOAN",
                loan.getId().toString()
        );
    }

    @Override
    public void approveLoan(
            UUID loanId,
            ApproveLoanRequest request
    ) {

        Loan loan =
                loanRepository.findById(loanId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Loan not found"
                                )
                        );

        if(
                request.getApprovedAmount()
                        .compareTo(
                                loan.getLoanAmount()
                        ) > 0
        ) {

            throw new RuntimeException(
                    "Approved amount cannot exceed requested amount"
            );
        }

        loan.setApprovedAmount(
                request.getApprovedAmount()
        );
        loan.setApprovalDate(LocalDateTime.now());

        loan.setLoanStatus(
                LoanStatus.APPROVED
        );
        
        

        loan.setApprovalDate(
                LocalDateTime.now()
        );

        loanRepository.save(loan);
        
        

        LoanApproval approval =
                LoanApproval.builder()
                        .loan(loan)
                        .decision(
                                ApprovalDecision.APPROVED
                        )
                        .remarks(
                                request.getRemarks()
                        )
                        .approvalDate(
                                LocalDateTime.now()
                        )
                        .build();

        loanApprovalRepository.save(approval);
        notificationService.notifyAllAdmins(
                "Loan Approved",
                "Loan " +
                loan.getId() +
                " has been approved."
        );
        if (loan != null && loan.getCreatedBy() != null) {
            notificationService.createNotificationForUser(
                "Loan Approved",
                "Congratulations! Your loan application has been approved.",
                loan.getCreatedBy()
            );
        }
        
        auditLogService.log(
                "SYSTEM",
                "APPROVE_LOAN",
                "LOAN",
                loan.getId().toString()
        );
    }

    @Override
    public void rejectLoan(
            UUID loanId,
            RejectLoanRequest request
    ) {

        Loan loan =
                loanRepository.findById(loanId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Loan not found"
                                )
                        );

        loan.setLoanStatus(
                LoanStatus.REJECTED
        );

        loanRepository.save(loan);

        LoanApproval approval =
                LoanApproval.builder()
                        .loan(loan)
                        .decision(
                                ApprovalDecision.REJECTED
                        )
                        .remarks(
                                request.getRemarks()
                        )
                        .approvalDate(
                                LocalDateTime.now()
                        )
                        .build();

        loanApprovalRepository.save(approval);
        notificationService.notifyAllAdmins(
                "Loan Rejected",
                "Loan " +
                loan.getId() +
                " has been rejected."
        );
        if (loan != null && loan.getCreatedBy() != null) {
            notificationService.createNotificationForUser(
                "Loan Rejected",
                "Unfortunately, your loan application has been rejected.",
                loan.getCreatedBy()
            );
        }
        
        auditLogService.log(
                "SYSTEM",
                "REJECT_LOAN",
                "LOAN",
                loan.getId().toString()
        );
    }

    @Override
    public void resubmitLoan(
            UUID loanId,
            SubmitLoanRequest request
    ) {

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Loan not found"
                        )
                );

        /*
         * Resubmission is only valid for
         * previously rejected loans.
         */
        if (loan.getLoanStatus() != LoanStatus.REJECTED) {

            throw new RuntimeException(
                    "Only rejected loans can be resubmitted"
            );
        }

        loan.setLoanStatus(
                LoanStatus.PENDING_APPROVAL
        );

        loanRepository.save(loan);

        LoanApproval approval =
                LoanApproval.builder()
                        .loan(loan)
                        .decision(
                                ApprovalDecision.RESUBMITTED
                        )
                        .remarks(
                                request != null
                                        ? request.getRemarks()
                                        : null
                        )
                        .approvalDate(
                                LocalDateTime.now()
                        )
                        .build();

        loanApprovalRepository.save(approval);

        notificationService.notifyAllAdmins(
                "Loan Resubmitted for Approval",
                "Loan " +
                        loan.getId() +
                        " has been resubmitted for approval."
        );
        if (loan != null && loan.getCreatedBy() != null) {
            notificationService.createNotificationForUser(
                "Loan Resubmitted",
                "Your loan application has been resubmitted for approval.",
                loan.getCreatedBy()
            );
        }

        auditLogService.log(
                "SYSTEM",
                "RESUBMIT_LOAN_FOR_APPROVAL",
                "LOAN",
                loan.getId().toString()
        );
    }

    @Override
    public List<LoanApprovalResponse>
    getApprovalHistory(
            UUID loanId
    ) {

        return loanApprovalRepository
                .findByLoanIdOrderByApprovalDateAsc(
                        loanId
                )
                .stream()
                .map(
                        loanApprovalMapper::toResponse
                )
                .toList();
    }
}
