package com.dapfintech.employee.entity;

import com.dapfintech.common.base.BaseEntity;
import com.dapfintech.employee.enums.DayBookStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "market_day_book")
public class MarketDayBook extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "market_id", nullable = false)
    private UUID marketId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "total_opening_balance")
    private BigDecimal totalOpeningBalance = BigDecimal.ZERO;

    @Column(name = "total_collections")
    private BigDecimal totalCollections = BigDecimal.ZERO;

    @Column(name = "total_incoming_transfers")
    private BigDecimal totalIncomingTransfers = BigDecimal.ZERO;

    @Column(name = "total_spends")
    private BigDecimal totalSpends = BigDecimal.ZERO;

    @Column(name = "total_loans_disbursed")
    private BigDecimal totalLoansDisbursed = BigDecimal.ZERO;

    @Column(name = "total_outgoing_transfers")
    private BigDecimal totalOutgoingTransfers = BigDecimal.ZERO;

    @Column(name = "total_office_remittance")
    private BigDecimal totalOfficeRemittance = BigDecimal.ZERO;

    @Column(name = "total_closing_balance")
    private BigDecimal totalClosingBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DayBookStatus status = DayBookStatus.OPEN;
}
