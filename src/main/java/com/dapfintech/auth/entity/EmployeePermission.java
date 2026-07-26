package com.dapfintech.auth.entity;

import java.util.UUID;

import com.dapfintech.common.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "employee_permissions",uniqueConstraints = {@UniqueConstraint(columnNames = {"employee_id","permission_id"})})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePermission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id",nullable = false)
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id",nullable = false)
    private Permission permission;

    @Column(nullable = false)
    private Boolean allowed;

}