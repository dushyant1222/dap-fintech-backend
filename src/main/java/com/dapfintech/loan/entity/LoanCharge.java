package com.dapfintech.loan.entity;

import java.math.BigDecimal;
import java.util.UUID;

import com.dapfintech.common.base.BaseEntity;
import com.dapfintech.loan.enums.ChargeType;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loan_charges")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanCharge extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type")
    private ChargeType chargeType;

    @Column(name = "charge_amount")
    private BigDecimal chargeAmount;

    @Column(name = "is_mandatory")
    private Boolean isMandatory;
}
