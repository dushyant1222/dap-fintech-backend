package com.dapfintech.loan.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.dapfintech.common.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loan_closures")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanClosure extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "loan_id",
            unique = true,
            nullable = false
    )
    private Loan loan;

    @Column(
            name = "closure_date",
            nullable = false
    )
    private LocalDateTime closureDate;

    @Column(name = "remarks")
    private String remarks;
}
