package com.dapfintech.employee.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "daybook_transactions")
@Data
public class DayBookTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(nullable = false)
    private String type;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    private String remarks;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daybook_id")
    private DayBook dayBook;
}
