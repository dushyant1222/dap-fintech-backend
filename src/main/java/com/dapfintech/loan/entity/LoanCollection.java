package com.dapfintech.loan.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.dapfintech.auth.entity.User;
import com.dapfintech.common.base.BaseEntity;
import com.dapfintech.loan.enums.CollectionMode;
import com.dapfintech.loan.enums.CollectionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "loan_collections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanCollection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @ManyToOne
    @JoinColumn(name = "repayment_schedule_id")
    private LoanRepaymentSchedule repaymentSchedule;

    @Column(name = "receipt_number")
    private String receiptNumber;

    @Column(name = "collected_amount")
    private BigDecimal collectedAmount;

    @Column(name = "collection_date")
    private LocalDateTime collectionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "collection_mode")
    private CollectionMode collectionMode;
    
    @ManyToOne
    @JoinColumn(name = "collected_by")
    private User collectedBy;
    

    @Column(name = "latitude")
    private BigDecimal latitude;

    @Column(name = "longitude")
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "collection_status")
    private CollectionStatus collectionStatus;

    private String remarks;
}
