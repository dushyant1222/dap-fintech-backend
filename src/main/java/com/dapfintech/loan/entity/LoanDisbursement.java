package com.dapfintech.loan.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.dapfintech.common.base.BaseEntity;
import com.dapfintech.loan.enums.DisbursementMode;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loan_disbursements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDisbursement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @Enumerated(EnumType.STRING)
    @Column(name = "disbursement_mode")
    private DisbursementMode disbursementMode;

    @Column(name = "approved_amount")
    private BigDecimal approvedAmount;

    @Column(name = "total_charges")
    private BigDecimal totalCharges;

    @Column(name = "net_disbursed_amount")
    private BigDecimal netDisbursedAmount;

    @Column(name = "transaction_reference")
    private String transactionReference;

    private String remarks;

    @Column(name = "disbursement_date")
    private LocalDateTime disbursementDate;
}