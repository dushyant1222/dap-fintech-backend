package com.dapfintech.notification.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "title")
    private String title;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "is_read")
    private Boolean isRead;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Navigation type: LOAN, CUSTOMER, ENQUIRY, COLLECTION, EMPLOYEE, GENERAL
    @Column(name = "navigation_type", length = 50)
    private String navigationType;

    // UUID of the related entity (loanId, customerId, enquiryId, etc.)
    @Column(name = "reference_id")
    private UUID referenceId;
}
