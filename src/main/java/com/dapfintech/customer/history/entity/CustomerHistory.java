package com.dapfintech.customer.history.entity;

import java.util.UUID;

import com.dapfintech.auth.entity.User;
import com.dapfintech.common.base.BaseEntity;
import com.dapfintech.customer.entity.Customer;
import com.dapfintech.customer.history.enums.CustomerHistoryAction;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "customer_history",
        indexes = {
                @Index(
                        name = "idx_customer_history_customer_id",
                        columnList = "customer_id"
                ),
                @Index(
                        name = "idx_customer_history_action",
                        columnList = "action"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false
    )
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "action",
            nullable = false,
            length = 50
    )
    private CustomerHistoryAction action;

    @Column(
            name = "title",
            nullable = false,
            length = 150
    )
    private String title;

    @Column(
            name = "description",
            columnDefinition = "TEXT"
    )
    private String description;

    @Column(
            name = "old_value",
            columnDefinition = "TEXT"
    )
    private String oldValue;

    @Column(
            name = "new_value",
            columnDefinition = "TEXT"
    )
    private String newValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;
}