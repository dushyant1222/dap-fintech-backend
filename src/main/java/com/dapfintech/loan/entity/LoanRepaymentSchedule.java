package com.dapfintech.loan.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.dapfintech.common.base.BaseEntity;
import com.dapfintech.loan.enums.RepaymentStatus;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loan_repayment_schedules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRepaymentSchedule
        extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @Column(name = "installment_number")
    private Integer installmentNumber;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "principal_amount")
    private BigDecimal principalAmount;

    @Column(name = "interest_amount")
    private BigDecimal interestAmount;

    @Column(name = "installment_amount")
    private BigDecimal installmentAmount;

    @Column(name = "paid_amount")
    private BigDecimal paidAmount;
    
    @Column(name="due_amount")
    private BigDecimal dueAmount;

    @Column(name = "outstanding_amount")
    private BigDecimal outstandingAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "repayment_status")
    private RepaymentStatus repaymentStatus;
}