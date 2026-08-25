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
@Table(name = "day_book")
public class DayBook extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "opening_balance")
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "collections")
    private BigDecimal collections = BigDecimal.ZERO;

    @Column(name = "incoming_transfers")
    private BigDecimal incomingTransfers = BigDecimal.ZERO;

    @Column(name = "spends")
    private BigDecimal spends = BigDecimal.ZERO;

    @Column(name = "loans_disbursed")
    private BigDecimal loansDisbursed = BigDecimal.ZERO;

    @Column(name = "outgoing_transfers")
    private BigDecimal outgoingTransfers = BigDecimal.ZERO;

    @Column(name = "office_remittance")
    private BigDecimal officeRemittance = BigDecimal.ZERO;

    @Column(name = "cash_incoming_transfers")
    private BigDecimal cashIncomingTransfers = BigDecimal.ZERO;

    @Column(name = "cash_outgoing_transfers")
    private BigDecimal cashOutgoingTransfers = BigDecimal.ZERO;

    @Column(name = "closing_balance")
    private BigDecimal closingBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DayBookStatus status = DayBookStatus.OPEN;
}
